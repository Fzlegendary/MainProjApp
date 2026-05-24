package com.example.mainproj.messaging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatModel {

    private String chatId;
    private String username;
    private String nickname;
    private String lastMessage;
    private String bio;
    private boolean online;
    private String imageUrl;
    private Long unreadCount;
    private Long createdAt;
    private Long lastSeen;
    private Long timestamp;

    public ChatModel() {
        this.chatId = "";
        this.username = "";
        this.nickname = "";
        this.lastMessage = "";
        this.bio = "";
        this.online = false;
        this.imageUrl = "";
        this.unreadCount = 0L;
        this.createdAt = null;
        this.lastSeen = null;
        this.timestamp = null;
    }

    public ChatModel(String chatId, String username, String nickname, String lastMessage,
                     Long timestamp, String bio, Long lastSeen, boolean online,
                     String imageUrl, Long unreadCount) {
        this.chatId      = chatId;
        this.username    = username;
        this.nickname    = nickname;
        this.lastMessage = lastMessage;
        this.timestamp   = timestamp;
        this.bio         = bio;
        this.lastSeen    = lastSeen;
        this.online      = online;
        this.imageUrl    = imageUrl;
        this.unreadCount = unreadCount;
    }

    public String getTime() {
        if (timestamp == null || timestamp == 0) return "";
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    public String getLastSeenFormatted() {
        if (lastSeen == null || lastSeen == 0) return "";
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(lastSeen));
    }

    public String getChatId()        { return chatId; }
    public String getUsername()      { return username; }
    public String getNickname()      { return nickname; }
    public String getLastMessage()   { return lastMessage; }
    public String getBio()           { return bio; }
    public boolean isOnline()        { return online; }
    public String getImageUrl()      { return imageUrl; }
    public Long getUnreadCount()     { return unreadCount; }
    public Long getCreatedAt()       { return createdAt; }
    public Long getLastSeen()        { return lastSeen; }
    public Long getTimestamp()       { return timestamp; }

    public void setChatId(String chatId)              { this.chatId = chatId; }
    public void setUsername(String username)           { this.username = username; }
    public void setNickname(String nickname)           { this.nickname = nickname; }
    public void setLastMessage(String lastMessage)     { this.lastMessage = lastMessage; }
    public void setBio(String bio)                     { this.bio = bio; }
    public void setOnline(boolean online)              { this.online = online; }
    public void setImageUrl(String imageUrl)           { this.imageUrl = imageUrl; }
    public void setUnreadCount(Long unreadCount)       { this.unreadCount = unreadCount; }
    public void setCreatedAt(Long createdAt)           { this.createdAt = createdAt; }
    public void setLastSeen(Long lastSeen)             { this.lastSeen = lastSeen; }
    public void setTimestamp(Long timestamp)           { this.timestamp = timestamp; }
}
