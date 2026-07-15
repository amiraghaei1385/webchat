package models;

import java.time.LocalDateTime;

// مدل مدیا پیام
public class MediaMessage {

    public enum MediaType {
        IMAGE,
        VIDEO,
        AUDIO,
        VOICE,
        DOCUMENT,
        STICKER;
    }

    private String idmessage;
    private String idchat;
    private String idsender;
    private MediaType mediatype;
    private String filepath;
    private String orgfilename;
    private String mimetype;
    private long filesize;
    private int durationseconds;
    private int widthpx;
    private int heightpx;
    private String caption;
    private String thumbnailpath;
    private LocalDateTime sentat;
    private boolean isdeleted;

    // کانسراکتور
    public MediaMessage() {
    }

    public MediaMessage(String idmessage, String idchat, String idsender,
            MediaType mediatype, String filepath,
            String originalfilename, String mimetype,
            long filesizebytes) {
        this.idmessage = idmessage;
        this.idchat = idchat;
        this.idsender = idsender;
        this.mediatype = mediatype;
        this.filepath = filepath;
        this.orgfilename = originalfilename;
        this.mimetype = mimetype;
        this.filesize = filesizebytes;
        this.sentat = LocalDateTime.now();
        this.isdeleted = false;
    }

    // بررسی اینکه مدیا دارای مدت زمان است یا خیر
    public boolean hasDuration() {
        return mediatype == MediaType.AUDIO
                || mediatype == MediaType.VIDEO
                || mediatype == MediaType.VOICE;
    }

    // بررسی اینکه مدیا دارای ابعاد تصویری است یا خیر
    public boolean hasVisualDimensions() {
        return mediatype == MediaType.IMAGE
                || mediatype == MediaType.VIDEO
                || mediatype == MediaType.STICKER;
    }

    // بررسی وجود تصویر بندانگشتی
    public boolean hasThumbnail() {
        return thumbnailpath != null && !thumbnailpath.isBlank();
    }

    // Getters

    public String getMessageId() {
        return idmessage;
    }

    public String getChatId() {
        return idchat;
    }

    public String getSenderId() {
        return idsender;
    }

    public MediaType getMediaType() {
        return mediatype;
    }

    public String getFilePath() {
        return filepath;
    }

    public String getOriginalFileName() {
        return orgfilename;
    }

    public String getMimeType() {
        return mimetype;
    }

    public long getFileSizeBytes() {
        return filesize;
    }

    public int getDurationSeconds() {
        return durationseconds;
    }

    public int getWidthPx() {
        return widthpx;
    }

    public int getHeightPx() {
        return heightpx;
    }

    public String getCaption() {
        return caption;
    }

    public String getThumbnailPath() {
        return thumbnailpath;
    }

    public LocalDateTime getSentAt() {
        return sentat;
    }

    public boolean isDeleted() {
        return isdeleted;
    }

    // Setters

    public void setMessageId(String idmessage) {
        this.idmessage = idmessage;
    }

    public void setChatId(String idchat) {
        this.idchat = idchat;
    }

    public void setSenderId(String idsender) {
        this.idsender = idsender;
    }

    public void setMediaType(MediaType mediatype) {
        this.mediatype = mediatype;
    }

    public void setFilePath(String filepath) {
        this.filepath = filepath;
    }

    public void setOriginalFileName(String originalfilename) {
        this.orgfilename = originalfilename;
    }

    public void setMimeType(String mimetype) {
        this.mimetype = mimetype;
    }

    public void setFileSizeBytes(long filesizebytes) {
        this.filesize = filesizebytes;
    }

    public void setDurationSeconds(int durationseconds) {
        this.durationseconds = durationseconds;
    }

    public void setWidthPx(int widthpx) {
        this.widthpx = widthpx;
    }

    public void setHeightPx(int heightpx) {
        this.heightpx = heightpx;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setThumbnailPath(String thumbnailpath) {
        this.thumbnailpath = thumbnailpath;
    }

    public void setSentAt(LocalDateTime sentat) {
        this.sentat = sentat;
    }

    public void setDeleted(boolean deleted) {
        this.isdeleted = deleted;
    }

    @Override
    public String toString() {
        return "MediaMessage{messageId='" + idmessage
                + "', mediaType=" + mediatype
                + "', mimeType='" + mimetype
                + "', fileSizeBytes=" + filesize
                + ", isDeleted=" + isdeleted + "}";
    }
}