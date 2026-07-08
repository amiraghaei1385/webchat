package repository.file;

import models.Message;
import repository.MessageRepository;
import utils.FileUtil;
import utils.JsonUtil;
import utils.PathUtil;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ذخیره‌سازی فایل‌محور پیام‌ها؛ هر پیام یک فایل storage/messages/{id}.txt دارد.
 *
 * نکته مهم درباره رمزنگاری: این کلاس هیچ‌گونه encrypt/decrypt انجام
 * نمی‌دهد و صرفاً هر مقداری که در فیلد encryptedContent پیام قرار دارد
 * را عیناً روی دیسک می‌نویسد. مسئولیت رمزنگاری بر عهده‌ی لایه‌ی بالاتر
 * (MessageService) است: قبل از فراخوانی save/update، سرویس باید محتوای
 * متن ساده را با security.MessageEncryptor.encrypt(...) رمزنگاری کرده
 * و نتیجه را در encryptedContent قرار داده باشد.
 */
public class FileMessageRepository implements MessageRepository {

    private final Map<String, Message> store = new ConcurrentHashMap<>();

    public FileMessageRepository() {
        loadAll();
    }

    private void loadAll() {
        List<String> contents = FileUtil.readAllInDirectory(PathUtil.messagesDir());
        for (String json : contents) {
            Message message = JsonUtil.fromJson(json, Message.class);
            if (message != null && message.getId() != null) {
                store.put(message.getId(), message);
            }
        }
    }

    private void persist(Message message) {
        Path path = PathUtil.messageFile(message.getId());
        FileUtil.writeAtomic(path, JsonUtil.toJson(message));
    }

    @Override
    public void save(Message message) {
        store.put(message.getId(), message);
        persist(message);
    }

    @Override
    public Optional<Message> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Message> findByChatId(String chatId) {
        // طبق قرارداد اینترفیس: قدیمی‌ترین به جدیدترین (صعودی بر اساس sentAt)
        return store.values().stream()
                .filter(m -> m.getChatId().equals(chatId))
                .sorted((a, b) -> {
                    LocalDateTime at = a.getSentAt();
                    LocalDateTime bt = b.getSentAt();
                    if (at == null && bt == null) return 0;
                    if (at == null) return -1;
                    if (bt == null) return 1;
                    return at.compareTo(bt);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void update(Message message) {
        store.put(message.getId(), message);
        persist(message);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
        FileUtil.delete(PathUtil.messageFile(id));
    }
}