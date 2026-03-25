package com.example.mainproj.messaging;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChatCacheManager {

    private static final String PREFS_NAME = "message_cache";
    private static final String KEY_PREFIX = "messages_";

    private static ChatCacheManager instance;
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

    public void saveMessages(String chatId, ArrayList<MessageModel> messages) {
        try {
            JSONArray array = new JSONArray();
            for (MessageModel msg : messages) {
                JSONObject obj = new JSONObject();
                obj.put("text", msg.getText());
                obj.put("senderUsername", msg.getSenderUsername());
                obj.put("timestamp", msg.getTimestamp());
                obj.put("timestampLong", msg.getTimestampLong());
                obj.put("sentByMe", msg.isSentByMe());
                array.put(obj);
            }
            prefs.edit().putString(KEY_PREFIX + chatId, array.toString()).apply();
        } catch (Exception ignored) {}
    }

    public ArrayList<MessageModel> loadMessages(String chatId) {
        ArrayList<MessageModel> list = new ArrayList<>();
        String json = prefs.getString(KEY_PREFIX + chatId, null);
        if (json == null) return list;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new MessageModel(
                        obj.optString("text"),
                        obj.optString("senderUsername"),
                        obj.optString("timestamp"),
                        obj.optLong("timestampLong", 0),
                        obj.optBoolean("sentByMe", false)
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public void clearChat(String chatId) {
        prefs.edit().remove(KEY_PREFIX + chatId).apply();
    }
}