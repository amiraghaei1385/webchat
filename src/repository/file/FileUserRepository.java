package repository.file;

import java.time.LocalDate;
import models.User;
import repository.UserRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileUserRepository implements UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final File fold = new File("storage/users");

    public FileUserRepository() {
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
            User user = readUserFromFile(file);
            if (user != null) {
                users.put(user.getId(), user);
            }
        }
    }

    // خواندن یکی
    private User readUserFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String id = reader.readLine();
            String username = reader.readLine();
            String passhash = reader.readLine();
            String profpath = reader.readLine();
            String bio = reader.readLine();
            String createdatline = reader.readLine();
            String lastseenatline = reader.readLine();
            String onlineline = reader.readLine();
            String deleteline = reader.readLine();
            String emailline = reader.readLine();
            String birthdateline = reader.readLine();
            reader.close();
            User user = new User();
            user.setId(fixEmpty(id));
            user.setUsername(fixEmpty(username));
            user.setPasswordHash(fixEmpty(passhash));
            user.setProfilePicPath(fixEmpty(profpath));
            user.setBio(fixEmpty(bio));
            if (createdatline != null && !createdatline.equals("null")) {
                user.setCreatedAt(LocalDateTime.parse(createdatline));
            }
            if (lastseenatline != null && !lastseenatline.equals("null")) {
                user.setLastSeenAt(LocalDateTime.parse(lastseenatline));
            }
            user.setOnline(Boolean.parseBoolean(onlineline));
            user.setDeleted(Boolean.parseBoolean(deleteline));
            user.setEmail(fixEmpty(emailline));
            if (birthdateline != null && !birthdateline.equals("null")) {
                user.setBirthDate(LocalDate.parse(birthdateline));
            }
            return user;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // جلوگیری از مقدار خالی
    private String fixEmpty(String value) {
        if (value == null || value.equals("null")) {
            return null;
        }
        return value;
    }

    // نوشتن
    private void saveFile(User user) {
        File file = new File(fold, user.getId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(user.getId()));
            writer.newLine();
            writer.write(safe(user.getUsername()));
            writer.newLine();
            writer.write(safe(user.getPasswordHash()));
            writer.newLine();
            writer.write(safe(user.getProfilePicPath()));
            writer.newLine();
            writer.write(safe(user.getBio()));
            writer.newLine();
            if (user.getCreatedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(user.getCreatedAt().toString()));
            }
            writer.newLine();
            if (user.getLastSeenAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(user.getLastSeenAt().toString()));
            }
            writer.newLine();
            writer.write(String.valueOf(user.isOnline()));
            writer.newLine();
            writer.write(String.valueOf(user.isDeleted()));
            writer.newLine();
            writer.write(safe(user.getEmail()));
            writer.newLine();
            if (user.getBirthDate() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(user.getBirthDate().toString()));
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
    public void save(User user) {
        users.put(user.getId(), user);
        saveFile(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        for (User user : users.values()) {
            if (user.getUsername().equals(username)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void update(User user) {
        users.put(user.getId(), user);
        saveFile(user);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void delete(String id) {
        users.remove(id);
        File file = new File(fold, id + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }
}