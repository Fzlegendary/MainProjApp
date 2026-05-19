package com.example.mainproj.messaging;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.example.mainproj.MainActivity;

public class NotificationHelper {

    public static final String CHANNEL_ID_MESSAGES = "messages_channel";
    public static final String CHANNEL_NAME_MESSAGES = "Chat Messages";

    public static final String ACTION_MARK_AS_READ = "com.example.mainproj.ACTION_MARK_AS_READ";
    public static final String ACTION_REPLY = "com.example.mainproj.ACTION_REPLY";

    public static final String EXTRA_CHAT_ID = "extra_chat_id";
    public static final String EXTRA_SENDER_USERNAME = "extra_sender_username";
    public static final String EXTRA_MESSAGE_TEXT = "extra_message_text";
    public static final String EXTRA_MESSAGE_KEY = "extra_message_key";

    public static final String REPLY_LABEL = "Reply";
    private static final int NOTIFICATION_ID_BASE = 1000;

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_MESSAGES,
                    CHANNEL_NAME_MESSAGES,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for incoming chat messages");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void showMessageNotification(
            Context context,
            String chatId,
            String senderUsername,
            String messageText,
            String messageTime,
            String messageKey
    ) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        int notificationId = (CHANNEL_ID_MESSAGES + chatId).hashCode();

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra(EXTRA_CHAT_ID, chatId);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notificationId,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent markReadIntent = new Intent(context, NotificationActionReceiver.class);
        markReadIntent.setAction(ACTION_MARK_AS_READ);
        markReadIntent.putExtra(EXTRA_CHAT_ID, chatId);
        markReadIntent.putExtra(EXTRA_MESSAGE_KEY, messageKey);
        PendingIntent markReadPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1,
                markReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_MESSAGE_TEXT)
                .setLabel(REPLY_LABEL)
                .build();

        Intent replyIntent = new Intent(context, NotificationActionReceiver.class);
        replyIntent.setAction(ACTION_REPLY);
        replyIntent.putExtra(EXTRA_CHAT_ID, chatId);
        replyIntent.putExtra(EXTRA_SENDER_USERNAME, senderUsername);
        replyIntent.putExtra(EXTRA_MESSAGE_KEY, messageKey);
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_view,
                "Mark as Read",
                markReadPendingIntent
        ).build();

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                "Reply",
                replyPendingIntent
        ).addRemoteInput(remoteInput).build();

        String contentTitle = "From: " + senderUsername;
        String contentText = messageText;
        String subText = "Sent at " + messageTime;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSubText(subText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageText))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .addAction(markAsReadAction)
                .addAction(replyAction);

        notificationManager.notify(notificationId, builder.build());
    }

    public static void cancelNotification(Context context, String chatId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = (CHANNEL_ID_MESSAGES + chatId).hashCode();
            notificationManager.cancel(notificationId);
        }
    }

    public static String getMessageTextFromIntent(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(EXTRA_MESSAGE_TEXT, "").toString();
        }
        return null;
    }
}
