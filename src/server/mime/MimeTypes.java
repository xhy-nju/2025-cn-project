package server.mime;

import java.util.HashMap;
import java.util.Map;

/**
 * MIME类型映射
 * 根据文件扩展名返回对应的MIME类型
 */
public class MimeTypes {
    private static final Map<String, String> MIME_MAP = new HashMap<>();

    static {
        // 文本类型
        MIME_MAP.put(".html", "text/html; charset=UTF-8");
        MIME_MAP.put(".htm", "text/html; charset=UTF-8");
        MIME_MAP.put(".css", "text/css; charset=UTF-8");
        MIME_MAP.put(".js", "application/javascript; charset=UTF-8");
        MIME_MAP.put(".json", "application/json; charset=UTF-8");
        MIME_MAP.put(".xml", "application/xml; charset=UTF-8");
        MIME_MAP.put(".txt", "text/plain; charset=UTF-8");

        // 图片类型（非文本）
        MIME_MAP.put(".png", "image/png");
        MIME_MAP.put(".jpg", "image/jpeg");
        MIME_MAP.put(".jpeg", "image/jpeg");
        MIME_MAP.put(".gif", "image/gif");
        MIME_MAP.put(".ico", "image/x-icon");
        MIME_MAP.put(".svg", "image/svg+xml");
        MIME_MAP.put(".webp", "image/webp");

        // 其他二进制类型
        MIME_MAP.put(".pdf", "application/pdf");
        MIME_MAP.put(".zip", "application/zip");
        MIME_MAP.put(".woff", "font/woff");
        MIME_MAP.put(".woff2", "font/woff2");
    }

    /**
     * 根据文件名获取MIME类型
     * 
     * @param fileName 文件名或路径
     * @return MIME类型字符串
     */
    public static String getMimeType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return "application/octet-stream";
        }

        String extension = fileName.substring(dotIndex).toLowerCase();
        String mimeType = MIME_MAP.get(extension);

        return mimeType != null ? mimeType : "application/octet-stream";
    }

    /**
     * 判断是否为文本类型
     */
    public static boolean isTextType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("text/") ||
                mimeType.contains("json") ||
                mimeType.contains("xml") ||
                mimeType.contains("javascript");
    }

    /**
     * 判断是否为图片类型
     */
    public static boolean isImageType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("image/");
    }
}
