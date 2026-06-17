package repository.file;

import models.Session;
import repository.SessionRepository;
import java.util.*;
import java.util.stream.Collectors;

// ذخیره‌سازی در حافظه
public class FileSessionRepository implements SessionRepository {

    private final Map<String, Session> store = new HashMap<>();

    @Override
    public void save(Session session) {
        store.put(session.getSessionToken(), session);
    }

    @Override
    public Optional<Session> findByToken(String token) {
        return Optional.ofNullable(store.get(token));
    }

    @Override
    public List<Session> findByUserId(String userId) {
        return store.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public void update(Session session) {
        store.put(session.getSessionToken(), session);
    }

    @Override
    public void deleteByToken(String token) {
        store.remove(token);
    }

    @Override
    public void deleteAllByUserId(String userId) {
        store.entrySet().removeIf(e -> e.getValue().getUserId().equals(userId));
    }
}