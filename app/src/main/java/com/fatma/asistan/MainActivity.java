package com.fatma.asistan;

import android.app.Activity;
import android.net.Uri;
import android.media.AudioManager;
import android.content.ActivityNotFoundException;
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.List;
import java.util.Collections;
import android.view.KeyEvent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.widget.SeekBar;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private LinearLayout content;
    private String assistantName;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String voicePreference = "Kadın";
    private String selectedVoiceName = "";
    private String lastDestination = "";
    private float speechRate = 0.95f;
    private int batteryThreshold = 20;
    private int batteryPercent = -1;
    private int lastBatteryPercent = -1;
    private TextView batteryLabel;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level < 0 || scale <= 0) return;
            int percent = Math.round(100f * level / scale);
            batteryPercent = percent;
            if (batteryLabel != null) batteryLabel.setText("Telefon pili: %" + percent);
            if (lastBatteryPercent > batteryThreshold && percent <= batteryThreshold) {
                speak("Dikkat! Telefonunun şarjı yüzde " + percent + " seviyesine düştü.");
            }
            lastBatteryPercent = percent;
        }
    };
    private static final int SPEECH_REQUEST = 100;
    private static final int BG = Color.rgb(15, 19, 29);
    private static final int PANEL = Color.rgb(31, 39, 55);
    private static final int ACCENT = Color.rgb(49, 211, 170);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        assistantName = getPreferences(MODE_PRIVATE).getString("assistant_name", "Fatma");
        voicePreference = getPreferences(MODE_PRIVATE).getString("voice_preference", "Kadın");
        speechRate = getPreferences(MODE_PRIVATE).getFloat("speech_rate", 0.95f);
        selectedVoiceName = getPreferences(MODE_PRIVATE).getString("selected_voice", "");
        lastDestination = getPreferences(MODE_PRIVATE).getString("last_destination", "");
        batteryThreshold = getPreferences(MODE_PRIVATE).getInt("battery_threshold", 20);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        tts = new TextToSpeech(this, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady) {
                int result = tts.setLanguage(new Locale("tr", "TR"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsReady = false;
                    runOnUiThread(() -> Toast.makeText(this, "Türkçe ses paketi bulunamadı", Toast.LENGTH_LONG).show());
                } else { applyVoice(); }
            }
        });
        showHome();
    }
    private void applyVoice() {
        if (!ttsReady) return;
        Voice selected = null;
        if (!selectedVoiceName.isEmpty()) {
            for (Voice v : tts.getVoices() == null ? Collections.<Voice>emptySet() : tts.getVoices()) {
                if (v.getName().equals(selectedVoiceName) && "tr".equals(v.getLocale().getLanguage())) { selected = v; break; }
            }
        }
        if (selected == null) {
        for (Voice v : tts.getVoices() == null ? java.util.Collections.<Voice>emptySet() : tts.getVoices()) {
            if (!"tr".equals(v.getLocale().getLanguage())) continue;
            String n = v.getName().toLowerCase(Locale.ROOT);
            boolean male = n.contains("male") && !n.contains("female");
            boolean female = n.contains("female");
            if ((voicePreference.equals("Erkek") && male) || (voicePreference.equals("Kadın") && female)) { selected = v; break; }
            if (selected == null) selected = v;
        }
        }
        if (selected != null) tts.setVoice(selected);
        tts.setSpeechRate(speechRate);
    }
    private void speak(String words) {
        if (ttsReady) tts.speak(words, TextToSpeech.QUEUE_FLUSH, null, "fatma_reply");
        else Toast.makeText(this, "Ses motoru henüz hazır değil", Toast.LENGTH_SHORT).show();
    }
    private void listen() {
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, assistantName + " seni dinliyor");
            startActivityForResult(i, SPEECH_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Telefonunda ses tanıma hizmeti bulunamadı", Toast.LENGTH_LONG).show();
        }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK && data != null) {
            ArrayList<String> words = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (words != null && !words.isEmpty()) {
                String phrase = words.get(0).toLowerCase(new Locale("tr", "TR"));
                String reply;
                if (phrase.contains("benzin") || phrase.contains("akaryakıt") || phrase.contains("petrol istasyonu")) {
                    openMapSearch("benzin istasyonu"); reply = "Yakındaki benzin istasyonlarını haritada açıyorum.";
                } else if (phrase.contains("dinlenme") || phrase.contains("mola yeri")) {
                    openMapSearch("dinlenme tesisi"); reply = "Dinlenme tesislerini haritada arıyorum.";
                } else if (phrase.contains("gezilecek yer") || phrase.contains("turistik yer")) {
                    openMapSearch("turistik yerler"); reply = "Gezilecek yerleri haritada arıyorum.";
                } else if (phrase.contains("devam edelim") && !lastDestination.isEmpty()) {
                    openDirections(lastDestination); reply = "Son rotanı açıyorum: " + lastDestination;
                } else if (phrase.contains("yol tarifi") || phrase.contains("rota") || phrase.contains("navigasyon") || phrase.contains("götür") || phrase.contains("gitmek istiyorum") || phrase.contains("harita aç") || phrase.contains("haritayı aç")) {
                    String destination = phrase.replace(assistantName.toLowerCase(new Locale("tr","TR")), "")
                        .replace("fatma", "").replace("zilli", "").replace("bana", "")
                        .replace("yol tarifi", "").replace("rota oluştur", "").replace("rota aç", "")
                        .replace("navigasyon başlat", "").replace("navigasyon aç", "")
                        .replace("haritayı aç", "").replace("harita aç", "")
                        .replace("götür", "").replace("gitmek istiyorum", "")
                        .replace("rotayı", "").replace("rota", "").replace("navigasyon", "")
                        .replace("başlat", "").replace("aç", "").replace("ver", "")
                        .replace("için", "").replace("lütfen", "").trim();
                    destination = destination.replaceAll("(?iu)[\u0027’](ya|ye|a|e)$", "").trim();
                    if (destination.isEmpty()) {
                        showNavigation();
                        reply = "Navigasyon ekranını açtım. Hedef söyleyebilirsin: Antalya'ya götür.";
                    } else {
                        openDirections(destination);
                        reply = destination + " için yol tarifi açıyorum.";
                    }
                } else if (phrase.contains("sonraki şarkı") || phrase.contains("şarkıyı geç") || phrase.contains("sonraki müzik")) {
                    mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT); reply = "Sonraki şarkıya geçiyorum.";
                } else if (phrase.contains("önceki şarkı") || phrase.contains("önceki müzik")) {
                    mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS); reply = "Önceki şarkıya dönüyorum.";
                } else if (phrase.contains("müziği durdur") || phrase.contains("müziği duraklat") || phrase.contains("müzik durdur") || phrase.contains("müzik kapat")) {
                    mediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE); reply = "Müziği duraklatıyorum.";
                } else if (phrase.contains("müziği aç") || phrase.contains("müziği devam ettir") || phrase.contains("müzik aç") || phrase.contains("müzik başlat") || phrase.contains("şarkı aç") || phrase.contains("şarkı çal")) {
                    mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY); reply = "Müziği devam ettiriyorum.";
                } else if (phrase.contains("devam edelim") && !lastDestination.isEmpty()) {
                    openDirections(lastDestination); reply = "Son rotanı açıyorum: " + lastDestination;
                } else if (phrase.contains("müzik ara") || phrase.contains("şarkı ara") || phrase.contains("şarkısını aç")) {
                    String q = phrase.replace("müzik ara", "").replace("şarkı ara", "").replace("şarkısını aç", "").trim();
                    if (q.isEmpty()) reply = "Aramak istediğin şarkıyı söyle.";
                    else { searchMusic(q); reply = q + " için YouTube Music araması açıyorum."; }
                } else if (phrase.contains("sesi yükselt") || phrase.contains("ses aç")) {
                    adjustVolume(true); reply = "Medya sesini yükseltiyorum.";
                } else if (phrase.contains("sesi kıs") || phrase.contains("ses azalt")) {
                    adjustVolume(false); reply = "Medya sesini kısıyorum.";
                } else if (phrase.contains("acil arama")) {
                    openDialer("112"); reply = "112 arama ekranını açıyorum. Aramayı sen başlatmalısın.";
                } else if (phrase.contains("şarjım kaç") || phrase.contains("pilim kaç")) {
                    reply = batteryPercent < 0 ? "Pil seviyesini okuyamadım." : "Telefonunun şarjı yüzde " + batteryPercent + ".";
                } else if (phrase.contains("saat kaç") || phrase.contains("saati söyle")) {
                    reply = "Saat " + new SimpleDateFormat("HH:mm", new Locale("tr", "TR")).format(Calendar.getInstance().getTime());
                } else if (phrase.contains("adın ne") || phrase.contains("ismin ne")) {
                    reply = "Benim adım " + assistantName;
                } else if (phrase.contains("merhaba") || phrase.contains("selam")) {
                    reply = "Merhaba! Ben " + assistantName + ". Sana nasıl yardımcı olabilirim?";
                } else if (phrase.contains("nasılsın")) {
                    reply = "İyiyim, teşekkür ederim. Sen nasılsın?";
                } else {
                    reply = "Seni duydum: " + words.get(0) + ". Bu komutu henüz öğrenmedim.";
                }
                saveHistory("🎙 " + words.get(0) + " → " + reply);
                Toast.makeText(this, reply, Toast.LENGTH_LONG).show();
                speak(reply);
            }
        }
    }
    @Override protected void onDestroy() {
        unregisterReceiver(batteryReceiver);
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
    private GradientDrawable shape(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(18));
        return d;
    }
    private int dp(int n) { return (int)(getResources().getDisplayMetrics().density * n); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(8), 0, dp(8));
        return t;
    }
    private void page(String title) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(38), dp(24), dp(24));
        scroll.addView(content);
        setContentView(scroll);
        content.addView(text(title, 30, Color.WHITE, true));
    }
    private void addCard(String heading, String detail, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(13), dp(18), dp(13));
        card.setBackground(shape(PANEL));
        card.addView(text(heading, 19, Color.WHITE, true));
        card.addView(text(detail, 14, Color.rgb(190, 201, 214), false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(15);
        content.addView(card, lp);
        if (action != null) { card.setClickable(true); card.setOnClickListener(v -> action.run()); }
    }
    private void showHome() {
        page("Merhaba!");
        content.addView(text("Ben " + assistantName + ".", 26, ACCENT, true));
        content.addView(text("Kişisel sürüş asistanın geliştirme aşamasında.", 16, Color.LTGRAY, false));
        batteryLabel = text("Telefon pili: " + (batteryPercent < 0 ? "ölçülüyor" : "%" + batteryPercent), 18, ACCENT, true);
        content.addView(batteryLabel);
        addCard("Asistan ayarları", "Asistanın ismini değiştir.", this::showSettings);
        addCard("Bluetooth", "Telefonun Bluetooth ayarlarını aç.", () -> {
            try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
            catch (Exception e) { Toast.makeText(this, "Bluetooth ayarları açılamadı", Toast.LENGTH_SHORT).show(); }
        });
        addCard("🎙️ Beni dinle", "Türkçe sesli komut vermek için dokun.", this::listen);
        addCard("🔊 Sesimi dene", "Asistanın Türkçe konuşmasını dinle.", () -> speak("Merhaba, ben " + assistantName + ". Sana nasıl yardımcı olabilirim?"));
        addCard("🧭 Navigasyon", "Hedef belirle, yol tarifi ve benzin istasyonu ara.", this::showNavigation);
        addCard("🎵 Müzik kontrolü", "Oynat, durdur, sonraki ve önceki şarkı.", this::showMusic);
        addCard("🚨 Acil durum", "112 arama ekranı ve konum seçenekleri.", this::showEmergency);
        addCard("📜 Geçmiş", "Sesli komut ve rota kayıtları.", this::showHistory);
        addCard("Kaza algılama", "Henüz aktif değil; acil durum sistemi olarak kullanmayın.", this::comingSoon);
        content.addView(text("Sürüm 0.5 - Sesler, müzik arama, geçmiş ve acil durum", 13, Color.GRAY, false));
    }
    private void launchMap(Uri uri) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            i.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Harita açılamadı: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }
    private void openDirections(String destination) {
        String clean = destination.trim();
        if (clean.isEmpty()) return;
        lastDestination = clean;
        getPreferences(MODE_PRIVATE).edit().putString("last_destination", clean).apply();
        saveHistory("🧭 Rota: " + clean);
        // Android geo navigation intent first; Google Maps URL is a browser-compatible fallback.
        Uri navigation = Uri.parse("google.navigation:q=" + Uri.encode(clean));
        Intent direct = new Intent(Intent.ACTION_VIEW, navigation);
        direct.setPackage("com.google.android.apps.maps");
        try {
            startActivity(direct);
        } catch (Exception e) {
            launchMap(Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(clean) + "&travelmode=driving"));
        }
    }
    private void openMapSearch(String query) {
        launchMap(Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)));
    }
    private void mediaKey(int code) {
        AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (manager != null) {
            manager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
            manager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
        }
    }
    private void adjustVolume(boolean up) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am != null) am.adjustStreamVolume(AudioManager.STREAM_MUSIC, up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }
    private void searchMusic(String query) {
        launchMap(Uri.parse("https://music.youtube.com/search?q=" + Uri.encode(query)));
    }
    private void openDialer(String number) {
        try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number))); }
        catch (Exception e) { Toast.makeText(this, "Arama ekranı açılamadı", Toast.LENGTH_LONG).show(); }
    }
    private void saveHistory(String item) {
        SharedPreferences p = getPreferences(MODE_PRIVATE);
        String old = p.getString("history", "");
        String entry = new SimpleDateFormat("dd.MM HH:mm", new Locale("tr","TR")).format(Calendar.getInstance().getTime()) + " " + item;
        String updated = entry + "\n" + old;
        if (updated.length() > 9000) updated = updated.substring(0, 9000);
        p.edit().putString("history", updated).apply();
    }
    private void showHistory() {
        page("Geçmiş");
        content.addView(text(getPreferences(MODE_PRIVATE).getString("history", "Henüz kayıt yok."), 15, Color.WHITE, false));
        addCard("Geçmişi temizle", "Cihazda tutulan kayıtları sil.", () -> {
            new android.app.AlertDialog.Builder(this).setTitle("Geçmiş silinsin mi?")
                .setPositiveButton("Sil", (d,w) -> { getPreferences(MODE_PRIVATE).edit().remove("history").apply(); showHistory(); })
                .setNegativeButton("Vazgeç", null).show();
        });
        addCard("Ana ekrana dön", "", this::showHome);
    }
    private void showEmergency() {
        page("Acil durum");
        content.addView(text("Bu uygulama otomatik kaza algılama veya acil çağrı yapmaz. Acil tehlikede doğrudan 112'yi ara.", 16, Color.LTGRAY, false));
        addCard("📞 112 arama ekranı", "Aramayı sen onaylayıp başlatırsın.", () -> openDialer("112"));
        addCard("📍 Konumumu paylaş", "Harita uygulamasında mevcut konumunu aç; paylaşımı kendin onayla.", () -> launchMap(Uri.parse("geo:0,0?q=My+Location")));
        addCard("Ana ekrana dön", "", this::showHome);
    }
    private void showVoicePicker() {
        page("Türkçe ses seçimi");
        if (!ttsReady || tts.getVoices() == null) {
            content.addView(text("Türkçe ses motoru hazır değil. Telefonun metin okuma ayarlarını kontrol et.", 16, Color.WHITE, false));
        } else {
            ArrayList<Voice> voices = new ArrayList<>();
            for (Voice v : tts.getVoices()) if ("tr".equals(v.getLocale().getLanguage())) voices.add(v);
            Collections.sort(voices, (a,b) -> a.getName().compareTo(b.getName()));
            if (voices.isEmpty()) content.addView(text("Telefonda Türkçe ses bulunamadı.", 16, Color.WHITE, false));
            for (Voice v : voices) {
                String description = v.getName() + (v.isNetworkConnectionRequired() ? " (internet gerekli)" : " (çevrimdışı kullanılabilir)");
                addCard((v.getName().equals(selectedVoiceName) ? "✓ " : "") + description,
                    "Bu sesi seç ve dene. Sesin kadın/erkek niteliği telefondaki ses motoruna bağlıdır.", () -> {
                        selectedVoiceName = v.getName();
                        getPreferences(MODE_PRIVATE).edit().putString("selected_voice", selectedVoiceName).apply();
                        applyVoice(); speak("Merhaba, ben " + assistantName + ". Bu benim sesim.");
                    });
            }
        }
        addCard("Ayarlar ekranına dön", "", this::showSettings);
    }
    private void showNavigation() {
        page("Navigasyon");
        content.addView(text("Hedefini yaz, harita uygulamasında yol tarifini açalım.", 16, Color.LTGRAY, false));
        EditText destination = new EditText(this);
        destination.setSingleLine(true);
        destination.setHint("Örn. Antalya");
        destination.setHintTextColor(Color.GRAY);
        destination.setTextColor(Color.WHITE);
        content.addView(destination);
        addCard("🧭 Yol tarifi başlat", "Seçilen hedef için harita uygulamasını aç.", () -> {
            String target = destination.getText().toString().trim();
            if (target.isEmpty()) destination.setError("Hedef yazmalısın");
            else { Toast.makeText(this, target + " için yol tarifi açılıyor", Toast.LENGTH_SHORT).show(); openDirections(target); }
        });
        addCard("⛽ Benzin istasyonları", "Haritada istasyon ara.", () -> openMapSearch("benzin istasyonu"));
        addCard("☕ Dinlenme tesisleri", "Haritada mola yeri ara.", () -> openMapSearch("dinlenme tesisi"));
        addCard("🔁 Son rotaya dön", lastDestination.isEmpty() ? "Henüz rota yok." : lastDestination, () -> { if (!lastDestination.isEmpty()) openDirections(lastDestination); });
        addCard("🏛️ Gezilecek yerler", "Yakındaki turistik yerleri ara.", () -> openMapSearch("turistik yerler"));
        content.addView(text("Rota yönlendirmesini yüklü harita uygulaması yapar. Uygulama içi navigasyon henüz yok.", 13, Color.LTGRAY, false));
        addCard("Ana ekrana dön", "", this::showHome);
    }
    private void showMusic() {
        page("Müzik kontrolü");
        content.addView(text("Telefonda aktif müzik oynatıcısına temel medya komutları gönderilir.", 16, Color.LTGRAY, false));
        addCard("▶️ Oynat", "Müziği devam ettir.", () -> mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY));
        addCard("⏸️ Duraklat", "Müziği durdur.", () -> mediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE));
        addCard("⏭️ Sonraki", "Sonraki şarkı.", () -> mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT));
        addCard("⏮️ Önceki", "Önceki şarkı.", () -> mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        EditText musicQuery = new EditText(this);
        musicQuery.setSingleLine(true); musicQuery.setHint("Şarkı veya sanatçı ara"); musicQuery.setTextColor(Color.WHITE);
        content.addView(musicQuery);
        addCard("🔎 YouTube Music ara", "Arama sonuçlarını müzik uygulamasında aç.", () -> { String q = musicQuery.getText().toString().trim(); if (!q.isEmpty()) searchMusic(q); else musicQuery.setError("Arama yaz"); });
        addCard("🔊 Sesi yükselt", "", () -> adjustVolume(true));
        addCard("🔉 Sesi kıs", "", () -> adjustVolume(false));
        content.addView(text("Müzik uygulaması komutları desteklemiyorsa çalışmayabilir. Şarkı arama henüz eklenmedi.", 13, Color.LTGRAY, false));
        addCard("Ana ekrana dön", "", this::showHome);
    }
    private void comingSoon() { Toast.makeText(this, "Bu özellik henüz geliştirilmedi", Toast.LENGTH_SHORT).show(); }
    private void showSettings() {
        page("Asistan ayarları");
        content.addView(text("Asistanın adı", 17, Color.WHITE, true));
        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setText(assistantName);
        nameInput.setTextColor(Color.WHITE);
        nameInput.setHintTextColor(Color.GRAY);
        nameInput.setHint("Örn. Fatma");
        content.addView(nameInput);
        Button save = new Button(this);
        save.setText("Kaydet");
        content.addView(save);
        save.setOnClickListener(v -> {
            String value = nameInput.getText().toString().trim();
            if (value.isEmpty()) { nameInput.setError("İsim boş olamaz"); return; }
            if (value.length() > 30) { nameInput.setError("En fazla 30 karakter"); return; }
            assistantName = value;
            getPreferences(MODE_PRIVATE).edit().putString("assistant_name", value).apply();
            Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show();
            showHome();
        });
        content.addView(text("Ses tercihi (cihazdaki Türkçe seslere bağlı)", 17, Color.WHITE, true));
        addCard("Kadın sesi" + (voicePreference.equals("Kadın") ? " ✓" : ""), "Kadın sesi varsa kullan; yoksa varsayılan Türkçe ses.", () -> { setVoicePreference("Kadın"); });
        addCard("Erkek sesi" + (voicePreference.equals("Erkek") ? " ✓" : ""), "Erkek sesi varsa kullan; yoksa varsayılan Türkçe ses.", () -> { setVoicePreference("Erkek"); });
        addCard("🎤 Türkçe sesleri listele ve dene", "Telefondaki tüm Türkçe sesleri tek tek seç.", this::showVoicePicker);
        content.addView(text("Konuşma hızı: " + String.format(Locale.getDefault(), "%.2f", speechRate) + "x", 17, Color.WHITE, true));
        SeekBar speed = new SeekBar(this);
        speed.setMax(150);
        speed.setProgress(Math.round((speechRate - 0.5f) * 100));
        TextView speedLabel = text("Hız: " + String.format(Locale.getDefault(), "%.2f", speechRate) + "x", 15, ACCENT, false);
        content.addView(speedLabel);
        content.addView(speed);
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                speedLabel.setText("Hız: " + String.format(Locale.getDefault(), "%.2f", 0.5f + progress / 100f) + "x");
            }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {
                speechRate = 0.5f + bar.getProgress() / 100f;
                getPreferences(MODE_PRIVATE).edit().putFloat("speech_rate", speechRate).apply();
                if (ttsReady) tts.setSpeechRate(speechRate);
                speak("Yeni konuşma hızım bu şekilde.");
            }
        });
        content.addView(text("Pil uyarı eşiği", 17, Color.WHITE, true));
        for (int threshold : new int[]{10, 20, 30}) {
            addCard("%" + threshold + (batteryThreshold == threshold ? " ✓" : ""), "Pil bu seviyeye düştüğünde sesli uyarı ver.", () -> {
                batteryThreshold = threshold;
                getPreferences(MODE_PRIVATE).edit().putInt("battery_threshold", threshold).apply();
                showSettings();
            });
        }
        content.addView(text("Pil uyarıları uygulama açıkken çalışır. Arka plan izleme sonraki sürümlerde geliştirilecek.", 13, Color.LTGRAY, false));
        addCard("Ana ekrana dön", "", this::showHome);
    }
    private void setVoicePreference(String preference) {
        voicePreference = preference;
        getPreferences(MODE_PRIVATE).edit().putString("voice_preference", preference).apply();
        applyVoice();
        showSettings();
        speak("Merhaba, ben " + assistantName);
    }
    @Override public void onBackPressed() { showHome(); }
}
