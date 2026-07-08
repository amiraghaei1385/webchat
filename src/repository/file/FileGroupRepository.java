package repository.file;

import models.Group;
import models.GroupMember;
import repository.GroupRepository;
import utils.FileUtil;
import utils.JsonUtil;
import utils.PathUtil;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// ذخیره‌سازی فایل‌محور گروه‌ها و اعضای آن‌ها
// گروه‌ها: storage/groups/{groupId}.txt
// اعضا:    storage/groups/{groupId}/members/{userId}.txt
public class FileGroupRepository implements GroupRepository {

    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final Map<String, GroupMember> members = new ConcurrentHashMap<>();

    public FileGroupRepository() {
        loadGroups();
        loadMembers();
    }

    private void loadGroups() {
        List<String> contents = FileUtil.readAllInDirectory(PathUtil.groupsDir());
        for (String json : contents) {
            Group group = JsonUtil.fromJson(json, Group.class);
            if (group != null && group.getId() != null) {
                groups.put(group.getId(), group);
            }
        }
    }

    // اعضا در زیرپوشه‌ی هر گروه هستند، پس باید هر زیرپوشه را جداگانه پیمایش کنیم
    private void loadMembers() {
        List<Path> groupDirs = FileUtil.listSubDirectories(PathUtil.groupsDir());
        for (Path groupDir : groupDirs) {
            Path membersDir = groupDir.resolve("members");
            List<String> contents = FileUtil.readAllInDirectory(membersDir);
            for (String json : contents) {
                GroupMember member = JsonUtil.fromJson(json, GroupMember.class);
                if (member != null && member.getGroupId() != null && member.getUserId() != null) {
                    members.put(memberKey(member.getGroupId(), member.getUserId()), member);
                }
            }
        }
    }

    private String memberKey(String groupId, String userId) {
        return groupId + ":" + userId;
    }

    private void persistGroup(Group group) {
        Path path = PathUtil.groupFile(group.getId());
        FileUtil.writeAtomic(path, JsonUtil.toJson(group));
    }

    private void persistMember(GroupMember member) {
        Path path = PathUtil.groupMemberFile(member.getGroupId(), member.getUserId());
        FileUtil.writeAtomic(path, JsonUtil.toJson(member));
    }

    // گروه‌ها //

    @Override
    public void save(Group group) {
        groups.put(group.getId(), group);
        persistGroup(group);
    }

    @Override
    public Optional<Group> findById(String id) {
        return Optional.ofNullable(groups.get(id));
    }

    @Override
    public Optional<Group> findByChatId(String chatId) {
        return groups.values().stream()
                .filter(g -> g.getChatId().equals(chatId))
                .findFirst();
    }

    @Override
    public List<Group> findAll() {
        return new ArrayList<>(groups.values());
    }

    @Override
    public void update(Group group) {
        groups.put(group.getId(), group);
        persistGroup(group);
    }

    @Override
    public void delete(String id) {
        groups.remove(id);
        FileUtil.delete(PathUtil.groupFile(id));

        // پاک‌سازی تمام اعضای این گروه (هم از کش و هم از دیسک)
        List<String> memberKeysToRemove = members.values().stream()
                .filter(m -> m.getGroupId().equals(id))
                .map(m -> memberKey(m.getGroupId(), m.getUserId()))
                .collect(Collectors.toList());
        for (String key : memberKeysToRemove) {
            members.remove(key);
        }
        FileUtil.deleteDirectoryRecursive(PathUtil.groupMembersDir(id));
    }

    // اعضای گروه //

    @Override
    public void saveMember(GroupMember member) {
        members.put(memberKey(member.getGroupId(), member.getUserId()), member);
        persistMember(member);
    }

    @Override
    public List<GroupMember> findMembersByGroupId(String groupId) {
        return members.values().stream()
                .filter(m -> m.getGroupId().equals(groupId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<GroupMember> findMember(String groupId, String userId) {
        return Optional.ofNullable(members.get(memberKey(groupId, userId)));
    }

    @Override
    public void updateMember(GroupMember member) {
        members.put(memberKey(member.getGroupId(), member.getUserId()), member);
        persistMember(member);
    }

    @Override
    public void deleteMember(String groupId, String userId) {
        members.remove(memberKey(groupId, userId));
        FileUtil.delete(PathUtil.groupMemberFile(groupId, userId));
    }
}