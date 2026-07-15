package repository;

import models.MediaMessage;
import java.util.Optional;

public interface MediaMessageRepository {

    void save(MediaMessage media);

    Optional<MediaMessage> findByMessageId(String chatId, String messageId);

    void update(MediaMessage media);

    void delete(String chatId, String messageId);
}