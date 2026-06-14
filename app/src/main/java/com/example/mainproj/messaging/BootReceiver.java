package com.example.mainproj.messaging;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                MessageNotificationService.startService(context);
            }

            final PendingResult pendingResult = goAsync();
            rescheduleAlarms(context, pendingResult);
        }
    }

    private void rescheduleAlarms(Context context, PendingResult pendingResult) {
        long now = System.currentTimeMillis();
        int[] expCount = {0};
        int[] delCount = {0};

        DatabaseReference listsRef = FirebaseDatabase.getInstance().getReference("lists");
        listsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot listTypeSnap : task.getResult().getChildren()) {
                    String listType = listTypeSnap.getKey();
                    for (DataSnapshot itemSnap : listTypeSnap.getChildren()) {
                        String pushId = itemSnap.getKey();
                        Long expiresAt = itemSnap.child("timestampExpiresAt").getValue(Long.class);
                        Boolean isExpired = itemSnap.child("isExpired").getValue(Boolean.class);
                        Long deleteAt = itemSnap.child("deleteAt").getValue(Long.class);

                        // Re-schedule expiration alarm for items that haven't expired yet
                        if (pushId != null && expiresAt != null && expiresAt > now) {
                            String name = itemSnap.child("name").getValue(String.class);
                            String byWho = itemSnap.child("byWho").getValue(String.class);
                            String reason = itemSnap.child("reason").getValue(String.class);
                            Long whenAdded = itemSnap.child("timestampWhenAdded").getValue(Long.class);

                            Intent expIntent = new Intent(context, ExpirationReceiver.class);
                            expIntent.putExtra("pushId", pushId);
                            expIntent.putExtra("listType", listType);
                            expIntent.putExtra("itemName", name);
                            expIntent.putExtra("byWho", byWho);
                            expIntent.putExtra("reason", reason);
                            expIntent.putExtra("timestampWhenAdded", whenAdded != null ? whenAdded : 0L);
                            expIntent.putExtra("timestampExpiresAt", expiresAt);

                            NotificationHelper.scheduleAlarm(context, pushId.hashCode(), expiresAt, expIntent);
                            expCount[0]++;
                        }

                        // Re-schedule deletion job for items already expired but not yet deleted
                        if (pushId != null && isExpired != null && isExpired
                                && deleteAt != null && deleteAt > now) {
                            ComponentName component = new ComponentName(context, DeletionJobService.class);
                            PersistableBundle extras = new PersistableBundle();
                            extras.putString("pushId", pushId);
                            extras.putString("listType", listType);

                            JobInfo jobInfo = new JobInfo.Builder(("del_" + pushId).hashCode(), component)
                                    .setMinimumLatency(deleteAt - now)
                                    .setOverrideDeadline(deleteAt - now + 60000L)
                                    .setPersisted(true)
                                    .setExtras(extras)
                                    .build();

                            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                            if (scheduler != null) {
                                scheduler.schedule(jobInfo);
                                delCount[0]++;
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "Re-scheduled " + expCount[0] + " expiration alarms, " + delCount[0] + " deletion jobs on boot");
            pendingResult.finish();
        });
    }
}
