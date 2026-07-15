package repository;

import models.ChatFolder;
import java.util.List;
import java.util.Optional;

public interface ChatFolderRepository {

    void save(ChatFolder folder);

    Optional<ChatFolder> findById(String id);

    List<ChatFolder> findByOwnerId(String ownerId);

    void update(ChatFolder folder);

    void delete(String id);
}
