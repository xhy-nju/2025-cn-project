package common;

/**
 * HTTP请求方法枚举
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    HEAD,
    OPTIONS,
    PATCH,
    UNKNOWN;

    public static HttpMethod fromString(String method) {
        if (method == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
