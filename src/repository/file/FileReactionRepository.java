package repository.file;

import models.Reaction;
import repository.ReactionRepository;
import utils.FileUtil;
import utils.JsonUtil;
import utils.PathUtil;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// ذخیره‌سازی فایل‌محور ری‌اکشن‌ها؛ هر رکورد یک فایل storage/reactions/{id}.txt دارد
public class FileReactionRepository implements ReactionRepository {

    private final Map<String, Reaction> store = new ConcurrentHashMap<>();

    public FileReactionRepository() {
        loadAll();
    }

    private String userKey(String messageId, String userId) {
        return messageId + ":" + userId;
    }

    private void loadAll() {
        List<String> contents = FileUtil.readAllInDirectory(PathUtil.reactionsDir());
        for (String json : contents) {
            Reaction reaction = JsonUtil.fromJson(json, Reaction.class);
            if (reaction != null && reaction.getId() != null) {
                store.put(reaction.getId(), reaction);
            }
        }
    }

    private void persist(Reaction reaction) {
        Path path = PathUtil.reactionFile(reaction.getId());
        FileUtil.writeAtomic(path, JsonUtil.toJson(reaction));
    }

    @Override
    public void save(Reaction reaction) {
        store.put(reaction.getId(), reaction);
        persist(reaction);
    }

    @Override
    public Optional<Reaction> findByMessageAndUser(String messageId, String userId) {
        return store.values().stream()
                .filter(r -> r.getMessageId().equals(messageId) && r.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public List<Reaction> findByMessageId(String messageId) {
        return store.values().stream()
                .filter(r -> r.getMessageId().equals(messageId))
                .collect(Collectors.toList());
    }

    @Override
    public void update(Reaction reaction) {
        store.put(reaction.getId(), reaction);
        persist(reaction);
    }

    @Override
    public void delete(String messageId, String userId) {
        Optional<Reaction> existing = findByMessageAndUser(messageId, userId);
        existing.ifPresent(r -> {
            store.remove(r.getId());
            FileUtil.delete(PathUtil.reactionFile(r.getId()));
        });
    }

    @Override
    public void deleteByMessageId(String messageId) {
        List<String> idsToRemove = store.values().stream()
                .filter(r -> r.getMessageId().equals(messageId))
                .map(Reaction::getId)
                .collect(Collectors.toList());

        for (String id : idsToRemove) {
            store.remove(id);
            FileUtil.delete(PathUtil.reactionFile(id));
        }
    }
}
