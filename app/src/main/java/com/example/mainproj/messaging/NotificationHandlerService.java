package com.example.mainproj.messaging;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationHandlerService extends FirebaseMessagingService {

    private static final String TAG = "NotificationHandler";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM token: " + token);
        sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM Message received from: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            String chatId = remoteMessage.getData().get("chatId");
            String senderUsername = remoteMessage.getData().get("senderUsername");
            String messageText = remoteMessage.getData().get("messageText");
            String messageTime = remoteMessage.getData().get("messageTime");
            String messageKey = remoteMessage.getData().get("messageKey");

            if (chatId != null && senderUsername != null && messageText != null) {
                if (isChatCurrentlyOpen(chatId)) {
                    Log.d(TAG, "Chat is open, suppressing notification for: " + chatId);
                    return;
                }

                if (isMessageFromCurrentUser(senderUsername)) {
                    Log.d(TAG, "Message from current user, suppressing notification");
                    return;
                }

                MutedChatManager mutedChatManager = MutedChatManager.getInstance(this);
                if (mutedChatManager.isMuted(chatId)) {
                    Log.d(TAG, "Chat is muted, suppressing notification for: " + chatId);
                    return;
                }

                if (ChatCacheApplication.isAppInForeground()) {
                    showInAppDialog(chatId, senderUsername, messageText);
                } else {
                    NotificationHelper.showMessageNotification(
                            this,
                            chatId,
                            senderUsername,
                            messageText,
                            messageTime != null ? messageTime : "",
                            messageKey != null ? messageKey : ""
                    );
                }
            }
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification body: " + remoteMessage.getNotification().getBody());
        }
    }

    private boolean isChatCurrentlyOpen(String chatId) {
        return ChatCacheApplication.getCurrentOpenChatId() != null
                && ChatCacheApplication.getCurrentOpenChatId().equals(chatId);
    }

    private void showInAppDialog(String chatId, String senderUsername, String messageText) {
        Activity foregroundActivity = ChatCacheApplication.getCurrentForegroundActivity();
        if (foregroundActivity == null || foregroundActivity.isFinishing()) {
            return;
        }

        String displayTitle = "New message from " + senderUsername;
        String displayMessage = messageText.length() > 100 ? messageText.substring(0, 100) + "..." : messageText;

        new AlertDialog.Builder(foregroundActivity)
                .setTitle(displayTitle)
                .setMessage(displayMessage)
                .setPositiveButton("View Chat", (dialog, which) -> {
                    Intent intent = new Intent(foregroundActivity, ChatActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("username", senderUsername);
                    foregroundActivity.startActivity(intent);
                })
                .setNegativeButton("Mute", (dialog, which) -> {
                    MutedChatManager.getInstance(this).muteChat(chatId);
                })
                .setNeutralButton("Dismiss", null)
                .setCancelable(true)
                .show();
    }

    private boolean isMessageFromCurrentUser(String senderUsername) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            return false;
        }

        String safeEmail = user.getEmail().replace(".", "_");
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail);

        final boolean[] isFromCurrentUser = {false};

        try {
            Thread thread = new Thread(() -> {
                // This is a synchronous check - in production you'd want to use a callback
            });
            thread.start();
            thread.join(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return senderUsername != null && senderUsername.equals(ChatCacheApplication.getCurrentUsername());
    }

    private void sendTokenToServer(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            return;
        }

        String safeEmail = user.getEmail().replace(".", "_");
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail);

        userRef.child("fcmToken").setValue(token).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "FCM token saved to server");
            } else {
                Log.e(TAG, "Failed to save FCM token", task.getException());
            }
        });
    }
}
