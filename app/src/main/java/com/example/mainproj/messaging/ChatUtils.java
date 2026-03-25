package com.example.mainproj.messaging;

public class ChatUtils {

    public static String generateChatId(String username1, String username2) {
        if (username1 == null || username2 == null) {
            return null;
        }
        if (username1.compareTo(username2) < 0) {
            return username1 + "_" + username2;
        } else {
            return username2 + "_" + username1;
        }
    }
}
