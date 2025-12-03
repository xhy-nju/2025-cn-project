package client;

import common.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP请求构建器
 * 用于构建HTTP请求报文
 */
public class HttpRequestBuilder {
    private HttpMethod method;
    private String host;
    private int port;
    private String path;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String body;

    public HttpRequestBuilder() {
        this.method = HttpMethod.GET;
        this.port = 80;
        this.path = "/";
        this.headers = new LinkedHashMap<>();
        this.queryParams = new LinkedHashMap<>();

        // 默认头部
        headers.put("User-Agent", "SimpleHttpClient/1.0");
        headers.put("Accept", "*/*");
        headers.put("Connection", "keep-alive");
    }

    public HttpRequestBuilder method(HttpMethod method) {
        this.method = method;
        return this;
    }

    public HttpRequestBuilder get() {
        this.method = HttpMethod.GET;
        return this;
    }

    public HttpRequestBuilder post() {
        this.method = HttpMethod.POST;
        return this;
    }

    public HttpRequestBuilder host(String host) {
        this.host = host;
        return this;
    }

    public HttpRequestBuilder port(int port) {
        this.port = port;
        return this;
    }

    public HttpRequestBuilder path(String path) {
        this.path = path;
        return this;
    }

    public HttpRequestBuilder header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public HttpRequestBuilder queryParam(String name, String value) {
        this.queryParams.put(name, value);
        return this;
    }

    public HttpRequestBuilder body(String body) {
        this.body = body;
        return this;
    }

    public HttpRequestBuilder jsonBody(String json) {
        this.body = json;
        this.headers.put("Content-Type", "application/json; charset=UTF-8");
        return this;
    }

    public HttpRequestBuilder formBody(Map<String, String> formData) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            try {
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            first = false;
        }
        this.body = sb.toString();
        this.headers.put("Content-Type", "application/x-www-form-urlencoded");
        return this;
    }

    public HttpRequestBuilder ifModifiedSince(String date) {
        this.headers.put("If-Modified-Since", date);
        return this;
    }

    /**
     * 构建请求报文字符串
     */
    public String build() {
        StringBuilder request = new StringBuilder();

        // 构建URI（包含查询参数）
        String uri = buildUri();

        // 请求行
        request.append(method.name()).append(" ").append(uri).append(" HTTP/1.1\r\n");

        // Host头（必须）
        if (port == 80) {
            request.append("Host: ").append(host).append("\r\n");
        } else {
            request.append("Host: ").append(host).append(":").append(port).append("\r\n");
        }

        // 其他头部
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase("Host")) {
                request.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
            }
        }

        // Content-Length
        if (body != null && !body.isEmpty()) {
            try {
                byte[] bodyBytes = body.getBytes("UTF-8");
                request.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
            } catch (UnsupportedEncodingException e) {
                request.append("Content-Length: ").append(body.length()).append("\r\n");
            }
        }

        // 空行
        request.append("\r\n");

        // 请求体
        if (body != null && !body.isEmpty()) {
            request.append(body);
        }

        return request.toString();
    }

    /**
     * 构建URI
     */
    private String buildUri() {
        if (queryParams.isEmpty()) {
            return path;
        }

        StringBuilder uri = new StringBuilder(path);
        uri.append("?");

        boolean first = true;
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (!first) {
                uri.append("&");
            }
            try {
                uri.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                uri.append("=");
                uri.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                uri.append(entry.getKey()).append("=").append(entry.getValue());
            }
            first = false;
        }

        return uri.toString();
    }

    // Getters
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public HttpMethod getMethod() {
        return method;
    }
}
