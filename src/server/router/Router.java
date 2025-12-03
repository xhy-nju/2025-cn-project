package server.router;

import common.HttpMethod;
import server.request.HttpRequest;
import server.response.HttpResponse;
import server.response.ResponseBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 路由分发器
 * 根据请求路径和方法分发到对应的处理器
 */
public class Router {
    // 存储路由: Method -> Path -> Handler
    private final Map<HttpMethod, Map<String, RouteHandler>> routes;
    // 默认处理器（用于处理静态文件等）
    private RouteHandler defaultHandler;

    public Router() {
        this.routes = new HashMap<>();
        for (HttpMethod method : HttpMethod.values()) {
            routes.put(method, new HashMap<>());
        }
    }

    /**
     * 注册GET路由
     */
    public Router get(String path, RouteHandler handler) {
        routes.get(HttpMethod.GET).put(path, handler);
        return this;
    }

    /**
     * 注册POST路由
     */
    public Router post(String path, RouteHandler handler) {
        routes.get(HttpMethod.POST).put(path, handler);
        return this;
    }

    /**
     * 注册PUT路由
     */
    public Router put(String path, RouteHandler handler) {
        routes.get(HttpMethod.PUT).put(path, handler);
        return this;
    }

    /**
     * 注册DELETE路由
     */
    public Router delete(String path, RouteHandler handler) {
        routes.get(HttpMethod.DELETE).put(path, handler);
        return this;
    }

    /**
     * 设置默认处理器（用于静态文件服务）
     */
    public Router setDefaultHandler(RouteHandler handler) {
        this.defaultHandler = handler;
        return this;
    }

    /**
     * 路由请求到对应的处理器
     */
    public HttpResponse route(HttpRequest request) {
        HttpMethod method = request.getMethod();
        String path = request.getPath();

        // 查找精确匹配的路由
        Map<String, RouteHandler> methodRoutes = routes.get(method);
        if (methodRoutes != null) {
            RouteHandler handler = methodRoutes.get(path);
            if (handler != null) {
                try {
                    return handler.handle(request);
                } catch (Exception e) {
                    e.printStackTrace();
                    return ResponseBuilder.internalServerError(e.getMessage());
                }
            }
        }

        // 检查其他方法是否有这个路径（用于返回405）
        for (HttpMethod m : HttpMethod.values()) {
            if (m != method && routes.get(m).containsKey(path)) {
                return ResponseBuilder.methodNotAllowed();
            }
        }

        // 使用默认处理器（静态文件服务）
        if (defaultHandler != null) {
            try {
                return defaultHandler.handle(request);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseBuilder.internalServerError(e.getMessage());
            }
        }

        // 没有找到匹配的路由
        return ResponseBuilder.notFound();
    }

    /**
     * 打印所有注册的路由
     */
    public void printRoutes() {
        System.out.println("=== Registered Routes ===");
        for (HttpMethod method : HttpMethod.values()) {
            Map<String, RouteHandler> methodRoutes = routes.get(method);
            for (String path : methodRoutes.keySet()) {
                System.out.println(method + " " + path);
            }
        }
        System.out.println("========================");
    }
}
