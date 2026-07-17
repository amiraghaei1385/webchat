package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.UserService;
import java.io.IOException;
import java.util.Optional;
import java.util.Base64;

// کنترلر پروفایل کاربران
public class UserController implements HttpHandler {
    private final UserService userserv;
    private final SessionManager sessionmanager;

    public UserController(UserService userService, SessionManager sessionManager) {
        this.userserv = userService;
        this.sessionmanager = sessionManager;
    }

    // ورودی اصلی درخواست‌ها
    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);
        // چک احراز هویت واسه همه‌ی مسیرهای این کنترلر
        User requester = sessionmanager.validate(ctx.getSessionToken()).orElse(null);
        if (requester == null) {
            HttpApiServer.sendResponse(exchange, 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }
        String path = ctx.getPath();
        String method = ctx.getMethod();
        try {
            if (method.equals("GET") && path.matches("/api/users/[^/]+")) {
                doGetProfile(ctx);
            } else if (method.equals("PUT") && path.equals("/api/users/me/username")) {
                doUpdateUsername(ctx, requester);
            } else if (method.equals("PUT") && path.equals("/api/users/me/id")) {
                doUpdateUserId(ctx, requester);
            } else if (method.equals("DELETE") && path.equals("/api/users/me/email")) {
                doRemoveEmail(ctx, requester);
            } else if (method.equals("PUT") && path.equals("/api/users/me/bio")) {
                doUpdateBio(ctx, requester);
            } else if (method.equals("PUT") && path.equals("/api/users/me/email")) {
                doUpdateEmail(ctx, requester);
            } else if (method.equals("PUT") && path.equals("/api/users/me/birth-date")) {
                doUpdateBirthDate(ctx, requester);
            } else if (method.equals("DELETE") && path.equals("/api/users/me/birth-date")) {
                doRemoveBirthDate(ctx, requester);
            } else if (method.equals("PUT") && path.equals("/api/users/me/background-picture")) {
                doUpdateBackgroundPicture(ctx, requester);
            } else if (method.equals("PUT") && path.equals("/api/users/me/profile-picture")) {
                doUpdateProfilePicture(ctx, requester);
            } else if (method.equals("DELETE") && path.equals("/api/users/me/background-picture")) {
                doRemoveBackgroundPicture(ctx, requester);
            } else if (method.equals("POST") && path.equals("/api/users/me/profile-picture/upload")) {
                doUploadProfilePicture(ctx, requester);
            } else if (method.equals("DELETE") && path.equals("/api/users/me/profile-picture")) {
                doRemoveProfilePicture(ctx, requester);
            } else if (method.equals("DELETE") && path.equals("/api/users/me")) {
                doDeleteAccount(ctx, requester);
            } else {
                HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Not found.\"}");
            }
        } catch (IllegalArgumentException e) {
            HttpApiServer.sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            HttpApiServer.sendResponse(exchange, 500, "{\"error\":\"Internal server error.\"}");
        }
    }

    // مشاهده پروفایل، اطلاعات عمومی کاربر بدون فیلد حساس
    private void doGetProfile(RequestContext ctx) throws IOException {
        String iduser = getUserId(ctx.getPath());
        Optional<User> found = userserv.findById(iduser);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = found.get();
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(user));
    }

    // ویرایش پروفایل //
    private void doUploadProfilePicture(RequestContext ctx, User requester) throws IOException {
        String body = ctx.getBody();
        String fileBase64 = getStr(body, "fileBase64");
        String originalFileName = getStr(body, "originalFileName");
        if (fileBase64.isEmpty()) {
            throw new IllegalArgumentException("File content (fileBase64) is required.");
        }
        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(fileBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 file content.");
        }
        User updated = userserv.saveProfilePictureFile(requester.getId(), fileBytes, originalFileName);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // تغییر آیدی منحصربه‌فرد کاربر
    private void doUpdateUserId(RequestContext ctx, User requester) throws IOException {
        String newuserid = getStr(ctx.getBody(), "userId");
        User updated = userserv.updateUserId(requester.getId(), newuserid);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // تغییر نام نمایشی کاربر
    private void doUpdateUsername(RequestContext ctx, User requester) throws IOException {
        String newusername = getStr(ctx.getBody(), "username");
        User updated = userserv.updateUsername(requester.getId(), newusername);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // حذف ایمیل اربر
    private void doRemoveEmail(RequestContext ctx, User requester) throws IOException {
        User updated = userserv.removeEmail(requester.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // ثبت یا تغییر ایمیل کاربر
    private void doUpdateEmail(RequestContext ctx, User requester) throws IOException {
        String newemail = getStr(ctx.getBody(), "email");
        User updated = userserv.updateEmail(requester.getId(), newemail);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // بایو
    private void doUpdateBio(RequestContext ctx, User requester) throws IOException {
        String newbio = getStr(ctx.getBody(), "bio");
        User updated = userserv.updateBio(requester.getId(), newbio);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // حذف تاریخ تولد کاربر
    private void doRemoveBirthDate(RequestContext ctx, User requester) throws IOException {
        User updated = userserv.removeBirthDate(requester.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // ثبت یا تغییر تاریخ تولد کاربر، فرمت
    private void doUpdateBirthDate(RequestContext ctx, User requester) throws IOException {
        String newbirthdate = getStr(ctx.getBody(), "birthDate");
        User updated = userserv.updateBirthDate(requester.getId(), newbirthdate);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // تنظیم عکس پروفایل، مسیر فایلی که قبلاً آپلود شده
    private void doUpdateProfilePicture(RequestContext ctx, User requester) throws IOException {
        String picturepath = getStr(ctx.getBody(), "picturePath");
        User updated = userserv.updateProfilePicture(requester.getId(), picturepath);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // حذف عکس پروفایل
    private void doRemoveProfilePicture(RequestContext ctx, User requester) throws IOException {
        User updated = userserv.removeProfilePicture(requester.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // حذف عکس پس‌زمینه
    private void doRemoveBackgroundPicture(RequestContext ctx, User requester) throws IOException {
        User updated = userserv.removeBackgroundPicture(requester.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // تنظیم عکس پس‌زمینه
    private void doUpdateBackgroundPicture(RequestContext ctx, User requester) throws IOException {
        String picturepath = getStr(ctx.getBody(), "picturePath");
        User updated = userserv.updateBackgroundPicture(requester.getId(), picturepath);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, userToJson(updated));
    }

    // حذف حساب کاربری (حذف نرم)
    private void doDeleteAccount(RequestContext ctx, User requester) throws IOException {
        userserv.deleteAccount(requester.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Account deleted.\"}");
    }

    // تبدیل کاربر به جیسون
    private String userToJson(User user) {
        return "{\"id\":\"" + user.getId() + "\","
                + "\"username\":\"" + escape(user.getUsername()) + "\","
                + "\"profilePicPath\":\"" + escape(user.getProfilePicPath()) + "\","
                + "\"backgroundPicPath\":\"" + escape(user.getBackgroundPicPath()) + "\","
                + "\"bio\":\"" + escape(user.getBio()) + "\","
                + "\"email\":\"" + escape(user.getEmail()) + "\","
                + "\"birthDate\":\"" + (user.getBirthDate() != null ? user.getBirthDate() : "") + "\","
                + "\"isOnline\":" + user.isOnline() + ","
                + "\"lastSeenAt\":\"" + (user.getLastSeenAt() != null ? user.getLastSeenAt() : "") + "\"}";
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
    private String getUserId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "";
    }
}