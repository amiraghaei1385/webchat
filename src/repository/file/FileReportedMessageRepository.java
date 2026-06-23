package repository.file;

import models.ReportedMessage;
import repository.ReportedMessageRepository;
import java.util.*;

// ذخیره‌سازی در حافظه
public class FileReportedMessageRepository implements ReportedMessageRepository {

    private final Map<String, ReportedMessage> store = new HashMap<>();

    @Override
    public void save(ReportedMessage report) {
        store.put(report.getId(), report);
    }

    @Override
    public Optional<ReportedMessage> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ReportedMessage> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }
}