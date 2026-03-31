package com.example.mainproj.messaging;

/**
 * MessageModel
 * Represents a single message in a conversation.
 *
 * sentByMe = true  → rendered on the RIGHT (blue bubble, no avatar)
 * sentByMe = false → rendered on the LEFT  (white bubble + avatar, tap avatar → profile sheet)
 * 
 * isDateSeparator = true → rendered as a centered date pill between messages
 */
public class MessageModel {

    private final String  text;
    private final String  senderUsername;
    private final String  timestamp;
    private final long    timestampLong;
    private final boolean sentByMe;
    private final boolean isDateSeparator;
    private final String  dateLabel;

    public MessageModel(String text, String senderUsername, String timestamp, long timestampLong, boolean sentByMe) {
        this(text, senderUsername, timestamp, timestampLong, sentByMe, false, null);
    }

    public MessageModel(String text, String senderUsername, String timestamp, long timestampLong, boolean sentByMe, boolean isDateSeparator, String dateLabel) {
        this.text            = text;
        this.senderUsername  = senderUsername;
        this.timestamp       = timestamp;
        this.timestampLong   = timestampLong;
        this.sentByMe        = sentByMe;
        this.isDateSeparator = isDateSeparator;
        this.dateLabel       = dateLabel;
    }

    public static MessageModel createDateSeparator(String dateLabel, long timestampLong) {
        return new MessageModel(null, null, null, timestampLong, false, true, dateLabel);
    }

    public String  getText()            { return text; }
    public String  getSenderUsername()   { return senderUsername; }
    public String  getTimestamp()       { return timestamp; }
    public long    getTimestampLong()   { return timestampLong; }
    public boolean isSentByMe()         { return sentByMe; }
    public boolean isDateSeparator()    { return isDateSeparator; }
    public String  getDateLabel()       { return dateLabel; }
}