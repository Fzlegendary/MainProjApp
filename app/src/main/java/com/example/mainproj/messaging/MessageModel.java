package com.example.mainproj.messaging;

public class MessageModel {

    private final String  text;
    private final String  senderUsername;
    private final long    timestamp;
    private final boolean sentByMe;
    private String        messageId;
    private String        imageUrl;

    public MessageModel(String text, String senderUsername, long timestamp, boolean sentByMe) {
        this.text           = text;
        this.senderUsername = senderUsername;
        this.timestamp      = timestamp;
        this.sentByMe       = sentByMe;
    }

    public String  getText()           { return text; }
    public String  getSenderUsername() { return senderUsername; }
    public long    getTimestamp()      { return timestamp; }
    public boolean isSentByMe()        { return sentByMe; }
    public String  getMessageId()      { return messageId; }
    public void    setMessageId(String messageId) { this.messageId = messageId; }
    public String  getImageUrl()       { return imageUrl; }
    public void    setImageUrl(String imageUrl)   { this.imageUrl = imageUrl; }
}
