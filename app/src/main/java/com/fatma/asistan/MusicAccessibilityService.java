package com.fatma.asistan;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.util.Locale;

public class MusicAccessibilityService extends AccessibilityService {
    private static final String MUSIC_PACKAGE = "com.google.android.apps.youtube.music";
    private boolean busy = false;
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (busy || event == null || event.getPackageName() == null ||
            !MUSIC_PACKAGE.contentEquals(event.getPackageName())) return;
        SharedPreferences prefs = getSharedPreferences("music_accessibility", MODE_PRIVATE);
        String query = prefs.getString("pending_song", "").trim();
        long started = prefs.getLong("pending_since", 0L);
        if (query.isEmpty() || started <= 0L) return;
        if (System.currentTimeMillis() - started > 20000L) {
            prefs.edit().remove("pending_song").remove("pending_since").apply();
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        busy = true;
        try {
            AccessibilityNodeInfo result = findResult(root, query.toLowerCase(new Locale("tr","TR")), 0);
            if (result != null) {
                AccessibilityNodeInfo target = result;
                for (int i = 0; i < 4 && target != null; i++, target = target.getParent()) {
                    if (target.isClickable() && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        prefs.edit().remove("pending_song").remove("pending_since").apply();
                        break;
                    }
                }
            }
        } finally { busy = false; root.recycle(); }
    }
    private AccessibilityNodeInfo findResult(AccessibilityNodeInfo node, String query, int depth) {
        if (node == null || depth > 30) return null;
        CharSequence label = node.getText();
        if (label == null) label = node.getContentDescription();
        if (label != null) {
            String text = label.toString().toLowerCase(new Locale("tr","TR"));
            String[] tokens = query.split("\\s+");
            int matches = 0;
            for (String token : tokens) if (token.length() > 2 && text.contains(token)) matches++;
            if (matches > 0 && matches >= Math.max(1, tokens.length - 1)) return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findResult(node.getChild(i), query, depth + 1);
            if (found != null) return found;
        }
        return null;
    }
    @Override public void onInterrupt() {}
}
