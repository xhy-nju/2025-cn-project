package common;

/**
 * HTTP常量定义
 */
public class HttpConstants {
    // HTTP版本
    public static final String HTTP_1_0 = "HTTP/1.0";
    public static final String HTTP_1_1 = "HTTP/1.1";

    // 换行符
    public static final String CRLF = "\r\n";
    public static final String DOUBLE_CRLF = "\r\n\r\n";

    // 默认配置
    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_TIMEOUT = 60000; // 60秒
    public static final int MAX_KEEP_ALIVE_REQUESTS = 100;
    public static final int BUFFER_SIZE = 8192;

    // 服务器信息
    public static final String SERVER_NAME = "SimpleHttpServer/1.0";

    // 默认编码
    public static final String DEFAULT_CHARSET = "UTF-8";

    private HttpConstants() {
        // 私有构造函数，防止实例化
    }
}
