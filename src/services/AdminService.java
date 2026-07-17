package services;

import models.Group;
import models.GroupMember;
import models.ReportedMessage;
import models.User;
import repository.GroupRepository;
import repository.ReportedMessageRepository;
import repository.UserRepository;
import security.PasswordHasher;
import security.PasswordValidator;
import java.util.*;
import repository.ChatRepository;
import utils.IdGenerator;

// عملیات مدیریتی ادمین
public class AdminService {

    private final UserRepository userrepo;
    private final GroupRepository grouprepo;
    private final ReportedMessageRepository reportedmessagerepo;
    private final GroupService groupserv;
    private final ChatRepository chatrepo;

    public AdminService(UserRepository userRepository,
            GroupRepository groupRepository,
            ReportedMessageRepository reportedMessageRepository,
            GroupService groupService,
            ChatRepository chatRepository) {
        this.userrepo = userRepository;
        this.grouprepo = groupRepository;
        this.reportedmessagerepo = reportedMessageRepository;
        this.groupserv = groupService;
        this.chatrepo = chatRepository;
    }

    // افزودن کاربر جدید
    public User addUser(String userId, String username, String plainPassword) {
        if (userrepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken.");
        }
        if (userrepo.findById(userId).isPresent()) {
            throw new IllegalArgumentException("User ID already taken.");
        }
        PasswordValidator.ValidationResult res = PasswordValidator.validate(plainPassword, username);
        if (!res.isValid()) {
            throw new IllegalArgumentException(res.getErrorsSummary());
        }
        User user = new User(userId, username, PasswordHasher.hash(plainPassword));
        userrepo.save(user);
        return user;
    }

    // لیست تمام کاربران ریترن میشه
    public List<User> getAllUsers() {
        return userrepo.findAll();
    }

    // کاربر توسط ادمین حذف میشه
    public void deleteUser(String userId) {
        Optional<User> optuser = userrepo.findById(userId);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        user.setDeleted(true);
        user.setOnline(false);
        userrepo.update(user);
    }

    // ویرایش کاربر توسط ادمین
    public User editUser(String userId, String newUsername, String newUserId) {
        Optional<User> optuser = userrepo.findById(userId);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();

        if (newUsername != null && !newUsername.isBlank()) {
            Optional<User> sameUsername = userrepo.findByUsername(newUsername);
            if (sameUsername.isPresent() && !sameUsername.get().getId().equals(userId)) {
                throw new IllegalArgumentException("Username already taken.");
            }
            user.setUsername(newUsername);
        }

        if (newUserId != null && !newUserId.isBlank() && !newUserId.equals(userId)) {
            if (userrepo.findById(newUserId).isPresent()) {
                throw new IllegalArgumentException("User ID already taken.");
            }
            user.setId(newUserId);
        }

        userrepo.update(user);
        return user;
    }

    // ساخت گروه توسط ادمین
    public Group createGroup(String name, String ownerId) {
        String idgroup = IdGenerator.generate();
        return groupserv.createGroup(idgroup, name, ownerId);
    }

    // لیست اعضای گروه
    public List<GroupMember> getGroupMembers(String groupId) {
        return grouprepo.findMembersByGroupId(groupId);
    }

    // لیست تمام گروه‌ها
    public List<Group> getAllGroups() {
        return grouprepo.findAll();
    }

    // حذف کاربر از گروه
    public void removeUserFromGroup(String groupId, String userId) {
        if (grouprepo.findMember(groupId, userId).isEmpty()) {
            throw new IllegalArgumentException("User is not a member of this group.");
        }
        grouprepo.deleteMember(groupId, userId);
    }

    // حذف گروه توسط ادمین
    public void deleteGroup(String groupId) {
        Optional<Group> optgroup = grouprepo.findById(groupId);
        if (optgroup.isEmpty()) {
            throw new IllegalArgumentException("Group not found.");
        }
        Group group = optgroup.get();
        grouprepo.delete(groupId);
        chatrepo.delete(group.getChatId());
    }

    // ویرایش گروه توسط ادمین
    public Group editGroup(String groupId, String newName, String newDescription) {
        Optional<Group> optgroup = grouprepo.findById(groupId);
        if (optgroup.isEmpty()) {
            throw new IllegalArgumentException("Group not found.");
        }
        Group group = optgroup.get();
        if (newDescription != null) {
            group.setDescription(newDescription);
        }
        if (newName != null && !newName.isBlank()) {
            group.setName(newName);
        }
        grouprepo.update(group);
        return group;
    }

    // افزودن کاربر به گروه
    public void addUserToGroup(String groupId, String userId) {
        if (grouprepo.findById(groupId).isEmpty()) {
            throw new IllegalArgumentException("Group not found.");
        }
        if (grouprepo.findMember(groupId, userId).isPresent()) {
            throw new IllegalArgumentException("User is already a member.");
        }
        if (userrepo.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        GroupMember member = new GroupMember(groupId, userId, GroupMember.Role.MEMBER);
        grouprepo.saveMember(member);
    }

    // لیست پیام‌های گزارش‌شده
    public List<ReportedMessage> getReportedMessages() {
        return reportedmessagerepo.findAll();
    }

    // رد کردن یک گزارش
    public void dismissReport(String reportId) {
        if (reportedmessagerepo.findById(reportId).isEmpty()) {
            throw new IllegalArgumentException("Report not found.");
        }
        reportedmessagerepo.delete(reportId);
    }
}