package cn.ningmo.geminicraftchat.security;

import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * 输入验证和XSS防护工具类
 * 提供用户输入的安全检查和清理功能
 */
public class InputValidator {
    private static final Logger logger = Logger.getLogger(InputValidator.class.getName());
    
    // 危险的HTML标签和属性模式
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
        "<[^>]+>", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
        "javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ON_EVENT_PATTERN = Pattern.compile(
        "on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    
    // SQL注入关键词
    private static final List<String> SQL_KEYWORDS = Arrays.asList(
        "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
        "UNION", "OR", "AND", "WHERE", "FROM", "TABLE", "DATABASE",
        "EXEC", "EXECUTE", "SCRIPT", "DECLARE", "CAST", "CONVERT"
    );
    
    // 命令注入关键词
    private static final List<String> COMMAND_KEYWORDS = Arrays.asList(
        "cmd", "powershell", "bash", "sh", "exec", "system", "eval",
        "rm", "del", "format", "shutdown", "reboot", "kill"
    );
    
    // 最大输入长度限制
    private static final int MAX_INPUT_LENGTH = 2000;
    private static final int MAX_COMMAND_LENGTH = 100;
    
    /**
     * 验证聊天消息输入
     */
    public static ValidationResult validateChatMessage(String message) {
        if (message == null) {
            return new ValidationResult(false, "消息不能为空");
        }
        
        // 长度检查
        if (message.length() > MAX_INPUT_LENGTH) {
            return new ValidationResult(false, "消息长度超过限制（" + MAX_INPUT_LENGTH + "字符）");
        }
        
        // XSS检查
        if (containsXSS(message)) {
            logger.warning("检测到XSS攻击尝试: " + sanitizeForLog(message));
            return new ValidationResult(false, "消息包含不安全的内容");
        }
        
        // SQL注入检查
        if (containsSQLInjection(message)) {
            logger.warning("检测到SQL注入尝试: " + sanitizeForLog(message));
            return new ValidationResult(false, "消息包含不安全的内容");
        }
        
        // 命令注入检查
        if (containsCommandInjection(message)) {
            logger.warning("检测到命令注入尝试: " + sanitizeForLog(message));
            return new ValidationResult(false, "消息包含不安全的内容");
        }
        
        return new ValidationResult(true, "验证通过");
    }
    
    /**
     * 验证命令输入
     */
    public static ValidationResult validateCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return new ValidationResult(false, "命令不能为空");
        }
        
        // 长度检查
        if (command.length() > MAX_COMMAND_LENGTH) {
            return new ValidationResult(false, "命令长度超过限制（" + MAX_COMMAND_LENGTH + "字符）");
        }
        
        // 基本格式检查
        if (!command.matches("^[a-zA-Z0-9\\s\\u4e00-\\u9fa5_-]+$")) {
            return new ValidationResult(false, "命令包含非法字符");
        }
        
        return new ValidationResult(true, "验证通过");
    }
    
    /**
     * 验证人格名称
     */
    public static ValidationResult validatePersonaName(String personaName) {
        if (personaName == null || personaName.trim().isEmpty()) {
            return new ValidationResult(false, "人格名称不能为空");
        }
        
        // 长度检查
        if (personaName.length() > 50) {
            return new ValidationResult(false, "人格名称长度超过限制（50字符）");
        }
        
        // 只允许字母、数字、中文字符、下划线和连字符
        if (!personaName.matches("^[\\w\\u4e00-\\u9fa5_-]+$")) {
            return new ValidationResult(false, "人格名称包含非法字符");
        }
        
        return new ValidationResult(true, "验证通过");
    }
    
    /**
     * 清理用户输入，移除危险内容
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        String cleaned = input;
        
        // 移除HTML标签
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");
        
        // 移除JavaScript
        cleaned = JAVASCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        
        // 移除事件处理器
        cleaned = ON_EVENT_PATTERN.matcher(cleaned).replaceAll("");
        
        // HTML实体编码
        cleaned = htmlEncode(cleaned);
        
        return cleaned;
    }
    
    /**
     * 检查是否包含XSS攻击
     */
    private static boolean containsXSS(String input) {
        String lowerInput = input.toLowerCase();
        
        // 检查脚本标签
        if (SCRIPT_PATTERN.matcher(lowerInput).find()) {
            return true;
        }
        
        // 检查JavaScript协议
        if (JAVASCRIPT_PATTERN.matcher(lowerInput).find()) {
            return true;
        }
        
        // 检查事件处理器
        if (ON_EVENT_PATTERN.matcher(lowerInput).find()) {
            return true;
        }
        
        // 检查其他危险模式
        String[] dangerousPatterns = {
            "<iframe", "<object", "<embed", "<form", "<input",
            "vbscript:", "data:", "expression(", "url(",
            "@import", "\\x", "&#", "&lt;script"
        };
        
        for (String pattern : dangerousPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否包含SQL注入
     */
    private static boolean containsSQLInjection(String input) {
        String upperInput = input.toUpperCase();
        
        // 检查SQL关键词组合
        for (String keyword : SQL_KEYWORDS) {
            if (upperInput.contains(keyword)) {
                // 检查是否是SQL注入模式
                if (upperInput.contains(keyword + " ") || 
                    upperInput.contains(" " + keyword) ||
                    upperInput.contains("'" + keyword) ||
                    upperInput.contains(keyword + "'")) {
                    return true;
                }
            }
        }
        
        // 检查常见SQL注入模式
        String[] sqlPatterns = {
            "' OR '1'='1", "' OR 1=1", "'; DROP", "'; DELETE",
            "UNION SELECT", "' UNION", "/*", "*/", "--",
            "1=1", "'=''", "admin'--"
        };
        
        for (String pattern : sqlPatterns) {
            if (upperInput.contains(pattern.toUpperCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否包含命令注入
     */
    private static boolean containsCommandInjection(String input) {
        String lowerInput = input.toLowerCase();
        
        // 检查命令注入关键词
        for (String keyword : COMMAND_KEYWORDS) {
            if (lowerInput.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        // 检查命令注入符号
        String[] commandPatterns = {
            ";", "|", "&", "`", "$(", "${", "<(", ">(",
            "../", "..\\\\", "/etc/", "c:\\\\", "cmd.exe"
        };
        
        for (String pattern : commandPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * HTML实体编码
     */
    private static String htmlEncode(String input) {
        if (input == null) {
            return null;
        }
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;");
    }
    
    /**
     * 为日志记录清理敏感信息
     */
    private static String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        
        // 限制长度并移除换行符
        String sanitized = input.length() > 100 ? input.substring(0, 100) + "..." : input;
        return sanitized.replaceAll("[\\r\\n]", " ");
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}