package repository;

import models.UserSettings;
import java.util.Optional;

public interface SettingsRepository {

    void save(UserSettings settings);

    Optional<UserSettings> findByUserId(String userId);

    void update(UserSettings settings);
}