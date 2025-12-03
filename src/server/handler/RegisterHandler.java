package server.handler;

import server.request.HttpRequest;
import server.response.HttpResponse;
import server.response.ResponseBuilder;
import server.router.RouteHandler;

/**
 * 用户注册处理器
 * POST /api/register
 * 请求体: {"username": "xxx", "password": "xxx"}
 */
public class RegisterHandler implements RouteHandler {

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

        // 用户名长度验证
        if (username.length() < 3 || username.length() > 20) {
            return ResponseBuilder.badRequest("用户名长度应在3-20个字符之间");
        }

        // 密码长度验证
        if (password.length() < 6) {
            return ResponseBuilder.badRequest("密码长度不能少于6个字符");
        }

        // 尝试注册
        boolean success = UserStore.register(username.trim(), password);

        if (success) {
            String jsonResponse = JsonUtils.buildResponse(200, "注册成功");
            return ResponseBuilder.json(jsonResponse);
        } else {
            String jsonResponse = JsonUtils.buildResponse(400, "用户名已存在");
            HttpResponse response = ResponseBuilder.json(jsonResponse);
            response.setStatus(common.HttpStatus.BAD_REQUEST);
            return response;
        }
    }
}
