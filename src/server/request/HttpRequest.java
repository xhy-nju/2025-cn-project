package server.request;

import common.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求类
 * 封装解析后的HTTP请求信息
 */
public class HttpRequest {
    private HttpMethod method;
    private String uri;
    private String path;
    private String queryString;
    private Map<String, String> queryParams;
    private String httpVersion;
    private Map<String, String> headers;
    private byte[] body;
    private Map<String, String> formData;

    public HttpRequest() {
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.formData = new HashMap<>();
    }

    // Getters and Setters
    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
        parseUri(uri);
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        // HTTP头部不区分大小写
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void setHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
        parseFormData();
    }

    public String getBodyAsString() {
        if (body == null) {
            return null;
        }
        try {
            return new String(body, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return new String(body);
        }
    }

    public Map<String, String> getFormData() {
        return formData;
    }

    public String getFormParam(String name) {
        return formData.get(name);
    }

    public int getContentLength() {
        String contentLength = getHeader("Content-Length");
        if (contentLength != null) {
            try {
                return Integer.parseInt(contentLength.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    public String getContentType() {
        return getHeader("Content-Type");
    }

    public boolean isKeepAlive() {
        String connection = getHeader("Connection");
        if (connection != null) {
            return connection.equalsIgnoreCase("keep-alive");
        }
        // HTTP/1.1 默认是 keep-alive
        return "HTTP/1.1".equals(httpVersion);
    }

    /**
     * 解析URI，分离路径和查询字符串
     */
    private void parseUri(String uri) {
        if (uri == null) {
            this.path = "/";
            this.queryString = "";
            return;
        }

        int queryIndex = uri.indexOf('?');
        if (queryIndex != -1) {
            this.path = uri.substring(0, queryIndex);
            this.queryString = uri.substring(queryIndex + 1);
            parseQueryString(this.queryString);
        } else {
            this.path = uri;
            this.queryString = "";
        }

        // URL解码路径
        try {
            this.path = URLDecoder.decode(this.path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // 忽略解码错误
        }
    }

    /**
     * 解析查询字符串
     */
    private void parseQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return;
        }

        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            try {
                if (idx != -1) {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    queryParams.put(key, value);
                } else {
                    String key = URLDecoder.decode(pair, "UTF-8");
                    queryParams.put(key, "");
                }
            } catch (UnsupportedEncodingException e) {
                // 忽略解码错误
            }
        }
    }

    /**
     * 解析表单数据
     */
    private void parseFormData() {
        String contentType = getContentType();
        if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
            String bodyStr = getBodyAsString();
            if (bodyStr != null && !bodyStr.isEmpty()) {
                String[] pairs = bodyStr.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf('=');
                    try {
                        if (idx != -1) {
                            String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                            String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                            formData.put(key, value);
                        } else {
                            String key = URLDecoder.decode(pair, "UTF-8");
                            formData.put(key, "");
                        }
                    } catch (UnsupportedEncodingException e) {
                        // 忽略解码错误
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(uri).append(" ").append(httpVersion).append("\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        if (body != null && body.length > 0) {
            sb.append("\n").append(getBodyAsString());
        }
        return sb.toString();
    }
}
