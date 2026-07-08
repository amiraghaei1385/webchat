package repository.file;

import models.Session;
import repository.SessionRepository;
import utils.FileUtil;
import utils.JsonUtil;
import utils.PathUtil;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// ذخیره‌سازی فایل‌محور نشست‌ها؛ هر نشست یک فایل storage/sessions/{token}.txt دارد
public class FileSessionRepository implements SessionRepository {

    private final Map<String, Session> store = new ConcurrentHashMap<>();

    public FileSessionRepository() {
        loadAll();
    }

    private void loadAll() {
        List<String> contents = FileUtil.readAllInDirectory(PathUtil.sessionsDir());
        for (String json : contents) {
            Session session = JsonUtil.fromJson(json, Session.class);
            if (session != null && session.getSessionToken() != null) {
                store.put(session.getSessionToken(), session);
            }
        }
    }

    private void persist(Session session) {
        Path path = PathUtil.sessionFile(session.getSessionToken());
        FileUtil.writeAtomic(path, JsonUtil.toJson(session));
    }

    @Override
    public void save(Session session) {
        store.put(session.getSessionToken(), session);
        persist(session);
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
        persist(session);
    }

    @Override
    public void deleteByToken(String token) {
        store.remove(token);
        FileUtil.delete(PathUtil.sessionFile(token));
    }

    @Override
    public void deleteAllByUserId(String userId) {
        List<String> tokensToRemove = store.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .map(Session::getSessionToken)
                .collect(Collectors.toList());

        for (String token : tokensToRemove) {
            store.remove(token);
            FileUtil.delete(PathUtil.sessionFile(token));
        }
    }
}