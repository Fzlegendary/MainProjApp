package com.example.mainproj.messaging;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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

        long deleteAt = System.currentTimeMillis() + 3 * 3600 * 1000L;

        Map<String, Object> data = new HashMap<>();
        data.put("name", intent.getStringExtra("itemName"));
        data.put("byWho", intent.getStringExtra("byWho"));
        data.put("reason", intent.getStringExtra("reason"));
        data.put("timestampWhenAdded", intent.getLongExtra("timestampWhenAdded", 0L));
        data.put("timestampExpiresAt", intent.getLongExtra("timestampExpiresAt", 0L));
        data.put("deleteAt", deleteAt);

        DatabaseReference scheduledRef = FirebaseDatabase.getInstance()
                .getReference("scheduledForDeletion").child(listType).child(pushId);
        scheduledRef.setValue(data);
        Log.d(TAG, "Moved to scheduledForDeletion: pushId=" + pushId + ", deleteAt=" + deleteAt);

        DatabaseReference listRef = FirebaseDatabase.getInstance()
                .getReference("lists").child(listType).child(pushId);
        listRef.removeValue();

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

        Intent delIntent = new Intent(context, DeletionReceiver.class);
        delIntent.putExtra("pushId", pushId);
        delIntent.putExtra("listType", listType);
        int delRequestId = (pushId + "_del").hashCode();

        PendingIntent delPendingIntent = PendingIntent.getBroadcast(
                context, delRequestId, delIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(deleteAt, null), delPendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, deleteAt, delPendingIntent);
            }
            Log.d(TAG, "Deletion alarm scheduled for: " + deleteAt);
        }
    }
}
