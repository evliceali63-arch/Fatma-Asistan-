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
import android.view.Gravity;
import android.widget.GridLayout;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;

public class MainActivity extends Activity {
    private LinearLayout content;
    private String assistantName;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String voicePreference = "Kadın";
    private String selectedVoiceName = "";
    private String lastDestination = "";
    private float speechRate = 0.95f;
    private float speechPitch = 1.0f;
    private String femaleVoiceName = "";
    private String maleVoiceName = "";
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
    private static final int BG = Color.rgb(9, 9, 12);
    private static final int PANEL = Color.rgb(23, 20, 23);
    private static final int ACCENT = Color.rgb(255, 103, 29);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        assistantName = getPreferences(MODE_PRIVATE).getString("assistant_name", "Fatma");
        voicePreference = getPreferences(MODE_PRIVATE).getString("voice_preference", "Kadın");
        speechRate = getPreferences(MODE_PRIVATE).getFloat("speech_rate", 0.95f);
        speechPitch = 1.0f;
        selectedVoiceName = getPreferences(MODE_PRIVATE).getString("selected_voice", "");
        femaleVoiceName = getPreferences(MODE_PRIVATE).getString("female_voice", "tr-TR-language");
        maleVoiceName = getPreferences(MODE_PRIVATE).getString("male_voice", "tr-tr-x-ama-local");
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
        String preferredName = voicePreference.equals("Kadın") ? femaleVoiceName : maleVoiceName;
        if (preferredName.isEmpty()) preferredName = voicePreference.equals("Kadın") ? "tr-TR-language" : "tr-tr-x-ama-local";
        if (!preferredName.isEmpty()) {
            for (Voice v : tts.getVoices() == null ? Collections.<Voice>emptySet() : tts.getVoices()) {
                if (v.getName().equals(preferredName) && "tr".equals(v.getLocale().getLanguage())) { selected = v; break; }
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
        tts.setPitch(speechPitch);
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
                } else if (phrase.contains("çevrimdışı harita") || phrase.contains("haritayı indir") || phrase.contains("rotayı indir")) {
                    if (lastDestination.isEmpty()) { showNavigation(); reply = "Önce hedefini belirle, sonra çevrimdışı harita hazırlayalım."; }
                    else { tripPreparation(lastDestination); reply = "Çevrimdışı harita hazırlığını açtım."; }
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
    private void homeTile(LinearLayout row, String emoji, String title, String detail, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(5), dp(14), dp(5), dp(14));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(34,23,21), Color.rgb(12,13,17)});
        bg.setCornerRadius(dp(17)); bg.setStroke(dp(1), Color.rgb(67,48,42));
        tile.setBackground(bg);
        TextView symbol = text(emoji, 29, ACCENT, true);
        symbol.setGravity(Gravity.CENTER); tile.addView(symbol);
        TextView name = text(title, 15, Color.WHITE, true);
        name.setGravity(Gravity.CENTER); tile.addView(name);
        TextView desc = text(detail, 11, Color.LTGRAY, false);
        desc.setGravity(Gravity.CENTER); tile.addView(desc);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(132), 1);
        lp.setMargins(dp(3), dp(4), dp(3), dp(4));
        row.addView(tile, lp);
        tile.setOnClickListener(v -> action.run());
    }
    private void showBluetooth() {
        page("Bluetooth");
        content.addView(text("Şu anda telefonun Bluetooth ayarlarını açabiliriz. Güvenilir interkom seçimi ve otomatik etkinleşme henüz geliştirilmedi.", 16, Color.LTGRAY, false));
        addCard("🔵 Bluetooth ayarlarını aç", "İnterkomunu telefonla eşleştir.", () -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        addCard("Ana sayfa", "", this::showHome);
    }
    private void showHome() {
        batteryLabel = null;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        GradientDrawable backdrop = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(32,12,9), Color.rgb(8,8,12), Color.rgb(20,9,8)});
        scroll.setBackground(backdrop);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(22), dp(12), dp(24));
        scroll.addView(content);
        setContentView(scroll);

        TextView emblem = text("☠", 78, ACCENT, true);
        emblem.setGravity(Gravity.CENTER);
        content.addView(emblem);
        TextView heading = text(assistantName, 38, Color.WHITE, true);
        heading.setGravity(Gravity.CENTER); content.addView(heading);
        TextView sub = text("Sesli Asistan ve Güvenlik Sistemi", 14, Color.LTGRAY, false);
        sub.setGravity(Gravity.CENTER); content.addView(sub);
        batteryLabel = text("Telefon pili: " + (batteryPercent < 0 ? "ölçülüyor" : "%" + batteryPercent), 14, ACCENT, true);
        batteryLabel.setGravity(Gravity.CENTER); content.addView(batteryLabel);

        TextView mic = text("🎙  BENİ DİNLE", 23, Color.WHITE, true);
        mic.setGravity(Gravity.CENTER);
        mic.setPadding(dp(12), dp(24), dp(12), dp(24));
        GradientDrawable micBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.rgb(42,17,11), Color.rgb(19,17,19)});
        micBg.setCornerRadius(dp(26)); micBg.setStroke(dp(2), ACCENT);
        mic.setBackground(micBg);
        LinearLayout.LayoutParams micLp = new LinearLayout.LayoutParams(-1, -2);
        micLp.setMargins(dp(8), dp(28), dp(8), dp(18));
        content.addView(mic, micLp);
        mic.setOnClickListener(v -> listen());
        TextView hint = text("Sesli komut için dokun • Otomatik dinleme henüz aktif değil", 12, Color.LTGRAY, false);
        hint.setGravity(Gravity.CENTER); content.addView(hint);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL); content.addView(row1);
        homeTile(row1, "📞", "Ara", "Arama ekranı", () -> openDialer(""));
        homeTile(row1, "💬", "WhatsApp", "Uygulamayı aç", () -> {
            Intent i = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
            if (i != null) startActivity(i); else Toast.makeText(this, "WhatsApp yüklü değil", Toast.LENGTH_SHORT).show();
        });
        homeTile(row1, "🎵", "Müzik", "Oynat / Durdur", this::showMusic);
        homeTile(row1, "🧭", "Navigasyon", "Rota oluştur", this::showNavigation);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL); content.addView(row2);
        homeTile(row2, "🏍", "Sürüş", "Hazırlık", this::showDrive);
        homeTile(row2, "🛡", "Uyg. Kilidi", "Geliştiriliyor", this::comingSoon);
        homeTile(row2, "🆘", "Acil Durum", "112 / Konum", this::showEmergency);
        homeTile(row2, "⚙", "Ayarlar", "Kişiselleştir", this::showSettings);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(-1, -2);
        bottomLp.topMargin = dp(22); content.addView(bottom, bottomLp);
        homeTile(bottom, "⌂", "Ana Sayfa", "", this::showHome);
        homeTile(bottom, "🎙", "Komutlar", "", this::listen);
        homeTile(bottom, "🎧", "Bluetooth", "", this::showBluetooth);
        homeTile(bottom, "◷", "Geçmiş", "", this::showHistory);
        content.addView(text("Fatma 0.6 • Önizleme tasarımına yakınlaştırılmış arayüz", 11, Color.GRAY, false));
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
                        if (voicePreference.equals("Kadın")) femaleVoiceName = selectedVoiceName;
                        else maleVoiceName = selectedVoiceName;
                        getPreferences(MODE_PRIVATE).edit().putString("selected_voice", selectedVoiceName)
                            .putString(voicePreference.equals("Kadın") ? "female_voice" : "male_voice", selectedVoiceName).apply();
                        speechPitch = 1.0f;
                        getPreferences(MODE_PRIVATE).edit().putFloat("speech_pitch", speechPitch).apply();
                        applyVoice(); speak("Merhaba, ben " + assistantName + ". Bu benim sesim.");
                        Toast.makeText(this, "Ses seçildi: " + v.getName(), Toast.LENGTH_LONG).show();
                    });
            }
        }
        addCard("Ayarlar ekranına dön", "", this::showSettings);
    }
    private void tripPreparation(String destination) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Yolculuk hazırlığı")
            .setMessage(destination + " rotası için internet kesilmeden çevrimdışı harita hazırlamak ister misin? Haritayı Google Haritalar içinde sen indireceksin.")
            .setPositiveButton("Haritayı hazırla", (dialog, which) -> {
                try {
                    Intent maps = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
                    if (maps != null) startActivity(maps);
                    else launchMap(Uri.parse("https://www.google.com/maps"));
                    Toast.makeText(this, "Google Haritalar > Profil > Çevrimdışı haritalar bölümünden bölgeyi indir.", Toast.LENGTH_LONG).show();
                } catch (Exception e) { Toast.makeText(this, "Haritalar açılamadı", Toast.LENGTH_LONG).show(); }
            })
            .setNegativeButton("Rotaya başla", (dialog, which) -> openDirections(destination))
            .setNeutralButton("Vazgeç", null)
            .show();
    }
    private void showDrive() {
        page("Sürüş modu");
        content.addView(text("Yolculuk öncesi hazırlık. Arka planda otomatik sürüş algılama henüz etkin değil.", 15, Color.LTGRAY, false));
        addCard("🏍 Motosiklet", "Haritalarda sürüş rotası oluştur.", this::showNavigation);
        addCard("🚗 Araba", "Haritalarda sürüş rotası oluştur.", this::showNavigation);
        addCard("🚶 Yürüyüş", "Haritalarda yürüyüş rotasını seç.", this::showNavigation);
        addCard("🗺 Çevrimdışı haritalar", "Haritalar uygulamasında indirme bölümüne git.", () -> {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
                if (i != null) startActivity(i); else launchMap(Uri.parse("https://www.google.com/maps"));
                Toast.makeText(this, "Profil > Çevrimdışı haritalar yolunu izle.", Toast.LENGTH_LONG).show();
            } catch (Exception e) { Toast.makeText(this, "Haritalar açılamadı", Toast.LENGTH_LONG).show(); }
        });
        addCard("🎵 İndirilen müzikleri kontrol et", "YouTube Music içinde indirmelerini denetle.", () -> {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.youtube.music");
                if (i != null) startActivity(i);
                else launchMap(Uri.parse("https://music.youtube.com"));
            } catch (Exception e) { Toast.makeText(this, "YouTube Music açılamadı", Toast.LENGTH_LONG).show(); }
        });
        addCard("Ana sayfa", "", this::showHome);
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
        addCard("🗺 Çevrimdışı rota hazırlığı", "Haritayı indirmek ister misin?", () -> {
            String target = destination.getText().toString().trim();
            if (target.isEmpty()) target = lastDestination;
            if (target.isEmpty()) destination.setError("Önce hedef yaz");
            else tripPreparation(target);
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
        content.addView(text("Müzik uygulaması komutları desteklemiyorsa çalışmayabilir. Şarkı araması YouTube Music üzerinden açılır.", 13, Color.LTGRAY, false));
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
        addCard("Kadın sesi" + (voicePreference.equals("Kadın") ? " ✓" : ""), femaleVoiceName.isEmpty() ? "Önce Türkçe sesler listesinden beğendiğin sesi seç." : "Kaydedilen ses: " + femaleVoiceName, () -> { setVoicePreference("Kadın"); });
        addCard("Erkek sesi" + (voicePreference.equals("Erkek") ? " ✓" : ""), maleVoiceName.isEmpty() ? "Önce Türkçe sesler listesinden beğendiğin sesi seç." : "Kaydedilen ses: " + maleVoiceName, () -> { setVoicePreference("Erkek"); });
        addCard("🎤 Türkçe sesleri listele ve dene", "Telefondaki tüm Türkçe sesleri tek tek seç.", this::showVoicePicker);
        addCard("🔧 Telefonun ses motoru ayarları", "Türkçe ses paketlerini ve ses motorunu yönet.", () -> {
            try { startActivity(new Intent("com.android.settings.TTS_SETTINGS")); }
            catch (Exception e) { try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception ignored) { } }
        });
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
        content.addView(text("Ses perdesi (ince / kalın)", 17, Color.WHITE, true));
        TextView pitchLabel = text("Ses perdesi: " + String.format(Locale.getDefault(), "%.2f", speechPitch) + "x", 15, ACCENT, false);
        content.addView(pitchLabel);
        SeekBar pitch = new SeekBar(this);
        pitch.setMax(120);
        pitch.setProgress(Math.round((speechPitch - 0.7f) * 100));
        content.addView(pitch);
        pitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                pitchLabel.setText("Ses perdesi: " + String.format(Locale.getDefault(), "%.2f", 0.7f + progress / 100f) + "x");
            }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {
                speechPitch = 0.7f + bar.getProgress() / 100f;
                getPreferences(MODE_PRIVATE).edit().putFloat("speech_pitch", speechPitch).apply();
                if (ttsReady) tts.setPitch(speechPitch);
                speak("Merhaba, ben " + assistantName + ". Sesimi şimdi deneyebilirsin.");
            }
        });
        addCard("🔊 Seçili sesi dene", "Kaydedilen ses ve perde ayarıyla konuş.", () -> speak("Merhaba, ben " + assistantName + ". Yolculuğunda sana yardımcı olacağım."));
        content.addView(text("Not: Ses perdesi sesi inceltir; gerçek bir kadın sesinin yerine geçmez.", 13, Color.LTGRAY, false));
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
        selectedVoiceName = preference.equals("Kadın") ? femaleVoiceName : maleVoiceName;
        getPreferences(MODE_PRIVATE).edit().putString("voice_preference", preference).apply();
        speechPitch = 1.0f;
        getPreferences(MODE_PRIVATE).edit().putFloat("speech_pitch", speechPitch).apply();
        applyVoice();
        showSettings();
        speak("Merhaba, ben " + assistantName);
    }
    @Override public void onBackPressed() { showHome(); }
}
