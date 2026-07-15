package models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// مدل استوری
public class Story {

    public enum MediaType {
        IMAGE, VIDEO, TEXT
    }

    private String id;
    private String idowner;
    private MediaType mediatype;
    private String filepath;
    private String caption;
    private LocalDateTime createat;
    private LocalDateTime expiresat;
    private boolean isdeleted;
    private Set<String> idviewer;

    public Story() {
        this.idviewer = new HashSet<>();
    }

    public Story(String id, String idowner, MediaType mediatype, String filepath, String caption) {
        this.id = id;
        this.idowner = idowner;
        this.mediatype = mediatype;
        this.filepath = filepath;
        this.caption = caption;
        this.createat = LocalDateTime.now();
        this.expiresat = this.createat.plusHours(24);
        this.isdeleted = false;
        this.idviewer = new HashSet<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return idowner;
    }

    public MediaType getMediaType() {
        return mediatype;
    }

    public String getFilePath() {
        return filepath;
    }

    public String getCaption() {
        return caption;
    }

    public LocalDateTime getCreatedAt() {
        return createat;
    }

    public LocalDateTime getExpiresAt() {
        return expiresat;
    }

    public boolean isDeleted() {
        return isdeleted;
    }

    public Set<String> getViewerIds() {
        return idviewer;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setOwnerId(String idowner) {
        this.idowner = idowner;
    }

    public void setMediaType(MediaType mediatype) {
        this.mediatype = mediatype;
    }

    public void setFilePath(String filepath) {
        this.filepath = filepath;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setCreatedAt(LocalDateTime createat) {
        this.createat = createat;
    }

    public void setExpiresAt(LocalDateTime expiresat) {
        this.expiresat = expiresat;
    }

    public void setDeleted(boolean deleted) {
        this.isdeleted = deleted;
    }

    public void setViewerIds(Set<String> idviewer) {
        this.idviewer = idviewer;
    }

    // آیا استوری منقضی شده است؟
    public boolean isExpired() {
        return expiresat != null && LocalDateTime.now().isAfter(expiresat);
    }

    // ثبت بازدید یک کاربر
    public void addViewer(String iduser) {
        if (this.idviewer == null) {
            this.idviewer = new HashSet<>();
        }
        this.idviewer.add(iduser);
    }

    @Override
    public String toString() {
        return "Story{id='" + id + "', ownerId='" + idowner + "', mediaType=" + mediatype
                + ", expiresAt=" + expiresat + ", isDeleted=" + isdeleted + "}";
    }
}