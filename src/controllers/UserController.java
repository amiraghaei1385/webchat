package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.UserService;
import java.io.IOException;

// مدیریت اطلاعات پروفایل کاربران (فاز ۱: فقط مشاهده پروفایل)
public class UserController implements HttpHandler {

    private final UserService userService;
    private final SessionManager sessionManager;

    public UserController(UserService userService, SessionManager sessionManager) {
        this.userService = userService;
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);

        // بررسی احراز هویت برای تمام endpoint های این کنترلر
        User requester = sessionManager.validate(ctx.getSessionToken()).orElse(null);
        if (requester == null) {
            HttpApiServer.sendResponse(exchange, 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }

        String path = ctx.getPath();
        String method = ctx.getMethod();

        try {
            if ("GET".equals(method) && path.matches("/api/users/[^/]+")) {
                handleGetProfile(ctx);
            } else {
                HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Not found.\"}");
            }
        } catch (IllegalArgumentException e) {
            HttpApiServer.sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            HttpApiServer.sendResponse(exchange, 500, "{\"error\":\"Internal server error.\"}");
        }
    }

    // مشاهده پروفایل //
    // دریافت اطلاعات عمومی یک کاربر (بدون اطلاعات حساس مانند پسورد)
    private void handleGetProfile(RequestContext ctx) throws IOException {
        String userId = extractUserId(ctx.getPath());

        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(user));
    }

    // کمکی //
    private String extractUserId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "";
    }

    // تبدیل کاربر به JSON بدون افشای فیلدهای حساس (passwordHash)
    private String userToJson(User user) {
        return "{\"id\":\"" + user.getId() + "\","
                + "\"username\":\"" + escape(user.getUsername()) + "\","
                + "\"profilePicPath\":\"" + escape(user.getProfilePicPath()) + "\","
                + "\"bio\":\"" + escape(user.getBio()) + "\","
                + "\"isOnline\":" + user.isOnline() + ","
                + "\"lastSeenAt\":\"" + (user.getLastSeenAt() != null ? user.getLastSeenAt() : "") + "\"}";
    }

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}