package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// مدل چت
public class Chat {
    private String id;
    private ChatType type;
    private List<String> idmember;
    private String idlastmessage;
    private LocalDateTime lastmessageat;
    private boolean isarchive;
    private boolean ispin;
    private boolean ismute;
    private String folderId;
    private LocalDateTime createat;
    private Map<String, LocalDateTime> lastreadat;

    public Chat() {
        this.idmember = new ArrayList<>();
        this.lastreadat = new HashMap<>();
    }

    public Chat(String id, ChatType type) {
        this.id = id;
        this.type = type;
        this.idmember = new ArrayList<>();
        this.isarchive = false;
        this.ispin = false;
        this.ismute = false;
        this.createat = LocalDateTime.now();
        this.lastreadat = new HashMap<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public ChatType getType() {
        return type;
    }

    public List<String> getMemberIds() {
        return idmember;
    }

    public String getLastMessageId() {
        return idlastmessage;
    }

    public LocalDateTime getLastMessageAt() {
        return lastmessageat;
    }

    public boolean isMuted() {
        return ismute;
    }

    public boolean isArchived() {
        return isarchive;
    }

    public boolean isPinned() {
        return ispin;
    }

    public LocalDateTime getCreatedAt() {
        return createat;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setType(ChatType type) {
        this.type = type;
    }

    public void setMemberIds(List<String> ids) {
        this.idmember = ids;
    }

    public void setLastMessageId(String id) {
        this.idlastmessage = id;
    }

    public void setLastMessageAt(LocalDateTime t) {
        this.lastmessageat = t;
    }

    public void setArchived(boolean archived) {
        this.isarchive = archived;
    }

    public void setMuted(boolean muted) {
        this.ismute = muted;
    }

    public void setPinned(boolean pinned) {
        this.ispin = pinned;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createat = t;
    }

    public void addMember(String userId) {
        if (!idmember.contains(userId)) {
            idmember.add(userId);
        }
    }

    public void removeMember(String userId) {
        idmember.remove(userId);
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public Map<String, LocalDateTime> getLastReadAt() {
        return lastreadat;
    }

    public void setLastReadAt(Map<String, LocalDateTime> lastReadAt) {
        this.lastreadat = lastReadAt;
    }

    public void markReadBy(String userId) {
        if (lastreadat == null) {
            lastreadat = new HashMap<>();
        }
        lastreadat.put(userId, LocalDateTime.now());
    }

    public LocalDateTime getLastReadAt(String userId) {
        return lastreadat == null ? null : lastreadat.get(userId);
    }

    @Override
    public String toString() {
        return "Chat{id='" + id + "', type=" + type + ", members=" + idmember.size() + "}";
    }
}