package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP客户端命令行界面
 * 提供交互式的HTTP请求测试工具
 */
public class HttpClientCLI {
    private final HttpClient client;
    private final BufferedReader reader;
    private boolean running;

    public HttpClientCLI() {
        this.client = new HttpClient();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.running = true;
    }

    public void start() {
        printWelcome();

        while (running) {
            try {
                System.out.print("\nhttp> ");
                String input = reader.readLine();

                if (input == null || input.trim().isEmpty()) {
                    continue;
                }

                processCommand(input.trim());

            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        client.close();
        System.out.println("Goodbye!");
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        try {
            switch (command) {
                case "get":
                    doGet(args);
                    break;
                case "post":
                    doPost(args);
                    break;
                case "register":
                    doRegister();
                    break;
                case "login":
                    doLogin();
                    break;
                case "test":
                    doTest();
                    break;
                case "help":
                    printHelp();
                    break;
                case "clear":
                    client.clearCache();
                    System.out.println("Cache cleared.");
                    break;
                case "exit":
                case "quit":
                    running = false;
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Type 'help' for available commands.");
            }
        } catch (IOException e) {
            System.err.println("Request failed: " + e.getMessage());
        }
    }

    private void doGet(String url) throws IOException {
        if (url.isEmpty()) {
            System.out.println("Usage: get <url>");
            System.out.println("Example: get http://localhost:8080/index.html");
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        HttpResponseParser response = client.get(url);
        printResponse(response);
    }

    private void doPost(String args) throws IOException {
        System.out.println("Enter URL:");
        System.out.print("> ");
        String url = reader.readLine().trim();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        System.out.println("Enter JSON body (or empty for form input):");
        System.out.print("> ");
        String body = reader.readLine().trim();

        HttpResponseParser response;
        if (body.isEmpty()) {
            // 表单输入
            Map<String, String> formData = new HashMap<>();
            System.out.println("Enter form data (key=value, empty line to finish):");
            while (true) {
                System.out.print("  > ");
                String line = reader.readLine().trim();
                if (line.isEmpty())
                    break;

                int idx = line.indexOf('=');
                if (idx != -1) {
                    formData.put(line.substring(0, idx), line.substring(idx + 1));
                }
            }
            response = client.postForm(url, formData);
        } else {
            response = client.postJson(url, body);
        }

        printResponse(response);
    }

    private void doRegister() throws IOException {
        System.out.println("=== User Registration ===");
        System.out.print("Enter username: ");
        String username = reader.readLine().trim();
        System.out.print("Enter password: ");
        String password = reader.readLine().trim();

        String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        HttpResponseParser response = client.postJson("http://localhost:8080/api/register", json);

        System.out.println("\n=== Response ===");
        System.out.println("Status: " + response.getStatusCode() + " " + response.getReasonPhrase());
        System.out.println("Body: " + response.getBodyAsString());
    }

    private void doLogin() throws IOException {
        System.out.println("=== User Login ===");
        System.out.print("Enter username: ");
        String username = reader.readLine().trim();
        System.out.print("Enter password: ");
        String password = reader.readLine().trim();

        String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        HttpResponseParser response = client.postJson("http://localhost:8080/api/login", json);

        System.out.println("\n=== Response ===");
        System.out.println("Status: " + response.getStatusCode() + " " + response.getReasonPhrase());
        System.out.println("Body: " + response.getBodyAsString());
    }

    private void doTest() throws IOException {
        String baseUrl = "http://localhost:8080";

        System.out.println("=== Running Tests ===\n");

        // Test 1: GET首页
        System.out.println("1. Testing GET /");
        try {
            HttpResponseParser resp = client.get(baseUrl + "/");
            System.out.println("   Status: " + resp.getStatusCode());
        } catch (Exception e) {
            System.out.println("   Failed: " + e.getMessage());
        }

        // Test 2: GET API状态
        System.out.println("2. Testing GET /api/status");
        try {
            HttpResponseParser resp = client.get(baseUrl + "/api/status");
            System.out.println("   Status: " + resp.getStatusCode());
            System.out.println("   Body: " + resp.getBodyAsString());
        } catch (Exception e) {
            System.out.println("   Failed: " + e.getMessage());
        }

        // Test 3: 301重定向
        System.out.println("3. Testing 301 redirect /old-page");
        try {
            HttpResponseParser resp = client.get(baseUrl + "/old-page");
            System.out.println("   Final Status: " + resp.getStatusCode());
        } catch (Exception e) {
            System.out.println("   Failed: " + e.getMessage());
        }

        // Test 4: 注册用户
        System.out.println("4. Testing POST /api/register");
        try {
            String json = "{\"username\":\"testuser\",\"password\":\"123456\"}";
            HttpResponseParser resp = client.postJson(baseUrl + "/api/register", json);
            System.out.println("   Status: " + resp.getStatusCode());
            System.out.println("   Body: " + resp.getBodyAsString());
        } catch (Exception e) {
            System.out.println("   Failed: " + e.getMessage());
        }

        // Test 5: 登录
        System.out.println("5. Testing POST /api/login");
        try {
            String json = "{\"username\":\"testuser\",\"password\":\"123456\"}";
            HttpResponseParser resp = client.postJson(baseUrl + "/api/login", json);
            System.out.println("   Status: " + resp.getStatusCode());
            System.out.println("   Body: " + resp.getBodyAsString());
        } catch (Exception e) {
            System.out.println("   Failed: " + e.getMessage());
        }

        // Test 6: 404
        System.out.println("6. Testing 404 /not-exists");
        try {
            HttpResponseParser resp = client.get(baseUrl + "/not-exists");
            System.out.println("   Status: " + resp.getStatusCode());
        } catch (Exception e) {
            System.out.println("   Failed: " + e.getMessage());
        }

        System.out.println("\n=== Tests Completed ===");
    }

    private void printResponse(HttpResponseParser response) {
        System.out.println("\n========== Response ==========");
        System.out.println("Status: " + response.getStatusCode() + " " + response.getReasonPhrase());
        System.out.println("Headers:");
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        String body = response.getBodyAsString();
        if (body != null && !body.isEmpty()) {
            System.out.println("Body:");
            if (body.length() > 500) {
                System.out.println(body.substring(0, 500) + "...(truncated)");
            } else {
                System.out.println(body);
            }
        }
        System.out.println("==============================");
    }

    private void printWelcome() {
        System.out.println("========================================");
        System.out.println("   Simple HTTP Client v1.0");
        System.out.println("========================================");
        System.out.println("Type 'help' for available commands.");
    }

    private void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  get <url>     - Send GET request");
        System.out.println("  post          - Send POST request (interactive)");
        System.out.println("  register      - Register a new user");
        System.out.println("  login         - Login with username/password");
        System.out.println("  test          - Run automated tests");
        System.out.println("  clear         - Clear response cache");
        System.out.println("  help          - Show this help");
        System.out.println("  exit/quit     - Exit the client");
        System.out.println("\nExamples:");
        System.out.println("  get http://localhost:8080/");
        System.out.println("  get localhost:8080/api/status");
    }

    public static void main(String[] args) {
        new HttpClientCLI().start();
    }
}
