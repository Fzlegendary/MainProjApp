package com.example.mainproj.messaging;

/**
 * MessageModel
 * Represents a single message in a conversation.
 *
 * sentByMe = true  → rendered on the RIGHT (blue bubble, no avatar)
 * sentByMe = false → rendered on the LEFT  (white bubble + avatar, tap avatar → profile sheet)
 */
public class MessageModel {

    private final String  text;
    private final String  senderUsername;
    private final String  timestamp;     // display string e.g. "14:32"
    private final boolean sentByMe;      // determined at load time by comparing senderUsername with current user

    public MessageModel(String text, String senderUsername, String timestamp, boolean sentByMe) {
        this.text           = text;
        this.senderUsername = senderUsername;
        this.timestamp      = timestamp;
        this.sentByMe       = sentByMe;
    }

    public String  getText()           { return text; }
    public String  getSenderUsername() { return senderUsername; }
    public String  getTimestamp()      { return timestamp; }
    public boolean isSentByMe()        { return sentByMe; }
}
