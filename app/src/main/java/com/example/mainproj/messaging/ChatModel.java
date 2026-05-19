package com.example.mainproj.messaging;

public class ChatModel {
    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }

    private String chatId;
    private String username;
    private String nickname;
    private String lastMessage;
    private String time;
    private String bio;
    private String lastSeen;
    private boolean online;
    private String imageUrl;
    private int unreadCount;

    public ChatModel() {
        this.chatId = "";
        this.username = "";
        this.nickname = "";
        this.lastMessage = "";
        this.time = "";
        this.bio = "";
        this.lastSeen = "";
        this.online = false;
        this.imageUrl = "";
        this.unreadCount = 0;
    }

    public ChatModel(String chatId, String username, String nickname, String lastMessage, String time,
                     String bio, String lastSeen, boolean online, String imageUrl, int unreadCount) {
        this.chatId      = chatId;
        this.username    = username;
        this.nickname    = nickname;
        this.lastMessage = lastMessage;
        this.time        = time;
        this.bio         = bio;
        this.lastSeen    = lastSeen;
        this.online      = online;
        this.imageUrl    = imageUrl;
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

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}