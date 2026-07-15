package services;

import models.Reaction;
import repository.ChatRepository;
import repository.MessageRepository;
import repository.ReactionRepository;
import utils.IdGenerator;
import java.util.*;

// مدیریت ری‌اکشن کاربران روی پیام‌ها
public class ReactionService {

    private final ReactionRepository reactionrepo;
    private final ChatRepository chatrepo;
    private final MessageRepository messagerepo;

    public ReactionService(ReactionRepository reactionRepository,
            ChatRepository chatRepository,
            MessageRepository messageRepository) {
        this.reactionrepo = reactionRepository;
        this.chatrepo = chatRepository;
        this.messagerepo = messageRepository;
    }

    // ثبت یا تغییر یا حذف ری‌اکشن
    public Reaction toggleReaction(String messageId, String userId, String emoji) {
        if (emoji == null || emoji.isBlank()) {
            throw new IllegalArgumentException("Emoji cannot be empty.");
        }
        Optional<models.Message> optmessage = messagerepo.findById(messageId);
        if (optmessage.isEmpty()) {
            throw new IllegalArgumentException("Message not found.");
        }
        models.Message message = optmessage.get();
        // بررسی عضویت کاربر در چت
        Optional<models.Chat> optchat = chatrepo.findById(message.getChatId());
        if (optchat.isEmpty() || !optchat.get().getMemberIds().contains(userId)) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        Optional<Reaction> optexisting = reactionrepo.findByMessageAndUser(messageId, userId);
        if (optexisting.isPresent()) {
            Reaction current = optexisting.get();
            if (current.getEmoji().equals(emoji)) {
                reactionrepo.delete(messageId, userId);
                return null;
            } else {
                current.setEmoji(emoji);
                current.setReactedAt(java.time.LocalDateTime.now());
                reactionrepo.update(current);
                return current;
            }
        } else {
            Reaction reaction = new Reaction(IdGenerator.generate(), messageId, userId, emoji);
            reactionrepo.save(reaction);
            return reaction;
        }
    }

    // خلاصه گروه‌بندی شده ری‌اکشن‌ها
    public Map<String, Long> getReactionSummary(String messageId) {
        Map<String, Long> summary = new HashMap<>();
        for (Reaction reaction : reactionrepo.findByMessageId(messageId)) {
            String emoji = reaction.getEmoji();
            Long count = summary.get(emoji);
            if (count == null) {
                summary.put(emoji, 1L);
            } else {
                summary.put(emoji, count + 1);
            }
        }
        return summary;
    }

    // همه ری‌اکشن‌های یک پیام دریافت میشه
    public List<Reaction> getReactionsForMessage(String messageId) {
        return reactionrepo.findByMessageId(messageId);
    }

    // حری‌اکشن یک کاربر حذف میشه
    public void removeReaction(String messageId, String userId) {
        reactionrepo.delete(messageId, userId);
    }

    // حذف تمام ری‌اکشن‌های یک پیام
    public void deleteAllForMessage(String messageId) {
        reactionrepo.deleteByMessageId(messageId);
    }
}