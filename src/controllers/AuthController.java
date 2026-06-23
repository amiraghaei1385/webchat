package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Session;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.AuthService;
import java.io.IOException;

// مدیریت ثبت‌نام، ورود و خروج کاربران
public class AuthController implements HttpHandler {

    private final AuthService authService;
    private final SessionManager sessionManager;

    public AuthController(AuthService authService, SessionManager sessionManager) {
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);
        String path = ctx.getPath();
        String method = ctx.getMethod();

        try {
            if ("POST".equals(method) && "/api/auth/login".equals(path)) {
                handleLogin(ctx);
            } else if ("POST".equals(method) && "/api/auth/register".equals(path)) {
                handleRegister(ctx);
            } else if ("POST".equals(method) && "/api/auth/logout".equals(path)) {
                handleLogout(ctx);
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

    // ورود //
    private void handleLogin(RequestContext ctx) throws IOException {
        String body = ctx.getBody();
        String username = parseStr(body, "username");
        String password = parseStr(body, "password");
        boolean rememberMe = parseBool(body, "rememberMe");

        Session session = authService.login(username, password, rememberMe);

        String response = "{\"token\":\"" + session.getSessionToken() + "\","
                + "\"userId\":\"" + session.getUserId() + "\"}";
        HttpApiServer.sendResponse(ctx.getExchange(), 200, response);
    }

    // ثبت‌نام //
    private void handleRegister(RequestContext ctx) throws IOException {
        String body = ctx.getBody();
        String userId = parseStr(body, "userId");
        String username = parseStr(body, "username");
        String password = parseStr(body, "password");
        String confirmPassword = parseStr(body, "confirmPassword");

        Session session = authService.register(userId, username, password, confirmPassword);

        String response = "{\"token\":\"" + session.getSessionToken() + "\","
                + "\"userId\":\"" + session.getUserId() + "\"}";
        HttpApiServer.sendResponse(ctx.getExchange(), 201, response);
    }

    // خروج //
    private void handleLogout(RequestContext ctx) throws IOException {
        if (sessionManager.validate(ctx.getSessionToken()).isEmpty()) {
            HttpApiServer.sendResponse(ctx.getExchange(), 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }

        authService.logout(ctx.getSessionToken());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Logged out.\"}");
    }

    // کمکی //
    // استخراج مقدار string از JSON بدون کتابخانه خارجی
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

    // استخراج مقدار boolean از JSON
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
}