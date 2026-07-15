package repository;
 
import models.Story;
import java.util.List;
import java.util.Optional;
 
public interface StoryRepository {
 
    void save(Story story);
 
    Optional<Story> findById(String id);
 
    List<Story> findByOwnerId(String ownerId);
 
    List<Story> findAll();
 
    void update(Story story);
 
    void delete(String id);
}