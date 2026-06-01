package com.example.mainproj.messaging;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Action {

    private static final String TAG = "Action";
    private static final String PREFS_NAME = "action_queue";
    private static final String KEY_ACTIONS = "actions";

    private String id;
    private String type;
    private String chatId;
    private String messageId;
    private String payloadJson;
    private long createdAt;
    private String status;

    public Action() {}

    public Action(String type, String chatId, String messageId, String payloadJson) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.chatId = chatId;
        this.messageId = messageId;
        this.payloadJson = payloadJson != null ? payloadJson : "{}";
        this.createdAt = System.currentTimeMillis();
        this.status = "pending";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // -------------------------------------------------------------------------
    // Static queue operations (SharedPreferences-backed JSON array)
    // -------------------------------------------------------------------------

    public static void enqueue(@NonNull Context ctx, @NonNull Action action) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray array = readArray(prefs);
        array.put(action.toJson());
        prefs.edit().putString(KEY_ACTIONS, array.toString()).apply();
        Log.d(TAG, "Enqueued " + action.type + " (queue size: " + array.length() + ")");
    }

    public static void dequeue(@NonNull Context ctx, @NonNull String actionId) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray array = readArray(prefs);
        JSONArray updated = new JSONArray();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!actionId.equals(obj.optString("id"))) {
                    updated.put(obj);
                }
            }
            prefs.edit().putString(KEY_ACTIONS, updated.toString()).apply();
            Log.d(TAG, "Dequeued " + actionId + " (queue size: " + updated.length() + ")");
        } catch (Exception e) {
            Log.e(TAG, "Failed to dequeue " + actionId, e);
        }
    }

    @NonNull
    public static List<Action> getAllSorted(@NonNull Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray array = readArray(prefs);
        List<Action> actions = new ArrayList<>();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Action a = fromJson(obj);
                if (a != null) actions.add(a);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read queue", e);
        }
        Collections.sort(actions, new Comparator<Action>() {
            @Override
            public int compare(Action a, Action b) {
                return Long.compare(a.createdAt, b.createdAt);
            }
        });
        return actions;
    }

    public static int size(@NonNull Context ctx) {
        return readArray(ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)).length();
    }

    public static void clearAll(@NonNull Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ACTIONS)
                .apply();
    }

    // -------------------------------------------------------------------------
    // Sync: execute this action against Firebase
    // -------------------------------------------------------------------------

    public void sync(@NonNull Context ctx) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Log.w(TAG, "No logged-in user, cannot sync " + id);
            return;
        }
        String safeEmail = user.getEmail().replace(".", "_");
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        String currentChatId = chatId;

        switch (type == null ? "" : type) {
            case "send_message": {
                try {
                    JSONObject payload = payloadJson != null ? new JSONObject(payloadJson) : new JSONObject();
                    Map<String, Object> data = jsonToMap(payload);
                    db.child("chats").child(currentChatId).child("messages")
                            .push().setValue(data)
                            .addOnSuccessListener(aVoid -> {
                                dequeue(ctx, id);
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "send_message failed: " + e.getMessage());
                            });
                } catch (Exception e) {
                    Log.e(TAG, "send_message parse error", e);
                }
                break;
            }
            case "delete_message": {
                db.child("chats").child(currentChatId).child("messages").child(messageId)
                        .removeValue()
                        .addOnSuccessListener(aVoid -> dequeue(ctx, id))
                        .addOnFailureListener(e -> Log.w(TAG, "delete_message failed: " + e.getMessage()));
                break;
            }
            case "delete_chat": {
                db.child("users").child(safeEmail).child("chats").child(currentChatId)
                        .removeValue()
                        .addOnSuccessListener(aVoid -> dequeue(ctx, id))
                        .addOnFailureListener(e -> Log.w(TAG, "delete_chat failed: " + e.getMessage()));
                break;
            }
            case "rename_chat": {
                String newName = "";
                try {
                    if (payloadJson != null) {
                        newName = new JSONObject(payloadJson).optString("chatName", "");
                    }
                } catch (Exception ignored) {}
                db.child("users").child(safeEmail).child("chats").child(currentChatId)
                        .child("chatName").setValue(newName)
                        .addOnSuccessListener(aVoid -> dequeue(ctx, id))
                        .addOnFailureListener(e -> Log.w(TAG, "rename_chat failed: " + e.getMessage()));
                break;
            }
            case "update_chat_header": {
                try {
                    JSONObject payload = payloadJson != null ? new JSONObject(payloadJson) : new JSONObject();
                    Map<String, Object> updates = jsonToMap(payload);
                    db.child("users").child(safeEmail).child("chats").child(currentChatId)
                            .updateChildren(updates)
                            .addOnSuccessListener(aVoid -> dequeue(ctx, id))
                            .addOnFailureListener(e -> Log.w(TAG, "update_chat_header failed: " + e.getMessage()));
                } catch (Exception e) {
                    Log.e(TAG, "update_chat_header parse error", e);
                }
                break;
            }
            default:
                Log.w(TAG, "Unknown action type: " + type);
        }
    }

    public static void syncAll(@NonNull Context ctx) {
        List<Action> actions = getAllSorted(ctx);
        if (actions.isEmpty()) return;
        Log.d(TAG, "syncAll: replaying " + actions.size() + " actions");
        for (Action a : actions) {
            a.sync(ctx);
        }
    }

    // -------------------------------------------------------------------------
    // JSON helpers
    // -------------------------------------------------------------------------

    private JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("type", type);
            obj.put("chatId", chatId);
            obj.put("messageId", messageId == null ? JSONObject.NULL : messageId);
            obj.put("payloadJson", payloadJson);
            obj.put("createdAt", createdAt);
            obj.put("status", status);
            return obj;
        } catch (Exception e) {
            Log.e(TAG, "toJson error", e);
            return new JSONObject();
        }
    }

    private static Action fromJson(JSONObject obj) {
        try {
            Action a = new Action();
            a.id = obj.optString("id", UUID.randomUUID().toString());
            a.type = obj.optString("type", "");
            a.chatId = obj.optString("chatId", "");
            a.messageId = obj.isNull("messageId") ? null : obj.optString("messageId", null);
            a.payloadJson = obj.optString("payloadJson", "{}");
            a.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            a.status = obj.optString("status", "pending");
            return a;
        } catch (Exception e) {
            Log.e(TAG, "fromJson error", e);
            return null;
        }
    }

    private static JSONArray readArray(SharedPreferences prefs) {
        String raw = prefs.getString(KEY_ACTIONS, null);
        if (raw == null || raw.isEmpty()) return new JSONArray();
        try {
            return new JSONArray(raw);
        } catch (Exception e) {
            Log.e(TAG, "readArray parse error", e);
            return new JSONArray();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new java.util.HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.opt(key);
            map.put(key, value);
        }
        return map;
    }
}
