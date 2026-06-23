package server;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// نگه‌داری اطلاعات هر درخواست HTTP
public class RequestContext {

    private final String method;
    private final String path;
    private final Map<String, String> queryParams;
    private final String body;
    private final String sessionToken;
    private final HttpExchange exchange;

    public RequestContext(HttpExchange exchange) throws IOException {
        this.exchange = exchange;
        this.method = exchange.getRequestMethod();
        this.path = exchange.getRequestURI().getPath();
        this.queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
        this.body = readBody(exchange.getRequestBody());
        this.sessionToken = exchange.getRequestHeaders().getFirst("Authorization");
    }

    // خواندن بدنه درخواست
    private String readBody(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    // پارس کردن query string به map
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null)
            return params;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                params.put(parts[0], parts[1]);
            }
        }
        return params;
    }

    // Getters
    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getBody() {
        return body;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public HttpExchange getExchange() {
        return exchange;
    }
}