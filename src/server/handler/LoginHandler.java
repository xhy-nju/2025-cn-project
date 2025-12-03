package server.handler;

import server.request.HttpRequest;
import server.response.HttpResponse;
import server.response.ResponseBuilder;
import server.router.RouteHandler;

import java.util.UUID;

/**
 * 用户登录处理器
 * POST /api/login
 * 请求体: {"username": "xxx", "password": "xxx"}
 */
public class LoginHandler implements RouteHandler {

    @Override
    public HttpResponse handle(HttpRequest request) {
        // 获取请求体
        String body = request.getBodyAsString();
        if (body == null || body.isEmpty()) {
            return ResponseBuilder.badRequest("请求体不能为空");
        }

        // 解析JSON
        String username = JsonUtils.getString(body, "username");
        String password = JsonUtils.getString(body, "password");

        // 参数验证
        if (username == null || username.trim().isEmpty()) {
            return ResponseBuilder.badRequest("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseBuilder.badRequest("密码不能为空");
        }

        // 尝试登录
        boolean success = UserStore.login(username.trim(), password);

        if (success) {
            // 生成简单的token（实际应用中应该使用JWT等）
            String token = UUID.randomUUID().toString().replace("-", "");
            String jsonResponse = JsonUtils.buildResponse(200, "登录成功", "token", token);
            return ResponseBuilder.json(jsonResponse);
        } else {
            String jsonResponse = JsonUtils.buildResponse(401, "用户名或密码错误");
            HttpResponse response = ResponseBuilder.json(jsonResponse);
            response.setStatus(common.HttpStatus.UNAUTHORIZED);
            return response;
        }
    }
}
