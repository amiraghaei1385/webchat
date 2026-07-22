package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Chat;
import models.Group;
import models.Message;
import models.MessageHistory;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.ChatService;
import services.GroupService;
import services.HistoryService;
import services.MessageService;
import services.PinAndFolderService;
import services.UserService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

// کنترلر چت‌ها و پیام‌ها
public class ChatController implements HttpHandler {

    private final ChatService chatserv;
    private final MessageService messageserv;
    private final HistoryService historyserv;
    private final PinAndFolderService pinfolderserv;
    private final SessionManager sessionManager;
    private final UserService userserv;
    private final GroupService groupserv;

    public ChatController(ChatService chatService, MessageService messageService,
            HistoryService historyService, PinAndFolderService pinAndFolderService,
            SessionManager sessionManager, UserService userService, GroupService groupService) {
        this.chatserv = chatService;
        this.messageserv = messageService;
        this.historyserv = historyService;
        this.pinfolderserv = pinAndFolderService;
        this.sessionManager = sessionManager;
        this.userserv = userService;
        this.groupserv = groupService;
    }

    // ورودی اصلی درخواست‌ها
    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);
        // چک احراز هویت واسه همه‌ی مسیرهای این کنترلر
        User user = sessionManager.validate(ctx.getSessionToken()).orElse(null);
        if (user == null) {
            HttpApiServer.sendResponse(exchange, 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }
        String path = ctx.getPath();
        String method = ctx.getMethod();
        try {
            if (method.equals("GET") && path.equals("/api/chats")) {
                doGetChats(ctx, user);
            } else if (method.equals("POST") && path.equals("/api/chats/private")) {
                doGetOrCreatePrivate(ctx, user);
            } else if (method.equals("POST") && path.matches("/api/chats/[^/]+/read")) {
                doMarkAsRead(ctx, user);
            } else if (method.equals("PUT") && path.matches("/api/chats/[^/]+/pin")) {
                doSetPinned(ctx, user);
            } else if (method.equals("PUT") && path.matches("/api/chats/[^/]+/archive")) {
                doSetArchived(ctx, user);
            } else if (method.equals("GET") && path.matches("/api/chats/[^/]+/messages")) {
                doGetMessages(ctx, user);
            } else if (method.equals("PUT") && path.matches("/api/chats/[^/]+/mute")) {
                doSetMuted(ctx, user);
            } else if (method.equals("POST") && path.matches("/api/chats/[^/]+/messages")) {
                doSendMessage(ctx, user);
            } else if (method.equals("PUT") && path.matches("/api/chats/[^/]+/messages/[^/]+")) {
                doEditMessage(ctx, user);
            } else if (method.equals("DELETE") && path.matches("/api/chats/[^/]+/messages/[^/]+")) {
                doDeleteMessage(ctx, user);
            } else if (method.equals("POST") && path.matches("/api/chats/[^/]+/messages/[^/]+/report")) {
                doReportMessage(ctx, user);
            } else if (method.equals("GET") && path.matches("/api/chats/[^/]+/messages/[^/]+/history")) {
                doGetMessageHistory(ctx, user);
            } else if (method.equals("GET") && path.matches("/api/chats/[^/]+/history")) {
                doGetChatHistory(ctx, user);
            } else {
                HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Not found.\"}");
            }
        } catch (IllegalArgumentException e) {
            HttpApiServer.sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (IllegalStateException e) {
            HttpApiServer.sendResponse(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            HttpApiServer.sendResponse(exchange, 500, "{\"error\":\"Internal server error.\"}");
        }
    }

    // چت‌ها //
    // گرفتن لیست چت‌ها به ترتیب آخرین پیام با تعداد نخونده هر چت
    private void doGetChats(RequestContext ctx, User user) throws IOException {
        List<Chat> chats = chatserv.getUserChats(user.getId());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < chats.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(chatToJson(chats.get(i), user));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // خوانده‌شده کردن چت
    private void doMarkAsRead(RequestContext ctx, User user) throws IOException {
        String idchat = getChatId(ctx.getPath());
        chatserv.markAsRead(idchat, user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Marked as read.\"}");
    }

    private void doSetMuted(RequestContext ctx, User user) throws IOException {
        String idchat = getChatId(ctx.getPath());
        boolean muted = getBool(ctx.getBody(), "muted");
        pinfolderserv.setMuted(idchat, user.getId(), muted);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Updated.\"}");
    }

    // ساخت یا گرفتن چت خصوصی با یه کاربر دیگه
    private void doGetOrCreatePrivate(RequestContext ctx, User user) throws IOException {
        String idusertarget = getStr(ctx.getBody(), "targetUserId");
        Chat chat = chatserv.getOrCreatePrivateChat(user.getId(), idusertarget);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, chatToJson(chat, user));
    }

    // آرشیو یا خارج از آرشیو کردن
    private void doSetArchived(RequestContext ctx, User user) throws IOException {
        String chatId = getChatId(ctx.getPath());
        boolean archived = getBool(ctx.getBody(), "archived");
        pinfolderserv.setArchived(chatId, user.getId(), archived);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Updated.\"}");
    }

    // پین یا برداشتن پین چت
    private void doSetPinned(RequestContext ctx, User user) throws IOException {
        String idchat = getChatId(ctx.getPath());
        boolean pin = getBool(ctx.getBody(), "pinned");
        pinfolderserv.setPinned(idchat, user.getId(), pin);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Updated.\"}");
    }

    // پیام‌ها //
    // گرفتن پیام‌های یه چت
    private void doGetMessages(RequestContext ctx, User user) throws IOException {
        String idchat = getChatId(ctx.getPath());
        List<Message> messages = messageserv.getChatMessages(idchat, user.getId());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(messageToJson(messages.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // فرستادن پیام جدید
    private void doSendMessage(RequestContext ctx, User user) throws IOException {
        String content = getStr(ctx.getBody(), "content");
        String idchat = getChatId(ctx.getPath());
        Message message = messageserv.sendMessage(idchat, user.getId(), content);
        HttpApiServer.sendResponse(ctx.getExchange(), 201, messageToJson(message));
    }

    // حذف یه پیام
    private void doDeleteMessage(RequestContext ctx, User user) throws IOException {
        String idmessage = getMessageId(ctx.getPath());
        messageserv.deleteMessage(idmessage, user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Message deleted.\"}");
    }

    // ویرایش یه پیام
    private void doEditMessage(RequestContext ctx, User user) throws IOException {
        String newcontent = getStr(ctx.getBody(), "content");
        String idmessage = getMessageId(ctx.getPath());
        messageserv.editMessage(idmessage, user.getId(), newcontent);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Message updated.\"}");
    }

    // تاریخچه‌ی کامل یک گفتگو
    private void doGetChatHistory(RequestContext ctx, User user) throws IOException {
        String idchat = getChatId(ctx.getPath());
        List<MessageHistory> listhistory = historyserv.getChatHistory(idchat, user.getId());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < listhistory.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(historyToJson(listhistory.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // گرفتن تاریخچه‌ی ویرایش و حذف یه پیام
    private void doGetMessageHistory(RequestContext ctx, User user) throws IOException {
        String idchat = getChatId(ctx.getPath());
        String idmessage = getMessageId(ctx.getPath());
        List<MessageHistory> history = historyserv.getHistory(idchat, idmessage, user.getId());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(historyToJson(history.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // گزارش کردن یه پیام
    private void doReportMessage(RequestContext ctx, User user) throws IOException {
        String idmessage = getMessageId(ctx.getPath());
        String reason = getStr(ctx.getBody(), "reason");
        messageserv.reportMessage(idmessage, user.getId(), reason);
        HttpApiServer.sendResponse(ctx.getExchange(), 201, "{\"message\":\"Message reported.\"}");
    }

    // تبدیل به جیسون //
    private String chatToJson(Chat chat, User requester) {
        long unreadcount = messageserv.getUnreadCount(chat, requester.getId());
        String extra;
        if (chat.getType() == models.ChatType.GROUP) {
            extra = "\"group\":" + groupToJsonOrNull(chat.getId()) + ",\"peer\":null";
        } else if (chat.getType() == models.ChatType.PRIVATE) {
            extra = "\"peer\":" + peerToJsonOrNull(chat, requester) + ",\"group\":null";
        } else {
            extra = "\"peer\":null,\"group\":null";
        }
        return "{\"id\":\"" + chat.getId() + "\","
                + "\"type\":\"" + chat.getType() + "\","
                + "\"pinned\":" + chat.isPinned() + ","
                + "\"archived\":" + chat.isArchived() + ","
                + "\"muted\":" + chat.isMuted() + ","
                + "\"unreadCount\":" + unreadcount + ","
                + extra + ","
                + "\"lastMessageAt\":\""
                + (chat.getLastMessageAt() != null ? chat.getLastMessageAt() : "") + "\"}";
    }

    // اطلاعات کاربر مقابل رو برای چت خصوصی می‌سازه (نام، آنلاین بودن، عکس پروفایل)
    private String peerToJsonOrNull(Chat chat, User requester) {
        String idpeer = null;
        for (String memberId : chat.getMemberIds()) {
            if (!memberId.equals(requester.getId())) {
                idpeer = memberId;
                break;
            }
        }
        if (idpeer == null) {
            return "null";
        }
        Optional<User> found = userserv.findById(idpeer);
        if (found.isEmpty()) {
            return "null";
        }
        User peer = found.get();
        return "{\"id\":\"" + peer.getId() + "\","
                + "\"username\":\"" + escape(peer.getUsername()) + "\","
                + "\"isOnline\":" + peer.isOnline() + ","
                + "\"profilePicPath\":" + (peer.getProfilePicPath() != null
                        ? "\"" + escape(peer.getProfilePicPath()) + "\""
                        : "null")
                + "}";
    }

    // اطلاعات گروه رو برای چت گروهی می‌سازه (نام، عکس گروه)
    private String groupToJsonOrNull(String idchat) {
        Optional<Group> found = groupserv.findByChatId(idchat);
        if (found.isEmpty()) {
            return "null";
        }
        Group group = found.get();
        return "{\"id\":\"" + group.getId() + "\","
                + "\"name\":\"" + escape(group.getName()) + "\","
                + "\"picturePath\":" + (group.getPicturePath() != null
                        ? "\"" + escape(group.getPicturePath()) + "\""
                        : "null")
                + "}";
    }

    private String messageToJson(Message msg) {
        String idmediamessage = msg.getMediaMessageId();
        String mediapart;
        if (idmediamessage != null) {
            mediapart = "\"" + idmediamessage + "\"";
        } else {
            mediapart = "null";
        }
        return "{\"id\":\"" + msg.getId() + "\","
                + "\"chatId\":\"" + msg.getChatId() + "\","
                + "\"senderId\":\"" + msg.getSenderId() + "\","
                + "\"content\":\"" + escape(msg.getEncryptedContent()) + "\","
                + "\"sentAt\":\"" + msg.getSentAt() + "\","
                + "\"editedAt\":\"" + (msg.getEditedAt() != null ? msg.getEditedAt() : "") + "\","
                + "\"isDeleted\":" + msg.isDeleted() + ","
                + "\"hasMedia\":" + msg.isHasMedia() + ","
                + "\"mediaMessageId\":" + mediapart + "}";
    }

    private String historyToJson(MessageHistory h) {
        return "{\"id\":\"" + h.getId() + "\","
                + "\"messageId\":\"" + h.getMessageId() + "\","
                + "\"editorId\":\"" + h.getEditorId() + "\","
                + "\"contentBefore\":\"" + escape(h.getEncryptedContentBefore()) + "\","
                + "\"isDeletion\":" + h.isDeletion() + ","
                + "\"version\":" + h.getVersion() + ","
                + "\"editedAt\":\"" + (h.getEditedAt() != null ? h.getEditedAt() : "") + "\"}";
    }

    // کمکی //
    // استخراج بخش سوم
    private String getChatId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "";
    }

    // استخراج بخش پنج
    private String getMessageId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 6 ? parts[5] : "";
    }

    // خواندن مقدار رشته‌ای از جیسون دستی
    private String getStr(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1)
            return "";
        int vs = idx + search.length();
        while (vs < json.length() && json.charAt(vs) == ' ')
            vs++;
        if (vs >= json.length() || json.charAt(vs) != '"')
            return "";
        int start = vs + 1;
        int end = json.indexOf('"', start);
        return end == -1 ? "" : json.substring(start, end);
    }

    // خواندن مقدار بولین از جیسون
    private boolean getBool(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1)
            return false;
        int vs = idx + search.length();
        while (vs < json.length() && json.charAt(vs) == ' ')
            vs++;
        return json.startsWith("true", vs);
    }

    // فرار دادن کاراکترهای خاص
    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}