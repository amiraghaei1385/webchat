package repository.file;

import models.LoginAttempt;
import repository.LoginAttemptRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileLoginAttemptRepository implements LoginAttemptRepository {
    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private final File fold = new File("storage/login-attempts");

    public FileLoginAttemptRepository() {
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
            LoginAttempt attempt = readLoginAttemptFromFile(file);
            if (attempt != null) {
                attempts.put(attempt.getUserId(), attempt);
            }
        }
    }

    // خواندن یکی
    private LoginAttempt readLoginAttemptFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String iduser = reader.readLine();
            String failcount = reader.readLine();
            String lastattempt = reader.readLine();
            String lockuntil = reader.readLine();
            reader.close();
            LoginAttempt attempt = new LoginAttempt();
            attempt.setUserId(fixEmpty(iduser));
            attempt.setFailedCount(Integer.parseInt(failcount));
            if (lastattempt != null && !lastattempt.equals("null")) {
                attempt.setLastAttemptAt(LocalDateTime.parse(lastattempt));
            }
            if (lockuntil != null && !lockuntil.equals("null")) {
                attempt.setLockedUntil(LocalDateTime.parse(lockuntil));
            }
            return attempt;
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
    private void saveFile(LoginAttempt attempt) {
        File file = new File(fold, attempt.getUserId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(attempt.getUserId()));
            writer.newLine();
            writer.write(String.valueOf(attempt.getFailedCount()));
            writer.newLine();
            if (attempt.getLastAttemptAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(attempt.getLastAttemptAt().toString()));
            }
            writer.newLine();
            if (attempt.getLockedUntil() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(attempt.getLockedUntil().toString()));
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
    public void update(LoginAttempt attempt) {
        attempts.put(attempt.getUserId(), attempt);
        saveFile(attempt);
    }

    @Override
    public void save(LoginAttempt attempt) {
        attempts.put(attempt.getUserId(), attempt);
        saveFile(attempt);
    }

    @Override
    public Optional<LoginAttempt> findByUserId(String userId) {
        return Optional.ofNullable(attempts.get(userId));
    }

    @Override
    public void delete(String userId) {
        attempts.remove(userId);
        File file = new File(fold, userId + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }
}