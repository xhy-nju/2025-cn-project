package server.router;

import server.request.HttpRequest;
import server.response.HttpResponse;

/**
 * 路由处理器接口
 * 所有业务处理器需要实现此接口
 */
@FunctionalInterface
public interface RouteHandler {
    /**
     * 处理HTTP请求
     * 
     * @param request HTTP请求对象
     * @return HTTP响应对象
     */
    HttpResponse handle(HttpRequest request);
}
