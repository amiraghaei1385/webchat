package repository.file;

import models.ReportedMessage;
import repository.ReportedMessageRepository;
import utils.FileUtil;
import utils.JsonUtil;
import utils.PathUtil;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ذخیره‌سازی فایل‌محور گزارش‌های پیام؛ هر گزارش یک فایل storage/reports/{id}.txt دارد
// مدیر سیستم این گزارش‌ها را از طریق CLI مشاهده می‌کند (طبق قرارداد اینترفیس)
public class FileReportedMessageRepository implements ReportedMessageRepository {

    private final Map<String, ReportedMessage> store = new ConcurrentHashMap<>();

    public FileReportedMessageRepository() {
        loadAll();
    }

    private void loadAll() {
        List<String> contents = FileUtil.readAllInDirectory(PathUtil.reportsDir());
        for (String json : contents) {
            ReportedMessage report = JsonUtil.fromJson(json, ReportedMessage.class);
            if (report != null && report.getId() != null) {
                store.put(report.getId(), report);
            }
        }
    }

    private void persist(ReportedMessage report) {
        Path path = PathUtil.reportFile(report.getId());
        FileUtil.writeAtomic(path, JsonUtil.toJson(report));
    }

    @Override
    public void save(ReportedMessage report) {
        store.put(report.getId(), report);
        persist(report);
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
        FileUtil.delete(PathUtil.reportFile(id));
    }
}