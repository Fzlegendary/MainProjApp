package com.example.mainproj.messaging;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;

public class DeletionJobService extends JobService {

    private static final String TAG = "DeletionJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        String pushId = params.getExtras().getString("pushId");
        String listType = params.getExtras().getString("listType");

        Log.d(TAG, "Job started: deleting pushId=" + pushId + " from listType=" + listType);

        if (pushId == null || listType == null) {
            jobFinished(params, false);
            return false;
        }

        FirebaseDatabase.getInstance()
                .getReference("lists").child(listType).child(pushId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Deleted expired item: pushId=" + pushId);
                    } else {
                        Log.e(TAG, "Failed to delete expired item: pushId=" + pushId);
                    }
                    jobFinished(params, false);
                });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job stopped: pushId=" + params.getExtras().getString("pushId"));
        return false;
    }
}
