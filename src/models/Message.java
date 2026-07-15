package models;

import java.time.LocalDateTime;

// مدل میسیج
public class Message {

    private String id;
    private String idchat;
    private String idsender;
    private String encryptedcontent;
    private LocalDateTime sentat;
    private LocalDateTime editedat;
    private boolean isdeleted;
    private String idreplytomessage;
    private boolean hasmedia;
    private String idmediamessage;
    private String idforwardedfrommessage;

    public Message() {
    }

    public Message(String id, String idchat, String idsender, String encryptedcontent) {
        this.id = id;
        this.idchat = idchat;
        this.idsender = idsender;
        this.encryptedcontent = encryptedcontent;
        this.sentat = LocalDateTime.now();
        this.isdeleted = false;
        this.hasmedia = false;
    }

    public boolean isEdited() {
        return editedat != null;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getChatId() {
        return idchat;
    }

    public String getSenderId() {
        return idsender;
    }

    public String getEncryptedContent() {
        return encryptedcontent;
    }

    public LocalDateTime getSentAt() {
        return sentat;
    }

    public LocalDateTime getEditedAt() {
        return editedat;
    }

    public boolean isDeleted() {
        return isdeleted;
    }

    public String getReplyToMessageId() {
        return idreplytomessage;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setChatId(String idchat) {
        this.idchat = idchat;
    }

    public void setSenderId(String idsender) {
        this.idsender = idsender;
    }

    public void setEncryptedContent(String content) {
        this.encryptedcontent = content;
    }

    public void setSentAt(LocalDateTime t) {
        this.sentat = t;
    }

    public void setEditedAt(LocalDateTime t) {
        this.editedat = t;
    }

    public void setDeleted(boolean deleted) {
        this.isdeleted = deleted;
    }

    public void setReplyToMessageId(String id) {
        this.idreplytomessage = id;
    }

    public boolean isHasMedia() {
        return hasmedia;
    }

    public String getMediaMessageId() {
        return idmediamessage;
    }

    public String getForwardedFromMessageId() {
        return idforwardedfrommessage;
    }

    public void setHasMedia(boolean hasmedia) {
        this.hasmedia = hasmedia;
    }

    public void setMediaMessageId(String idmediamessage) {
        this.idmediamessage = idmediamessage;
    }

    public void setForwardedFromMessageId(String id) {
        this.idforwardedfrommessage = id;
    }

    @Override
    public String toString() {
        return "Message{id='" + id + "', chatId='" + idchat + "', senderId='" + idsender + "', isDeleted=" + isdeleted
                + "}";
    }
}