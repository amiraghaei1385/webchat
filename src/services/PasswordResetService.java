package services;

import models.PasswordResetToken;
import models.User;
import repository.PasswordResetTokenRepository;
import repository.SessionRepository;
import repository.UserRepository;
import security.PasswordHasher;
import security.PasswordValidator;
import security.TokenGenerator;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

// مدیریت فراموشی رمز عبور
public class PasswordResetService {
    private static final int TOKEN_min = 15;
    private static final int passlenghth = 10;
    private static final String passchars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private final UserRepository userrepo;
    private final PasswordResetTokenRepository tokenrepo;
    private final SessionRepository sessionrepo;
    private final SecureRandom securerandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            SessionRepository sessionRepository) {
        this.userrepo = userRepository;
        this.tokenrepo = tokenRepository;
        this.sessionrepo = sessionRepository;
    }

    // درخواست بازیابی
    public ResetRequestResult requestReset(String username) {
        Optional<User> optuser = userrepo.findByUsername(username);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        synchronized (user.getId().intern()) {
            // حذف توکن‌های قبلی کاربر
            tokenrepo.deleteAllByUserId(user.getId());
            String token = TokenGenerator.generatePasswordResetToken();
            String plainTempPassword = generateTempPassword();
            String hashedTempPassword = PasswordHasher.hash(plainTempPassword);
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_min);
            PasswordResetToken resetToken = new PasswordResetToken(token, user.getId(), hashedTempPassword, expiresAt);
            tokenrepo.save(resetToken);
            return new ResetRequestResult(token, plainTempPassword);
        }
    }

    // رمز موقت ساخته میشه
    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(passlenghth);
        for (int i = 0; i < passlenghth; i++) {
            sb.append(passchars.charAt(securerandom.nextInt(passchars.length())));
        }
        return sb.toString();
    }

    // تایید بازیابی
    public void confirmReset(String token, String plainTempPassword, String newPassword, String confirmNewPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be empty.");
        }

        // قفل روی خود توکن
        synchronized (token.intern()) {
            Optional<PasswordResetToken> opttoken = tokenrepo.findByToken(token);
            if (opttoken.isEmpty()) {
                throw new IllegalArgumentException("Invalid or expired reset token.");
            }
            PasswordResetToken resetToken = opttoken.get();
            if (!PasswordHasher.verify(plainTempPassword, resetToken.getTempPassword())) {
                throw new IllegalArgumentException("Invalid temporary password.");
            }
            if (!resetToken.isValid()) {
                throw new IllegalStateException("Reset token is expired or already used.");
            }
            if (newPassword == null || !newPassword.equals(confirmNewPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
            Optional<User> optuser = userrepo.findById(resetToken.getUserId());
            if (optuser.isEmpty()) {
                throw new IllegalStateException("User no longer exists.");
            }
            User user = optuser.get();
            PasswordValidator.ValidationResult validation = PasswordValidator.validate(newPassword,
                    user.getUsername());
            if (!validation.isValid()) {
                throw new IllegalArgumentException(validation.getErrorsSummary());
            }
            user.setPasswordHash(PasswordHasher.hash(newPassword));
            userrepo.update(user);
            resetToken.setUsed(true);
            tokenrepo.update(resetToken);
            sessionrepo.deleteAllByUserId(user.getId());
        }
    }

    // نتیجه درخواست بازیابی
    public static class ResetRequestResult {
        private final String token;
        private final String temppass;

        public String getToken() {
            return token;
        }

        public ResetRequestResult(String token, String tempPassword) {
            this.token = token;
            this.temppass = tempPassword;
        }

        public String getTempPassword() {
            return temppass;
        }
    }
}
