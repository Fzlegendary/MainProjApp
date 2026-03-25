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

    public ChatModel(String chatId, String username, String nickname, String lastMessage, String time,
                     String bio, String lastSeen, boolean online) {
        this.chatId      = chatId;
        this.username    = username;
        this.nickname    = nickname;
        this.lastMessage = lastMessage;
        this.time        = time;
        this.bio         = bio;
        this.lastSeen    = lastSeen;
        this.online      = online;
    }

    public String getChatId()     { return chatId; }
    public String getUsername()    { return username; }
    public String getNickname()    { return nickname; }
    public String getLastMessage() { return lastMessage; }
    public String getTime()        { return time; }
    public String getBio()         { return bio; }
    public String getLastSeen()    { return lastSeen; }
    public boolean isOnline()      { return online; }
}