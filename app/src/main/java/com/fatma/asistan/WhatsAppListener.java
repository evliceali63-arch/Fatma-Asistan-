package com.fatma.asistan;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class WhatsAppListener extends NotificationListenerService {
    private static volatile String sender = "";
    private static volatile String message = "";
    private static volatile Notification.Action replyAction;

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !("com.whatsapp".equals(sbn.getPackageName()) || "com.whatsapp.w4b".equals(sbn.getPackageName()))) return;
        Notification n = sbn.getNotification();
        if (n == null || n.extras == null) return;
        CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence body = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (title == null || body == null || body.length() == 0) return;
        sender = title.toString();
        message = body.toString();
        replyAction = null;
        if (n.actions != null) for (Notification.Action a : n.actions) {
            if (a.getRemoteInputs() != null && a.getRemoteInputs().length > 0) { replyAction = a; break; }
        }
    }

    public static String lastMessage() {
        return sender.isEmpty() ? "" : sender + ": " + message;
    }

    public static boolean reply(String text) {
        Notification.Action action = replyAction;
        if (action == null || text == null || text.trim().isEmpty()) return false;
        RemoteInput[] inputs = action.getRemoteInputs();
        if (inputs == null || inputs.length == 0) return false;
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        for (RemoteInput input : inputs) bundle.putCharSequence(input.getResultKey(), text);
        RemoteInput.addResultsToIntent(inputs, intent, bundle);
        try {
            action.actionIntent.send(null, 0, intent);
            return true;
        } catch (PendingIntent.CanceledException | RuntimeException e) {
            return false;
        }
    }
}
