package server;

import common.HttpConstants;
import server.handler.LoginHandler;
import server.handler.RegisterHandler;
import server.handler.StaticFileHandler;
import server.router.Router;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP服务器主类
 * 基于Java Socket API实现的简单HTTP服务器
 * 
 * 功能特性：
 * - 支持GET和POST请求
 * - 支持HTTP长连接(Keep-Alive)
 * - 支持多种MIME类型
 * - 支持304缓存机制
 * - 支持301/302重定向
 * - 提供用户注册/登录API
 */
public class HttpServer {
    private final int port;
    private final Router router;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private String staticDir;

    public HttpServer(int port) {
        this.port = port;
        this.router = new Router();
        this.threadPool = Executors.newCachedThreadPool();
        this.running = false;
    }

    /**
     * 配置路由
     */
    public void configureRoutes() {
        // 注册API路由
        router.post("/api/register", new RegisterHandler());
        router.post("/api/login", new LoginHandler());

        // 演示重定向路由
        router.get("/old-page", request -> {
            // 301 永久重定向示例
            return server.response.ResponseBuilder.movedPermanently("/index.html");
        });

        router.get("/temp-redirect", request -> {
            // 302 临时重定向示例
            return server.response.ResponseBuilder.found("/index.html");
        });

        // API: 获取服务器状态
        router.get("/api/status", request -> {
            String json = "{\"status\":\"running\",\"port\":" + port + "}";
            return server.response.ResponseBuilder.json(json);
        });

        // API: 测试500内部服务器错误
        router.get("/api/error", request -> {
            // 故意抛出异常来测试500错误处理
            throw new RuntimeException("This is a test error for 500 status code");
        });

        // 设置静态文件处理器作为默认处理器
        this.staticDir = getStaticDirectory();
        router.setDefaultHandler(new StaticFileHandler(staticDir));

        System.out.println("Static files directory: " + staticDir);
    }

    /**
     * 获取静态资源目录
     */
    private String getStaticDirectory() {
        // 尝试多个可能的位置
        String[] possiblePaths = {
                "resources",
                "src/resources",
                "../resources",
                "."
        };

        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                try {
                    return dir.getCanonicalPath();
                } catch (IOException e) {
                    return dir.getAbsolutePath();
                }
            }
        }

        // 如果都不存在，创建resources目录
        File resourcesDir = new File("resources");
        resourcesDir.mkdirs();
        return resourcesDir.getAbsolutePath();
    }

    /**
     * 启动服务器
     */
    public void start() {
        configureRoutes();

        try {
            serverSocket = new ServerSocket(port);
            running = true;

            System.out.println("========================================");
            System.out.println("   Simple HTTP Server v1.0");
            System.out.println("========================================");
            System.out.println("Server started on port " + port);
            System.out.println("Access: http://localhost:" + port);
            System.out.println("");
            System.out.println("Available endpoints:");
            System.out.println("  GET  /              - Home page");
            System.out.println("  GET  /api/status    - Server status");
            System.out.println("  POST /api/register  - User registration");
            System.out.println("  POST /api/login     - User login");
            System.out.println("  GET  /old-page      - 301 redirect demo");
            System.out.println("  GET  /temp-redirect - 302 redirect demo");
            System.out.println("========================================");
            System.out.println("Press Ctrl+C to stop the server");
            System.out.println("");

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                stop();
            }));

            // 接受客户端连接
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New connection from: " +
                            clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

                    // 使用线程池处理客户端请求
                    threadPool.execute(new ClientHandler(clientSocket, router));

                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting connection: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 停止服务器
     */
    public void stop() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // 忽略
        }

        threadPool.shutdown();
        System.out.println("Server stopped.");
    }

    /**
     * 获取路由器（用于扩展路由）
     */
    public Router getRouter() {
        return router;
    }

    /**
     * 主入口
     */
    public static void main(String[] args) {
        int port = HttpConstants.DEFAULT_PORT;

        // 从命令行参数获取端口
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }

        HttpServer server = new HttpServer(port);
        server.start();
    }
}
