package com.example.mainproj.messaging;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessaging;

public class ChatCacheApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private int activityCount = 0;
    private static String currentOpenChatId = null;
    private static String currentUsername = null;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        NotificationHelper.createChannel(this);
        fetchFCMToken();
    }

    private void fetchFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        return;
                    }
                    String token = task.getResult();
                });
    }

    public static void setCurrentOpenChatId(String chatId) {
        currentOpenChatId = chatId;
    }

    public static String getCurrentOpenChatId() {
        return currentOpenChatId;
    }

    public static void setCurrentUsername(String username) {
        currentUsername = username;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        activityCount++;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        activityCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {
        activityCount--;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        activityCount--;
        if (activityCount <= 0) {
            activityCount = 0;
            ChatCacheManager.getInstance(this).clearAll();
        }
    }
}
