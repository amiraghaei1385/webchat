package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Reaction;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.ReactionService;
import java.io.IOException;
import java.util.*;

// کنترلر ری‌اکشن‌های (
public class ReactionController implements HttpHandler {
    private final ReactionService reactionserv;
    private final SessionManager sessionmanager;

    public ReactionController(ReactionService reactionService, SessionManager sessionManager) {
        this.reactionserv = reactionService;
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
            if (method.equals("POST") && path.matches("/api/messages/[^/]+/reactions")) {
                doToggleReaction(ctx, user);
            } else if (method.equals("GET") && path.matches("/api/messages/[^/]+/reactions/summary")) {
                doGetReactionSummary(ctx);
            } else if (method.equals("GET") && path.matches("/api/messages/[^/]+/reactions")) {
                doGetReactions(ctx);
            } else if (method.equals("DELETE") && path.matches("/api/messages/[^/]+/reactions")) {
                doRemoveReaction(ctx, user);
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

    // دریافت خلاصه‌ی گروه‌بندی‌شده‌ی ری‌اکشن‌ها
    private void doGetReactionSummary(RequestContext ctx) throws IOException {
        String idmessage = getMessageId(ctx.getPath());
        Map<String, Long> summary = reactionserv.getReactionSummary(idmessage);
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Long> entry : summary.entrySet()) {
            if (!first)
                sb.append(",");
            first = false;
            sb.append("\"").append(escape(entry.getKey())).append("\":").append(entry.getValue());
        }
        sb.append("}");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // افزودن یا تغییر یا حذف
    private void doToggleReaction(RequestContext ctx, User user) throws IOException {
        String emoji = getStr(ctx.getBody(), "emoji");
        String idmessage = getMessageId(ctx.getPath());
        Reaction result = reactionserv.toggleReaction(idmessage, user.getId(), emoji);
        if (result == null) {
            HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Reaction removed.\"}");
        } else {
            HttpApiServer.sendResponse(ctx.getExchange(), 200, reactionToJson(result));
        }
    }

    // دریافت تمام ری‌اکشن‌های خام یه پیام
    private void doGetReactions(RequestContext ctx) throws IOException {
        String idmessage = getMessageId(ctx.getPath());
        List<Reaction> reactions = reactionserv.getReactionsForMessage(idmessage);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < reactions.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(reactionToJson(reactions.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // حذف صریح ری‌اکشن کاربر فعلی از یه پیام
    private void doRemoveReaction(RequestContext ctx, User user) throws IOException {
        String idmessage = getMessageId(ctx.getPath());
        reactionserv.removeReaction(idmessage, user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Reaction removed.\"}");
    }

    // تبدیل به جیسن
    private String reactionToJson(Reaction r) {
        return "{\"id\":\"" + r.getId() + "\","
                + "\"messageId\":\"" + r.getMessageId() + "\","
                + "\"userId\":\"" + r.getUserId() + "\","
                + "\"emoji\":\"" + escape(r.getEmoji()) + "\"}";
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

    // فرار دادن کاراکترهای خاص
    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // کمکی //
    private String getMessageId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "";
    }
}