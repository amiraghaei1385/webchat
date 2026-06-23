package models;

import java.time.LocalDateTime;

// Represents the membership of a user in a group,
// including their role (admin or regular member).

public class GroupMember {
    // Role of the member inside a group.
    public enum Role {
        OWNER,
        ADMIN,
        MEMBER
    }

    private String groupId;
    private String userId;
    private Role role;
    private LocalDateTime joinedAt;

    public GroupMember() {
    }

    public GroupMember(String groupId, String userId, Role role) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    // Returns true if this member has admin-level privileges.
    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.OWNER;
    }

    // Getters
    public String getGroupId() {
        return groupId;
    }

    public String getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    // Setters
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setJoinedAt(LocalDateTime t) {
        this.joinedAt = t;
    }

    @Override
    public String toString() {
        return "GroupMember{groupId='" + groupId + "', userId='" + userId + "', role=" + role + "}";
    }
}