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
import java.util.List;

/**
 * عملیات مدیریتی که از طریق CLI ادمین انجام می‌شود.
 * فاز اول: مدیریت کاربران، گروه‌ها و مشاهده پیام‌های گزارش‌شده.
 */
public class AdminService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ReportedMessageRepository reportedMessageRepository;
    private final GroupService groupService;

    public AdminService(UserRepository userRepository,
            GroupRepository groupRepository,
            ReportedMessageRepository reportedMessageRepository,
            GroupService groupService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.reportedMessageRepository = reportedMessageRepository;
        this.groupService = groupService;
    }

    // مدیریت کاربران //

    // دریافت لیست تمام کاربران.

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * افزودن کاربر جدید توسط ادمین.
     * مقادیر ثبت‌نام به صورت دستی توسط ادمین وارد می‌شود.
     */
    public User addUser(String userId, String username, String plainPassword) {
        if (userRepository.findById(userId).isPresent()) {
            throw new IllegalArgumentException("User ID already taken.");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken.");
        }

        PasswordValidator.ValidationResult result = PasswordValidator.validate(plainPassword, username);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getErrorsSummary());
        }

        User user = new User(userId, username, PasswordHasher.hash(plainPassword));
        userRepository.save(user);
        return user;
    }

    // حذف کاربر توسط ادمین.

    public void deleteUser(String userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        userRepository.delete(userId);
    }

    // مدیریت گروه‌ها //

    // دریافت لیست تمام گروه‌ها.

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    // دریافت لیست اعضای یک گروه.

    public List<GroupMember> getGroupMembers(String groupId) {
        return groupRepository.findMembersByGroupId(groupId);
    }

    // ایجاد گروه جدید توسط ادمین.

    public Group createGroup(String name, String ownerId) {
        return groupService.createGroup(name, ownerId);
    }

    // حذف گروه توسط ادمین.

    public void deleteGroup(String groupId) {
        if (groupRepository.findById(groupId).isEmpty()) {
            throw new IllegalArgumentException("Group not found.");
        }
        groupRepository.delete(groupId);
    }

    // افزودن کاربر به گروه توسط ادمین (بدون نیاز به بررسی سطح دسترسی).

    public void addUserToGroup(String groupId, String userId) {
        if (groupRepository.findById(groupId).isEmpty()) {
            throw new IllegalArgumentException("Group not found.");
        }
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        if (groupRepository.findMember(groupId, userId).isPresent()) {
            throw new IllegalArgumentException("User is already a member.");
        }

        GroupMember member = new GroupMember(groupId, userId, GroupMember.Role.MEMBER);
        groupRepository.saveMember(member);
    }

    // حذف کاربر از گروه توسط ادمین (بدون نیاز به بررسی سطح دسترسی).

    public void removeUserFromGroup(String groupId, String userId) {
        if (groupRepository.findMember(groupId, userId).isEmpty()) {
            throw new IllegalArgumentException("User is not a member of this group.");
        }
        groupRepository.deleteMember(groupId, userId);
    }

    // پیام‌های گزارش‌شده 

    // دریافت لیست تمام پیام‌های گزارش‌شده.
    // برای مشاهده در CLI ادمین استفاده می‌شود.

    public List<ReportedMessage> getReportedMessages() {
        return reportedMessageRepository.findAll();
    }

    // حذف یک گزارش پس از بررسی توسط ادمین.

    public void dismissReport(String reportId) {
        if (reportedMessageRepository.findById(reportId).isEmpty()) {
            throw new IllegalArgumentException("Report not found.");
        }
        reportedMessageRepository.delete(reportId);
    }
}