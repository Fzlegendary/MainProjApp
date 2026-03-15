package com.example.mainproj.messaging;

public class ChatModel {

    private String username;
    private String lastMessage;
    private String time;
    private String bio;         // added — shown in profile sheet
    private String lastSeen;    // added — shown when user is offline
    private boolean online;     // added — true = green dot, false = show lastSeen

    // Original constructor — still works, bio/lastSeen/online default to empty/false
    public ChatModel(String username, String lastMessage, String time) {
        this.username    = username;
        this.lastMessage = lastMessage;
        this.time        = time;
        this.bio         = "";
        this.lastSeen    = "";
        this.online      = false;
    }

    // Full constructor — use this when you have all the data
    public ChatModel(String username, String lastMessage, String time,
                     String bio, String lastSeen, boolean online) {
        this.username    = username;
        this.lastMessage = lastMessage;
        this.time        = time;
        this.bio         = bio;
        this.lastSeen    = lastSeen;
        this.online      = online;
    }

    // Original getters — unchanged
    public String getUsername()    { return username; }
    public String getLastMessage() { return lastMessage; }
    public String getTime()        { return time; }

    // New getters — needed by the profile bottom sheet
    public String getBio()      { return bio; }
    public String getLastSeen() { return lastSeen; }
    public boolean isOnline()   { return online; }
}