package model;

/**
 * 用户实体类
 */
public class User {
    private String username;
    private String password;
    private long createTime;

    public User() {
        this.createTime = System.currentTimeMillis();
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.createTime = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
