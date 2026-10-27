package com.fatma.asistan;

import android.app.Activity;
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
    private static final int SPEECH_REQUEST = 100;
    private static final int BG = Color.rgb(15, 19, 29);
    private static final int PANEL = Color.rgb(31, 39, 55);
    private static final int ACCENT = Color.rgb(49, 211, 170);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        assistantName = getPreferences(MODE_PRIVATE).getString("assistant_name", "Fatma");
        voicePreference = getPreferences(MODE_PRIVATE).getString("voice_preference", "Kadın");
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
        for (Voice v : tts.getVoices() == null ? java.util.Collections.<Voice>emptySet() : tts.getVoices()) {
            if (!"tr".equals(v.getLocale().getLanguage())) continue;
            String n = v.getName().toLowerCase(Locale.ROOT);
            boolean male = n.contains("male") && !n.contains("female");
            boolean female = n.contains("female");
            if ((voicePreference.equals("Erkek") && male) || (voicePreference.equals("Kadın") && female)) { selected = v; break; }
            if (selected == null) selected = v;
        }
        if (selected != null) tts.setVoice(selected);
        tts.setSpeechRate(0.95f);
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
                if (phrase.contains("saat kaç") || phrase.contains("saati söyle")) {
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
                Toast.makeText(this, reply, Toast.LENGTH_LONG).show();
                speak(reply);
            }
        }
    }
    @Override protected void onDestroy() {
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
        addCard("Asistan ayarları", "Asistanın ismini değiştir.", this::showSettings);
        addCard("Bluetooth", "Telefonun Bluetooth ayarlarını aç.", () -> {
            try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
            catch (Exception e) { Toast.makeText(this, "Bluetooth ayarları açılamadı", Toast.LENGTH_SHORT).show(); }
        });
        addCard("🎙️ Beni dinle", "Türkçe sesli komut vermek için dokun.", this::listen);
        addCard("🔊 Sesimi dene", "Asistanın Türkçe konuşmasını dinle.", () -> speak("Merhaba, ben " + assistantName + ". Sana nasıl yardımcı olabilirim?"));
        addCard("Navigasyon ve sürüş", "Sonraki sürümde eklenecek.", this::comingSoon);
        addCard("Kaza algılama", "Henüz aktif değil; acil durum sistemi olarak kullanmayın.", this::comingSoon);
        content.addView(text("Sürüm 0.2 - Sesli test uygulaması", 13, Color.GRAY, false));
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
