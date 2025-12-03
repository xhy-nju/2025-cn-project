package client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP响应解析器
 * 解析服务器返回的HTTP响应
 */
public class HttpResponseParser {
    private String httpVersion;
    private int statusCode;
    private String reasonPhrase;
    private Map<String, String> headers;
    private byte[] body;
    private String rawResponse;

    public HttpResponseParser() {
        this.headers = new HashMap<>();
    }

    /**
     * 从输入流解析HTTP响应
     */
    public void parse(InputStream inputStream) throws IOException {
        // 读取状态行
        String statusLine = readLine(inputStream);
        if (statusLine == null || statusLine.isEmpty()) {
            throw new IOException("Empty response");
        }

        parseStatusLine(statusLine);

        // 读取响应头
        String headerLine;
        while ((headerLine = readLine(inputStream)) != null && !headerLine.isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex != -1) {
                String name = headerLine.substring(0, colonIndex).trim();
                String value = headerLine.substring(colonIndex + 1).trim();
                headers.put(name, value);
            }
        }

        // 读取响应体
        int contentLength = getContentLength();
        if (contentLength > 0) {
            body = new byte[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = inputStream.read(body, totalRead, contentLength - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
        } else if (isChunkedTransfer()) {
            // 处理分块传输
            body = readChunkedBody(inputStream);
        }
    }

    /**
     * 解析状态行
     */
    private void parseStatusLine(String statusLine) throws IOException {
        // HTTP/1.1 200 OK
        String[] parts = statusLine.split(" ", 3);
        if (parts.length < 2) {
            throw new IOException("Invalid status line: " + statusLine);
        }

        this.httpVersion = parts[0];
        try {
            this.statusCode = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid status code: " + parts[1]);
        }
        this.reasonPhrase = parts.length > 2 ? parts[2] : "";
    }

    /**
     * 读取一行（以CRLF结尾）
     */
    private String readLine(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int prevByte = -1;
        int currByte;

        while ((currByte = is.read()) != -1) {
            if (prevByte == '\r' && currByte == '\n') {
                byte[] bytes = baos.toByteArray();
                if (bytes.length > 0) {
                    return new String(bytes, 0, bytes.length - 1, "UTF-8");
                }
                return "";
            }
            baos.write(currByte);
            prevByte = currByte;
        }

        if (baos.size() == 0) {
            return null;
        }

        return new String(baos.toByteArray(), "UTF-8");
    }

    /**
     * 读取分块传输的响应体
     */
    private byte[] readChunkedBody(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while (true) {
            String chunkSizeLine = readLine(is);
            if (chunkSizeLine == null || chunkSizeLine.isEmpty()) {
                break;
            }

            int chunkSize;
            try {
                chunkSize = Integer.parseInt(chunkSizeLine.trim(), 16);
            } catch (NumberFormatException e) {
                break;
            }

            if (chunkSize == 0) {
                // 读取结尾的CRLF
                readLine(is);
                break;
            }

            byte[] chunk = new byte[chunkSize];
            int totalRead = 0;
            while (totalRead < chunkSize) {
                int read = is.read(chunk, totalRead, chunkSize - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            baos.write(chunk);

            // 读取chunk后的CRLF
            readLine(is);
        }

        return baos.toByteArray();
    }

    // Getters
    public String getHttpVersion() {
        return httpVersion;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        if (body == null) {
            return null;
        }
        try {
            return new String(body, "UTF-8");
        } catch (Exception e) {
            return new String(body);
        }
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

    public String getLocation() {
        return getHeader("Location");
    }

    public String getLastModified() {
        return getHeader("Last-Modified");
    }

    public boolean isKeepAlive() {
        String connection = getHeader("Connection");
        if (connection != null) {
            return connection.equalsIgnoreCase("keep-alive");
        }
        return httpVersion != null && httpVersion.equals("HTTP/1.1");
    }

    public boolean isRedirect() {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

    public boolean isNotModified() {
        return statusCode == 304;
    }

    private boolean isChunkedTransfer() {
        String transferEncoding = getHeader("Transfer-Encoding");
        return transferEncoding != null && transferEncoding.equalsIgnoreCase("chunked");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(httpVersion).append(" ").append(statusCode).append(" ").append(reasonPhrase).append("\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        if (body != null && body.length > 0) {
            String contentType = getContentType();
            if (contentType != null && (contentType.contains("text") || contentType.contains("json"))) {
                sb.append("\n").append(getBodyAsString());
            } else {
                sb.append("\n[Binary data: ").append(body.length).append(" bytes]");
            }
        }
        return sb.toString();
    }
}
