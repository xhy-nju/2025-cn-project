package server.request;

import common.HttpMethod;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * HTTP请求解析器
 * 从输入流中解析HTTP请求
 */
public class RequestParser {

    /**
     * 从输入流解析HTTP请求
     * 
     * @param inputStream 输入流
     * @return 解析后的HttpRequest对象，如果连接关闭返回null
     * @throws IOException IO异常
     */
    public static HttpRequest parse(InputStream inputStream) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(inputStream);

        // 读取请求行
        String requestLine = readLine(bis);
        if (requestLine == null || requestLine.isEmpty()) {
            return null; // 连接已关闭
        }

        HttpRequest request = new HttpRequest();

        // 解析请求行: METHOD URI HTTP/VERSION
        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new IOException("Invalid request line: " + requestLine);
        }

        request.setMethod(HttpMethod.fromString(parts[0]));
        request.setUri(parts[1]);
        request.setHttpVersion(parts[2]);

        // 读取请求头
        String headerLine;
        while ((headerLine = readLine(bis)) != null && !headerLine.isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex != -1) {
                String name = headerLine.substring(0, colonIndex).trim();
                String value = headerLine.substring(colonIndex + 1).trim();
                request.setHeader(name, value);
            }
        }

        // 读取请求体
        int contentLength = request.getContentLength();
        if (contentLength > 0) {
            byte[] body = new byte[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = bis.read(body, totalRead, contentLength - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            request.setBody(body);
        }

        return request;
    }

    /**
     * 从输入流读取一行（以CRLF结尾）
     */
    private static String readLine(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int prevByte = -1;
        int currByte;

        while ((currByte = is.read()) != -1) {
            if (prevByte == '\r' && currByte == '\n') {
                // 移除末尾的\r
                byte[] bytes = baos.toByteArray();
                if (bytes.length > 0) {
                    return new String(bytes, 0, bytes.length - 1, "UTF-8");
                }
                return "";
            }
            baos.write(currByte);
            prevByte = currByte;
        }

        // 如果没有读取到任何内容，返回null表示连接关闭
        if (baos.size() == 0) {
            return null;
        }

        return new String(baos.toByteArray(), "UTF-8");
    }
}
