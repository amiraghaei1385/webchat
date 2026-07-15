package repository.file;

import models.PasswordResetToken;
import repository.PasswordResetTokenRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FilePasswordResetTokenRepository implements PasswordResetTokenRepository {
    private final Map<String, PasswordResetToken> tokens = new ConcurrentHashMap<>();
    private final File folder = new File("storage/password-resets");

    public FilePasswordResetTokenRepository() {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        loadAll();
    }

    // خواندن همه
    private void loadAll() {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            PasswordResetToken token = readPasswordResetTokenFromFile(file);
            if (token != null) {
                tokens.put(token.getToken(), token);
            }
        }
    }

    // خواندن یکی
    private PasswordResetToken readPasswordResetTokenFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String token = reader.readLine();
            String iduser = reader.readLine();
            String tempassword = reader.readLine();
            String createdat = reader.readLine();
            String expiresat = reader.readLine();
            String used = reader.readLine();
            reader.close();
            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setToken(fixEmpty(token));
            passwordResetToken.setUserId(fixEmpty(iduser));
            passwordResetToken.setTempPassword(fixEmpty(tempassword));
            if (createdat != null && !createdat.equals("null")) {
                passwordResetToken.setCreatedAt(LocalDateTime.parse(createdat));
            }
            if (expiresat != null && !expiresat.equals("null")) {
                passwordResetToken.setExpiresAt(LocalDateTime.parse(expiresat));
            }
            passwordResetToken.setUsed(Boolean.parseBoolean(used));
            return passwordResetToken;
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
    private void saveFile(PasswordResetToken token) {
        File file = new File(folder, token.getToken() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(token.getToken()));
            writer.newLine();
            writer.write(safe(token.getUserId()));
            writer.newLine();
            writer.write(safe(token.getTempPassword()));
            writer.newLine();
            if (token.getCreatedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(token.getCreatedAt().toString()));
            }
            writer.newLine();
            if (token.getExpiresAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(token.getExpiresAt().toString()));
            }
            writer.newLine();
            writer.write(String.valueOf(token.isUsed()));
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
    public void save(PasswordResetToken token) {
        tokens.put(token.getToken(), token);
        saveFile(token);
    }

    @Override
    public void update(PasswordResetToken token) {
        tokens.put(token.getToken(), token);
        saveFile(token);
    }

    @Override
    public void delete(String token) {
        tokens.remove(token);
        File file = new File(folder, token + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return Optional.ofNullable(tokens.get(token));
    }

    @Override
    public void deleteAllByUserId(String userId) {
        ArrayList<String> tokent = new ArrayList<>();
        for (PasswordResetToken token : tokens.values()) {
            if (token.getUserId().equals(userId)) {
                tokent.add(token.getToken());
            }
        }
        for (String token : tokent) {
            tokens.remove(token);
            File file = new File(folder, token + ".txt");
            if (file.exists()) {
                file.delete();
            }
        }
    }
}