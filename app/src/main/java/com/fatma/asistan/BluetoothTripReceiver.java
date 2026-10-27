package com.fatma.asistan;

import android.Manifest;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

public class BluetoothTripReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) && !BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) return;
        if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) return;
        String trusted = context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getString("trusted_bluetooth", "");
        if (trusted.isEmpty() || !trusted.equals(device.getAddress())) return;
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        Intent service = new Intent(context, TripService.class);
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            service.setAction(TripService.START);
            service.putExtra("device", device.getName() == null ? "Bluetooth" : device.getName());
            try { context.startForegroundService(service); }
            catch (RuntimeException e) { /* Background start may be blocked by Android */ }
        } else {
            service.setAction(TripService.STOP);
            try { context.startService(service); } catch (RuntimeException ignored) {}
        }
    }
}
