package services;

import models.Chat;
import models.ChatType;
import models.Group;
import models.GroupMember;
import repository.ChatRepository;
import repository.GroupRepository;
import repository.UserRepository;
import utils.IdGenerator;
import java.util.*;
import java.io.File;

// مدیریت گروه‌ها و اعضای آن‌ها
public class GroupService {

    private final GroupRepository grouprepo;
    private final ChatRepository chatrepo;
    private final UserRepository userrepo;

    public GroupService(GroupRepository groupRepository,
            ChatRepository chatRepository,
            UserRepository userRepository) {
        this.grouprepo = groupRepository;
        this.chatrepo = chatRepository;
        this.userrepo = userRepository;
    }

    // آپدیت عکس گروه هر عضوی میتونه این کارو بکنه نه فقط ادمین
    public Group updateGroupPicture(String idgroup, String requesterId, byte[] filebytes, String orgfilename) {
        Optional<GroupMember> optrequester = grouprepo.findMember(idgroup, requesterId);
        if (optrequester.isEmpty()) {
            throw new IllegalStateException("You are not a member of this group.");
        }
        Optional<Group> optgroup = grouprepo.findById(idgroup);
        if (optgroup.isEmpty()) {
            throw new IllegalArgumentException("Group not found.");
        }
        File dir = new File("storage/group-avatars");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String extension = utils.PathUtil.extractExtension(orgfilename);
        if (extension.isEmpty()) {
            extension = "jpg";
        }
        File file = new File(dir, idgroup + "." + extension);
        utils.FileUtil.writeBytesAtomic(file, filebytes);
        Group group = optgroup.get();
        group.setPicturePath(file.getPath());
        grouprepo.update(group);
        return group;
    }

    // گروه ایحاد میشه
    public Group createGroup(String idgroup, String name, String ownerId) {
        if (idgroup == null || idgroup.isBlank()) {
            throw new IllegalArgumentException("Group ID cannot be empty.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be empty.");
        }
        boolean idalreadyused = grouprepo.findById(idgroup).isPresent();
        if (idalreadyused) {
            throw new IllegalArgumentException("Group ID already taken.");
        }
        Chat chat = new Chat(IdGenerator.generate(), ChatType.GROUP);
        Group group = new Group(idgroup, chat.getId(), name, ownerId);
        chat.addMember(ownerId);
        chatrepo.save(chat);
        grouprepo.save(group);
        GroupMember owner = new GroupMember(group.getId(), ownerId, GroupMember.Role.OWNER);
        grouprepo.saveMember(owner);
        return group;
    }

    // دریافت لیست اعضای یک گروه.
    public List<GroupMember> getMembers(String groupId) {
        return grouprepo.findMembersByGroupId(groupId);
    }

    // دریافت گروه با آیدی چت مرتبط.
    public Optional<Group> findByChatId(String chatId) {
        return grouprepo.findByChatId(chatId);
    }

    // دریافت لیست تمام گروه‌ها
    public List<Group> findAll() {
        return grouprepo.findAll();
    }

    // دریافت گروه با آیدی.
    public Optional<Group> findById(String groupId) {
        return grouprepo.findById(groupId);
    }

    // اعضو جدید به گروه اضافه میشه
    public void addMember(String groupId, String requesterId, String newUserId) {
        Optional<GroupMember> optrequester = grouprepo.findMember(groupId, requesterId);
        if (userrepo.findById(newUserId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }

        if (optrequester.isEmpty()) {
            throw new IllegalStateException("You are not a member of this group.");
        }
        GroupMember requester = optrequester.get();
        if (!requester.isAdmin()) {
            throw new IllegalStateException("Only admins can add members.");
        }
        if (grouprepo.findMember(groupId, newUserId).isPresent()) {
            throw new IllegalArgumentException("User is already a member.");
        }
        GroupMember newmember = new GroupMember(groupId, newUserId, GroupMember.Role.MEMBER);
        grouprepo.saveMember(newmember);
        Optional<Group> optgroup = grouprepo.findById(groupId);
        if (optgroup.isPresent()) {
            Optional<Chat> optchat = chatrepo.findById(optgroup.get().getChatId());
            if (optchat.isPresent()) {
                Chat chat = optchat.get();
                chat.addMember(newUserId);
                chatrepo.update(chat);
            }
        }
    }

    // کاربر گروه رو ترک میکنه
    public void leaveGroup(String groupId, String userId) {
        Optional<GroupMember> optmember = grouprepo.findMember(groupId, userId);
        if (optmember.isEmpty()) {
            throw new IllegalStateException("You are not a member of this group.");
        }
        GroupMember member = optmember.get();
        if (member.getRole() == GroupMember.Role.OWNER) {
            throw new IllegalStateException("Owner cannot leave the group. Transfer ownership first.");
        }
        grouprepo.deleteMember(groupId, userId);
        Optional<Group> optgroup = grouprepo.findById(groupId);
        if (optgroup.isPresent()) {
            Optional<Chat> optchat = chatrepo.findById(optgroup.get().getChatId());
            if (optchat.isPresent()) {
                Chat chat = optchat.get();
                chat.removeMember(userId);
                chatrepo.update(chat);
            }
        }
    }

    // حذف عضو از گروه
    public void removeMember(String groupId, String requesterId, String targetUserId) {
        Optional<GroupMember> optrequester = grouprepo.findMember(groupId, requesterId);
        Optional<GroupMember> opttarget = grouprepo.findMember(groupId, targetUserId);
        if (optrequester.isEmpty()) {
            throw new IllegalStateException("You are not a member of this group.");
        }
        if (opttarget.isEmpty()) {
            throw new IllegalArgumentException("Target user is not a member.");
        }
        GroupMember requester = optrequester.get();
        if (!requester.isAdmin()) {
            throw new IllegalStateException("Only admins can remove members.");
        }
        GroupMember target = opttarget.get();
        // چک میکنه ببینه این کسی که کاربرو اخراج میکنه اونر هست
        if (target.getRole() == GroupMember.Role.ADMIN
                && requester.getRole() != GroupMember.Role.OWNER) {
            throw new IllegalStateException("Only the owner can remove an admin.");
        }
        // مالک را نمی‌تونیم اخراج کنیم
        if (target.getRole() == GroupMember.Role.OWNER) {
            throw new IllegalStateException("Cannot remove the group owner.");
        }
        grouprepo.deleteMember(groupId, targetUserId);
        Optional<Group> optgroup = grouprepo.findById(groupId);
        if (optgroup.isPresent()) {
            Optional<Chat> optchat = chatrepo.findById(optgroup.get().getChatId());
            if (optchat.isPresent()) {
                Chat chat = optchat.get();
                chat.removeMember(targetUserId);
                chatrepo.update(chat);
            }
        }
    }

    // ویرایش نام و توضیحات گروه
    public void editGroup(String groupId, String requesterId, String newName, String newDescription) {
        Optional<GroupMember> optrequester = grouprepo.findMember(groupId, requesterId);
        if (optrequester.isEmpty()) {
            throw new IllegalStateException("You are not a member of this group.");
        }
        GroupMember requester = optrequester.get();
        if (!requester.isAdmin()) {
            throw new IllegalStateException("Only admins can edit group info.");
        }
        Optional<Group> optgroup = grouprepo.findById(groupId);
        if (optgroup.isPresent()) {
            Group group = optgroup.get();
            if (newName != null && !newName.isBlank()) {
                group.setName(newName);
            }
            if (newDescription != null) {
                group.setDescription(newDescription);
            }
            grouprepo.update(group);
        }
    }
}