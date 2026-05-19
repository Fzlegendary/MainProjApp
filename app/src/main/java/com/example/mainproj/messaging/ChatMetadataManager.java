package com.example.mainproj.messaging;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Manages persistent storage of chat metadata in SharedPreferences.
 * Stores each chat's data (username, nickname, lastMessage, time, bio,
 * lastSeen, online status, imageUrl, unreadCount) as a JSON string
 * keyed by "{userUid}_{chatId}" to avoid collisions.
 */
public class ChatMetadataManager {

    private static final String PREFS_NAME = "chat_metadata";

    private static final char KEY_SEPARATOR = '_';

    private static ChatMetadataManager instance;

    private final SharedPreferences prefs;

    private ChatMetadataManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ChatMetadataManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChatMetadataManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveChat(String userUid, ChatModel chat) {
        if (chat == null || chat.getChatId() == null) return;

        String key = buildKey(userUid, chat.getChatId());
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", chat.getUsername() != null ? chat.getUsername() : "");
            jsonObject.put("nickname", chat.getNickname() != null ? chat.getNickname() : "");
            jsonObject.put("lastMessage", chat.getLastMessage() != null ? chat.getLastMessage() : "");
            jsonObject.put("time", chat.getTime() != null ? chat.getTime() : "");
            jsonObject.put("bio", chat.getBio() != null ? chat.getBio() : "");
            jsonObject.put("lastSeen", chat.getLastSeen() != null ? chat.getLastSeen() : "");
            jsonObject.put("online", chat.isOnline());
            jsonObject.put("imageUrl", chat.getImageUrl() != null ? chat.getImageUrl() : "");
            jsonObject.put("unreadCount", chat.getUnreadCount());
            prefs.edit().putString(key, jsonObject.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public ArrayList<ChatModel> loadAllChats(String userUid) {
        ArrayList<ChatModel> chatList = new ArrayList<>();
        String userPrefix = userUid + KEY_SEPARATOR;

        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(userPrefix)) {
                String chatId = extractChatId(key, userUid);
                ChatModel chat = loadChatFromKey(key, chatId);
                if (chat != null) {
                    chatList.add(chat);
                }
            }
        }
        return chatList;
    }

    public void clearChat(String userUid, String chatId) {
        if (chatId == null || userUid == null) return;
        String key = buildKey(userUid, chatId);
        prefs.edit().remove(key).apply();
    }

    public void clearAllForUser(String userUid) {
        if (userUid == null) return;
        String userPrefix = userUid + KEY_SEPARATOR;
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(userPrefix)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    private String buildKey(String userUid, String chatId) {
        return userUid + KEY_SEPARATOR + chatId;
    }

    private String extractChatId(String key, String userUid) {
        int separatorIndex = key.indexOf(KEY_SEPARATOR);
        if (separatorIndex >= 0 && separatorIndex < key.length() - 1) {
            return key.substring(separatorIndex + 1);
        }
        return key;
    }

    private ChatModel loadChatFromKey(String key, String chatId) {
        String json = prefs.getString(key, null);
        if (json == null) return null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            ChatModel chat = new ChatModel();
            chat.setChatId(chatId);
            chat.setUsername(jsonObject.optString("username", ""));
            chat.setNickname(jsonObject.optString("nickname", ""));
            chat.setLastMessage(jsonObject.optString("lastMessage", ""));
            chat.setTime(jsonObject.optString("time", ""));
            chat.setBio(jsonObject.optString("bio", ""));
            chat.setLastSeen(jsonObject.optString("lastSeen", ""));
            chat.setOnline(jsonObject.optBoolean("online", false));
            chat.setImageUrl(jsonObject.optString("imageUrl", ""));
            chat.setUnreadCount(jsonObject.optInt("unreadCount", 0));
            return chat;
        } catch (Exception ignored) {
            return null;
        }
    }
}