package com.fatma.asistan;

import android.app.Activity;
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
    private static final int BG = Color.rgb(15, 19, 29);
    private static final int PANEL = Color.rgb(31, 39, 55);
    private static final int ACCENT = Color.rgb(49, 211, 170);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        assistantName = getPreferences(MODE_PRIVATE).getString("assistant_name", "Fatma");
        showHome();
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
        addCard("Sesli komutlar", "Sonraki sürümde eklenecek.", this::comingSoon);
        addCard("Navigasyon ve sürüş", "Sonraki sürümde eklenecek.", this::comingSoon);
        addCard("Kaza algılama", "Henüz aktif değil; acil durum sistemi olarak kullanmayın.", this::comingSoon);
        content.addView(text("Sürüm 0.1 - İlk test uygulaması", 13, Color.GRAY, false));
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
        addCard("Ana ekrana dön", "", this::showHome);
    }
    @Override public void onBackPressed() { showHome(); }
}
