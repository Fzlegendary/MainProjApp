package com.example.mainproj.messaging;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.mainproj.MainActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ExpirationReceiver extends BroadcastReceiver {

    private static final String TAG = "ExpirationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String pushId = intent.getStringExtra("pushId");
        String listType = intent.getStringExtra("listType");
        String itemName = intent.getStringExtra("itemName");

        Log.d(TAG, "Received expiration: pushId=" + pushId + ", listType=" + listType + ", item=" + itemName);

        if (pushId == null || listType == null) return;

        long now = System.currentTimeMillis();
        long deleteAt = now + 3 * 3600 * 1000L;

        // STEP 1: Mark item as expired in the active list (keeps it visible)
        DatabaseReference itemRef = FirebaseDatabase.getInstance()
                .getReference("lists").child(listType).child(pushId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isExpired", true);
        updates.put("deleteAt", deleteAt);
        itemRef.updateChildren(updates);
        Log.d(TAG, "Marked item as expired: pushId=" + pushId);

        // STEP 2: Schedule deletion via JobScheduler (3 hours later)
        ComponentName component = new ComponentName(context, DeletionJobService.class);
        PersistableBundle extras = new PersistableBundle();
        extras.putString("pushId", pushId);
        extras.putString("listType", listType);

        int jobId = ("del_" + pushId).hashCode();
        JobInfo jobInfo = new JobInfo.Builder(jobId, component)
                .setMinimumLatency(3 * 3600 * 1000L)
                .setOverrideDeadline(3 * 3600 * 1000L + 60000L)
                .setPersisted(true)
                .setExtras(extras)
                .build();

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            scheduler.schedule(jobInfo);
            Log.d(TAG, "Deletion job scheduled for: " + deleteAt + " (jobId=" + jobId + ")");
        }

        // STEP 3: Show notification on current device
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID_EXPIRATION)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Item Expired")
                .setContentText(itemName + " has expired. Will be deleted in 3 hours.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(itemName + " has expired. It will be permanently deleted in 3 hours. Open Settings to undo."))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(pushId.hashCode(), builder.build());
            Log.d(TAG, "Notification shown for: " + itemName);
        }

        // STEP 4: Notify all admins via Firebase DB
        AdminNotificationHelper.notifyAdmins(pushId, listType, itemName, deleteAt);
    }
}
