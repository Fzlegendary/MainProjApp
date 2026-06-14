package com.example.mainproj.messaging;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AdminNotificationHelper {

    private static final String TAG = "AdminNotificationHelper";

    public static void notifyAdmins(String pushId, String listType,
                                     String itemName, long deleteAt) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String safeEmail = userSnap.getKey();
                    Integer roleLevel = userSnap.child("roleLevel").getValue(Integer.class);

                    if (safeEmail != null && roleLevel != null && roleLevel >= 1) {
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("pushId", pushId);
                        notification.put("listType", listType);
                        notification.put("itemName", itemName);
                        notification.put("expiredAt", System.currentTimeMillis());
                        notification.put("deleteAt", deleteAt);
                        notification.put("read", false);

                        FirebaseDatabase.getInstance()
                                .getReference("adminNotifications")
                                .child(safeEmail)
                                .child(pushId)
                                .setValue(notification)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "Notification sent to admin: " + safeEmail))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Failed to notify admin: " + safeEmail, e));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to query users for admin notification", error.toException());
            }
        });
    }

    public static void removeNotifications(String pushId) {
        DatabaseReference adminNotifRef = FirebaseDatabase.getInstance()
                .getReference("adminNotifications");

        adminNotifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    if (userSnap.hasChild(pushId)) {
                        userSnap.child(pushId).getRef().removeValue();
                        Log.d(TAG, "Removed notification for pushId=" + pushId);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to remove admin notifications", error.toException());
            }
        });
    }
}
