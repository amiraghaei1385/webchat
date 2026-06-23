package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

// سرور HTTP برای REST API
// از کتابخانه استاندارد جاوا (com.sun.net.httpserver) استفاده می‌کند
public class HttpApiServer {

    private final int port;
    private HttpServer server;

    // نگه‌داری handler های ثبت‌شده: "METHOD /prefix" -> handler
    // هر handler خودش مسئول تشخیص دقیق زیرمسیرهاست
    private final Map<String, HttpHandler> routes = new HashMap<>();

    public HttpApiServer(int port) {
        this.port = port;
    }

    // ثبت یک endpoint جدید.
    public void register(String method, String path, HttpHandler handler) {
        routes.put(method.toUpperCase() + " " + path, handler);
    }

    // راه‌اندازی سرور
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // تمام درخواست‌ها به یک handler مرکزی می‌روند
        server.createContext("/", exchange -> {
            // افزودن هدرهای CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");

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
        });

        // thread pool برای پاسخ به چند کاربر همزمان
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("HTTP server started on port " + port);
    }

    // یافتن handler مناسب برای یک درخواست.
    // ابتدا exact match بررسی می‌شود (برای مسیرهای ثابت مثل /api/chats)
    private HttpHandler findHandler(String method, String requestPath) {
        String exactKey = method + " " + requestPath;
        if (routes.containsKey(exactKey)) {
            return routes.get(exactKey);
        }

        HttpHandler bestMatch = null;
        int bestLength = -1;
        for (Map.Entry<String, HttpHandler> entry : routes.entrySet()) {
            String key = entry.getKey();
            int spaceIdx = key.indexOf(' ');
            String registeredMethod = key.substring(0, spaceIdx);
            String registeredPath = key.substring(spaceIdx + 1);

            if (!registeredMethod.equals(method))
                continue;

            // پیشوند باید با "/" مرز مسیر را رعایت کند تا مثلا
            boolean matches = requestPath.equals(registeredPath)
                    || requestPath.startsWith(registeredPath + "/");

            if (matches && registeredPath.length() > bestLength) {
                bestMatch = entry.getValue();
                bestLength = registeredPath.length();
            }
        }
        return bestMatch;
    }

    public void stop() {
        if (server != null)
            server.stop(0);
    }

    // ارسال پاسخ JSON
    public static void sendResponse(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}