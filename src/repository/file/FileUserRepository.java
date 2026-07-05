package repository.file;

import models.User;
import repository.UserRepository;
import utils.FileUtil;
import utils.JsonUtil;
import utils.PathUtil;
import java.nio.file.Path;
import java.util.*;

// ذخیره‌سازی فایل‌محور کاربران؛ هر کاربر یک فایل storage/users/{id}.txt دارد
public class FileUserRepository implements UserRepository {

    // کش در حافظه برای دسترسی سریع؛ منبع حقیقت اصلی همان فایل‌های روی دیسک است
    private final Map<String, User> store = new HashMap<>();

    public FileUserRepository() {
        loadAll();
    }

    // بارگذاری تمام کاربران موجود از دیسک هنگام راه‌اندازی سرور
    private void loadAll() {
        List<String> contents = FileUtil.readAllInDirectory(PathUtil.usersDir());
        for (String json : contents) {
            User user = JsonUtil.fromJson(json, User.class);
            if (user != null && user.getId() != null) {
                store.put(user.getId(), user);
            }
        }
    }

    private void persist(User user) {
        Path path = PathUtil.userFile(user.getId());
        FileUtil.writeAtomic(path, JsonUtil.toJson(user));
    }

    @Override
    public void save(User user) {
        store.put(user.getId(), user);
        persist(user);
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return store.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void update(User user) {
        store.put(user.getId(), user);
        persist(user);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
        FileUtil.delete(PathUtil.userFile(id));
    }
}