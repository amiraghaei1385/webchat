package repository.file;

import models.Chat;
import repository.ChatRepository;
import java.util.*;
import java.util.stream.Collectors;

// ذخیره‌سازی در حافظه

public class FileChatRepository implements ChatRepository {

    private final Map<String, Chat> store = new HashMap<>();

    @Override
    public void save(Chat chat) {
        store.put(chat.getId(), chat);
    }

    @Override
    public Optional<Chat> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Chat> findByUserId(String userId) {
        return store.values().stream()
                .filter(c -> c.getMemberIds().contains(userId))
                .collect(Collectors.toList());
    }

    @Override
    public void update(Chat chat) {
        store.put(chat.getId(), chat);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }
}