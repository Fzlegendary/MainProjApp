package com.example.mainproj.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;

public class DeletionReceiver extends BroadcastReceiver {

    private static final String TAG = "DeletionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String pushId = intent.getStringExtra("pushId");
        String listType = intent.getStringExtra("listType");

        Log.d(TAG, "Received deletion: pushId=" + pushId + ", listType=" + listType);

        if (pushId == null || listType == null) return;

        final PendingResult pendingResult = goAsync();

        FirebaseDatabase.getInstance()
                .getReference("lists").child(listType).child(pushId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Deleted from Firebase: pushId=" + pushId);
                    } else {
                        Log.d(TAG, "Failed to delete from Firebase: pushId=" + pushId);
                    }
                    pendingResult.finish();
                });
    }
}
