package com.fatma.asistan;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.graphics.Color;
import android.view.KeyEvent;
import android.media.AudioManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.widget.*;
import android.view.Gravity;
import java.util.*;

public class MainActivity extends Activity {
    private TextToSpeech tts;
    private boolean voiceReady = false;
    private LinearLayout layout;
    private static final int VOICE_REQUEST = 100;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("tr","TR"));
                applyVoice();
                voiceReady = true;
            }
        });
        showHome();
    }
    private void applyVoice() {
        String preferred = getPreferences(MODE_PRIVATE).getString("voice", "tr-TR-language");
        if (tts.getVoices() != null) for (Voice v : tts.getVoices()) {
            if (preferred.equals(v.getName())) { tts.setVoice(v); break; }
        }
        tts.setPitch(1.0f);
        tts.setSpeechRate(0.95f);
    }
    private void say(String s) {
        if (voiceReady) tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "fatma");
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }
    private void button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(17);
        layout.addView(b, new LinearLayout.LayoutParams(-1,-2));
        b.setOnClickListener(v -> action.run());
    }
    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(13,13,17));
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25,35,25,25);
        scroll.addView(layout);
        setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("FATMA ASİSTAN\nSes • Harita • Müzik");
        title.setTextColor(Color.rgb(255,111,35));
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,0,0,30);
        layout.addView(title);
        button("🎤 Fatma, beni dinle", this::listen);
        button("📍 Konumumu haritada göster", this::showLocation);
        button("🗺️ Yol tarifi al", this::askDestination);
        button("▶️ Müziği aç / oynat", () -> media(KeyEvent.KEYCODE_MEDIA_PLAY));
        button("⏸️ Müziği duraklat", () -> media(KeyEvent.KEYCODE_MEDIA_PAUSE));
        button("⏭️ Sonraki şarkı", () -> media(KeyEvent.KEYCODE_MEDIA_NEXT));
        button("⏮️ Önceki şarkı", () -> media(KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        button("🎵 YouTube Music'te şarkı ara", this::askMusic);
        button("🔊 Kadın sesi", () -> changeVoice("tr-TR-language"));
        button("🔉 Erkek sesi", () -> changeVoice("tr-tr-x-ama-local"));
        button("🗣️ Sesi dene", () -> say("Merhaba, ben Fatma. Seni dinliyorum."));
        TextView note = new TextView(this);
        note.setText("Sesli komut için mikrofon düğmesine bas. Harita ve müzik uygulamaları telefonunda yüklü olmalıdır. Bu deneme sürümünde sürekli arka plan dinlemesi ve GPS yolculuk kaydı yoktur.");
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(14);
        note.setPadding(0,20,0,0);
        layout.addView(note);
    }
    private void changeVoice(String name) {
        getPreferences(MODE_PRIVATE).edit().putString("voice",name).apply();
        applyVoice();
        say("Merhaba, ben Fatma.");
    }
    private void listen() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Fatma seni dinliyor");
        try { startActivityForResult(i, VOICE_REQUEST); }
        catch (ActivityNotFoundException e) { say("Telefonunda ses tanıma hizmeti bulunamadı."); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data);
        if (request != VOICE_REQUEST || result != RESULT_OK || data == null) return;
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) return;
        String phrase = results.get(0).toLowerCase(new Locale("tr","TR")).trim();
        if (phrase.contains("konum") || phrase.contains("neredeyim")) { say("Konumunu haritada açıyorum."); showLocation(); }
        else if (phrase.contains("sonraki") || phrase.contains("şarkı değiştir")) { media(KeyEvent.KEYCODE_MEDIA_NEXT); say("Sonraki şarkı."); }
        else if (phrase.contains("önceki")) { media(KeyEvent.KEYCODE_MEDIA_PREVIOUS); say("Önceki şarkı."); }
        else if (phrase.contains("durdur") || phrase.contains("duraklat")) { media(KeyEvent.KEYCODE_MEDIA_PAUSE); say("Müziği duraklatıyorum."); }
        else if (phrase.equals("müzik aç") || phrase.equals("müziği aç") || phrase.equals("devam et") || phrase.equals("müzik çal")) { media(KeyEvent.KEYCODE_MEDIA_PLAY); say("Müziği başlatıyorum."); }
        else if (phrase.contains("şarkısını aç") || phrase.contains("şarkıyı aç") || phrase.contains("şarkı ara") || phrase.contains("şarkısını çal") || phrase.contains("müziğini aç") || phrase.contains(" çal") || phrase.contains("şarkı aç") || phrase.contains("müzik aç")) {
            String q = phrase.replace("şarkısını aç","").replace("şarkısını çal","").replace("şarkıyı aç","").replace("şarkı ara","").replace("müziğini aç","").replace("çal","").replace("aç","").replace("fatma","").replace("lütfen","").trim();
            if (q.isEmpty()) askMusic(); else searchMusic(q);
        }
        else if (phrase.contains("yol tarifi") || phrase.contains("navigasyon") || phrase.contains("gitmek istiyorum") || phrase.contains("götür") || phrase.contains("rota oluştur") || phrase.contains("yol göster") || phrase.contains("haritayı aç")) {
            String dest = phrase.replace("yol tarifi","").replace("navigasyon","").replace("gitmek istiyorum","").replace("götür","").replace("rota oluştur","").replace("yol göster","").replace("haritayı aç","").replace("fatma","").replace("beni","").replace("lütfen","").replace("için","").trim();
            if (dest.isEmpty()) askDestination(); else navigate(dest);
        }
        else say("Bu komutu henüz bilmiyorum: " + phrase);
    }
    private void media(int code) {
        AudioManager audio = (AudioManager)getSystemService(AUDIO_SERVICE);
        if (audio != null) {
            audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));
            audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));
        }
    }
    private void open(Intent i) {
        try { startActivity(i); }
        catch (Exception e) { say("Bu işlemi açacak uygulama bulunamadı."); }
    }
    private void showLocation() {
        Uri uri = Uri.parse("geo:0,0");
        Intent i = new Intent(Intent.ACTION_VIEW,uri);
        i.setPackage("com.google.android.apps.maps");
        try { startActivity(i); }
        catch (Exception e) { open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/@?api=1&map_action=map"))); }
    }
    private void navigate(String dest) {
        Uri uri = Uri.parse("google.navigation:q=" + Uri.encode(dest));
        Intent i = new Intent(Intent.ACTION_VIEW,uri);
        i.setPackage("com.google.android.apps.maps");
        try { startActivity(i); }
        catch (Exception e) { open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(dest)))); }
    }
    private void askDestination() {
        EditText field = new EditText(this);
        field.setHint("Nereye gidelim?");
        new android.app.AlertDialog.Builder(this).setTitle("Yol tarifi")
            .setView(field).setPositiveButton("Haritayı aç",(d,w)-> {
                String s = field.getText().toString().trim();
                if (!s.isEmpty()) navigate(s);
            }).setNegativeButton("Vazgeç",null).show();
    }
    private void searchMusic(String q) {
        Intent music = new Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q="+Uri.encode(q)));
        music.setPackage("com.google.android.apps.youtube.music");
        try { startActivity(music); } catch (Exception e) { open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://music.youtube.com/search?q="+Uri.encode(q)))); }
        say("Şarkıyı YouTube Music üzerinde arıyorum. Otomatik oynatma henüz desteklenmiyor.");
    }
    private void askMusic() {
        EditText field = new EditText(this);
        field.setHint("Şarkı veya sanatçı adı");
        new android.app.AlertDialog.Builder(this).setTitle("Müzik ara")
            .setView(field).setPositiveButton("Ara",(d,w)-> {
                String s = field.getText().toString().trim();
                if (!s.isEmpty()) searchMusic(s);
            }).setNegativeButton("Vazgeç",null).show();
    }
    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
