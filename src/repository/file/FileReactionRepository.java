package repository.file;

import models.Reaction;
import repository.ReactionRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileReactionRepository implements ReactionRepository {
    private final Map<String, Reaction> reactions = new ConcurrentHashMap<>();
    private final File fold = new File("storage/reactions");

    public FileReactionRepository() {
        if (!fold.exists()) {
            fold.mkdirs();
        }
        loadAll();
    }

    private String key(String messageId, String userId) {
        return messageId + ":" + userId;
    }

    // خواندن همه
    private void loadAll() {
        File[] files = fold.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            Reaction reaction = readReactionFromFile(file);
            if (reaction != null) {
                reactions.put(key(reaction.getMessageId(), reaction.getUserId()), reaction);
            }
        }
    }

    // خواندن یکی
    private Reaction readReactionFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String id = reader.readLine();
            String idmessage = reader.readLine();
            String iduser = reader.readLine();
            String emoji = reader.readLine();
            String reactedat = reader.readLine();
            reader.close();
            Reaction reaction = new Reaction();
            reaction.setId(fixEmpty(id));
            reaction.setMessageId(fixEmpty(idmessage));
            reaction.setUserId(fixEmpty(iduser));
            reaction.setEmoji(fixEmpty(emoji));
            if (reactedat != null && !reactedat.equals("null")) {
                reaction.setReactedAt(LocalDateTime.parse(reactedat));
            }
            return reaction;
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
    private void saveFile(Reaction reaction) {
        File file = new File(fold,
                reaction.getMessageId() + "__" + reaction.getUserId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(reaction.getId()));
            writer.newLine();
            writer.write(safe(reaction.getMessageId()));
            writer.newLine();
            writer.write(safe(reaction.getUserId()));
            writer.newLine();
            writer.write(safe(reaction.getEmoji()));
            writer.newLine();
            if (reaction.getReactedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(reaction.getReactedAt().toString());
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
    public void save(Reaction reaction) {
        reactions.put(key(reaction.getMessageId(), reaction.getUserId()), reaction);
        saveFile(reaction);
    }

    @Override
    public void update(Reaction reaction) {
        reactions.put(key(reaction.getMessageId(), reaction.getUserId()), reaction);
        saveFile(reaction);
    }

    @Override
    public Optional<Reaction> findByMessageAndUser(String messageId, String userId) {
        return Optional.ofNullable(reactions.get(key(messageId, userId)));
    }

    @Override
    public List<Reaction> findByMessageId(String messageId) {
        List<Reaction> res = new ArrayList<>();
        for (Reaction reaction : reactions.values()) {
            if (reaction.getMessageId().equals(messageId)) {
                res.add(reaction);
            }
        }
        return res;
    }

    @Override
    public void deleteByMessageId(String messageId) {
        List<String> toRemove = new ArrayList<>();
        for (Reaction reaction : reactions.values()) {
            if (reaction.getMessageId().equals(messageId)) {
                toRemove.add(reaction.getUserId());
            }
        }
        for (String userId : toRemove) {
            delete(messageId, userId);
        }
    }

    @Override
    public void delete(String messageId, String userId) {
        reactions.remove(key(messageId, userId));
        File file = new File(fold, messageId + "__" + userId + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }
}