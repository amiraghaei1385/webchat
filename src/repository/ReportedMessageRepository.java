package repository;
 
import models.ReportedMessage;
import java.util.List;
import java.util.Optional;
 
public interface ReportedMessageRepository {
 
    void save(ReportedMessage report);
 
    Optional<ReportedMessage> findById(String id);
 
    List<ReportedMessage> findAll();
 
    void delete(String id);
}