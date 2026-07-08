package services;

import models.Message;
import models.MessageHistory;
import repository.ChatRepository;
import repository.HistoryRepository;
import repository.MessageRepository;
import security.MessageEncryptor;
import utils.IdGenerator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * مدیریت تاریخچه‌ی ویرایش پیام‌ها.
 *
 * هر بار که یک پیام ویرایش می‌شود، محتوای رمزشده‌ی قبل از ویرایش
 * (پیش از اعمال تغییر) در این سرویس ثبت می‌شود تا یک لاگ غیرقابل‌تغییر
 * (immutable audit trail) از تمام نسخه‌های قبلی پیام حفظ شود؛ مشابه
 * قابلیت "Edited" در تلگرام که با کلیک روی آن می‌توان نسخه‌های قبلی
 * پیام را دید.
 */
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    public HistoryService(HistoryRepository historyRepository,
            ChatRepository chatRepository,
            MessageRepository messageRepository) {
        this.historyRepository = historyRepository;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * ثبت یک نسخه‌ی جدید از تاریخچه، درست قبل از این‌که محتوای پیام
     * در MessageRepository به‌روزرسانی شود.
     * پیامی که در حال ویرایش است (قبل از اعمال تغییر جدید)
     * کاربری که این ویرایش را انجام می‌دهد
     */
    public void recordEdit(Message message, String editorId) {
        // قفل بر اساس شناسه‌ی پیام تا محاسبه‌ی شماره نسخه و ذخیره‌ی آن
        // به‌صورت اتمیک انجام شود و دو ترد هم‌زمان یک شماره نسخه تکراری نسازند
        synchronized (message.getId().intern()) {
            int nextVersion = historyRepository.countByMessageId(message.getChatId(),
                    message.getId()) + 1;
            MessageHistory history = new MessageHistory(
                    IdGenerator.generate(),
                    message.getId(),
                    message.getChatId(),
                    editorId,
                    message.getEncryptedContent(),
                    nextVersion);
            historyRepository.save(history);
        }
    }

    /**
     * دریافت تمام نسخه‌های تاریخچه‌ی یک پیام، به ترتیب قدیمی به جدید،
     * با محتوای رمزگشایی‌شده (قابل‌نمایش برای کاربر).
     *
     * دسترسی فقط برای اعضای چتی که پیام در آن قرار دارد مجاز است
     * (جلوگیری از BOLA - مشابه الگوی امنیتی MessageService).
     */
    public List<MessageHistory> getHistory(String chatId, String messageId, String requesterId) {
        chatRepository.findById(chatId)
                .filter(chat -> chat.getMemberIds().contains(requesterId))
                .orElseThrow(() -> new IllegalStateException("Access denied: you are not a member of this chat."));

        // اطمینان از این‌که پیام واقعاً متعلق به این چت است
        messageRepository.findById(messageId)
                .filter(m -> m.getChatId().equals(chatId))
                .orElseThrow(() -> new IllegalArgumentException("Message not found in this chat."));

        return historyRepository.findByMessageId(chatId, messageId).stream()
                .map(this::toDecryptedCopy)
                .collect(Collectors.toList());
    }

    // حذف تاریخچه هنگام حذف کامل/دائمی یک پیام (در صورت نیاز در آینده).
    public void deleteHistory(String chatId, String messageId) {
        historyRepository.deleteByMessageId(chatId, messageId);
    }

    // یک نسخه‌ی جدید از رکورد تاریخچه می‌سازد که محتوایش رمزگشایی‌شده است،
    // بدون این‌که نسخه‌ی رمزشده‌ی ذخیره‌شده روی دیسک دست‌خورده شود.
    private MessageHistory toDecryptedCopy(MessageHistory original) {
        MessageHistory copy = new MessageHistory();
        copy.setId(original.getId());
        copy.setMessageId(original.getMessageId());
        copy.setChatId(original.getChatId());
        copy.setEditorId(original.getEditorId());
        copy.setEncryptedContentBefore(MessageEncryptor.decrypt(original.getEncryptedContentBefore()));
        copy.setVersion(original.getVersion());
        copy.setEditedAt(original.getEditedAt());
        return copy;
    }
}