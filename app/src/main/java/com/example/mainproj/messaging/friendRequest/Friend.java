package com.example.mainproj.messaging.friendRequest;

public class Friend {
    private String username;
    private String safeEmail;
    private String nickname;
    private String imageUrl;
    private String chatId;
    private long addedAt;

    public Friend() {}

    public Friend(String username, String safeEmail, String nickname, String imageUrl, String chatId, long addedAt) {
        this.username = username;
        this.safeEmail = safeEmail;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.chatId = chatId;
        this.addedAt = addedAt;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSafeEmail() { return safeEmail; }
    public void setSafeEmail(String safeEmail) { this.safeEmail = safeEmail; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public long getAddedAt() { return addedAt; }
    public void setAddedAt(long addedAt) { this.addedAt = addedAt; }
}
