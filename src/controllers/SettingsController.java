package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.User;
import models.UserSettings;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.SettingsService;
import java.io.IOException;

// کنترلر تنظیمات کاربر
public class SettingsController implements HttpHandler {
    private final SettingsService settingsserv;
    private final SessionManager sessionmanager;

    public SettingsController(SettingsService settingsService, SessionManager sessionManager) {
        this.settingsserv = settingsService;
        this.sessionmanager = sessionManager;
    }

    // تغییر حالت شب و روز
    private void doSetDarkMode(RequestContext ctx, User user) throws IOException {
        String body = ctx.getBody();
        UserSettings settings;
        if (body != null && body.contains("\"darkMode\"")) {
            boolean darkmode = getBool(body, "darkMode");
            settings = settingsserv.setDarkMode(user.getId(), darkmode);
        } else {
            settings = settingsserv.toggleDarkMode(user.getId());
        }
        HttpApiServer.sendResponse(ctx.getExchange(), 200, settingsToJson(settings));
    }

    // ورودی اصلی درخواست‌ها
    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);
        // چک احراز هویت واسه همه‌ی مسیرهای این کنترلر
        User user = sessionmanager.validate(ctx.getSessionToken()).orElse(null);
        if (user == null) {
            HttpApiServer.sendResponse(exchange, 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }
        String method = ctx.getMethod();
        String path = ctx.getPath();
        try {
            if (method.equals("GET") && path.equals("/api/settings")) {
                doGetSettings(ctx, user);
            } else if (method.equals("PUT") && path.equals("/api/settings/dark-mode")) {
                doSetDarkMode(ctx, user);
            } else {
                HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Not found.\"}");
            }
        } catch (IllegalArgumentException e) {
            HttpApiServer.sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            HttpApiServer.sendResponse(exchange, 500, "{\"error\":\"Internal server error.\"}");
        }
    }

    // اینجا تنظیمات کاربر برگردونده میشه
    private void doGetSettings(RequestContext ctx, User user) throws IOException {
        UserSettings settings = settingsserv.getSettings(user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, settingsToJson(settings));
    }

    // تبدیل به جیسون //
    private String settingsToJson(UserSettings settings) {
        return "{\"userId\":\"" + settings.getUserId() + "\","
                + "\"darkMode\":" + settings.isDarkMode() + ","
                + "\"notificationsEnabled\":" + settings.isNotificationsEnabled() + ","
                + "\"notificationPreviewEnabled\":" + settings.isNotificationPreviewEnabled() + ","
                + "\"language\":\"" + escape(settings.getLanguage()) + "\","
                + "\"backgroundImagePath\":\"" + escape(settings.getBackgroundImagePath()) + "\"}";
    }

    // فرار دادن کاراکترهای خاص
    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
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

}