package models;

import java.time.LocalDateTime;

// ری اکشن
public class Reaction {

    private String id;
    private String idmessage;
    private String iduser;
    private String emoji;
    private LocalDateTime reactedat;

    public Reaction() {
    }

    public Reaction(String id, String idmessage, String iduser, String emoji) {
        this.id = id;
        this.idmessage = idmessage;
        this.iduser = iduser;
        this.emoji = emoji;
        this.reactedat = LocalDateTime.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getMessageId() {
        return idmessage;
    }

    public String getUserId() {
        return iduser;
    }

    public String getEmoji() {
        return emoji;
    }

    public LocalDateTime getReactedAt() {
        return reactedat;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setMessageId(String idmessage) {
        this.idmessage = idmessage;
    }

    public void setUserId(String iduser) {
        this.iduser = iduser;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public void setReactedAt(LocalDateTime t) {
        this.reactedat = t;
    }

    @Override
    public String toString() {
        return "Reaction{messageId='" + idmessage + "', userId='" + iduser + "', emoji='" + emoji + "'}";
    }
}