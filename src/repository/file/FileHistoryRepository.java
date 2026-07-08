package repository.file;

import models.MessageHistory;
import repository.HistoryRepository;
import utils.FileUtil;
import utils.JsonUtil;
import utils.PathUtil;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ذخیره‌سازی فایل‌محور تاریخچه‌ی ویرایش پیام‌ها.
 *
 * چون تعداد رکوردهای تاریخچه می‌تواند نسبتاً زیاد باشد (هر ویرایش یک
 * رکورد جدید)، به‌جای نگه‌داشتن یک Map مسطح، از یک Map دوسطحی
 * (chatId:messageId -> List) برای دسترسی سریع در حافظه استفاده می‌شود.
 */
public class FileHistoryRepository implements HistoryRepository {

    // کلید: "{chatId}:{messageId}" -> لیست نسخه‌های تاریخچه آن پیام
    private final Map<String, List<MessageHistory>> store = new ConcurrentHashMap<>();

    public FileHistoryRepository() {
        loadAll();
    }

    private String key(String chatId, String messageId) {
        return chatId + ":" + messageId;
    }

    /**
     * بارگذاری تمام تاریخچه‌ها از دیسک هنگام راه‌اندازی سرور.
     * ساختار سلسله‌مراتبی است: ابتدا پوشه‌های chatId، سپس داخل هرکدام
     * پوشه‌های messageId، و در نهایت فایل‌های historyId.txt.
     */
    private void loadAll() {
        List<Path> chatDirs = FileUtil.listSubDirectories(PathUtil.historyDir());
        for (Path chatDir : chatDirs) {
            List<Path> messageDirs = FileUtil.listSubDirectories(chatDir);
            for (Path messageDir : messageDirs) {
                List<String> contents = FileUtil.readAllInDirectory(messageDir);
                for (String json : contents) {
                    MessageHistory history = JsonUtil.fromJson(json, MessageHistory.class);
                    if (history != null && history.getId() != null) {
                        String k = key(history.getChatId(), history.getMessageId());
                        store.computeIfAbsent(k, x -> new ArrayList<>()).add(history);
                    }
                }
            }
        }
        // مرتب‌سازی هر لیست بر اساس شماره نسخه، تا بعد از لود اولیه هم ترتیب درست باشد
        for (List<MessageHistory> list : store.values()) {
            list.sort(Comparator.comparingInt(MessageHistory::getVersion));
        }
    }

    private void persist(MessageHistory history) {
        Path path = PathUtil.historyFile(history.getChatId(), history.getMessageId(), history.getId());
        FileUtil.writeAtomic(path, JsonUtil.toJson(history));
    }

    @Override
    public void save(MessageHistory history) {
        String k = key(history.getChatId(), history.getMessageId());
        store.computeIfAbsent(k, x -> Collections.synchronizedList(new ArrayList<>())).add(history);
        persist(history);
    }

    @Override
    public List<MessageHistory> findByMessageId(String chatId, String messageId) {
        List<MessageHistory> list = store.get(key(chatId, messageId));
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream()
                .sorted(Comparator.comparingInt(MessageHistory::getVersion))
                .collect(Collectors.toList());
    }

    @Override
    public int countByMessageId(String chatId, String messageId) {
        List<MessageHistory> list = store.get(key(chatId, messageId));
        return list == null ? 0 : list.size();
    }

    @Override
    public void deleteByMessageId(String chatId, String messageId) {
        store.remove(key(chatId, messageId));
        FileUtil.deleteDirectoryRecursive(PathUtil.historyDir(chatId, messageId));
    }
}