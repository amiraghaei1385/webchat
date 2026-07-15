package repository.file;

import models.Chat;
import models.ChatType;
import repository.ChatRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileChatRepository implements ChatRepository {
    private final Map<String, Chat> chats = new ConcurrentHashMap<>();
    private final File fold = new File("storage/chats");

    public FileChatRepository() {
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
            Chat chat = readChatFromFile(file);
            if (chat != null) {
                chats.put(chat.getId(), chat);
            }
        }
    }

    // خواندن یکی
    private Chat readChatFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            Chat chat = new Chat();
            chat.setId(fixEmpty(reader.readLine()));
            String type = reader.readLine();
            if (type != null && !type.equals("null")) {
                chat.setType(ChatType.valueOf(type));
            }
            String members = reader.readLine();
            ArrayList<String> memberid = new ArrayList<>();
            if (members != null && !members.equals("null") && !members.isEmpty()) {
                String[] list = members.split(",");
                for (String id : list) {
                    memberid.add(id);
                }
            }
            chat.setMemberIds(memberid);
            chat.setLastMessageId(fixEmpty(reader.readLine()));
            String lastmessage = reader.readLine();
            if (lastmessage != null && !lastmessage.equals("null")) {
                chat.setLastMessageAt(LocalDateTime.parse(lastmessage));
            }
            chat.setArchived(Boolean.parseBoolean(reader.readLine()));
            chat.setPinned(Boolean.parseBoolean(reader.readLine()));
            chat.setFolderId(fixEmpty(reader.readLine()));
            String createdat = reader.readLine();
            if (createdat != null && !createdat.equals("null")) {
                chat.setCreatedAt(LocalDateTime.parse(createdat));
            }
            Map<String, LocalDateTime> lastread = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    lastread.put(parts[0], LocalDateTime.parse(parts[1]));
                }
            }
            chat.setLastReadAt(lastread);
            reader.close();
            return chat;
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
    private void saveFile(Chat chat) {
        File file = new File(fold, chat.getId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(chat.getId()));
            writer.newLine();
            if (chat.getType() == null) {
                writer.write(safe(null));
            } else {
                writer.write(chat.getType().name());
            }
            writer.newLine();
            StringBuilder build = new StringBuilder();
            for (int i = 0; i < chat.getMemberIds().size(); i++) {
                build.append(chat.getMemberIds().get(i));
                if (i != chat.getMemberIds().size() - 1) {
                    build.append(",");
                }
            }
            writer.write(build.toString());
            writer.newLine();
            writer.write(safe(chat.getLastMessageId()));
            writer.newLine();
            if (chat.getLastMessageAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(chat.getLastMessageAt().toString());
            }
            writer.newLine();
            writer.write(String.valueOf(chat.isArchived()));
            writer.newLine();
            writer.write(String.valueOf(chat.isPinned()));
            writer.newLine();
            writer.write(safe(chat.getFolderId()));
            writer.newLine();
            if (chat.getCreatedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(chat.getCreatedAt().toString());
            }
            writer.newLine();
            if (chat.getLastReadAt() != null) {
                for (Map.Entry<String, LocalDateTime> entry : chat.getLastReadAt().entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue());
                    writer.newLine();
                }
            }
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
    public void save(Chat chat) {
        chats.put(chat.getId(), chat);
        saveFile(chat);
    }

    @Override
    public void update(Chat chat) {
        chats.put(chat.getId(), chat);
        saveFile(chat);
    }

    @Override
    public void delete(String id) {
        chats.remove(id);
        File file = new File(fold, id + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public Optional<Chat> findById(String id) {
        return Optional.ofNullable(chats.get(id));
    }

    @Override
    public List<Chat> findByUserId(String userId) {
        ArrayList<Chat> res = new ArrayList<>();
        for (Chat chat : chats.values()) {
            if (chat.getMemberIds().contains(userId)) {
                res.add(chat);
            }
        }
        Collections.sort(res, new Comparator<Chat>() {
            @Override
            public int compare(Chat c1, Chat c2) {
                if (c1.getLastMessageAt() == null && c2.getLastMessageAt() == null) {
                    return 0;
                }
                if (c1.getLastMessageAt() == null) {
                    return 1;
                }
                if (c2.getLastMessageAt() == null) {
                    return -1;
                }
                return c2.getLastMessageAt().compareTo(c1.getLastMessageAt());
            }
        });
        return res;
    }

}