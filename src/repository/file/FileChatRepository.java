package repository.file;

import models.Chat;
import repository.ChatRepository;
import utils.FileUtil;
import utils.JsonUtil;
import utils.PathUtil;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// ذخیره‌سازی فایل‌محور چت‌ها؛ هر چت یک فایل storage/chats/{id}.txt دارد
public class FileChatRepository implements ChatRepository {

    private final Map<String, Chat> store = new HashMap<>();

    public FileChatRepository() {
        loadAll();
    }

    private void loadAll() {
        List<String> contents = FileUtil.readAllInDirectory(PathUtil.chatsDir());
        for (String json : contents) {
            Chat chat = JsonUtil.fromJson(json, Chat.class);
            if (chat != null && chat.getId() != null) {
                store.put(chat.getId(), chat);
            }
        }
    }

    private void persist(Chat chat) {
        Path path = PathUtil.chatFile(chat.getId());
        FileUtil.writeAtomic(path, JsonUtil.toJson(chat));
    }

    @Override
    public void save(Chat chat) {
        store.put(chat.getId(), chat);
        persist(chat);
    }

    @Override
    public Optional<Chat> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Chat> findByUserId(String userId) {
        // مرتب‌سازی بر اساس آخرین پیام: جدیدترین چت اول (طبق قرارداد اینترفیس)
        return store.values().stream()
                .filter(c -> c.getMemberIds().contains(userId))
                .sorted((a, b) -> {
                    LocalDateTime at = a.getLastMessageAt();
                    LocalDateTime bt = b.getLastMessageAt();
                    if (at == null && bt == null) return 0;
                    if (at == null) return 1;   // چت بدون پیام به انتها می‌رود
                    if (bt == null) return -1;
                    return bt.compareTo(at);    // نزولی: جدیدترین اول
                })
                .collect(Collectors.toList());
    }

    @Override
    public void update(Chat chat) {
        store.put(chat.getId(), chat);
        persist(chat);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
        FileUtil.delete(PathUtil.chatFile(id));
    }
}