package services;

import models.Message;
import models.MessageHistory;
import repository.ChatRepository;
import repository.HistoryRepository;
import repository.MessageRepository;
import security.MessageEncryptor;
import utils.IdGenerator;
import java.util.*;

// مدیریت تاریخچه ویرایش و حذف پیام‌ها
public class HistoryService {

    private final HistoryRepository historyrepo;
    private final ChatRepository chatrepo;
    private final MessageRepository messagerepo;

    public HistoryService(HistoryRepository historyRepository,
            ChatRepository chatRepository,
            MessageRepository messageRepository) {
        this.historyrepo = historyRepository;
        this.chatrepo = chatRepository;
        this.messagerepo = messageRepository;
    }

    // تاریخچه برای حذف پیام ثبت میشه
    public void recordDeletion(Message message, String editorId) {
        recordHistoryEntry(message, editorId, true);
    }

    // تاریخچه یک پیام کامل حذف میشه
    public void deleteHistory(String chatId, String messageId) {
        historyrepo.deleteByMessageId(chatId, messageId);
    }

    // ثبت تاریخچه برای ویرایش پیام
    public void recordEdit(Message message, String editorId) {
        recordHistoryEntry(message, editorId, false);
    }

    // دریافت تاریخچه یک پیام
    public List<MessageHistory> getHistory(String chatId, String messageId, String requesterId) {
        Optional<models.Chat> optchat = chatrepo.findById(chatId);
        if (optchat.isEmpty() || !optchat.get().getMemberIds().contains(requesterId)) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        Optional<Message> optmessage = messagerepo.findById(messageId);
        if (optmessage.isEmpty() || !optmessage.get().getChatId().equals(chatId)) {
            throw new IllegalArgumentException("Message not found in this chat.");
        }
        List<MessageHistory> res = new ArrayList<>();
        for (MessageHistory item : historyrepo.findByMessageId(chatId, messageId)) {
            res.add(toDecryptedCopy(item));
        }
        return res;
    }

    // دریافت کل تاریخچه یک گفتگو
    public List<MessageHistory> getChatHistory(String chatId, String requesterId) {
        Optional<models.Chat> optchat = chatrepo.findById(chatId);
        if (optchat.isEmpty() || !optchat.get().getMemberIds().contains(requesterId)) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        List<MessageHistory> listfromrepo = historyrepo.findByChatId(chatId);
        List<MessageHistory> res = new ArrayList<>();
        for (MessageHistory item : listfromrepo) {
            res.add(toDecryptedCopy(item));
        }
        return res;
    }

    // منطق مشترک ثبت یک نسخه جدید
    private void recordHistoryEntry(Message message, String editorId, boolean isDeletion) {
        synchronized (message.getId().intern()) {
            int nextVersion = historyrepo.countByMessageId(message.getChatId(), message.getId()) + 1;
            MessageHistory history = new MessageHistory(
                    IdGenerator.generate(),
                    message.getId(),
                    message.getChatId(),
                    editorId,
                    message.getEncryptedContent(),
                    nextVersion,
                    isDeletion);
            historyrepo.save(history);
        }
    }

    // یک کپی تاریخچه میسازه
    private MessageHistory toDecryptedCopy(MessageHistory original) {
        MessageHistory copy = new MessageHistory();
        copy.setDeletion(original.isDeletion());
        copy.setId(original.getId());
        copy.setEditorId(original.getEditorId());
        copy.setMessageId(original.getMessageId());
        copy.setEncryptedContentBefore(MessageEncryptor.decrypt(original.getEncryptedContentBefore()));
        copy.setVersion(original.getVersion());
        copy.setChatId(original.getChatId());
        copy.setEditedAt(original.getEditedAt());
        return copy;
    }
}