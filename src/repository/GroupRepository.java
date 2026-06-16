package repository;

import models.Group;
import models.GroupMember;
import java.util.List;
import java.util.Optional;

// قرارداد مربوط به تمام عملیات ذخیره‌سازی و بازیابی گروه‌ها و اعضای گروه
public interface GroupRepository {

    void save(Group group);

    Optional<Group> findById(String id);

    Optional<Group> findByChatId(String chatId);

    List<Group> findAll();

    void update(Group group);

    void delete(String id);

    // عملیات مدیریت اعضای گروه

    void saveMember(GroupMember member);

    List<GroupMember> findMembersByGroupId(String groupId);

    Optional<GroupMember> findMember(String groupId, String userId);

    void updateMember(GroupMember member);

    void deleteMember(String groupId, String userId);
}