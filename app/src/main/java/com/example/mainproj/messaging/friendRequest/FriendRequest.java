package com.example.mainproj.messaging.friendRequest;

public class FriendRequest {
    private String key;
    private String from;
    private String fromUsername;
    private long timestamp;
    private String status;

    public FriendRequest() {}

    public FriendRequest(String from, String fromUsername, long timestamp, String status) {
        this.from = from;
        this.fromUsername = fromUsername;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}