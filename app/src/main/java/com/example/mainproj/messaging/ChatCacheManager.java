package com.example.mainproj.messaging;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChatCacheManager {

    private static final String TAG = "ChatCacheManager";

    private static final String PREFS_NAME = "message_cache";
    private static final String KEY_PREFIX = "messages_";

    private static ChatCacheManager instance;
    private static boolean cachingEnabled = false;

    private final SharedPreferences prefs;

    private ChatCacheManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ChatCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChatCacheManager(context.getApplicationContext());
        }
        return instance;
    }

    public static boolean isCachingEnabled() {
        return cachingEnabled;
    }

    public static void setCachingEnabled(boolean enabled) {
        cachingEnabled = enabled;
    }

    public void saveMessages(String chatId, ArrayList<MessageModel> messages) {
        if (!cachingEnabled) return;

        try {
            JSONArray array = new JSONArray();
            for (MessageModel msg : messages) {
                JSONObject obj = new JSONObject();
                obj.put("text", msg.getText());
                obj.put("timestamp", msg.getTimestamp());
                obj.put("sentByMe", msg.isSentByMe());
                if (msg.getMessageId() != null && !"null".equals(msg.getMessageId())) {
                    obj.put("messageId", msg.getMessageId());
                } else {
                    obj.put("messageId", JSONObject.NULL);
                }
                array.put(obj);
            }
            prefs.edit().putString(KEY_PREFIX + chatId, array.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public ArrayList<MessageModel> loadMessages(String chatId) {
        if (!cachingEnabled) return new ArrayList<>();
        ArrayList<MessageModel> list = new ArrayList<>();
        String json = prefs.getString(KEY_PREFIX + chatId, null);
        if (json == null) return list;

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                MessageModel msg = new MessageModel(
                        obj.optString("text", ""),
                        "",
                        obj.optLong("timestamp", 0),
                        obj.optBoolean("sentByMe", false)
                );
                String messageId = null;
                if (!obj.isNull("messageId")) {
                    messageId = obj.optString("messageId", null);
                    if ("null".equals(messageId) || messageId.trim().isEmpty()) {
                        messageId = null;
                    }
                }
                msg.setMessageId(messageId);
                list.add(msg);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public void clearChat(String chatId) {
        prefs.edit().remove(KEY_PREFIX + chatId).apply();
    }
}
