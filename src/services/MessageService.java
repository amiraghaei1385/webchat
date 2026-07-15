package services;

import models.Chat;
import models.ChatType;
import models.Message;
import models.ReportedMessage;
import repository.ChatRepository;
import repository.MessageRepository;
import repository.ReportedMessageRepository;
import security.MessageEncryptor;
import security.RateLimiter;
import utils.IdGenerator;
import java.time.LocalDateTime;
import java.util.*;
import server.ChatWebSocketServer;

// مدیریت ارسال ویرایش حذف و گزارش پیام‌ها
public class MessageService {

    private final MessageRepository messagerepo;
    private final ChatRepository chatrepo;
    private final ReportedMessageRepository reportedmessagerepo;
    private final RateLimiter ratelimiter;
    private final HistoryService historyservice;
    private final ContactService contactservice;
    private ChatWebSocketServer websocketserver;

    public MessageService(MessageRepository messageRepository,
            ChatRepository chatRepository,
            ReportedMessageRepository reportedMessageRepository,
            RateLimiter rateLimiter,
            HistoryService historyService,
            ContactService contactService) {
        this.messagerepo = messageRepository;
        this.chatrepo = chatRepository;
        this.reportedmessagerepo = reportedMessageRepository;
        this.ratelimiter = rateLimiter;
        this.historyservice = historyService;
        this.contactservice = contactService;
    }

    public void setWebSocketServer(ChatWebSocketServer webSocketServer) {
        this.websocketserver = webSocketServer;
    }

    // پیام‌های یک چت دریافت میشه
    public List<Message> getChatMessages(String chatId, String requesterId) {
        Optional<Chat> optchat = chatrepo.findById(chatId);
        if (optchat.isEmpty() || !optchat.get().getMemberIds().contains(requesterId)) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        List<Message> res = new ArrayList<>();
        List<Message> all = messagerepo.findByChatId(chatId);
        for (Message message : all) {
            if (!message.isDeleted()) {
                res.add(message);
            }
        }
        res.sort(new java.util.Comparator<Message>() {
            public int compare(Message a, Message b) {
                return a.getSentAt().compareTo(b.getSentAt());
            }
        });
        List<Message> decrypted = new ArrayList<>();
        for (Message message : res) {
            decrypted.add(toDecryptedCopy(message));
        }
        return decrypted;
    }

    // پیام متنی در یک چت ارسال میشه
    public Message sendMessage(String chatId, String senderId, String content) {
        Optional<Chat> optchat = chatrepo.findById(chatId);
        if (optchat.isEmpty() || !optchat.get().getMemberIds().contains(senderId)) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        Chat chat = optchat.get();
        // بررسی بلاک بودن در پیوی
        if (chat.getType() == ChatType.PRIVATE) {
            String otherUserId = null;
            for (String id : chat.getMemberIds()) {
                if (!id.equals(senderId)) {
                    otherUserId = id;
                    break;
                }
            }
            if (otherUserId != null &&
                    (contactservice.isBlocked(senderId, otherUserId)
                            || contactservice.isBlocked(otherUserId, senderId))) {
                throw new IllegalStateException("Cannot send message: blocked.");
            }
        }
        if (!ratelimiter.allowSend(senderId)) {
            throw new IllegalStateException("Too many messages. Please slow down.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        if (content.length() > 4096) {
            throw new IllegalArgumentException("Message is too long.");
        }
        // رمزنگاری محتوا قبل از ذخیره
        String encryptedContent = MessageEncryptor.encrypt(content);
        Message message = new Message(IdGenerator.generate(), chatId, senderId, encryptedContent);
        messagerepo.save(message);
        // بروزرسانی آخرین پیام چت
        chat.setLastMessageId(message.getId());
        chat.setLastMessageAt(message.getSentAt());
        chatrepo.update(chat);
        if (websocketserver != null) {
            String payload = "{\"type\":\"new_message\",\"chatId\":\"" + chatId + "\","
                    + "\"messageId\":\"" + message.getId() + "\","
                    + "\"senderId\":\"" + senderId + "\","
                    + "\"sentAt\":\"" + message.getSentAt() + "\"}";
            websocketserver.sendToUsers(chat.getMemberIds(), payload);
        }
        return toDecryptedCopy(message);
    }

    // تعداد پیام‌های خونده نشده
    public long getUnreadCount(Chat chat, String userId) {
        LocalDateTime lastread = chat.getLastReadAt(userId);
        long count = 0;
        for (Message message : messagerepo.findByChatId(chat.getId())) {
            if (message.isDeleted()) {
                continue;
            }
            if (lastread == null || message.getSentAt().isAfter(lastread)) {
                count++;
            }
            if (message.getSenderId().equals(userId)) {
                continue;
            }
        }
        return count;
    }

    // ویرایش پیام
    public void editMessage(String messageId, String requesterId, String newContent) {
        Optional<Message> optmessage = messagerepo.findById(messageId);
        if (optmessage.isEmpty()) {
            throw new IllegalArgumentException("Message not found.");
        }
        Message message = optmessage.get();
        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted message.");
        }
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        if (!message.getSenderId().equals(requesterId)) {
            throw new IllegalStateException("You can only edit your own messages.");
        }
        historyservice.recordEdit(message, requesterId);
        message.setEncryptedContent(MessageEncryptor.encrypt(newContent));
        message.setEditedAt(LocalDateTime.now());
        messagerepo.update(message);
    }

    // پیدا کردن پیام با آیدی
    public Optional<Message> findById(String messageId) {
        Optional<Message> optmessage = messagerepo.findById(messageId);
        if (optmessage.isPresent()) {
            return Optional.of(toDecryptedCopy(optmessage.get()));
        }
        return Optional.empty();
    }

    // ریپورت پیام
    public void reportMessage(String messageId, String reporterId, String reason) {
        if (messagerepo.findById(messageId).isEmpty()) {
            throw new IllegalArgumentException("Message not found.");
        }
        ReportedMessage report = new ReportedMessage(
                IdGenerator.generate(), messageId, reporterId, reason);
        reportedmessagerepo.save(report);
    }

    // حذف یک پیام
    public void deleteMessage(String messageId, String requesterId) {
        Optional<Message> optmessage = messagerepo.findById(messageId);
        if (optmessage.isEmpty()) {
            throw new IllegalArgumentException("Message not found.");
        }
        Message message = optmessage.get();
        if (!message.getSenderId().equals(requesterId)) {
            throw new IllegalStateException("You can only delete your own messages.");
        }
        if (message.isDeleted()) {
            throw new IllegalStateException("Message is already deleted.");
        }
        // محتوای پیام پیش از حذف در تاریخچه ثبت میشه
        historyservice.recordDeletion(message, requesterId);
        message.setDeleted(true);
        messagerepo.update(message);
    }

    // رمزگشایی برای نمایش
    private Message toDecryptedCopy(Message original) {
        Message copy = new Message();
        copy.setId(original.getId());
        copy.setSenderId(original.getSenderId());
        copy.setEncryptedContent(MessageEncryptor.decrypt(original.getEncryptedContent()));
        copy.setSentAt(original.getSentAt());
        copy.setMediaMessageId(original.getMediaMessageId());
        copy.setEditedAt(original.getEditedAt());
        copy.setChatId(original.getChatId());
        copy.setReplyToMessageId(original.getReplyToMessageId());
        copy.setHasMedia(original.isHasMedia());
        copy.setDeleted(original.isDeleted());
        copy.setForwardedFromMessageId(original.getForwardedFromMessageId());
        return copy;
    }
}