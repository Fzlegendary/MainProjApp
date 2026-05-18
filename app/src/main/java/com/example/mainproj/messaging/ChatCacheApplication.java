package com.example.mainproj.messaging;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.example.mainproj.auth.SessionManager;
import com.google.firebase.messaging.FirebaseMessaging;

public class ChatCacheApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static int activityCount = 0;
    private static String currentOpenChatId = null;
    private static String currentUsername = null;
    private static Activity currentForegroundActivity = null;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        NotificationHelper.createChannel(this);
        fetchFCMToken();
        SessionManager.init();
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

    public static void setCurrentForegroundActivity(Activity activity) {
        currentForegroundActivity = activity;
    }

    public static Activity getCurrentForegroundActivity() {
        return currentForegroundActivity;
    }

    public static boolean isAppInForeground() {
        return activityCount > 0;
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
    public void onActivityResumed(Activity activity) {
        currentForegroundActivity = activity;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (currentForegroundActivity == activity) {
            currentForegroundActivity = null;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        activityCount--;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        activityCount--;
        if (currentForegroundActivity == activity) {
            currentForegroundActivity = null;
        }
        if (activityCount <= 0) {
            activityCount = 0;
            ChatCacheManager.getInstance(this).clearAll();
            MutedChatManager.getInstance(this).clearAll();
        }
    }
}
