package com.example.mainproj.auth;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {

    private static String currentSafeEmail = "";
    private static String currentUsername = "";
    private static String role = "user";
    private static int roleLevel = 0;

    public static void init() {
        refresh();
    }

    public static void refresh() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        String email = user.getEmail();
        currentSafeEmail = email.replace(".", "_");

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(currentSafeEmail);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String r = snapshot.child("role").getValue(String.class);
                if (r != null) role = r;
                else snapshot.getRef().child("role").setValue("user");

                Integer rl = snapshot.child("roleLevel").getValue(Integer.class);
                if (rl != null) roleLevel = rl;
                else snapshot.getRef().child("roleLevel").setValue(0);

                String un = snapshot.child("username").getValue(String.class);
                if (un != null) currentUsername = un;
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    public static boolean isAdmin() {
        return roleLevel >= 1;
    }

    public static boolean isSuperAdmin() {
        return roleLevel >= 2;
    }

    public static boolean canViewGate(int gateLevel) {
        return roleLevel >= gateLevel;
    }

    public static boolean canControlGate(int gateLevel) {
        return roleLevel >= gateLevel;
    }

    public static boolean canModifyUser(int targetRoleLevel) {
        return roleLevel > targetRoleLevel;
    }

    public static String getCurrentSafeEmail() {
        return currentSafeEmail;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static int getRoleLevel() {
        return roleLevel;
    }

    public static void setRoleLevel(int level) {
        roleLevel = level;
    }

    public static String getRole() {
        return role;
    }

    public static void setUserRole(String targetSafeEmail, String newRole, int newLevel) {
        if (!canModifyUser(newLevel)) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("role", newRole);
        updates.put("roleLevel", newLevel);
        FirebaseDatabase.getInstance()
                .getReference("users").child(targetSafeEmail)
                .updateChildren(updates);
    }

    public static void setGateLevels(String gateId, int visibilityLevel, int controlLevel) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("visibilityLevel", visibilityLevel);
        updates.put("controlLevel", controlLevel);
        FirebaseDatabase.getInstance()
                .getReference("gates").child(gateId)
                .updateChildren(updates);
    }

    public static void setGateDisabled(String gateId, boolean disabled) {
        FirebaseDatabase.getInstance()
                .getReference("gates").child(gateId).child("disabled")
                .setValue(disabled);
    }

    public static void sendFriendRequest(String targetSafeEmail, String targetUsername) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null || currentUsername.isEmpty()) return;

        String rawEmail = user.getEmail();
        String mySafeEmail = currentSafeEmail.isEmpty() ? rawEmail.replace(".", "_") : currentSafeEmail;

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("from", rawEmail);
        requestData.put("fromUsername", currentUsername);
        requestData.put("timestamp", System.currentTimeMillis());
        requestData.put("status", "pending");

        FirebaseDatabase.getInstance()
                .getReference("friendRequests").child(targetSafeEmail)
                .child(currentUsername).setValue(requestData);

        Map<String, Object> sentData = new HashMap<>();
        sentData.put("to", targetSafeEmail);
        sentData.put("toUsername", targetUsername);
        sentData.put("timestamp", System.currentTimeMillis());
        sentData.put("status", "pending");

        FirebaseDatabase.getInstance()
                .getReference("sentRequests").child(mySafeEmail)
                .child(targetUsername).setValue(sentData);
    }

    public static void clearAllPrefs(Context context) {
        String[] prefNames = {
                "MainProjPrefs",
                "chat_metadata",
                "message_cache"
        };
        for (String name : prefNames) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit().clear().apply();
        }
    }
}
