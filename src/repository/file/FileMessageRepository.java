package repository.file;

import models.Message;
import repository.MessageRepository;
import java.util.*;
import java.util.stream.Collectors;

// ذخیره‌سازی در حافظه
public class FileMessageRepository implements MessageRepository {

    private final Map<String, Message> store = new HashMap<>();

    @Override
    public void save(Message message) {
        store.put(message.getId(), message);
    }

    @Override
    public Optional<Message> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Message> findByChatId(String chatId) {
        return store.values().stream()
                .filter(m -> m.getChatId().equals(chatId))
                .collect(Collectors.toList());
    }

    @Override
    public void update(Message message) {
        store.put(message.getId(), message);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }
}