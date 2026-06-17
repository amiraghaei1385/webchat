package repository;
 
import models.ReportedMessage;
import java.util.List;
import java.util.Optional;
 
// قرارداد مربوط به مدیریت و ذخیره‌سازی پیام‌های گزارش‌شده
// مدیر سیستم می‌تواند این پیام‌ها را از طریق CLI مشاهده کند
public interface ReportedMessageRepository {
 
    void save(ReportedMessage report);
 
    Optional<ReportedMessage> findById(String id);
 
    List<ReportedMessage> findAll();
 
    void delete(String id);
}