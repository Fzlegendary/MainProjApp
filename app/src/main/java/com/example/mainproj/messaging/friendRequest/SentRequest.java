package com.example.mainproj.messaging.friendRequest;

public class SentRequest {
    private String key;
    private String to;
    private String toUsername;
    private String toImageUrl;
    private long timestamp;
    private String status;

    public SentRequest() {}

    public SentRequest(String to, String toUsername, long timestamp, String status) {
        this.to = to;
        this.toUsername = toUsername;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }

    public String getToImageUrl() { return toImageUrl; }
    public void setToImageUrl(String toImageUrl) { this.toImageUrl = toImageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}