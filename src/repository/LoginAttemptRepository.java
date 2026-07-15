package repository;

import models.LoginAttempt;
import java.util.Optional;

public interface LoginAttemptRepository {

    void save(LoginAttempt attempt);

    Optional<LoginAttempt> findByUserId(String userId);

    void update(LoginAttempt attempt);

    void delete(String userId);
}