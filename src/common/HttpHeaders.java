package common;

/**
 * HTTP头部常量
 */
public class HttpHeaders {
    // 通用头
    public static final String CONNECTION = "Connection";
    public static final String DATE = "Date";
    public static final String CACHE_CONTROL = "Cache-Control";

    // 请求头
    public static final String HOST = "Host";
    public static final String USER_AGENT = "User-Agent";
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_LANGUAGE = "Accept-Language";
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String IF_NONE_MATCH = "If-None-Match";

    // 响应头
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String LOCATION = "Location";
    public static final String SERVER = "Server";
    public static final String LAST_MODIFIED = "Last-Modified";
    public static final String ETAG = "ETag";
    public static final String KEEP_ALIVE = "Keep-Alive";

    // 常用值
    public static final String KEEP_ALIVE_VALUE = "keep-alive";
    public static final String CLOSE_VALUE = "close";

    private HttpHeaders() {
        // 私有构造函数，防止实例化
    }
}
