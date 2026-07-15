package repository.file;

import models.ChatFolder;
import repository.ChatFolderRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileChatFolderRepository implements ChatFolderRepository {
    private final Map<String, ChatFolder> folders = new ConcurrentHashMap<>();
    private final File fold = new File("storage/folders");

    public FileChatFolderRepository() {
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
            ChatFolder folder = readFolderFromFile(file);
            if (folder != null) {
                folders.put(folder.getId(), folder);
            }
        }
    }

    // خواندن یکی
    private ChatFolder readFolderFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String id = reader.readLine();
            String idowner = reader.readLine();
            String name = reader.readLine();
            String chatidlin = reader.readLine();
            String orderidx = reader.readLine();
            String createdat = reader.readLine();
            reader.close();
            ChatFolder folder = new ChatFolder();
            folder.setId(fixEmpty(id));
            folder.setOwnerId(fixEmpty(idowner));
            folder.setName(fixEmpty(name));
            List<String> chatIds = new ArrayList<>();
            if (chatidlin != null && !chatidlin.equals("null") && !chatidlin.isEmpty()) {
                String[] list = chatidlin.split(",");
                for (String cid : list) {
                    chatIds.add(cid);
                }
            }
            folder.setChatIds(chatIds);
            if (orderidx != null && !orderidx.equals("null")) {
                folder.setOrderIndex(Integer.parseInt(orderidx));
            }
            if (createdat != null && !createdat.equals("null")) {
                folder.setCreatedAt(LocalDateTime.parse(createdat));
            }
            return folder;
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
    private void saveFile(ChatFolder folder) {
        File file = new File(fold, folder.getId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(folder.getId()));
            writer.newLine();
            writer.write(safe(folder.getOwnerId()));
            writer.newLine();
            writer.write(safe(folder.getName()));
            writer.newLine();
            StringBuilder build = new StringBuilder();
            List<String> chatid = folder.getChatIds();
            for (int i = 0; i < chatid.size(); i++) {
                build.append(chatid.get(i));
                if (i != chatid.size() - 1) {
                    build.append(",");
                }
            }
            writer.write(build.toString());
            writer.newLine();
            writer.write(String.valueOf(folder.getOrderIndex()));
            writer.newLine();
            if (folder.getCreatedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(folder.getCreatedAt().toString());
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
    public void save(ChatFolder folder) {
        folders.put(folder.getId(), folder);
        saveFile(folder);
    }

    @Override
    public void update(ChatFolder folder) {
        folders.put(folder.getId(), folder);
        saveFile(folder);
    }

    @Override
    public List<ChatFolder> findByOwnerId(String ownerId) {
        List<ChatFolder> res = new ArrayList<>();
        for (ChatFolder folder : folders.values()) {
            if (folder.getOwnerId().equals(ownerId)) {
                res.add(folder);
            }
        }
        Collections.sort(res, new Comparator<ChatFolder>() {
            @Override
            public int compare(ChatFolder f1, ChatFolder f2) {
                return Integer.compare(f1.getOrderIndex(), f2.getOrderIndex());
            }
        });
        return res;
    }

    @Override
    public Optional<ChatFolder> findById(String id) {
        return Optional.ofNullable(folders.get(id));
    }

    @Override
    public void delete(String id) {
        folders.remove(id);
        File file = new File(fold, id + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }
}