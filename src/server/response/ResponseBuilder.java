package server.response;

import common.HttpStatus;

/**
 * HTTP响应构建器
 * 提供便捷的方法来创建各种类型的响应
 */
public class ResponseBuilder {

    /**
     * 创建成功响应 (200 OK)
     */
    public static HttpResponse ok() {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.OK);
        return response;
    }

    /**
     * 创建成功响应，带文本内容
     */
    public static HttpResponse ok(String body) {
        HttpResponse response = ok();
        response.setContentType("text/plain; charset=UTF-8");
        response.setBody(body);
        return response;
    }

    /**
     * 创建成功响应，带HTML内容
     */
    public static HttpResponse html(String htmlContent) {
        HttpResponse response = ok();
        response.setContentType("text/html; charset=UTF-8");
        response.setBody(htmlContent);
        return response;
    }

    /**
     * 创建成功响应，带JSON内容
     */
    public static HttpResponse json(String jsonContent) {
        HttpResponse response = ok();
        response.setContentType("application/json; charset=UTF-8");
        response.setBody(jsonContent);
        return response;
    }

    /**
     * 创建成功响应，带二进制内容
     */
    public static HttpResponse binary(byte[] data, String contentType) {
        HttpResponse response = ok();
        response.setContentType(contentType);
        response.setBody(data);
        return response;
    }

    /**
     * 创建301永久重定向响应
     */
    public static HttpResponse movedPermanently(String location) {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.MOVED_PERMANENTLY);
        response.setLocation(location);
        response.setContentType("text/html; charset=UTF-8");
        response.setBody("<html><body><h1>301 Moved Permanently</h1><p>Redirecting to <a href=\""
                + location + "\">" + location + "</a></p></body></html>");
        return response;
    }

    /**
     * 创建302临时重定向响应
     */
    public static HttpResponse found(String location) {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.FOUND);
        response.setLocation(location);
        response.setContentType("text/html; charset=UTF-8");
        response.setBody("<html><body><h1>302 Found</h1><p>Redirecting to <a href=\""
                + location + "\">" + location + "</a></p></body></html>");
        return response;
    }

    /**
     * 创建304未修改响应
     */
    public static HttpResponse notModified() {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.NOT_MODIFIED);
        // 304响应不应该有响应体
        return response;
    }

    /**
     * 创建400错误请求响应
     */
    public static HttpResponse badRequest(String message) {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.BAD_REQUEST);
        response.setContentType("application/json; charset=UTF-8");
        response.setBody("{\"code\":400,\"message\":\"" + escapeJson(message) + "\"}");
        return response;
    }

    /**
     * 创建404未找到响应
     */
    public static HttpResponse notFound() {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.NOT_FOUND);
        response.setContentType("text/html; charset=UTF-8");
        response.setBody(
                "<html><body><h1>404 Not Found</h1><p>The requested resource was not found on this server.</p></body></html>");
        return response;
    }

    /**
     * 创建404未找到响应，带自定义消息
     */
    public static HttpResponse notFound(String message) {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.NOT_FOUND);
        response.setContentType("application/json; charset=UTF-8");
        response.setBody("{\"code\":404,\"message\":\"" + escapeJson(message) + "\"}");
        return response;
    }

    /**
     * 创建405方法不允许响应
     */
    public static HttpResponse methodNotAllowed() {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
        response.setContentType("text/html; charset=UTF-8");
        response.setBody(
                "<html><body><h1>405 Method Not Allowed</h1><p>The request method is not supported for this resource.</p></body></html>");
        return response;
    }

    /**
     * 创建500服务器内部错误响应
     */
    public static HttpResponse internalServerError() {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setContentType("text/html; charset=UTF-8");
        response.setBody(
                "<html><body><h1>500 Internal Server Error</h1><p>An unexpected error occurred.</p></body></html>");
        return response;
    }

    /**
     * 创建500服务器内部错误响应，带自定义消息
     */
    public static HttpResponse internalServerError(String message) {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setContentType("application/json; charset=UTF-8");
        response.setBody("{\"code\":500,\"message\":\"" + escapeJson(message) + "\"}");
        return response;
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
