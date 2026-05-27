package com.example.mainproj.messaging;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.HashSet;
import java.util.Set;

public class MutedChatManager {

    private static final String PREFS_NAME = "muted_chats_prefs";
    private static final String MUTED_CHATS_KEY = "muted_chat_ids";

    private static MutedChatManager instance;
    private final SharedPreferences prefs;
    private final Set<String> mutedChatIds = new HashSet<>();

    private MutedChatManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadMutedChats();
    }

    public static synchronized MutedChatManager getInstance(Context context) {
        if (instance == null) {
            instance = new MutedChatManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadMutedChats() {
        mutedChatIds.clear();
        String json = prefs.getString(MUTED_CHATS_KEY, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                mutedChatIds.add(array.getString(i));
            }
        } catch (Exception ignored) {}
    }

    public void muteChat(String chatId) {
        mutedChatIds.add(chatId);
        saveMutedChats();
    }

    public void unmuteChat(String chatId) {
        mutedChatIds.remove(chatId);
        saveMutedChats();
    }

    public boolean isMuted(String chatId) {
        return mutedChatIds.contains(chatId);
    }

    public Set<String> getMutedChatIds() {
        return new HashSet<>(mutedChatIds);
    }

    public void clearAll() {
        mutedChatIds.clear();
        saveMutedChats();
    }

    private void saveMutedChats() {
        try {
            JSONArray array = new JSONArray();
            for (String chatId : mutedChatIds) {
                array.put(chatId);
            }
            prefs.edit().putString(MUTED_CHATS_KEY, array.toString()).apply();
        } catch (Exception ignored) {}
    }
}