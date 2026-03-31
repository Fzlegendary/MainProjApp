package com.example.mainproj.messaging;

public class ChatModel {

    private String chatId;
    private String username;
    private String nickname;
    private String lastMessage;
    private String time;
    private String bio;
    private String lastSeen;
    private boolean online;
    private int unreadCount;

    public ChatModel(String chatId, String username, String nickname, String lastMessage, String time,
                     String bio, String lastSeen, boolean online) {
        this(chatId, username, nickname, lastMessage, time, bio, lastSeen, online, 0);
    }

    public ChatModel(String chatId, String username, String nickname, String lastMessage, String time,
                     String bio, String lastSeen, boolean online, int unreadCount) {
        this.chatId      = chatId;
        this.username    = username;
        this.nickname    = nickname;
        this.lastMessage = lastMessage;
        this.time        = time;
        this.bio         = bio;
        this.lastSeen    = lastSeen;
        this.online      = online;
        this.unreadCount = unreadCount;
    }

    public String getChatId()     { return chatId; }
    public String getUsername()    { return username; }
    public String getNickname()    { return nickname; }
    public String getLastMessage() { return lastMessage; }
    public String getTime()        { return time; }
    public String getBio()         { return bio; }
    public String getLastSeen()    { return lastSeen; }
    public boolean isOnline()      { return online; }
    public int getUnreadCount()    { return unreadCount; }

    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setTime(String time) { this.time = time; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public void setOnline(boolean online) { this.online = online; }
}