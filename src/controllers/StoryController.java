package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Story;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.StoryService;
import java.io.IOException;
import java.util.*;

// کنترلر استوری
public class StoryController implements HttpHandler {
    private final StoryService storyserv;
    private final SessionManager sessionmanager;

    public StoryController(StoryService storyService, SessionManager sessionManager) {
        this.storyserv = storyService;
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
            if (method.equals("GET") && path.equals("/api/stories/feed")) {
                doGetHomeFeed(ctx, user);
            } else if (method.equals("POST") && path.matches("/api/stories/[^/]+/view")) {
                doMarkAsViewed(ctx, user);
            } else if (method.equals("POST") && path.equals("/api/stories")) {
                doCreateStory(ctx, user);
            } else if (method.equals("GET") && path.matches("/api/stories/user/[^/]+")) {
                doGetUserStories(ctx);
            } else if (method.equals("DELETE") && path.matches("/api/stories/[^/]+")) {
                doDeleteStory(ctx, user);
            } else if (method.equals("GET") && path.matches("/api/stories/[^/]+/media")) {
                doDownloadStoryMedia(ctx);
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

    // دریافت فید استوری‌های صفحه هوم
    private void doGetHomeFeed(RequestContext ctx, User user) throws IOException {
        List<StoryService.StoryFeedGroup> feed = storyserv.getHomeFeed(user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, feedToJson(feed));
    }

    // انتشار استوری جدید
    private void doCreateStory(RequestContext ctx, User user) throws IOException {
        String body = ctx.getBody();
        String mediatypestr = getStr(body, "mediaType");
        Story.MediaType mediatype;
        try {
            mediatype = Story.MediaType.valueOf(mediatypestr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid mediaType. Must be one of: IMAGE, VIDEO, TEXT.");
        }
        Story story;
        if (mediatype == Story.MediaType.TEXT) {
            String text = getStr(body, "text");
            story = storyserv.createTextStory(user.getId(), text);
        } else {
            String filebase64 = getStr(body, "fileBase64");
            String originalfilename = getStr(body, "originalFileName");
            String mimetype = getStr(body, "mimeType");
            String caption = getStr(body, "caption");
            if (filebase64.isEmpty()) {
                throw new IllegalArgumentException("File content is required.");
            }
            byte[] filebytes;
            try {
                filebytes = Base64.getDecoder().decode(filebase64);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid base64 file content.");
            }
            story = storyserv.createMediaStory(user.getId(), filebytes, originalfilename,
                    mimetype, mediatype, caption.isEmpty() ? null : caption);
        }
        HttpApiServer.sendResponse(ctx.getExchange(), 201, storyToJson(story));
    }

    // دانلود فایل رسانه‌ی استوری
    private void doDownloadStoryMedia(RequestContext ctx) throws IOException {
        String idstory = getStoryId(ctx.getPath());
        byte[] data = storyserv.downloadStoryMedia(idstory);
        String base64data = Base64.getEncoder().encodeToString(data);
        HttpApiServer.sendResponse(ctx.getExchange(), 200,
                "{\"storyId\":\"" + idstory + "\",\"fileBase64\":\"" + base64data + "\"}");
    }

    // حذف استوری
    private void doDeleteStory(RequestContext ctx, User user) throws IOException {
        String idstory = getStoryId(ctx.getPath());
        storyserv.deleteStory(idstory, user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Story deleted.\"}");
    }

    // ثبت بازدید استوری
    private void doMarkAsViewed(RequestContext ctx, User user) throws IOException {
        String idstory = getStoryId(ctx.getPath());
        Story story = storyserv.markAsViewed(idstory, user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, storyToJson(story));
    }

    // دریافت استوری‌های فعال یه کاربر خاص
    private void doGetUserStories(RequestContext ctx) throws IOException {
        String idowner = getUserId(ctx.getPath());
        List<Story> stories = storyserv.getActiveStoriesByOwner(idowner);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < stories.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(storyToJson(stories.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // تبدیل به جیسون //
    private String feedToJson(List<StoryService.StoryFeedGroup> feed) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < feed.size(); i++) {
            if (i > 0)
                sb.append(",");
            StoryService.StoryFeedGroup group = feed.get(i);
            sb.append("{\"ownerId\":\"").append(group.getOwnerId()).append("\",");
            sb.append("\"hasUnseen\":").append(group.isHasUnseen()).append(",");
            sb.append("\"stories\":[");
            List<Story> stories = group.getStories();
            for (int j = 0; j < stories.size(); j++) {
                if (j > 0)
                    sb.append(",");
                sb.append(storyToJson(stories.get(j)));
            }
            sb.append("]}");
        }
        sb.append("]");
        return sb.toString();
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

    private String storyToJson(Story s) {
        return "{\"id\":\"" + s.getId() + "\","
                + "\"ownerId\":\"" + s.getOwnerId() + "\","
                + "\"mediaType\":\"" + s.getMediaType() + "\","
                + "\"caption\":\"" + escape(s.getCaption()) + "\","
                + "\"createdAt\":\"" + (s.getCreatedAt() != null ? s.getCreatedAt() : "") + "\","
                + "\"expiresAt\":\"" + (s.getExpiresAt() != null ? s.getExpiresAt() : "") + "\","
                + "\"viewCount\":" + s.getViewerIds().size() + "}";
    }

    // فرار دادن کاراکترهای خاص
    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // کمکی //
    private String getStoryId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "";
    }

    private String getUserId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 5 ? parts[4] : "";
    }
}