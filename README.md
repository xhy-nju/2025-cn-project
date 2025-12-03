# 基于 Java Socket API 的 HTTP 客户端与服务器

## 项目简介

这是一个完全基于 Java Socket API 实现的简单 HTTP 客户端和服务器程序，不依赖任何第三方框架（如 Netty）。

## 功能特性

### HTTP 服务器端

- ✅ 支持 GET 和 POST 请求
- ✅ 支持 HTTP 长连接 (Keep-Alive)
- ✅ 支持多种 MIME 类型（text/html, text/css, application/json, image/png 等）
- ✅ 支持 304 缓存机制（基于 Last-Modified）
- ✅ 支持 301/302 重定向
- ✅ 支持状态码：200, 301, 302, 304, 404, 405, 500

### HTTP 客户端

- ✅ 发送 GET 和 POST 请求
- ✅ 自动处理 301/302 重定向
- ✅ 支持 304 缓存机制
- ✅ 命令行交互界面

### 业务功能

- ✅ 用户注册接口 (POST /api/register)
- ✅ 用户登录接口 (POST /api/login)
- ✅ 数据存储在内存中

## 项目结构

```
socket_project/
├── src/
│   ├── common/                 # 公共模块
│   │   ├── HttpMethod.java     # HTTP方法枚举
│   │   ├── HttpStatus.java     # HTTP状态码枚举
│   │   ├── HttpHeaders.java    # HTTP头部常量
│   │   └── HttpConstants.java  # HTTP常量定义
│   │
│   ├── server/                 # 服务器端模块
│   │   ├── HttpServer.java     # HTTP服务器主类
│   │   ├── ClientHandler.java  # 客户端连接处理器
│   │   ├── request/            # 请求处理
│   │   │   ├── HttpRequest.java
│   │   │   └── RequestParser.java
│   │   ├── response/           # 响应处理
│   │   │   ├── HttpResponse.java
│   │   │   └── ResponseBuilder.java
│   │   ├── router/             # 路由系统
│   │   │   ├── Router.java
│   │   │   └── RouteHandler.java
│   │   ├── handler/            # 业务处理器
│   │   │   ├── StaticFileHandler.java
│   │   │   ├── RegisterHandler.java
│   │   │   ├── LoginHandler.java
│   │   │   ├── UserStore.java
│   │   │   └── JsonUtils.java
│   │   └── mime/
│   │       └── MimeTypes.java
│   │
│   ├── client/                 # 客户端模块
│   │   ├── HttpClient.java
│   │   ├── HttpClientCLI.java
│   │   ├── HttpRequestBuilder.java
│   │   └── HttpResponseParser.java
│   │
│   └── model/                  # 数据模型
│       └── User.java
│
├── resources/                  # 静态资源
│   ├── index.html
│   ├── style.css
│   └── data.json
│
└── README.md
```

## 编译与运行

### 编译项目

```bash
# Windows
cd socket_project
mkdir out
javac -encoding UTF-8 -d out src/common/*.java src/model/*.java src/server/*.java src/server/request/*.java src/server/response/*.java src/server/router/*.java src/server/handler/*.java src/server/mime/*.java src/client/*.java
```

### 启动服务器

```bash
cd socket_project
java -cp out server.HttpServer
# 或指定端口
java -cp out server.HttpServer 8080
```

### 启动客户端

```bash
cd socket_project
java -cp out client.HttpClientCLI
```

## API 文档

### 1. 用户注册

```http
POST /api/register
Content-Type: application/json

{
    "username": "your_username",
    "password": "your_password"
}
```

成功响应 (200):

```json
{
  "code": 200,
  "message": "注册成功"
}
```

失败响应 (400):

```json
{
  "code": 400,
  "message": "用户名已存在"
}
```

### 2. 用户登录

```http
POST /api/login
Content-Type: application/json

{
    "username": "your_username",
    "password": "your_password"
}
```

成功响应 (200):

```json
{
  "code": 200,
  "message": "登录成功",
  "token": "generated_token"
}
```

失败响应 (401):

```json
{
  "code": 401,
  "message": "用户名或密码错误"
}
```

### 3. 服务器状态

```http
GET /api/status
```

响应:

```json
{
  "status": "running",
  "port": 8080
}
```

### 4. 重定向测试

- `GET /old-page` - 返回 301 永久重定向到 /index.html
- `GET /temp-redirect` - 返回 302 临时重定向到 /index.html

## 使用 Postman/curl 测试

### curl 测试示例

```bash
# 测试GET请求
curl http://localhost:8080/

# 测试注册
curl -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 测试登录
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 测试重定向
curl -v http://localhost:8080/old-page

# 测试304缓存
curl -H "If-Modified-Since: Wed, 01 Jan 2025 00:00:00 GMT" http://localhost:8080/index.html
```

## 客户端命令

启动客户端后，可以使用以下命令：

| 命令        | 说明                 |
| ----------- | -------------------- |
| `get <url>` | 发送 GET 请求        |
| `post`      | 交互式发送 POST 请求 |
| `register`  | 用户注册             |
| `login`     | 用户登录             |
| `test`      | 运行自动化测试       |
| `clear`     | 清除响应缓存         |
| `help`      | 显示帮助             |
| `exit`      | 退出客户端           |

## 技术实现要点

### 长连接实现

- 解析请求头中的 `Connection: keep-alive`
- 使用循环持续读取请求，设置 Socket 超时
- 正确设置 `Content-Length` 让客户端知道响应结束

### 304 缓存机制

- 服务器返回 `Last-Modified` 头
- 客户端后续请求携带 `If-Modified-Since` 头
- 服务器比较时间，未修改返回 304（无响应体）

### 重定向处理

- 服务器返回 301/302 状态码和 `Location` 头
- 客户端检测到重定向后自动请求新地址
- 限制最大重定向次数防止无限循环

