package com.fatma.asistan;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TripService extends Service implements LocationListener {
    public static final String START = "com.fatma.asistan.TRIP_START";
    public static final String STOP = "com.fatma.asistan.TRIP_STOP";
    private LocationManager manager;
    private Location previous;
    private long startMs, movingMs, lastSampleMs;
    private double meters, maxKmh;
    private StringBuilder track = new StringBuilder();
    private String deviceName = "Manuel";
    private boolean active = false;
    private static final String CHANNEL = "trip_tracking";

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Yolculuk takibi", NotificationManager.IMPORTANCE_LOW));
        manager = (LocationManager)getSystemService(LOCATION_SERVICE);
    }
    private Notification notification(String status) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Fatma • Yolculuk kaydı").setContentText(status)
            .setContentIntent(pi).setOngoing(true).build();
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) { finishTrip(); stopSelf(); return START_NOT_STICKY; }
        if (active) return START_STICKY;
        deviceName = intent == null ? "Manuel" : intent.getStringExtra("device");
        if (deviceName == null) deviceName = "Manuel";
        startForeground(201, notification("GPS ile rota kaydediliyor"));
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return START_NOT_STICKY;
        }
        startMs = System.currentTimeMillis(); lastSampleMs = startMs;
        active = true;
        getSharedPreferences("trip_state", MODE_PRIVATE).edit().putBoolean("active", true).apply();
        try { manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 5, this, getMainLooper()); }
        catch (Exception e) { finishTrip(); stopSelf(); }
        return START_STICKY;
    }
    @Override public void onLocationChanged(Location location) {
        if (!active || location == null || location.getAccuracy() > 70) return;
        long now = System.currentTimeMillis();
        if (previous != null) {
            float distance = previous.distanceTo(location);
            long delta = now - lastSampleMs;
            if (delta > 0 && distance > 2 && distance < 300 && distance * 3600.0 / delta < 180) {
                meters += distance;
                movingMs += delta;
            }
        }
        double speed = Math.max(0, location.hasSpeed() ? location.getSpeed() * 3.6 : 0);
        if (speed < 180) maxKmh = Math.max(maxKmh, speed);
        if (track.length() < 120000) {
            if (track.length() > 0) track.append(";");
            track.append(String.format(Locale.US, "%.5f,%.5f", location.getLatitude(), location.getLongitude()));
        }
        previous = location; lastSampleMs = now;
    }
    private void finishTrip() {
        if (!active) return;
        active = false;
        try { manager.removeUpdates(this); } catch (Exception ignored) {}
        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed >= 15000 && previous != null) {
            android.content.SharedPreferences p = getSharedPreferences("trips", MODE_PRIVATE);
            int next = p.getInt("next", 0) + 1;
            String date = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("tr","TR")).format(new Date(startMs));
            double avg = elapsed > 0 ? (meters / 1000.0) / (elapsed / 3600000.0) : 0;
            String record = date + "|" + deviceName.replace("|", " ") + "|" + Math.round(meters) + "|" + elapsed + "|" +
                String.format(Locale.US, "%.1f", avg) + "|" + String.format(Locale.US, "%.1f", maxKmh) + "|" + track.toString();
            p.edit().putInt("next", next).putString("trip_" + next, record).apply();
        }
        getSharedPreferences("trip_state", MODE_PRIVATE).edit().putBoolean("active", false).apply();
    }
    @Override public void onDestroy() { finishTrip(); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }
}
