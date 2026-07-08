package services;

import models.Message;
import models.ReportedMessage;
import repository.ChatRepository;
import repository.MessageRepository;
import repository.ReportedMessageRepository;
import security.MessageEncryptor;
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
    private final HistoryService historyService;

    public MessageService(MessageRepository messageRepository,
            ChatRepository chatRepository,
            ReportedMessageRepository reportedMessageRepository,
            RateLimiter rateLimiter,
            HistoryService historyService) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.reportedMessageRepository = reportedMessageRepository;
        this.rateLimiter = rateLimiter;
        this.historyService = historyService;
    }

    // ارسال پیام //

    // ارسال پیام متنی در یک چت

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

        // رمزنگاری محتوا قبل از ذخیره‌سازی (طبق سند پروژه، پیام‌ها باید
        // در پایگاه‌داده به‌صورت رمزنگاری‌شده و غیرقابل‌خواندن ذخیره شوند)
        String encryptedContent = MessageEncryptor.encrypt(content);
        Message message = new Message(IdGenerator.generate(), chatId, senderId, encryptedContent);
        messageRepository.save(message);

        // به‌روزرسانی آخرین پیام چت
        chatRepository.findById(chatId).ifPresent(chat -> {
            chat.setLastMessageId(message.getId());
            chat.setLastMessageAt(message.getSentAt());
            chatRepository.update(chat);
        });

        // خروجی به کاربر باید متن ساده باشد، نه رمزشده؛ یک کپی decrypt‌شده برمی‌گردانیم
        // تا نسخه‌ی رمزشده‌ی داخل کش/دیسک دست‌نخورده بماند
        return toDecryptedCopy(message);
    }

    // دریافت پیام‌ها //

    /**
     * دریافت پیام‌های یک چت به ترتیب زمان ارسال.
     * پیام‌های حذف‌شده فیلتر می‌شوند و محتوای هر پیام قبل از بازگشت
     * رمزگشایی (decrypt) می‌شود تا برای کاربر قابل‌خواندن باشد.
     */
    public List<Message> getChatMessages(String chatId, String requesterId) {
        // بررسی عضویت درخواست‌کننده در چت
        chatRepository.findById(chatId)
                .filter(chat -> chat.getMemberIds().contains(requesterId))
                .orElseThrow(() -> new IllegalStateException("Access denied: you are not a member of this chat."));
        return messageRepository.findByChatId(chatId).stream()
                .filter(m -> !m.isDeleted())
                .sorted((a, b) -> a.getSentAt().compareTo(b.getSentAt()))
                .map(this::toDecryptedCopy)
                .collect(Collectors.toList());
    }

    // دریافت یک پیام با آیدی (محتوا رمزگشایی‌شده برگردانده می‌شود).
    public Optional<Message> findById(String messageId) {
        return messageRepository.findById(messageId).map(this::toDecryptedCopy);
    }

    // ویرایش و حذف //

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
        // رمزنگاری محتوای جدید قبل از ذخیره‌سازی
        message.setEncryptedContent(MessageEncryptor.encrypt(newContent));
        message.setEditedAt(LocalDateTime.now());
        messageRepository.update(message);
        // ثبت نسخه‌ی قبل از ویرایش در تاریخچه، پیش از اعمال محتوای جدید
        historyService.recordEdit(message, requesterId);
        message.setEncryptedContent(MessageEncryptor.encrypt(newContent));
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
    // گزارش پیام //

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

    // کمکی (رمزگشایی برای نمایش) //

    /**
     * یک نسخه‌ی جدید و مستقل از پیام می‌سازد که محتوایش رمزگشایی‌شده
     * (decrypted) است، بدون این‌که نسخه‌ی رمزشده‌ی اصلی که داخل کش
     * حافظه یا فایل روی دیسک نگه‌داری می‌شود دست‌خورده یا بازنویسی شود.
     * تمام متدهایی که پیام را برای نمایش به کاربر برمی‌گردانند باید از
     * این متد استفاده کنند، نه این‌که مستقیماً خروجی Repository را برگردانند.
     */
    private Message toDecryptedCopy(Message original) {
        Message copy = new Message();
        copy.setId(original.getId());
        copy.setChatId(original.getChatId());
        copy.setSenderId(original.getSenderId());
        copy.setEncryptedContent(MessageEncryptor.decrypt(original.getEncryptedContent()));
        copy.setSentAt(original.getSentAt());
        copy.setEditedAt(original.getEditedAt());
        copy.setDeleted(original.isDeleted());
        copy.setReplyToMessageId(original.getReplyToMessageId());
        copy.setHasMedia(original.isHasMedia());
        copy.setMediaMessageId(original.getMediaMessageId());
        copy.setForwardedFromMessageId(original.getForwardedFromMessageId());
        return copy;
    }
}