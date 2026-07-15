package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Session;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.AuthService;
import services.PasswordResetService;
import java.io.IOException;

// کنترلر ورود ثبت‌نام و بازیابی رمز
public class AuthController implements HttpHandler {

    private final AuthService authserv;
    private final SessionManager sessionmanager;
    private final PasswordResetService passresetserv;

    public AuthController(AuthService authService, SessionManager sessionManager,
            PasswordResetService passwordResetService) {
        this.authserv = authService;
        this.sessionmanager = sessionManager;
        this.passresetserv = passwordResetService;
    }

    // ورودی اصلی درخواست‌ها
    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);
        String path = ctx.getPath();
        String method = ctx.getMethod();

        try {
            if (method.equals("POST") && path.equals("/api/auth/login")) {
                doLogin(ctx);
            } else if (method.equals("POST") && path.equals("/api/auth/register")) {
                doRegister(ctx);
            } else if (method.equals("POST") && path.equals("/api/auth/logout")) {
                doLogout(ctx);
            } else if (method.equals("POST") && path.equals("/api/auth/forgot-password")) {
                doForgotPassword(ctx);
            } else if (method.equals("POST") && path.equals("/api/auth/reset-password")) {
                doResetPassword(ctx);
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

    // اینجا ثبت‌نام کاربر جدید انجام میشه
    private void doRegister(RequestContext ctx) throws IOException {
        String body = ctx.getBody();
        String iduser = getStr(body, "userId");
        String user = getStr(body, "username");
        String pass = getStr(body, "password");
        String confirmpass = getStr(body, "confirmPassword");
        Session session = authserv.register(iduser, user, pass, confirmpass);
        String res = "{\"token\":\"" + session.getSessionToken() + "\","
                + "\"userId\":\"" + session.getUserId() + "\"}";
        HttpApiServer.sendResponse(ctx.getExchange(), 201, res);
    }

    // اینجا ورود کاربر انجام میشه
    private void doLogin(RequestContext ctx) throws IOException {
        String body = ctx.getBody();
        String user = getStr(body, "username");
        String pass = getStr(body, "password");
        boolean rememberme = getBool(body, "rememberMe");
        Session session = authserv.login(user, pass, rememberme);
        String res = "{\"token\":\"" + session.getSessionToken() + "\","
                + "\"userId\":\"" + session.getUserId() + "\"}";
        HttpApiServer.sendResponse(ctx.getExchange(), 200, res);
    }

    // اینجا خروج کاربر انجام میشه
    private void doLogout(RequestContext ctx) throws IOException {
        if (sessionmanager.validate(ctx.getSessionToken()).isEmpty()) {
            HttpApiServer.sendResponse(ctx.getExchange(), 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }
        authserv.logout(ctx.getSessionToken());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Logged out.\"}");
    }

    // تایید نهایی و تنظیم رمز جدید
    private void doResetPassword(RequestContext ctx) throws IOException {
        String body = ctx.getBody();
        String token = getStr(body, "token");
        String temppass = getStr(body, "tempPassword");
        String newpass = getStr(body, "newPassword");
        String newPassconfirm = getStr(body, "confirmNewPassword");
        passresetserv.confirmReset(token, temppass, newpass, newPassconfirm);
        HttpApiServer.sendResponse(ctx.getExchange(), 200,
                "{\"message\":\"Password has been reset successfully.\"}");
    }

    // درخواست بازیابی رمز، توکن و رمز موقت مستقیم برمیگرده چون ایمیل نداریم
    private void doForgotPassword(RequestContext ctx) throws IOException {
        String body = ctx.getBody();
        String user = getStr(body, "username");
        PasswordResetService.ResetRequestResult res = passresetserv.requestReset(user);
        String response = "{\"token\":\"" + res.getToken() + "\","
                + "\"tempPassword\":\"" + escape(res.getTempPassword()) + "\"}";
        HttpApiServer.sendResponse(ctx.getExchange(), 200, response);
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