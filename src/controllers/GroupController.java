package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Group;
import models.GroupMember;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.GroupService;
import java.io.IOException;
import java.util.List;

// مدیریت گروه‌ها 
public class GroupController implements HttpHandler {

    private final GroupService groupService;
    private final SessionManager sessionManager;

    public GroupController(GroupService groupService, SessionManager sessionManager) {
        this.groupService = groupService;
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
            if ("POST".equals(method) && "/api/groups".equals(path)) {
                handleCreateGroup(ctx, user);

            } else if ("GET".equals(method) && path.matches("/api/groups/[^/]+")) {
                handleGetGroup(ctx);

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

    // ساخت گروه //
    // ساخت یک گروه جدید با کاربر فعلی به عنوان مالک
    private void handleCreateGroup(RequestContext ctx, User user) throws IOException {
        String name = parseStr(ctx.getBody(), "name");

        Group group = groupService.createGroup(name, user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 201, groupToJson(group));
    }

    // مشاهده اطلاعات گروه //
    // دریافت اطلاعات گروه به همراه لیست اعضا
    private void handleGetGroup(RequestContext ctx) throws IOException {
        String groupId = extractGroupId(ctx.getPath());

        Group group = groupService.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        List<GroupMember> members = groupService.getMembers(groupId);

        HttpApiServer.sendResponse(ctx.getExchange(), 200, groupWithMembersToJson(group, members));
    }

    // تبدیل مدل به JSON //
    private String groupToJson(Group group) {
        return "{\"id\":\"" + group.getId() + "\","
                + "\"chatId\":\"" + group.getChatId() + "\","
                + "\"name\":\"" + escape(group.getName()) + "\","
                + "\"description\":\"" + escape(group.getDescription()) + "\","
                + "\"ownerId\":\"" + group.getOwnerId() + "\"}";
    }

    private String groupWithMembersToJson(Group group, List<GroupMember> members) {
        StringBuilder membersJson = new StringBuilder("[");
        for (int i = 0; i < members.size(); i++) {
            if (i > 0)
                membersJson.append(",");
            GroupMember m = members.get(i);
            membersJson.append("{\"userId\":\"" + m.getUserId() + "\","
                    + "\"role\":\"" + m.getRole() + "\"}");
        }
        membersJson.append("]");

        return "{\"id\":\"" + group.getId() + "\","
                + "\"chatId\":\"" + group.getChatId() + "\","
                + "\"name\":\"" + escape(group.getName()) + "\","
                + "\"description\":\"" + escape(group.getDescription()) + "\","
                + "\"ownerId\":\"" + group.getOwnerId() + "\","
                + "\"memberCount\":" + members.size() + ","
                + "\"members\":" + membersJson + "}";
    }

    // کمکی //
    // استخراج groupId از مسیر
    private String extractGroupId(String path) {
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

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}