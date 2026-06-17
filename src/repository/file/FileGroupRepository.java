package repository.file;

import models.Group;
import models.GroupMember;
import repository.GroupRepository;
import java.util.*;
import java.util.stream.Collectors;

// ذخیره‌سازی در حافظه
public class FileGroupRepository implements GroupRepository {

    private final Map<String, Group> groups = new HashMap<>();
    private final Map<String, GroupMember> members = new HashMap<>();

    @Override
    public void save(Group group) {
        groups.put(group.getId(), group);
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
    }

    @Override
    public void delete(String id) {
        groups.remove(id);
    }

    @Override
    public void saveMember(GroupMember member) {
        String key = member.getGroupId() + ":" + member.getUserId();
        members.put(key, member);
    }

    @Override
    public List<GroupMember> findMembersByGroupId(String groupId) {
        return members.values().stream()
                .filter(m -> m.getGroupId().equals(groupId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<GroupMember> findMember(String groupId, String userId) {
        return Optional.ofNullable(members.get(groupId + ":" + userId));
    }

    @Override
    public void updateMember(GroupMember member) {
        String key = member.getGroupId() + ":" + member.getUserId();
        members.put(key, member);
    }

    @Override
    public void deleteMember(String groupId, String userId) {
        members.remove(groupId + ":" + userId);
    }
}