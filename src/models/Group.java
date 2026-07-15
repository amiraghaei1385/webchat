package models;

import java.time.LocalDateTime;

// مدل گروپ
public class Group {

    private String id;
    private String idchat;
    private String name;
    private String description;
    private String picturepath;
    private String idowner;
    private LocalDateTime createat;

    public Group() {
    }

    public Group(String id, String idchat, String name, String idowner) {
        this.id = id;
        this.idchat = idchat;
        this.name = name;
        this.idowner = idowner;
        this.createat = LocalDateTime.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getChatId() {
        return idchat;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPicturePath() {
        return picturepath;
    }

    public String getOwnerId() {
        return idowner;
    }

    public LocalDateTime getCreatedAt() {
        return createat;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setChatId(String idchat) {
        this.idchat = idchat;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public void setPicturePath(String path) {
        this.picturepath = path;
    }

    public void setOwnerId(String idowner) {
        this.idowner = idowner;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createat = t;
    }

    @Override
    public String toString() {
        return "Group{id='" + id + "', name='" + name + "', ownerId='" + idowner + "'}";
    }
}