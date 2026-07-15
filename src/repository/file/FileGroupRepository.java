package repository.file;

import models.Group;
import models.GroupMember;
import repository.GroupRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileGroupRepository implements GroupRepository {
    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final Map<String, GroupMember> members = new ConcurrentHashMap<>();
    private final File groupfolder = new File("storage/groups");

    public FileGroupRepository() {
        if (!groupfolder.exists()) {
            groupfolder.mkdirs();
        }
        loadGroups();
        loadMembers();
    }

    private String memberKey(String groupId, String userId) {
        return groupId + ":" + userId;
    }

    // خواندن گروه
    private void loadGroups() {
        File[] files = groupfolder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            Group group = readGroupFromFile(file);
            if (group != null) {
                groups.put(group.getId(), group);
            }
        }
    }

    // خواند اعضا
    private void loadMembers() {
        File[] folders = groupfolder.listFiles();
        if (folders == null) {
            return;
        }
        for (File folder : folders) {
            if (!folder.isDirectory()) {
                continue;
            }
            File memberfolder = new File(folder, "members");
            if (!memberfolder.exists()) {
                continue;
            }
            File[] files = memberfolder.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                GroupMember member = readMemberFromFile(file);
                if (member != null) {
                    members.put(memberKey(member.getGroupId(), member.getUserId()), member);
                }
            }
        }
    }

    // خواندن گروه
    private Group readGroupFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String id = reader.readLine();
            String chatId = reader.readLine();
            String name = reader.readLine();
            String description = reader.readLine();
            String picturePath = reader.readLine();
            String ownerId = reader.readLine();
            String createdAt = reader.readLine();
            reader.close();
            Group group = new Group();
            group.setId(fixEmpty(id));
            group.setChatId(fixEmpty(chatId));
            group.setName(fixEmpty(name));
            group.setDescription(fixEmpty(description));
            group.setPicturePath(fixEmpty(picturePath));
            group.setOwnerId(fixEmpty(ownerId));
            if (createdAt != null && !createdAt.equals("null")) {
                group.setCreatedAt(LocalDateTime.parse(createdAt));
            }
            return group;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // خواندن عضو
    private GroupMember readMemberFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String idgroup = reader.readLine();
            String iduser = reader.readLine();
            String role = reader.readLine();
            String joinedat = reader.readLine();
            reader.close();
            GroupMember member = new GroupMember();
            member.setGroupId(fixEmpty(idgroup));
            member.setUserId(fixEmpty(iduser));
            if (role != null && !role.equals("null")) {
                member.setRole(GroupMember.Role.valueOf(role));
            }
            if (joinedat != null && !joinedat.equals("null")) {
                member.setJoinedAt(LocalDateTime.parse(joinedat));
            }
            return member;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // جلوگیری از خطای مقدار خالی
    private String fixEmpty(String value) {
        if (value == null || value.equals("null")) {
            return null;
        }
        return value;
    }

    // نوشتن گروه
    private void saveGroupFile(Group group) {
        File file = new File(groupfolder, group.getId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(group.getId()));
            writer.newLine();
            writer.write(safe(group.getChatId()));
            writer.newLine();
            writer.write(safe(group.getName()));
            writer.newLine();
            writer.write(safe(group.getDescription()));
            writer.newLine();
            writer.write(safe(group.getPicturePath()));
            writer.newLine();
            writer.write(safe(group.getOwnerId()));
            writer.newLine();
            if (group.getCreatedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(group.getCreatedAt().toString()));
            }
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // مقدار خالی برای نوشتن
    private String safe(String value) {
        if (value == null) {
            return "null";
        }
        return value;
    }

    @Override
    public void save(Group group) {
        groups.put(group.getId(), group);
        saveGroupFile(group);
    }

    @Override
    public Optional<Group> findById(String id) {
        return Optional.ofNullable(groups.get(id));
    }

    @Override
    public Optional<Group> findByChatId(String chatId) {
        for (Group group : groups.values()) {
            if (group.getChatId().equals(chatId)) {
                return Optional.of(group);
            }
        }
        return Optional.empty();
    }

    @Override
    public ArrayList<Group> findAll() {
        return new ArrayList<>(groups.values());
    }

    @Override
    public void update(Group group) {
        groups.put(group.getId(), group);
        saveGroupFile(group);
    }

    @Override
    public void delete(String id) {
        groups.remove(id);
        File file = new File(groupfolder, id + ".txt");
        if (file.exists()) {
            file.delete();
        }
        ArrayList<String> removelis = new ArrayList<>();
        for (GroupMember member : members.values()) {
            if (member.getGroupId().equals(id)) {
                removelis.add(memberKey(member.getGroupId(), member.getUserId()));
            }
        }
        for (String key : removelis) {
            members.remove(key);
        }
        File memberfolder = new File(groupfolder, id + "/members");
        deleteDirectory(memberfolder);
    }

    // نوشتن عضو
    private void saveMemberFile(GroupMember member) {
        File groupdir = new File(groupfolder, member.getGroupId());
        if (!groupdir.exists()) {
            groupdir.mkdirs();
        }
        File memberdir = new File(groupdir, "members");
        if (!memberdir.exists()) {
            memberdir.mkdirs();
        }
        File file = new File(memberdir, member.getUserId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(member.getGroupId()));
            writer.newLine();
            writer.write(safe(member.getUserId()));
            writer.newLine();
            if (member.getRole() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(member.getRole().name()));
            }
            writer.newLine();
            if (member.getJoinedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(member.getJoinedAt().toString()));
            }
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // حذف پوشه
    private void deleteDirectory(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectory(f);
                }
            }
        }
        file.delete();
    }

    @Override
    public List<GroupMember> findMembersByGroupId(String groupId) {
        ArrayList<GroupMember> res = new ArrayList<>();
        for (GroupMember member : members.values()) {
            if (member.getGroupId().equals(groupId)) {
                res.add(member);
            }
        }
        return res;
    }

    @Override
    public void saveMember(GroupMember member) {
        members.put(memberKey(member.getGroupId(), member.getUserId()), member);
        saveMemberFile(member);
    }

    @Override
    public void updateMember(GroupMember member) {
        members.put(memberKey(member.getGroupId(), member.getUserId()), member);
        saveMemberFile(member);
    }

    @Override
    public Optional<GroupMember> findMember(String groupId, String userId) {
        return Optional.ofNullable(members.get(memberKey(groupId, userId)));
    }

    @Override
    public void deleteMember(String groupId, String userId) {
        members.remove(memberKey(groupId, userId));
        File file = new File(groupfolder, groupId + "/members/" + userId + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }
}