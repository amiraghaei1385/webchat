package repository.file;

import models.MessageHistory;
import repository.HistoryRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileHistoryRepository implements HistoryRepository {
    // کلید: "{chatId}:{messageId}" -> لیست نسخه های تاریخچه
    private final Map<String, List<MessageHistory>> history = new ConcurrentHashMap<>();
    private final File fold = new File("storage/history");

    public FileHistoryRepository() {
        if (!fold.exists()) {
            fold.mkdirs();
        }
        loadAll();
    }

    private String key(String chatId, String messageId) {
        return chatId + ":" + messageId;
    }

    // خواندن همه
    private void loadAll() {
        File[] files = fold.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            MessageHistory item = readHistoryFromFile(file);
            if (item != null) {
                String k = key(item.getChatId(), item.getMessageId());
                List<MessageHistory> list = history.get(k);
                if (list == null) {
                    list = new ArrayList<>();
                    history.put(k, list);
                }
                list.add(item);
            }
        }
        for (List<MessageHistory> list : history.values()) {
            Collections.sort(list, new Comparator<MessageHistory>() {
                @Override
                public int compare(MessageHistory h1, MessageHistory h2) {
                    return Integer.compare(h1.getVersion(), h2.getVersion());
                }
            });
        }
    }

    // خواندن یکی
    private MessageHistory readHistoryFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String id = reader.readLine();
            String idmessage = reader.readLine();
            String idchat = reader.readLine();
            String ideditor = reader.readLine();
            String encryptedContentBefore = reader.readLine();
            String version = reader.readLine();
            String isdeletion = reader.readLine();
            String editedat = reader.readLine();
            reader.close();
            MessageHistory item = new MessageHistory();
            item.setId(fixEmpty(id));
            item.setMessageId(fixEmpty(idmessage));
            item.setChatId(fixEmpty(idchat));
            item.setEditorId(fixEmpty(ideditor));
            item.setEncryptedContentBefore(fixEmpty(encryptedContentBefore));

            if (version != null && !version.equals("null")) {
                item.setVersion(Integer.parseInt(version));
            }
            item.setDeletion(Boolean.parseBoolean(isdeletion));
            if (editedat != null && !editedat.equals("null")) {
                item.setEditedAt(LocalDateTime.parse(editedat));
            }
            return item;
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

    // نوشتن
    private void saveFile(MessageHistory item) {
        File file = new File(fold,
                item.getChatId() + "__" + item.getMessageId() + "__" + item.getId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(item.getId()));
            writer.newLine();
            writer.write(safe(item.getMessageId()));
            writer.newLine();
            writer.write(safe(item.getChatId()));
            writer.newLine();
            writer.write(safe(item.getEditorId()));
            writer.newLine();
            writer.write(safe(item.getEncryptedContentBefore()));
            writer.newLine();
            writer.write(String.valueOf(item.getVersion()));
            writer.newLine();
            writer.write(String.valueOf(item.isDeletion()));
            writer.newLine();

            if (item.getEditedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(item.getEditedAt().toString());
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
    public void save(MessageHistory item) {
        String k = key(item.getChatId(), item.getMessageId());
        List<MessageHistory> list = history.get(k);
        if (list == null) {
            list = new ArrayList<>();
            history.put(k, list);
        }
        list.add(item);
        saveFile(item);
    }

    @Override
    public int countByMessageId(String chatId, String messageId) {
        List<MessageHistory> list = history.get(key(chatId, messageId));
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    @Override
    public void deleteByMessageId(String chatId, String messageId) {
        history.remove(key(chatId, messageId));
        String prefix = chatId + "__" + messageId + "__";
        File[] files = fold.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.getName().startsWith(prefix)) {
                file.delete();
            }
        }
    }

    @Override
    public List<MessageHistory> findByMessageId(String chatId, String messageId) {
        List<MessageHistory> list = history.get(key(chatId, messageId));
        if (list == null) {
            return new ArrayList<>();
        }
        List<MessageHistory> res = new ArrayList<>(list);
        Collections.sort(res, new Comparator<MessageHistory>() {
            @Override
            public int compare(MessageHistory h1, MessageHistory h2) {
                return Integer.compare(h1.getVersion(), h2.getVersion());
            }
        });
        return res;
    }

}