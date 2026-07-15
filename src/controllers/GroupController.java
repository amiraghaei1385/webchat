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
import java.util.*;

// کنترلر گروه‌ها
public class GroupController implements HttpHandler {

    private final GroupService groupserv;
    private final SessionManager sessionmanager;

    public GroupController(GroupService groupService, SessionManager sessionManager) {
        this.groupserv = groupService;
        this.sessionmanager = sessionManager;
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
        String path = ctx.getPath();
        String method = ctx.getMethod();
        try {
            if (method.equals("POST") && path.equals("/api/groups")) {
                doCreateGroup(ctx, user);
            } else if (method.equals("GET") && path.matches("/api/groups/[^/]+")) {
                doGetGroup(ctx);
            } else if (method.equals("DELETE") && path.matches("/api/groups/[^/]+/members/[^/]+")) {
                doRemoveMember(ctx, user);
            } else if (method.equals("POST") && path.matches("/api/groups/[^/]+/leave")) {
                doLeaveGroup(ctx, user);
            } else if (method.equals("PUT") && path.matches("/api/groups/[^/]+")) {
                doEditGroup(ctx, user);
            } else if (method.equals("POST") && path.matches("/api/groups/[^/]+/members")) {
                doAddMember(ctx, user);
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

    // اینجا گروه ساخته میشه
    private void doCreateGroup(RequestContext ctx, User user) throws IOException {
        String name = getStr(ctx.getBody(), "name");
        Group group = groupserv.createGroup(name, user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 201, groupToJson(group));
    }

    // اینجا عضو حذف میشه
    private void doRemoveMember(RequestContext ctx, User user) throws IOException {
        String[] parts = ctx.getPath().split("/");
        String idtargetuser = parts.length >= 6 ? parts[5] : "";
        String idgroup = parts.length >= 4 ? parts[3] : "";
        groupserv.removeMember(idgroup, user.getId(), idtargetuser);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Member removed.\"}");
    }

    // اینجا اطلاعات گروه برگردونده میشه
    private void doGetGroup(RequestContext ctx) throws IOException {
        String idgroup = getGroupId(ctx.getPath());
        Optional<Group> found = groupserv.findById(idgroup);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Group not found.");
        }
        Group group = found.get();
        List<GroupMember> members = groupserv.getMembers(idgroup);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, groupWithMembersToJson(group, members));
    }

    // اینجا عضو اضافه میشه
    private void doAddMember(RequestContext ctx, User user) throws IOException {
        String idnewuser = getStr(ctx.getBody(), "userId");
        String idgroup = getGroupId(ctx.getPath());
        groupserv.addMember(idgroup, user.getId(), idnewuser);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Member added.\"}");
    }

    // اینجا نام و توضیحات گروه ویرایش میشه
    private void doEditGroup(RequestContext ctx, User user) throws IOException {
        String idgroup = getGroupId(ctx.getPath());
        String newdescription = getStr(ctx.getBody(), "description");
        String newname = getStr(ctx.getBody(), "name");
        groupserv.editGroup(idgroup, user.getId(), newname, newdescription);
        Optional<Group> found = groupserv.findById(idgroup);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Group not found.");
        }
        Group group = found.get();
        HttpApiServer.sendResponse(ctx.getExchange(), 200, groupToJson(group));
    }

    // اینجا کاربر گروه رو ترک میکنه
    private void doLeaveGroup(RequestContext ctx, User user) throws IOException {
        String idgroup = getGroupId(ctx.getPath());
        groupserv.leaveGroup(idgroup, user.getId());
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Left the group.\"}");
    }

    // تبدیل به جیسون //
    private String groupToJson(Group group) {
        return "{\"id\":\"" + group.getId() + "\","
                + "\"chatId\":\"" + group.getChatId() + "\","
                + "\"name\":\"" + escape(group.getName()) + "\","
                + "\"description\":\"" + escape(group.getDescription()) + "\","
                + "\"ownerId\":\"" + group.getOwnerId() + "\"}";
    }

    private String groupWithMembersToJson(Group group, List<GroupMember> members) {
        StringBuilder membersjson = new StringBuilder("[");
        for (int i = 0; i < members.size(); i++) {
            if (i > 0)
                membersjson.append(",");
            GroupMember m = members.get(i);
            membersjson.append("{\"userId\":\"" + m.getUserId() + "\","
                    + "\"role\":\"" + m.getRole() + "\"}");
        }
        membersjson.append("]");
        return "{\"id\":\"" + group.getId() + "\","
                + "\"chatId\":\"" + group.getChatId() + "\","
                + "\"name\":\"" + escape(group.getName()) + "\","
                + "\"description\":\"" + escape(group.getDescription()) + "\","
                + "\"ownerId\":\"" + group.getOwnerId() + "\","
                + "\"memberCount\":" + members.size() + ","
                + "\"members\":" + membersjson + "}";
    }

    private String getGroupId(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "";
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
}