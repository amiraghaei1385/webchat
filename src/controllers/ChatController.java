package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Chat;
import models.Message;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.ChatService;
import services.MessageService;
import java.io.IOException;
import java.util.List;

// مدیریت چت‌ها و پیام‌ها
public class ChatController implements HttpHandler {

    private final ChatService chatService;
    private final MessageService messageService;
    private final SessionManager sessionManager;

    public ChatController(ChatService chatService, MessageService messageService,
            SessionManager sessionManager) {
        this.chatService = chatService;
        this.messageService = messageService;
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);

        // بررسی احراز هویت برای تمام endpoint های این کنترلر
        User user = sessionManager.validate(ctx.getSessionToken()).orElse(null);
        if (user == null) {
            HttpApiServer.sendResponse(exchange, 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }

        String path = ctx.getPath();
        String method = ctx.getMethod();

        try {
            if ("GET".equals(method) && "/api/chats".equals(path)) {
                handleGetChats(ctx, user);

            } else if ("POST".equals(method) && "/api/chats/private".equals(path)) {
                handleGetOrCreatePrivate(ctx, user);

            } else if ("GET".equals(method) && path.matches("/api/chats/[^/]+/messages")) {
                handleGetMessages(ctx, user);

            } else if ("POST".equals(method) && path.matches("/api/chats/[^/]+/messages")) {
                handleSendMessage(ctx, user);

            } else if ("PUT".equals(method) && path.matches("/api/chats/[^/]+/pin")) {
                handleSetPinned(ctx, user);

            } else if ("PUT".equals(method) && path.matches("/api/chats/[^/]+/archive")) {
                handleSetArchived(ctx, user);

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
    // دریافت لیست چت‌های کاربر به ترتیب آخرین پیام
    private void handleGetChats(RequestContext ctx, User user) throws IOException {
        List<Chat> chats = chatService.getUserChats(user.getId());

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < chats.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(chatToJson(chats.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // ایجاد یا دریافت چت خصوصی با کاربر دیگر
    private void handleGetOrCreatePrivate(RequestContext ctx, User user) throws IOException {
        String targetUserId = parseStr(ctx.getBody(), "targetUserId");
        Chat chat = chatService.getOrCreatePrivateChat(user.getId(), targetUserId);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, chatToJson(chat));
    }

    // پین یا آنپین کردن چت
    private void handleSetPinned(RequestContext ctx, User user) throws IOException {
        String chatId = extractChatId(ctx.getPath());
        boolean pinned = parseBool(ctx.getBody(), "pinned");
        chatService.setPinned(chatId, pinned);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Updated.\"}");
    }

    // آرشیو یا خارج کردن از آرشیو
    private void handleSetArchived(RequestContext ctx, User user) throws IOException {
        String chatId = extractChatId(ctx.getPath());
        boolean archived = parseBool(ctx.getBody(), "archived");
        chatService.setArchived(chatId, archived);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Updated.\"}");
    }

    // پیام‌ها //
    // دریافت پیام‌های یک چت
    private void handleGetMessages(RequestContext ctx, User user) throws IOException {
        String chatId = extractChatId(ctx.getPath());
        List<Message> messages = messageService.getChatMessages(chatId, user.getId());

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(messageToJson(messages.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // ارسال پیام جدید
    private void handleSendMessage(RequestContext ctx, User user) throws IOException {
        String chatId = extractChatId(ctx.getPath());
        String content = parseStr(ctx.getBody(), "content");

        Message message = messageService.sendMessage(chatId, user.getId(), content);
        HttpApiServer.sendResponse(ctx.getExchange(), 201, messageToJson(message));
    }

    // تبدیل مدل به JSON //
    private String chatToJson(Chat chat) {
        return "{\"id\":\"" + chat.getId() + "\","
                + "\"type\":\"" + chat.getType() + "\","
                + "\"pinned\":" + chat.isPinned() + ","
                + "\"archived\":" + chat.isArchived() + ","
                + "\"lastMessageAt\":\""
                + (chat.getLastMessageAt() != null ? chat.getLastMessageAt() : "") + "\"}";
    }

    private String messageToJson(Message msg) {
        return "{\"id\":\"" + msg.getId() + "\","
                + "\"chatId\":\"" + msg.getChatId() + "\","
                + "\"senderId\":\"" + msg.getSenderId() + "\","
                + "\"content\":\"" + escape(msg.getEncryptedContent()) + "\","
                + "\"sentAt\":\"" + msg.getSentAt() + "\","
                + "\"isDeleted\":" + msg.isDeleted() + "}";
    }

    // کمکی //
    // استخراج chatId
    private String extractChatId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "";
    }

    private String parseStr(String json, String key) {
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

    private boolean parseBool(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1)
            return false;
        int vs = idx + search.length();
        while (vs < json.length() && json.charAt(vs) == ' ')
            vs++;
        return json.startsWith("true", vs);
    }

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}