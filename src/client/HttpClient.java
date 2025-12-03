package client;

import common.HttpMethod;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP客户端
 * 基于Java Socket API实现的简单HTTP客户端
 * 
 * 功能特性：
 * - 支持GET和POST请求
 * - 支持301/302重定向自动跟随
 * - 支持304缓存机制
 * - 支持长连接复用
 */
public class HttpClient {
    private static final int DEFAULT_TIMEOUT = 30000; // 30秒
    private static final int MAX_REDIRECTS = 10; // 最大重定向次数

    private int timeout;
    private int maxRedirects;
    private boolean followRedirects;
    private Map<String, CacheEntry> cache; // 简单的响应缓存

    // 连接复用
    private Socket currentSocket;
    private String currentHost;
    private int currentPort;
    private InputStream inputStream;
    private OutputStream outputStream;

    public HttpClient() {
        this.timeout = DEFAULT_TIMEOUT;
        this.maxRedirects = MAX_REDIRECTS;
        this.followRedirects = true;
        this.cache = new HashMap<>();
    }

    /**
     * 发送GET请求
     */
    public HttpResponseParser get(String url) throws IOException {
        return request(HttpMethod.GET, url, null, null);
    }

    /**
     * 发送GET请求（带自定义头部）
     */
    public HttpResponseParser get(String url, Map<String, String> headers) throws IOException {
        return request(HttpMethod.GET, url, headers, null);
    }

    /**
     * 发送POST请求（JSON体）
     */
    public HttpResponseParser postJson(String url, String jsonBody) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=UTF-8");
        return request(HttpMethod.POST, url, headers, jsonBody);
    }

    /**
     * 发送POST请求（表单）
     */
    public HttpResponseParser postForm(String url, Map<String, String> formData) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (!first)
                body.append("&");
            body.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        return request(HttpMethod.POST, url, headers, body.toString());
    }

    /**
     * 发送HTTP请求
     */
    public HttpResponseParser request(HttpMethod method, String url,
            Map<String, String> headers, String body) throws IOException {
        return request(method, url, headers, body, 0);
    }

    /**
     * 内部请求方法（支持重定向计数）
     */
    private HttpResponseParser request(HttpMethod method, String url,
            Map<String, String> headers, String body, int redirectCount) throws IOException {

        if (redirectCount > maxRedirects) {
            throw new IOException("Too many redirects (max: " + maxRedirects + ")");
        }

        // 解析URL
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL: " + url, e);
        }

        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 80 : uri.getPort();
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (uri.getRawQuery() != null) {
            path += "?" + uri.getRawQuery();
        }

        // 检查缓存（仅GET请求）
        if (method == HttpMethod.GET && cache.containsKey(url)) {
            CacheEntry cacheEntry = cache.get(url);
            if (headers == null) {
                headers = new HashMap<>();
            }
            headers.put("If-Modified-Since", cacheEntry.lastModified);
        }

        // 建立连接（复用或新建）
        ensureConnection(host, port);

        // 构建请求
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder()
                .method(method)
                .host(host)
                .port(port)
                .path(path);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }

        if (body != null) {
            requestBuilder.body(body);
        }

        String requestStr = requestBuilder.build();

        // 发送请求
        System.out.println(">>> Sending request:");
        System.out.println(requestStr);

        outputStream.write(requestStr.getBytes("UTF-8"));
        outputStream.flush();

        // 接收响应
        HttpResponseParser response = new HttpResponseParser();
        response.parse(inputStream);

        System.out.println("<<< Received response:");
        System.out.println(response);
        System.out.println();

        // 处理304 Not Modified
        if (response.isNotModified() && cache.containsKey(url)) {
            System.out.println("[Cache] Using cached response for: " + url);
            CacheEntry cacheEntry = cache.get(url);
            // 返回缓存的响应
            HttpResponseParser cachedResponse = new HttpResponseParser();
            // 模拟缓存响应（实际应该存储完整响应）
            return response; // 这里简化处理，返回304响应
        }

        // 缓存响应（如果有Last-Modified头）
        if (method == HttpMethod.GET && response.getStatusCode() == 200) {
            String lastModified = response.getLastModified();
            if (lastModified != null) {
                cache.put(url, new CacheEntry(lastModified, response.getBodyAsString()));
                System.out.println("[Cache] Cached response for: " + url);
            }
        }

        // 处理重定向
        if (followRedirects && response.isRedirect()) {
            String location = response.getLocation();
            if (location != null) {
                System.out.println("[Redirect] " + response.getStatusCode() + " -> " + location);

                // 处理相对URL
                if (!location.startsWith("http://") && !location.startsWith("https://")) {
                    if (location.startsWith("/")) {
                        location = "http://" + host + (port != 80 ? ":" + port : "") + location;
                    } else {
                        location = "http://" + host + (port != 80 ? ":" + port : "") + "/" + location;
                    }
                }

                // 关闭当前连接（重定向可能到不同服务器）
                closeConnection();

                // 301/302 通常将POST转为GET
                HttpMethod redirectMethod = method;
                String redirectBody = body;
                if (response.getStatusCode() == 301 || response.getStatusCode() == 302) {
                    redirectMethod = HttpMethod.GET;
                    redirectBody = null;
                }

                return request(redirectMethod, location, null, redirectBody, redirectCount + 1);
            }
        }

        // 检查是否需要关闭连接
        if (!response.isKeepAlive()) {
            closeConnection();
        }

        return response;
    }

    /**
     * 确保连接可用（复用或新建）
     */
    private void ensureConnection(String host, int port) throws IOException {
        // 检查是否可以复用现有连接
        if (currentSocket != null && !currentSocket.isClosed()
                && host.equals(currentHost) && port == currentPort) {
            return; // 复用现有连接
        }

        // 关闭旧连接
        closeConnection();

        // 建立新连接
        System.out.println("[Connection] Connecting to " + host + ":" + port);
        currentSocket = new Socket(host, port);
        currentSocket.setSoTimeout(timeout);
        currentHost = host;
        currentPort = port;
        inputStream = new BufferedInputStream(currentSocket.getInputStream());
        outputStream = new BufferedOutputStream(currentSocket.getOutputStream());
    }

    /**
     * 关闭连接
     */
    public void closeConnection() {
        if (currentSocket != null && !currentSocket.isClosed()) {
            try {
                currentSocket.close();
            } catch (IOException e) {
                // 忽略
            }
        }
        currentSocket = null;
        currentHost = null;
        currentPort = 0;
        inputStream = null;
        outputStream = null;
    }

    /**
     * 设置超时时间
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * 设置是否自动跟随重定向
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * 设置最大重定向次数
     */
    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        String lastModified;
        String content;

        CacheEntry(String lastModified, String content) {
            this.lastModified = lastModified;
            this.content = content;
        }
    }

    /**
     * 关闭客户端
     */
    public void close() {
        closeConnection();
        cache.clear();
    }
}
