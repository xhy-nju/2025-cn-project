package server.handler;

/**
 * 简单的JSON解析工具类
 * 由于不使用第三方库，手动实现简单的JSON解析
 */
public class JsonUtils {

    /**
     * 从JSON字符串中获取字符串值
     * 支持格式: {"key": "value"} 或 {"key":"value"}
     */
    public static String getString(String json, String key) {
        if (json == null || key == null) {
            return null;
        }

        // 查找 "key" 或 "key":
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }

        // 找到冒号位置
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }

        // 跳过空白字符找到值的起始引号
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return null;
        }

        // 找到值的结束引号
        int valueEnd = valueStart + 1;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (c == '"' && json.charAt(valueEnd - 1) != '\\') {
                break;
            }
            valueEnd++;
        }

        if (valueEnd >= json.length()) {
            return null;
        }

        String value = json.substring(valueStart + 1, valueEnd);
        // 处理转义字符
        return unescapeJson(value);
    }

    /**
     * 构建简单的JSON响应
     */
    public static String buildResponse(int code, String message) {
        return "{\"code\":" + code + ",\"message\":\"" + escapeJson(message) + "\"}";
    }

    /**
     * 构建带数据的JSON响应
     */
    public static String buildResponse(int code, String message, String dataKey, String dataValue) {
        return "{\"code\":" + code + ",\"message\":\"" + escapeJson(message)
                + "\",\"" + dataKey + "\":\"" + escapeJson(dataValue) + "\"}";
    }

    /**
     * 转义JSON字符串
     */
    public static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * 反转义JSON字符串
     */
    private static String unescapeJson(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                switch (next) {
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case 'b':
                        sb.append('\b');
                        i++;
                        break;
                    case 'f':
                        sb.append('\f');
                        i++;
                        break;
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 'r':
                        sb.append('\r');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    default:
                        sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
