package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Chat;
import models.Message;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.SearchService;
import java.io.IOException;
import java.util.List;

// کنترلر جستجو بین چت‌ها و پیام‌ها
public class SearchController implements HttpHandler {
    private final SearchService searchserv;
    private final SessionManager sessionmanager;

    public SearchController(SearchService searchService, SessionManager sessionManager) {
        this.searchserv = searchService;
        this.sessionmanager = sessionManager;
    }

    // ورودی اصلی درخواست‌ها
    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);
        User user = sessionmanager.validate(ctx.getSessionToken()).orElse(null);
        if (user == null) {
            HttpApiServer.sendResponse(exchange, 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }
        String path = ctx.getPath();
        String method = ctx.getMethod();
        try {
            if (method.equals("GET") && path.equals("/api/search/chats")) {
                doSearchChats(ctx, user);
            } else if (method.equals("GET") && path.matches("/api/search/messages/[^/]+")) {
                doSearchMessages(ctx, user);
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

    // جستجو در پیام‌های یه چت
    private void doSearchMessages(RequestContext ctx, User user) throws IOException {
        String query = ctx.getQueryParams().get("q");
        String idchat = getChatId(ctx.getPath());
        List<Message> messages = searchserv.searchMessagesInChat(idchat, user.getId(), query);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(messageToJson(messages.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // جستجو در چت‌ها
    private void doSearchChats(RequestContext ctx, User user) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        String query = ctx.getQueryParams().get("q");
        List<Chat> chats = searchserv.searchChats(user.getId(), query);
        for (int i = 0; i < chats.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(chatToJson(chats.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // تبدیل به جیسون //
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
                + "\"sentAt\":\"" + msg.getSentAt() + "\"}";
    }

    // فرار دادن کاراکترهای خاص
    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // کمکی //
    private String getChatId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 5 ? parts[4] : "";
    }

}