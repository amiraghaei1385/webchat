package services;

import models.Message;
import models.ReportedMessage;
import repository.ChatRepository;
import repository.MessageRepository;
import repository.ReportedMessageRepository;
import security.RateLimiter;
import utils.IdGenerator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// مدیریت ارسال، ویرایش، حذف و گزارش پیام‌ها.
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final ReportedMessageRepository reportedMessageRepository;
    private final RateLimiter rateLimiter;

    public MessageService(MessageRepository messageRepository,
                          ChatRepository chatRepository,
                          ReportedMessageRepository reportedMessageRepository,
                          RateLimiter rateLimiter) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.reportedMessageRepository = reportedMessageRepository;
        this.rateLimiter = rateLimiter;
    }

    //  ارسال پیام                                                        //

    //ارسال پیام متنی در یک چت
     
    public Message sendMessage(String chatId, String senderId, String content) {
        // بررسی عضویت فرستنده در چت (جلوگیری از BOLA)
        chatRepository.findById(chatId)
                .filter(chat -> chat.getMemberIds().contains(senderId))
                .orElseThrow(() -> new IllegalStateException("Access denied: you are not a member of this chat."));

        // بررسی محدودیت نرخ ارسال (حداکثر ۵ پیام در ثانیه)
        if (!rateLimiter.allowSend(senderId)) {
            throw new IllegalStateException("Too many messages. Please slow down.");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        if (content.length() > 4096) {
            throw new IllegalArgumentException("Message is too long.");
        }

        Message message = new Message(IdGenerator.generate(), chatId, senderId, content);
        messageRepository.save(message);

        // به‌روزرسانی آخرین پیام چت
        chatRepository.findById(chatId).ifPresent(chat -> {
            chat.setLastMessageId(message.getId());
            chat.setLastMessageAt(message.getSentAt());
            chatRepository.update(chat);
        });

        return message;
    }

    //  دریافت پیام‌ها                                                    //

    /**
     * دریافت پیام‌های یک چت به ترتیب زمان ارسال.
     * پیام‌های حذف‌شده و کاربران غیرعضو فیلتر می‌شوند.
     */
    public List<Message> getChatMessages(String chatId, String requesterId) {
        // بررسی عضویت درخواست‌کننده در چت
        chatRepository.findById(chatId)
                .filter(chat -> chat.getMemberIds().contains(requesterId))
                .orElseThrow(() -> new IllegalStateException("Access denied: you are not a member of this chat."));

        return messageRepository.findByChatId(chatId).stream()
                .filter(m -> !m.isDeleted())
                .sorted((a, b) -> a.getSentAt().compareTo(b.getSentAt()))
                .collect(Collectors.toList());
    }

    // دریافت یک پیام با آیدی.
    public Optional<Message> findById(String messageId) {
        return messageRepository.findById(messageId);
    }

    //  ویرایش و حذف                                                      //

     // ویرایش محتوای یک پیام.
     
    public void editMessage(String messageId, String requesterId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found."));

        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted message.");
        }
        if (!message.getSenderId().equals(requesterId)) {
            throw new IllegalStateException("You can only edit your own messages.");
        }
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        // فاز دوم: newContent = MessageEncryptor.encrypt(newContent)
        message.setEncryptedContent(newContent);
        message.setEditedAt(LocalDateTime.now());
        messageRepository.update(message);
    }

    // حذف یک پیام (soft delete).
     
    public void deleteMessage(String messageId, String requesterId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found."));

        if (message.isDeleted()) {
            throw new IllegalStateException("Message is already deleted.");
        }
        if (!message.getSenderId().equals(requesterId)) {
            throw new IllegalStateException("You can only delete your own messages.");
        }

        message.setDeleted(true);
        messageRepository.update(message);
    }
    //  گزارش پیام                                                        //

    /**
     * گزارش یک پیام توسط کاربر.
     * گزارش‌ها توسط ادمین از طریق CLI قابل مشاهده‌اند.
     */
    public void reportMessage(String messageId, String reporterId, String reason) {
        if (messageRepository.findById(messageId).isEmpty()) {
            throw new IllegalArgumentException("Message not found.");
        }

        ReportedMessage report = new ReportedMessage(
                IdGenerator.generate(), messageId, reporterId, reason);
        reportedMessageRepository.save(report);
    }
}