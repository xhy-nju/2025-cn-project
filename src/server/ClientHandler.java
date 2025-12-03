package server;

import common.HttpConstants;
import server.request.HttpRequest;
import server.request.RequestParser;
import server.response.HttpResponse;
import server.response.ResponseBuilder;
import server.router.Router;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * 客户端连接处理器
 * 处理单个客户端连接，支持HTTP长连接(Keep-Alive)
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Router router;
    private final int keepAliveTimeout;
    private final int maxRequests;

    public ClientHandler(Socket clientSocket, Router router) {
        this(clientSocket, router, HttpConstants.DEFAULT_TIMEOUT, HttpConstants.MAX_KEEP_ALIVE_REQUESTS);
    }

    public ClientHandler(Socket clientSocket, Router router, int keepAliveTimeout, int maxRequests) {
        this.clientSocket = clientSocket;
        this.router = router;
        this.keepAliveTimeout = keepAliveTimeout;
        this.maxRequests = maxRequests;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            // 设置Socket超时
            clientSocket.setSoTimeout(keepAliveTimeout);
            clientSocket.setKeepAlive(true);

            inputStream = clientSocket.getInputStream();
            outputStream = clientSocket.getOutputStream();

            int requestCount = 0;
            boolean keepAlive = true;

            // 长连接循环：持续处理来自同一连接的多个请求
            while (keepAlive && requestCount < maxRequests) {
                try {
                    // 解析HTTP请求
                    HttpRequest request = RequestParser.parse(inputStream);

                    // 如果请求为null，说明连接已关闭
                    if (request == null) {
                        break;
                    }

                    requestCount++;

                    // 打印请求信息
                    logRequest(request, requestCount);

                    // 判断是否保持连接
                    keepAlive = request.isKeepAlive();

                    // 路由到对应处理器
                    HttpResponse response;
                    try {
                        response = router.route(request);
                    } catch (Exception e) {
                        e.printStackTrace();
                        response = ResponseBuilder.internalServerError(e.getMessage());
                    }

                    // 设置连接头
                    response.setKeepAlive(keepAlive);

                    // 发送响应
                    response.send(outputStream);

                    // 打印响应信息
                    logResponse(response);

                } catch (SocketTimeoutException e) {
                    // 超时，关闭连接
                    System.out.println(
                            "[" + getClientInfo() + "] Connection timeout after " + requestCount + " requests");
                    break;
                }
            }

        } catch (IOException e) {
            // 连接异常（客户端可能主动关闭）
            if (!clientSocket.isClosed()) {
                System.err.println("[" + getClientInfo() + "] Error: " + e.getMessage());
            }
        } finally {
            closeQuietly();
        }
    }

    /**
     * 记录请求日志
     */
    private void logRequest(HttpRequest request, int requestCount) {
        System.out.println("[" + getClientInfo() + "] #" + requestCount + " " +
                request.getMethod() + " " + request.getPath() + " " + request.getHttpVersion());
    }

    /**
     * 记录响应日志
     */
    private void logResponse(HttpResponse response) {
        System.out.println("[" + getClientInfo() + "] -> " + response.getStatus());
    }

    /**
     * 获取客户端信息
     */
    private String getClientInfo() {
        return clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
    }

    /**
     * 静默关闭Socket
     */
    private void closeQuietly() {
        try {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            // 忽略关闭异常
        }
    }
}
