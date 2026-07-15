package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

// سHTTP .REST API
public class HttpApiServer {
    private final int port;
    private HttpServer server;
    // نگه‌داری مسیرها
    private final Map<String, HttpHandler> routes = new HashMap<>();

    public HttpApiServer(int port) {
        this.port = port;
    }

    // یه مسیر جدید ثبت میشه
    public void register(String method, String path, HttpHandler handler) {
        routes.put(method.toUpperCase() + " " + path, handler);
    }

    // سرور راه اندازی میشه
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("HTTP server started on port " + port);
    }

    // هندلر مرکزی مسیر رو پیدا می‌کنه و به هندلر درست می‌فرسته
    private class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
            // درخواست
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String method = exchange.getRequestMethod().toUpperCase();
            String requestPath = exchange.getRequestURI().getPath();
            HttpHandler handler = findHandler(method, requestPath);
            if (handler != null) {
                handler.handle(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        }
    }

    //  هندلر مسیر پیدا میشه
    private HttpHandler findHandler(String method, String requestPath) {
        String exactKey = method + " " + requestPath;
        if (routes.containsKey(exactKey)) {
            return routes.get(exactKey);
        }
        // تطابق پیشوندی برای زیرمسیرها انجام میشه
        HttpHandler bestMatch = null;
        int bestLength = -1;
        for (Map.Entry<String, HttpHandler> entry : routes.entrySet()) {
            String key = entry.getKey();
            int spaceidx = key.indexOf(' ');
            String methoderegistere = key.substring(0, spaceidx);
            String registerepath = key.substring(spaceidx + 1);

            if (!methoderegistere.equals(method))
                continue;
            boolean matches = requestPath.equals(registerepath)
                    || requestPath.startsWith(registerepath + "/");
            if (matches && registerepath.length() > bestLength) {
                bestMatch = entry.getValue();
                bestLength = registerepath.length();
            }
        }
        return bestMatch;
    }

    // توقف سرور
    public void stop() {
        if (server != null)
            server.stop(0);
    }

    // ارسال پاسخ جسون
    public static void sendResponse(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}