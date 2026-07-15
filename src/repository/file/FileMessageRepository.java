package repository.file;

import models.Message;
import repository.MessageRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileMessageRepository implements MessageRepository {
    private final Map<String, Message> messages = new ConcurrentHashMap<>();
    private final File fold = new File("storage/messages");

    public FileMessageRepository() {
        if (!fold.exists()) {
            fold.mkdirs();
        }
        loadAll();
    }

    // خواندن همه
    private void loadAll() {
        File[] files = fold.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            Message message = readMessageFromFile(file);
            if (message != null) {
                messages.put(message.getId(), message);
            }
        }
    }

    // خواندن یکی
    private Message readMessageFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String id = reader.readLine();
            String idchat = reader.readLine();
            String idsender = reader.readLine();
            String encryptedcontent = reader.readLine();
            String sentat = reader.readLine();
            String editedat = reader.readLine();
            String isdelete = reader.readLine();
            String idreplytoMessage = reader.readLine();
            String hasmedia = reader.readLine();
            String idmediaMessage = reader.readLine();
            String idforwardfromMessage = reader.readLine();
            reader.close();
            Message message = new Message();
            message.setId(fixEmpty(id));
            message.setChatId(fixEmpty(idchat));
            message.setSenderId(fixEmpty(idsender));
            message.setEncryptedContent(fixEmpty(encryptedcontent));
            if (sentat != null && !sentat.equals("null")) {
                message.setSentAt(LocalDateTime.parse(sentat));
            }
            if (editedat != null && !editedat.equals("null")) {
                message.setEditedAt(LocalDateTime.parse(editedat));
            }
            message.setDeleted(Boolean.parseBoolean(isdelete));
            message.setReplyToMessageId(fixEmpty(idreplytoMessage));
            message.setHasMedia(Boolean.parseBoolean(hasmedia));
            message.setMediaMessageId(fixEmpty(idmediaMessage));
            message.setForwardedFromMessageId(fixEmpty(idforwardfromMessage));
            return message;
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
    private void saveFile(Message message) {
        File file = new File(fold, message.getId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(message.getId()));
            writer.newLine();
            writer.write(safe(message.getChatId()));
            writer.newLine();
            writer.write(safe(message.getSenderId()));
            writer.newLine();
            writer.write(safe(message.getEncryptedContent()));
            writer.newLine();
            if (message.getSentAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(message.getSentAt().toString());
            }
            writer.newLine();
            if (message.getEditedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(message.getEditedAt().toString());
            }
            writer.newLine();
            writer.write(String.valueOf(message.isDeleted()));
            writer.newLine();
            writer.write(safe(message.getReplyToMessageId()));
            writer.newLine();
            writer.write(String.valueOf(message.isHasMedia()));
            writer.newLine();
            writer.write(safe(message.getMediaMessageId()));
            writer.newLine();
            writer.write(safe(message.getForwardedFromMessageId()));
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
    public void save(Message message) {
        messages.put(message.getId(), message);
        saveFile(message);
    }

    @Override
    public void update(Message message) {
        messages.put(message.getId(), message);
        saveFile(message);
    }

    @Override
    public Optional<Message> findById(String id) {
        return Optional.ofNullable(messages.get(id));
    }

    @Override
    public List<Message> findByChatId(String chatId) {
        List<Message> res = new ArrayList<>();
        for (Message message : messages.values()) {
            if (message.getChatId().equals(chatId)) {
                res.add(message);
            }
        }
        Collections.sort(res, new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) {
                if (m1.getSentAt() == null && m2.getSentAt() == null) {
                    return 0;
                }
                if (m1.getSentAt() == null) {
                    return -1;
                }
                if (m2.getSentAt() == null) {
                    return 1;
                }
                return m1.getSentAt().compareTo(m2.getSentAt());
            }
        });
        return res;
    }

    @Override
    public void delete(String id) {
        messages.remove(id);
        File file = new File(fold, id + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }

}