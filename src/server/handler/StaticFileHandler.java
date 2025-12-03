package server.handler;

import common.HttpMethod;
import common.HttpStatus;
import server.mime.MimeTypes;
import server.request.HttpRequest;
import server.response.HttpResponse;
import server.response.ResponseBuilder;
import server.router.RouteHandler;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 静态文件处理器
 * 处理静态资源请求，支持304缓存机制
 */
public class StaticFileHandler implements RouteHandler {
    private final String rootDirectory;
    private final SimpleDateFormat dateFormat;

    public StaticFileHandler(String rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        // 只处理GET请求
        if (request.getMethod() != HttpMethod.GET) {
            return ResponseBuilder.methodNotAllowed();
        }

        String path = request.getPath();

        // 默认首页
        if ("/".equals(path)) {
            path = "/index.html";
        }

        // 安全检查：防止目录遍历攻击
        if (path.contains("..")) {
            return ResponseBuilder.badRequest("Invalid path");
        }

        // 构建文件路径
        File file = new File(rootDirectory, path);

        // 文件不存在
        if (!file.exists() || !file.isFile()) {
            return ResponseBuilder.notFound();
        }

        // 检查是否需要返回304
        String ifModifiedSince = request.getHeader("If-Modified-Since");
        // HTTP日期格式只精确到秒，需要将毫秒部分截断
        long lastModifiedMs = (file.lastModified() / 1000) * 1000;
        Date lastModified = new Date(lastModifiedMs);

        if (ifModifiedSince != null) {
            try {
                Date clientDate = dateFormat.parse(ifModifiedSince);
                // 如果文件没有修改（客户端时间 >= 服务器文件时间），返回304
                if (lastModified.getTime() <= clientDate.getTime()) {
                    HttpResponse response = ResponseBuilder.notModified();
                    response.setLastModified(lastModified);
                    return response;
                }
            } catch (ParseException e) {
                // 解析失败，忽略If-Modified-Since头
                System.err.println("Failed to parse If-Modified-Since: " + ifModifiedSince);
            }
        }

        // 读取文件内容
        try {
            byte[] content = readFile(file);
            String mimeType = MimeTypes.getMimeType(file.getName());

            HttpResponse response = ResponseBuilder.binary(content, mimeType);
            response.setLastModified(lastModified);

            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseBuilder.internalServerError("Failed to read file");
        }
    }

    /**
     * 读取文件内容
     */
    private byte[] readFile(File file) throws IOException {
        FileInputStream fis = null;
        ByteArrayOutputStream baos = null;

        try {
            fis = new FileInputStream(file);
            baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            return baos.toByteArray();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
