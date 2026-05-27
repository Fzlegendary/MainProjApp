package com.example.mainproj.messaging;

public class MessageModel {

    private final String  text;
    private final String  senderUsername;
    private final String  timestamp;
    private final long    timestampLong;
    private final boolean sentByMe;
    private String        messageId;
    private String        imageUrl;

    public MessageModel(String text, String senderUsername, String timestamp, long timestampLong, boolean sentByMe) {
        this.text           = text;
        this.senderUsername = senderUsername;
        this.timestamp      = timestamp;
        this.timestampLong  = timestampLong;
        this.sentByMe       = sentByMe;
    }

    public String  getText()           { return text; }
    public String  getSenderUsername() { return senderUsername; }
    public String  getTimestamp()      { return timestamp; }
    public long    getTimestampLong()  { return timestampLong; }
    public boolean isSentByMe()        { return sentByMe; }
    public String  getMessageId()      { return messageId; }
    public void    setMessageId(String messageId) { this.messageId = messageId; }
    public String  getImageUrl()       { return imageUrl; }
    public void    setImageUrl(String imageUrl)   { this.imageUrl = imageUrl; }
}