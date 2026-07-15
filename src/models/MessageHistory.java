package models;

import java.time.LocalDateTime;

// مدل تاریخچه میسیج
public class MessageHistory {
    private String id;
    private String idmessage;
    private String idchat;
    private String ideditor;
    private String encryptedcontentbefore;
    private int version;
    private boolean isdeletion;
    private LocalDateTime editedat;

    // کانسراکتور
    public MessageHistory() {
    }

    public MessageHistory(String id,
            String idmessage,
            String idchat,
            String ideditor,
            String encryptedcontentbefore,
            int version,
            boolean isdeletion) {
        this.id = id;
        this.idmessage = idmessage;
        this.idchat = idchat;
        this.ideditor = ideditor;
        this.encryptedcontentbefore = encryptedcontentbefore;
        this.version = version;
        this.isdeletion = isdeletion;
        this.editedat = LocalDateTime.now();
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getMessageId() {
        return idmessage;
    }

    public String getChatId() {
        return idchat;
    }

    public String getEditorId() {
        return ideditor;
    }

    public String getEncryptedContentBefore() {
        return encryptedcontentbefore;
    }

    public int getVersion() {
        return version;
    }

    public boolean isDeletion() {
        return isdeletion;
    }

    public LocalDateTime getEditedAt() {
        return editedat;
    }

    // Setters

    public void setId(String id) {
        this.id = id;
    }

    public void setMessageId(String idmessage) {
        this.idmessage = idmessage;
    }

    public void setChatId(String idchat) {
        this.idchat = idchat;
    }

    public void setEditorId(String ideditor) {
        this.ideditor = ideditor;
    }

    public void setEncryptedContentBefore(String encryptedcontentbefore) {
        this.encryptedcontentbefore = encryptedcontentbefore;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setDeletion(boolean deletion) {
        this.isdeletion = deletion;
    }

    public void setEditedAt(LocalDateTime editedat) {
        this.editedat = editedat;
    }

    @Override
    public String toString() {
        return "MessageHistory{id='" + id
                + "', messageId='" + idmessage
                + "', editorId='" + ideditor
                + "', version=" + version
                + ", isDeletion=" + isdeletion
                + ", editedAt=" + editedat + "}";
    }
}