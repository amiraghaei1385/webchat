package models;

import java.time.LocalDateTime;

// مدل ممبر گروپ
public class GroupMember {
    public enum Role {
        OWNER,
        ADMIN,
        MEMBER
    }

    private String idgroup;
    private String iduser;
    private Role role;
    private LocalDateTime joinat;

    public GroupMember() {
    }

    public GroupMember(String idgroup, String iduser, Role role) {
        this.idgroup = idgroup;
        this.iduser = iduser;
        this.role = role;
        this.joinat = LocalDateTime.now();
    }

    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.OWNER;
    }

    // Getters
    public String getGroupId() {
        return idgroup;
    }

    public String getUserId() {
        return iduser;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getJoinedAt() {
        return joinat;
    }

    // Setters
    public void setGroupId(String idgroup) {
        this.idgroup = idgroup;
    }

    public void setUserId(String iduser) {
        this.iduser = iduser;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setJoinedAt(LocalDateTime t) {
        this.joinat = t;
    }

    @Override
    public String toString() {
        return "GroupMember{groupId='" + idgroup + "', userId='" + iduser + "', role=" + role + "}";
    }
}