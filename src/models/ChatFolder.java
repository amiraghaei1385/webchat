package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// مدل فولدر

public class ChatFolder {

    private String id;
    private String idowner;
    private String name;
    private List<String> idchat;
    private int orderidx;
    private LocalDateTime createat;

    public ChatFolder() {
        this.idchat = new ArrayList<>();
    }

    public ChatFolder(String id, String idowner, String name) {
        this.id = id;
        this.idowner = idowner;
        this.name = name;
        this.idchat = new ArrayList<>();
        this.createat = LocalDateTime.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return idowner;
    }

    public String getName() {
        return name;
    }

    public List<String> getChatIds() {
        return idchat;
    }

    public int getOrderIndex() {
        return orderidx;
    }

    public LocalDateTime getCreatedAt() {
        return createat;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setOwnerId(String idowner) {
        this.idowner = idowner;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setChatIds(List<String> ids) {
        this.idchat = ids;
    }

    public void setOrderIndex(int index) {
        this.orderidx = index;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createat = t;
    }

    @Override
    public String toString() {
        return "ChatFolder{id='" + id + "', name='" + name + "', chats=" + idchat.size() + "}";
    }
}