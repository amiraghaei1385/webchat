package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.MediaMessage;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.MediaService;
import java.io.IOException;
import java.util.*;

// کنترلر آپلود دانلود ویرایش و حذف رسانه
public class MediaController implements HttpHandler {

    private final MediaService mediaserv;
    private final SessionManager sessionmanager;

    public MediaController(MediaService mediaService, SessionManager sessionManager) {
        this.mediaserv = mediaService;
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
            if (method.equals("POST") && path.matches("/api/media/[^/]+")) {
                doUploadMedia(ctx, user);
            } else if (method.equals("PUT") && path.matches("/api/media/[^/]+/[^/]+")) {
                doEditCaption(ctx, user);
            } else if (method.equals("DELETE") && path.matches("/api/media/[^/]+/[^/]+")) {
                doDeleteMedia(ctx, user);
            } else if (method.equals("GET") && path.matches("/api/media/[^/]+/[^/]+/info")) {
                doGetMediaInfo(ctx);
            } else if (method.equals("GET") && path.matches("/api/media/[^/]+/[^/]+")) {
                doDownloadMedia(ctx, user);
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

    // دانلود فایل رسانه
    private void doDownloadMedia(RequestContext ctx, User user) throws IOException {
        String idmessage = getMessageId(ctx.getPath());
        String idchat = getChatId(ctx.getPath());
        byte[] data = mediaserv.downloadMedia(idchat, idmessage, user.getId());
        Optional<MediaMessage> found = mediaserv.findMetadata(idchat, idmessage);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Media not found.");
        }
        MediaMessage media = found.get();
        String base64data = Base64.getEncoder().encodeToString(data);
        String response = "{\"messageId\":\"" + media.getMessageId() + "\","
                + "\"mimeType\":\"" + escape(media.getMimeType()) + "\","
                + "\"originalFileName\":\"" + escape(media.getOriginalFileName()) + "\","
                + "\"fileBase64\":\"" + base64data + "\"}";
        HttpApiServer.sendResponse(ctx.getExchange(), 200, response);
    }

    // حذف رسانه
    private void doDeleteMedia(RequestContext ctx, User user) throws IOException {
        String idchat = getChatId(ctx.getPath());
        String idmessage = getMessageId(ctx.getPath());
        mediaserv.deleteMedia(idchat, idmessage, user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Media deleted.\"}");
    }

    // آپلود رسانه‌ی جدید در یک چت
    private void doUploadMedia(RequestContext ctx, User user) throws IOException {
        String body = ctx.getBody();
        String file = getStr(body, "fileBase64");
        String idchat = getChatId(ctx.getPath());
        String originalfilename = getStr(body, "originalFileName");
        String mimetype = getStr(body, "mimeType");
        String mediatypestr = getStr(body, "mediaType");
        String caption = getStr(body, "caption");
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File content (fileBase64) is required.");
        }
        MediaMessage.MediaType mediatype;
        try {
            mediatype = MediaMessage.MediaType.valueOf(mediatypestr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid mediaType. Must be one of: IMAGE, VIDEO, AUDIO, VOICE, DOCUMENT, STICKER.");
        }
        byte[] filebytes;
        try {
            filebytes = Base64.getDecoder().decode(file);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 file content.");
        }
        MediaMessage media = mediaserv.uploadMedia(idchat, user.getId(), filebytes,
                originalfilename, mimetype, mediatype, caption.isEmpty() ? null : caption);
        HttpApiServer.sendResponse(ctx.getExchange(), 201, mediaMetaToJson(media));
    }

    // ویرایش کپشن رسانه
    private void doEditCaption(RequestContext ctx, User user) throws IOException {
        String idmessage = getMessageId(ctx.getPath());
        String idchat = getChatId(ctx.getPath());
        String newcaption = getStr(ctx.getBody(), "caption");
        mediaserv.editCaption(idchat, idmessage, user.getId(), newcaption);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Caption updated.\"}");
    }

    // دریافت فقط متادیتای رسانه بدون بایت‌های فایل
    private void doGetMediaInfo(RequestContext ctx) throws IOException {
        String idmessage = getMessageId(ctx.getPath());
        String idchat = getChatId(ctx.getPath());
        Optional<MediaMessage> found = mediaserv.findMetadata(idchat, idmessage);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Media not found.");
        }
        MediaMessage media = found.get();
        HttpApiServer.sendResponse(ctx.getExchange(), 200, mediaMetaToJson(media));
    }

    // تبدیل به جیسون
    private String mediaMetaToJson(MediaMessage m) {
        return "{\"messageId\":\"" + m.getMessageId() + "\","
                + "\"chatId\":\"" + m.getChatId() + "\","
                + "\"senderId\":\"" + m.getSenderId() + "\","
                + "\"mediaType\":\"" + m.getMediaType() + "\","
                + "\"originalFileName\":\"" + escape(m.getOriginalFileName()) + "\","
                + "\"mimeType\":\"" + escape(m.getMimeType()) + "\","
                + "\"fileSizeBytes\":" + m.getFileSizeBytes() + ","
                + "\"caption\":\"" + escape(m.getCaption()) + "\","
                + "\"sentAt\":\"" + (m.getSentAt() != null ? m.getSentAt() : "") + "\","
                + "\"isDeleted\":" + m.isDeleted() + "}";
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

    // کمکی //
    private String getChatId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "";
    }

    private String getMessageId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 5 ? parts[4] : "";
    }

    // فرار دادن کاراکترهای خاص
    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}