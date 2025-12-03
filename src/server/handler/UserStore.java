package server.handler;

import model.User;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户存储服务
 * 使用内存存储用户数据
 */
public class UserStore {
    private static final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    /**
     * 注册用户
     * 
     * @param username 用户名
     * @param password 密码
     * @return true 注册成功，false 用户已存在
     */
    public static boolean register(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return false;
        }

        User newUser = new User(username, password);
        // putIfAbsent 如果key已存在则返回旧值，否则返回null
        return users.putIfAbsent(username, newUser) == null;
    }

    /**
     * 验证用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return true 登录成功，false 用户名或密码错误
     */
    public static boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        User user = users.get(username);
        return user != null && password.equals(user.getPassword());
    }

    /**
     * 检查用户是否存在
     */
    public static boolean exists(String username) {
        return users.containsKey(username);
    }

    /**
     * 获取用户
     */
    public static User getUser(String username) {
        return users.get(username);
    }

    /**
     * 获取用户数量
     */
    public static int getUserCount() {
        return users.size();
    }

    /**
     * 清空所有用户（仅用于测试）
     */
    public static void clear() {
        users.clear();
    }
}
