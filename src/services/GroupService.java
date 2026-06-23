package services;

import models.Chat;
import models.ChatType;
import models.Group;
import models.GroupMember;
import repository.ChatRepository;
import repository.GroupRepository;
import repository.UserRepository;
import utils.IdGenerator;
import java.util.List;
import java.util.Optional;

// مدیریت گروه‌ها و اعضای آن‌ها.
public class GroupService {

    private final GroupRepository groupRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
            ChatRepository chatRepository,
            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }

    // ایجاد گروه جدید.
    // سازنده گروه به عنوان OWNER اضافه می‌شود.
    public Group createGroup(String name, String ownerId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be empty.");
        }

        Chat chat = new Chat(IdGenerator.generate(), ChatType.GROUP);
        chat.addMember(ownerId);
        chatRepository.save(chat);

        Group group = new Group(IdGenerator.generate(), chat.getId(), name, ownerId);
        groupRepository.save(group);

        GroupMember owner = new GroupMember(group.getId(), ownerId, GroupMember.Role.OWNER);
        groupRepository.saveMember(owner);

        return group;
    }

    // دریافت گروه با آیدی.
    public Optional<Group> findById(String groupId) {
        return groupRepository.findById(groupId);
    }

    // دریافت گروه با آیدی چت مرتبط.
    public Optional<Group> findByChatId(String chatId) {
        return groupRepository.findByChatId(chatId);
    }

    // دریافت لیست اعضای یک گروه.
    public List<GroupMember> getMembers(String groupId) {
        return groupRepository.findMembersByGroupId(groupId);
    }

    // دریافت لیست تمام گروه‌ها (برای CLI ادمین).
    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    // افزودن عضو جدید به گروه.
    // فقط ادمین یا مالک گروه می‌تواند این کار را انجام دهد.
    public void addMember(String groupId, String requesterId, String newUserId) {
        // بررسی وجود کاربر در سیستم
        if (userRepository.findById(newUserId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }

        GroupMember requester = groupRepository.findMember(groupId, requesterId)
                .orElseThrow(() -> new IllegalStateException("You are not a member of this group."));

        if (!requester.isAdmin()) {
            throw new IllegalStateException("Only admins can add members.");
        }

        if (groupRepository.findMember(groupId, newUserId).isPresent()) {
            throw new IllegalArgumentException("User is already a member.");
        }

        GroupMember newMember = new GroupMember(groupId, newUserId, GroupMember.Role.MEMBER);
        groupRepository.saveMember(newMember);

        groupRepository.findById(groupId)
                .ifPresent(group -> chatRepository.findById(group.getChatId()).ifPresent(chat -> {
                    chat.addMember(newUserId);
                    chatRepository.update(chat);
                }));
    }

    // حذف عضو از گروه.
    // ادمین نمی‌تواند مالک یا ادمین دیگر را حذف کند.
    public void removeMember(String groupId, String requesterId, String targetUserId) {
        GroupMember requester = groupRepository.findMember(groupId, requesterId)
                .orElseThrow(() -> new IllegalStateException("You are not a member of this group."));

        if (!requester.isAdmin()) {
            throw new IllegalStateException("Only admins can remove members.");
        }

        GroupMember target = groupRepository.findMember(groupId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user is not a member."));

        // مالک را نمی‌توان اخراج کرد
        if (target.getRole() == GroupMember.Role.OWNER) {
            throw new IllegalStateException("Cannot remove the group owner.");
        }

        // فقط مالک می‌تواند ادمین را اخراج کند
        if (target.getRole() == GroupMember.Role.ADMIN
                && requester.getRole() != GroupMember.Role.OWNER) {
            throw new IllegalStateException("Only the owner can remove an admin.");
        }

        groupRepository.deleteMember(groupId, targetUserId);

        groupRepository.findById(groupId)
                .ifPresent(group -> chatRepository.findById(group.getChatId()).ifPresent(chat -> {
                    chat.removeMember(targetUserId);
                    chatRepository.update(chat);
                }));
    }

    // ترک گروه توسط کاربر.
    public void leaveGroup(String groupId, String userId) {
        GroupMember member = groupRepository.findMember(groupId, userId)
                .orElseThrow(() -> new IllegalStateException("You are not a member of this group."));

        if (member.getRole() == GroupMember.Role.OWNER) {
            throw new IllegalStateException("Owner cannot leave the group. Transfer ownership first.");
        }

        groupRepository.deleteMember(groupId, userId);

        groupRepository.findById(groupId)
                .ifPresent(group -> chatRepository.findById(group.getChatId()).ifPresent(chat -> {
                    chat.removeMember(userId);
                    chatRepository.update(chat);
                }));
    }

    // ویرایش نام و توضیحات گروه.
    // فقط ادمین یا مالک گروه می‌تواند این کار را انجام دهد.
    public void editGroup(String groupId, String requesterId, String newName, String newDescription) {
        GroupMember requester = groupRepository.findMember(groupId, requesterId)
                .orElseThrow(() -> new IllegalStateException("You are not a member of this group."));

        if (!requester.isAdmin()) {
            throw new IllegalStateException("Only admins can edit group info.");
        }

        groupRepository.findById(groupId).ifPresent(group -> {
            if (newName != null && !newName.isBlank()) {
                group.setName(newName);
            }
            if (newDescription != null) {
                group.setDescription(newDescription);
            }
            groupRepository.update(group);
        });
    }
}