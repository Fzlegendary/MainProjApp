package com.example.mainproj.messaging;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.mainproj.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MessageNotificationService extends Service {

    private static final int FOREGROUND_NOTIFICATION_ID = 999999;
    private static final String CHANNEL_ID_FOREGROUND = "message_service_channel";

    private DatabaseReference chatsRef;
    private Map<String, ChildEventListener> chatListeners = new HashMap<>();
    private String currentUserSafeEmail;
    private String currentUsername;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification());
        initializeUserAndStartListening();
    }

    private void initializeUserAndStartListening() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            stopSelf();
            return;
        }

        currentUserSafeEmail = user.getEmail().replace(".", "_");

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(currentUserSafeEmail);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUsername = snapshot.child("username").getValue(String.class);
                if (currentUsername != null) {
                    ChatCacheApplication.setCurrentUsername(currentUsername);
                }
                startListeningToChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                startListeningToChats();
            }
        });
    }

    private void startListeningToChats() {
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        
        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    String chatId = chatSnap.getKey();
                    if (chatId == null) continue;

                    if (!chatSnap.hasChild("participants")) continue;

                    DataSnapshot participants = chatSnap.child("participants");
                    if (!participants.hasChild(currentUserSafeEmail)) continue;

                    if (chatListeners.containsKey(chatId)) continue;

                    String partnerUsername = chatSnap.child("username").getValue(String.class);

                    ChildEventListener listener = buildMessageListener(chatId, partnerUsername);
                    chatListeners.put(chatId, listener);

                    FirebaseDatabase.getInstance()
                            .getReference("chats").child(chatId).child("messages")
                            .addChildEventListener(listener);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private ChildEventListener buildMessageListener(String chatId, String senderUsername) {
        return new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String messageSender = snapshot.child("senderUsername").getValue(String.class);
                String messageText = snapshot.child("text").getValue(String.class);
                String messageTime = snapshot.child("time").getValue(String.class);
                String messageKey = snapshot.getKey();

                if (messageSender == null || messageText == null) return;

                if (messageSender.equals(currentUsername)) {
                    return;
                }

                String currentOpenChatId = ChatCacheApplication.getCurrentOpenChatId();
                if (currentOpenChatId != null && currentOpenChatId.equals(chatId)) {
                    return;
                }

                MutedChatManager mutedChatManager = MutedChatManager.getInstance(MessageNotificationService.this);
                if (mutedChatManager.isMuted(chatId)) {
                    return;
                }

                if (ChatCacheApplication.isAppInForeground()) {
                    showInAppDialog(chatId, senderUsername != null ? senderUsername : "Unknown", messageText);
                } else {
                    NotificationHelper.showMessageNotification(
                            MessageNotificationService.this,
                            chatId,
                            senderUsername != null ? senderUsername : "Unknown",
                            messageText,
                            messageTime != null ? messageTime : "",
                            messageKey != null ? messageKey : ""
                    );
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
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
                    MutedChatManager.getInstance(MessageNotificationService.this).muteChat(chatId);
                })
                .setNeutralButton("Dismiss", null)
                .setCancelable(true)
                .show();
    }

    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_MESSAGES)
                .setContentTitle("Chat Service Active")
                .setContentText("Monitoring for new messages")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Map.Entry<String, ChildEventListener> entry : chatListeners.entrySet()) {
            FirebaseDatabase.getInstance()
                    .getReference("chats").child(entry.getKey()).child("messages")
                    .removeEventListener(entry.getValue());
        }
        chatListeners.clear();
    }

    public static void startService(android.content.Context context) {
        Intent intent = new Intent(context, MessageNotificationService.class);
        context.startForegroundService(intent);
    }

    public static void stopService(android.content.Context context) {
        Intent intent = new Intent(context, MessageNotificationService.class);
        context.stopService(intent);
    }
}
