package services;

import models.Reaction;
import repository.ChatRepository;
import repository.MessageRepository;
import repository.ReactionRepository;
import utils.IdGenerator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * مدیریت ری‌اکشن‌های (ایموجی) کاربران روی پیام‌ها.
 *
 * رفتار طراحی‌شده مشابه تلگرام/واتساپ: هر کاربر روی هر پیام حداکثر یک
 * ری‌اکشن فعال دارد.
 * - اگر کاربر برای اولین بار ایموجی بزند: یک ری‌اکشن جدید ثبت می‌شود.
 * - اگر کاربر قبلاً ایموجی دیگری زده و ایموجی جدید بزند: ری‌اکشن قبلی
 *   جایگزین می‌شود (نه اضافه).
 * - اگر کاربر همان ایموجی قبلی را دوباره بزند: ری‌اکشن برداشته می‌شود
 *   (toggle off).
 */
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    public ReactionService(ReactionRepository reactionRepository,
                            ChatRepository chatRepository,
                            MessageRepository messageRepository) {
        this.reactionRepository = reactionRepository;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * ثبت یا تغییر یا حذف (toggle) ری‌اکشن یک کاربر روی یک پیام.
     *
     * @return ری‌اکشن نهایی بعد از عملیات، یا null اگر ری‌اکشن حذف شده باشد
     */
    public Reaction toggleReaction(String messageId, String userId, String emoji) {
        if (emoji == null || emoji.isBlank()) {
            throw new IllegalArgumentException("Emoji cannot be empty.");
        }

        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found."));

        // بررسی عضویت کاربر در چت مربوط به این پیام (جلوگیری از BOLA)
        chatRepository.findById(message.getChatId())
                .filter(chat -> chat.getMemberIds().contains(userId))
                .orElseThrow(() -> new IllegalStateException("Access denied: you are not a member of this chat."));

        var existing = reactionRepository.findByMessageAndUser(messageId, userId);

        if (existing.isPresent()) {
            Reaction current = existing.get();
            if (current.getEmoji().equals(emoji)) {
                // همان ایموجی دوباره زده شده -> حذف (toggle off)
                reactionRepository.delete(messageId, userId);
                return null;
            } else {
                // ایموجی متفاوت -> جایگزینی
                current.setEmoji(emoji);
                current.setReactedAt(java.time.LocalDateTime.now());
                reactionRepository.update(current);
                return current;
            }
        } else {
            Reaction reaction = new Reaction(IdGenerator.generate(), messageId, userId, emoji);
            reactionRepository.save(reaction);
            return reaction;
        }
    }

    // حذف صریح ری‌اکشن یک کاربر از یک پیام (مثلاً از طریق دکمه‌ی جداگانه در UI).
    public void removeReaction(String messageId, String userId) {
        reactionRepository.delete(messageId, userId);
    }

    // دریافت تمام ری‌اکشن‌های ثبت‌شده روی یک پیام (خام، بدون گروه‌بندی).
    public List<Reaction> getReactionsForMessage(String messageId) {
        return reactionRepository.findByMessageId(messageId);
    }

    /**
     * دریافت خلاصه‌ی گروه‌بندی‌شده‌ی ری‌اکشن‌های یک پیام: هر ایموجی و
     * تعداد کاربرانی که آن را زده‌اند. مناسب برای نمایش مستقیم در UI
     * (مثلاً "👍 3، ❤️ 1").
     */
    public Map<String, Long> getReactionSummary(String messageId) {
        return reactionRepository.findByMessageId(messageId).stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji, Collectors.counting()));
    }

    // حذف تمام ری‌اکشن‌های یک پیام (فراخوانی‌شده هنگام حذف کامل پیام).
    public void deleteAllForMessage(String messageId) {
        reactionRepository.deleteByMessageId(messageId);
    }
}
