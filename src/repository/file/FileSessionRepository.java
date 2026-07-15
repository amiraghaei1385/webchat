package repository.file;

import models.Session;
import repository.SessionRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileSessionRepository implements SessionRepository {
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final File fold = new File("storage/sessions");

    public FileSessionRepository() {
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
            Session session = readSessionFromFile(file);
            if (session != null) {
                sessions.put(session.getSessionToken(), session);
            }
        }
    }

    // خواندن یکی
    private Session readSessionFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String token = reader.readLine();
            String iduser = reader.readLine();
            String createdat = reader.readLine();
            String expiresat = reader.readLine();
            String active = reader.readLine();
            reader.close();
            Session session = new Session();
            session.setSessionToken(fixEmpty(token));
            session.setUserId(fixEmpty(iduser));
            if (createdat != null && !createdat.equals("null")) {
                session.setCreatedAt(LocalDateTime.parse(createdat));
            }
            if (expiresat != null && !expiresat.equals("null")) {
                session.setExpiresAt(LocalDateTime.parse(expiresat));
            }
            session.setActive(Boolean.parseBoolean(active));
            return session;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // نوشتن
    private void saveFile(Session session) {
        File file = new File(fold, session.getSessionToken() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(session.getSessionToken()));
            writer.newLine();
            writer.write(safe(session.getUserId()));
            writer.newLine();
            if (session.getCreatedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(session.getCreatedAt().toString()));
            }
            writer.newLine();

            if (session.getExpiresAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(session.getExpiresAt().toString()));
            }
            writer.newLine();
            writer.write(String.valueOf(session.isActive()));
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // جلوگیری از مقدار خالی
    private String safe(String value) {
        if (value == null) {
            return "null";
        }
        return value;
    }

    // مقدار خالی نوشتن
    private String fixEmpty(String value) {
        if (value == null || value.equals("null")) {
            return null;
        }
        return value;
    }

    @Override
    public void save(Session session) {
        sessions.put(session.getSessionToken(), session);
        saveFile(session);
    }

    @Override
    public void update(Session session) {
        sessions.put(session.getSessionToken(), session);
        saveFile(session);
    }

    @Override
    public List<Session> findByUserId(String userId) {
        List<Session> result = new ArrayList<>();
        for (Session session : sessions.values()) {
            if (session.getUserId().equals(userId)) {
                result.add(session);
            }
        }
        return result;
    }

    @Override
    public Optional<Session> findByToken(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    @Override
    public void deleteByToken(String token) {
        sessions.remove(token);
        File file = new File(fold, token + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void deleteAllByUserId(String userId) {
        List<String> tokens = new ArrayList<>();
        for (Session session : sessions.values()) {
            if (session.getUserId().equals(userId)) {
                tokens.add(session.getSessionToken());
            }
        }
        for (String token : tokens) {
            sessions.remove(token);
            File file = new File(fold, token + ".txt");
            if (file.exists()) {
                file.delete();
            }
        }
    }
}