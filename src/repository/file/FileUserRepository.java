package repository.file;

import models.User;
import repository.UserRepository;
import java.util.*;

// ذخیره سازی در حافظه
public class FileUserRepository implements UserRepository {

    private final Map<String, User> store = new HashMap<>();

    @Override
    public void save(User user) {
        store.put(user.getId(), user);
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
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }
}