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
import com.example.mainproj.messaging.MessagingActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MessageNotificationService extends Service {

    private static final int FOREGROUND_NOTIFICATION_ID = 999999;
    private static final String CHANNEL_ID_FOREGROUND = "message_service_channel";

    private DatabaseReference userChatsRef;
    private ValueEventListener userChatsListener;
    private Map<String, ChildEventListener> chatListeners = new HashMap<>();
    private final Map<String, Boolean> listenerReady = new HashMap<>();
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
        userChatsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(currentUserSafeEmail).child("chats");

        userChatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    String chatId = chatSnap.getKey();
                    if (chatId == null) continue;

                    if (chatListeners.containsKey(chatId)) continue;

                    String partnerNickname = chatSnap.child("nickname").getValue(String.class);
                    String partnerUsername = chatSnap.child("username").getValue(String.class);
                    String displayName = partnerNickname != null ? partnerNickname : (partnerUsername != null ? partnerUsername : "Unknown");

                    ChildEventListener listener = buildMessageListener(chatId, displayName);
                    chatListeners.put(chatId, listener);

                    FirebaseDatabase.getInstance()
                            .getReference("chats").child(chatId).child("messages")
                            .addChildEventListener(listener);

                    FirebaseDatabase.getInstance()
                            .getReference("chats").child(chatId).child("messages")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    listenerReady.put(chatId, true);
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    listenerReady.put(chatId, true);
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        userChatsRef.addValueEventListener(userChatsListener);
    }

    private ChildEventListener buildMessageListener(String chatId, String senderUsername) {
        return new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String messageSender = snapshot.child("senderUsername").getValue(String.class);
                String messageText = snapshot.child("text").getValue(String.class);
                Long messageTs = snapshot.child("timestamp").getValue(Long.class);
                String messageTime = messageTs != null && messageTs > 0
                        ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(messageTs))
                        : "";
                String messageKey = snapshot.getKey();

                if (messageSender == null || messageText == null) return;

                if (messageSender.equals(currentUsername)) {
                    return;
                }

                String currentOpenChatId = ChatCacheApplication.getCurrentOpenChatId();
                if (currentOpenChatId != null && currentOpenChatId.equals(chatId)) {
                    return;
                }

                if (!Boolean.TRUE.equals(listenerReady.get(chatId))) return;
                if (!NetworkMonitor.getInstance(MessageNotificationService.this).isConnected()) return;

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
                    Intent intent = new Intent(foregroundActivity, MessagingActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("username", senderUsername);
                    foregroundActivity.startActivity(intent);
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
        if (userChatsRef != null && userChatsListener != null) {
            userChatsRef.removeEventListener(userChatsListener);
        }
        for (Map.Entry<String, ChildEventListener> entry : chatListeners.entrySet()) {
            FirebaseDatabase.getInstance()
                    .getReference("chats").child(entry.getKey()).child("messages")
                    .removeEventListener(entry.getValue());
        }
        chatListeners.clear();
        listenerReady.clear();
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
