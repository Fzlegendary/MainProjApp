package com.example.mainproj.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

        String chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID);
        String messageKey = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE_KEY);

        if (chatId == null) return;

        NotificationHelper.cancelNotification(context, chatId);

        if (action.equals(NotificationHelper.ACTION_MARK_AS_READ)) {
            handleMarkAsRead(context, chatId, messageKey);
        } else if (action.equals(NotificationHelper.ACTION_REPLY)) {
            String replyText = NotificationHelper.getMessageTextFromIntent(intent);
            String senderUsername = intent.getStringExtra(NotificationHelper.EXTRA_SENDER_USERNAME);
            handleReply(context, chatId, senderUsername, replyText);
        }
    }

    private void handleMarkAsRead(Context context, String chatId, String messageKey) {
    }

    private void handleReply(Context context, String chatId, String senderUsername, String replyText) {
        if (replyText == null || replyText.trim().isEmpty()) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentEmail = user.getEmail();
        if (currentEmail == null) return;

        String safeEmail = currentEmail.replace(".", "_");

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail);

        userRef.child("username").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(context, "Error getting username", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentUsername = (String) task.getResult().getValue();
            if (currentUsername == null) {
                Toast.makeText(context, "Error: username not found", Toast.LENGTH_SHORT).show();
                return;
            }

            sendReplyMessage(context, chatId, currentUsername, replyText.trim());
        });
    }

    private void sendReplyMessage(Context context, String chatId, String currentUsername, String replyText) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                .getReference("chats").child(chatId).child("messages");

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", replyText);
        messageData.put("senderUsername", currentUsername);
        messageData.put("timestamp", System.currentTimeMillis());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        messageData.put("time", sdf.format(new Date()));

        messagesRef.push().setValue(messageData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference chatRef = FirebaseDatabase.getInstance()
                        .getReference("chats").child(chatId);

                Map<String, Object> lastMessageUpdate = new HashMap<>();
                lastMessageUpdate.put("lastMessage", replyText);
                lastMessageUpdate.put("time", sdf.format(new Date()));

                chatRef.updateChildren(lastMessageUpdate);

                Toast.makeText(context, "Reply sent", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to send reply", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
