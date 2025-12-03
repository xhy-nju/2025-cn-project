package server.response;

import common.HttpConstants;
import common.HttpHeaders;
import common.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * HTTP响应类
 * 用于构建和发送HTTP响应
 */
public class HttpResponse {
    private String httpVersion;
    private HttpStatus status;
    private Map<String, String> headers;
    private byte[] body;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public HttpResponse() {
        this.httpVersion = HttpConstants.HTTP_1_1;
        this.status = HttpStatus.OK;
        this.headers = new LinkedHashMap<>();

        // 设置默认头部
        setHeader(HttpHeaders.SERVER, HttpConstants.SERVER_NAME);
        setHeader(HttpHeaders.DATE, DATE_FORMAT.format(new Date()));
    }

    // Getters and Setters
    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public void setHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
        if (body != null) {
            setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));
        }
    }

    public void setBody(String body) {
        try {
            setBody(body.getBytes("UTF-8"));
        } catch (Exception e) {
            setBody(body.getBytes());
        }
    }

    public void setContentType(String contentType) {
        setHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }

    public void setKeepAlive(boolean keepAlive) {
        if (keepAlive) {
            setHeader(HttpHeaders.CONNECTION, HttpHeaders.KEEP_ALIVE_VALUE);
            setHeader(HttpHeaders.KEEP_ALIVE, "timeout=60, max=100");
        } else {
            setHeader(HttpHeaders.CONNECTION, HttpHeaders.CLOSE_VALUE);
        }
    }

    public void setLastModified(Date date) {
        setHeader(HttpHeaders.LAST_MODIFIED, DATE_FORMAT.format(date));
    }

    public void setLocation(String location) {
        setHeader(HttpHeaders.LOCATION, location);
    }

    /**
     * 构建响应报文字节数组
     */
    public byte[] build() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 状态行
        String statusLine = httpVersion + " " + status.getCode() + " " + status.getReasonPhrase();
        baos.write(statusLine.getBytes("UTF-8"));
        baos.write(HttpConstants.CRLF.getBytes("UTF-8"));

        // 响应头
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String headerLine = entry.getKey() + ": " + entry.getValue();
            baos.write(headerLine.getBytes("UTF-8"));
            baos.write(HttpConstants.CRLF.getBytes("UTF-8"));
        }

        // 空行
        baos.write(HttpConstants.CRLF.getBytes("UTF-8"));

        // 响应体
        if (body != null && body.length > 0) {
            baos.write(body);
        }

        return baos.toByteArray();
    }

    /**
     * 发送响应到输出流
     */
    public void send(OutputStream outputStream) throws IOException {
        byte[] responseBytes = build();
        outputStream.write(responseBytes);
        outputStream.flush();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(httpVersion).append(" ").append(status).append("\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        if (body != null && body.length > 0 && body.length < 1000) {
            try {
                sb.append("\n").append(new String(body, "UTF-8"));
            } catch (Exception e) {
                sb.append("\n[Binary data: ").append(body.length).append(" bytes]");
            }
        }
        return sb.toString();
    }
}
