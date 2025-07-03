package cn.ningmo.geminicraftchat.logging;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class LogManager {
    private final GeminiCraftChat plugin;
    private final File logDirectory;
    private final Map<String, PrintWriter> logWriters;
    private String currentLogFile;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timestampFormat;
    private final boolean loggingEnabled;
    private final boolean separateFiles;
    private final int retentionDays;

    // 日志类型开关
    private final boolean logChat;
    private final boolean logCommands;
    private final boolean logErrors;
    private final boolean logModelChanges;
    private final boolean logTemperatureChanges;
    private final boolean logApiCalls;
    private final boolean logApiRequests;
    private final boolean logApiResponses;
    private final boolean logPerformance;
    private final boolean logPlayerStats;

    // 详细程度设置
    private final String apiRequestDetail;
    private final String apiResponseDetail;
    private final String performanceDetail;

    // 统计数据
    private final Map<String, AtomicLong> playerCallCounts;
    private final Map<String, AtomicLong> modelUsageCounts;
    private final Map<String, Long> apiResponseTimes;
    private final AtomicLong totalApiCalls;
    private final AtomicLong totalErrors;

    public LogManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();

        this.loggingEnabled = config.getBoolean("logging.enabled", true);
        this.separateFiles = config.getBoolean("logging.separate_files", true);
        this.retentionDays = config.getInt("logging.retention_days", 30);

        // 日志类型开关
        this.logChat = config.getBoolean("logging.include.chat", true);
        this.logCommands = config.getBoolean("logging.include.commands", true);
        this.logErrors = config.getBoolean("logging.include.errors", true);
        this.logModelChanges = config.getBoolean("logging.include.model_changes", true);
        this.logTemperatureChanges = config.getBoolean("logging.include.temperature_changes", true);
        this.logApiCalls = config.getBoolean("logging.include.api_calls", true);
        this.logApiRequests = config.getBoolean("logging.include.api_requests", true);
        this.logApiResponses = config.getBoolean("logging.include.api_responses", true);
        this.logPerformance = config.getBoolean("logging.include.performance", true);
        this.logPlayerStats = config.getBoolean("logging.include.player_stats", true);

        // 详细程度设置
        this.apiRequestDetail = config.getString("logging.detail_level.api_requests", "full");
        this.apiResponseDetail = config.getString("logging.detail_level.api_responses", "content_only");
        this.performanceDetail = config.getString("logging.detail_level.performance", "summary");

        this.dateFormat = new SimpleDateFormat(config.getString("logging.format", "yyyy-MM-dd_HH-mm-ss"));
        this.timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        this.logDirectory = new File(plugin.getDataFolder(), config.getString("logging.directory", "logs"));
        this.logWriters = new ConcurrentHashMap<>();

        // 初始化统计数据
        this.playerCallCounts = new ConcurrentHashMap<>();
        this.modelUsageCounts = new ConcurrentHashMap<>();
        this.apiResponseTimes = new ConcurrentHashMap<>();
        this.totalApiCalls = new AtomicLong(0);
        this.totalErrors = new AtomicLong(0);

        if (loggingEnabled) {
            initializeLogging();
            cleanOldLogs();
        }
    }

    private void initializeLogging() {
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            plugin.getLogger().warning("无法创建日志目录！");
            return;
        }

        String timestamp = dateFormat.format(new Date());

        if (separateFiles) {
            // 创建分类日志文件
            initializeLogWriter("general", "general_" + timestamp + ".log");
            initializeLogWriter("chat", "chat_" + timestamp + ".log");
            initializeLogWriter("api", "api_" + timestamp + ".log");
            initializeLogWriter("performance", "performance_" + timestamp + ".log");
            initializeLogWriter("errors", "errors_" + timestamp + ".log");
            initializeLogWriter("stats", "stats_" + timestamp + ".log");
        } else {
            // 创建统一日志文件
            initializeLogWriter("general", "log_" + timestamp + ".log");
        }

        // 记录会话开始信息
        logToCategory("general", "=== 日志会话开始 ===");
        logToCategory("general", "服务器版本: " + plugin.getServer().getVersion());
        logToCategory("general", "插件版本: " + plugin.getDescription().getVersion());
        logToCategory("general", "日志配置: 分离文件=" + separateFiles + ", 保留天数=" + retentionDays);
    }

    private void initializeLogWriter(String category, String filename) {
        File logFile = new File(logDirectory, filename);
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(logFile, true));
            logWriters.put(category, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法创建日志文件 " + filename + ": " + e.getMessage());
        }
    }

    public void closeLog() {
        logToCategory("general", "=== 日志会话结束 ===");

        // 输出最终统计信息
        if (logPlayerStats) {
            logFinalStats();
        }

        // 关闭所有日志写入器
        for (PrintWriter writer : logWriters.values()) {
            if (writer != null) {
                writer.close();
            }
        }
        logWriters.clear();
    }

    public void logChat(String playerName, String message, String response) {
        if (!loggingEnabled || !logChat) return;

        String category = separateFiles ? "chat" : "general";
        logToCategory(category, "[聊天] " + playerName + " > " + message);
        logToCategory(category, "[回复] " + response);

        // 更新统计
        playerCallCounts.computeIfAbsent(playerName, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void logCommand(String playerName, String command) {
        if (!loggingEnabled || !logCommands) return;
        String category = separateFiles ? "general" : "general";
        logToCategory(category, "[命令] " + playerName + " 执行: " + command);
    }

    public void logError(String playerName, String error) {
        if (!loggingEnabled || !logErrors) return;
        String category = separateFiles ? "errors" : "general";
        logToCategory(category, "[错误] " + playerName + " - " + error);
        totalErrors.incrementAndGet();
    }

    public void logModelChange(String playerName, String newModel) {
        if (!loggingEnabled || !logModelChanges) return;
        String category = separateFiles ? "general" : "general";
        logToCategory(category, "[模型] " + playerName + " 切换到: " + newModel);

        // 更新模型使用统计
        modelUsageCounts.computeIfAbsent(newModel, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void logTemperatureChange(String playerName, double newTemp) {
        if (!loggingEnabled || !logTemperatureChanges) return;
        String category = separateFiles ? "general" : "general";
        logToCategory(category, "[温度] " + playerName + " 设置为: " + newTemp);
    }

    // 新增：API调用记录方法
    public void logApiCall(String playerName, String modelName, String apiUrl, long responseTime, boolean success) {
        if (!loggingEnabled || !logApiCalls) return;

        String category = separateFiles ? "api" : "general";
        String status = success ? "成功" : "失败";
        String message = String.format("[API调用] 玩家:%s 模型:%s URL:%s 响应时间:%dms 状态:%s",
            playerName, modelName, apiUrl, responseTime, status);

        logToCategory(category, message);

        // 更新统计
        totalApiCalls.incrementAndGet();
        apiResponseTimes.put(playerName + "_" + System.currentTimeMillis(), responseTime);

        // 记录性能数据
        if (logPerformance) {
            logPerformanceData(playerName, modelName, responseTime, success);
        }
    }

    public void logApiRequest(String playerName, String modelName, String requestUrl,
                             Map<String, String> headers, String requestBody) {
        if (!loggingEnabled || !logApiRequests) return;

        String category = separateFiles ? "api" : "general";

        logToCategory(category, "[API请求] 玩家:" + playerName + " 模型:" + modelName);
        logToCategory(category, "  URL: " + requestUrl);

        if ("full".equals(apiRequestDetail)) {
            // 记录完整请求信息
            logToCategory(category, "  请求头:");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String value = header.getKey().toLowerCase().contains("authorization") ?
                    "Bearer ***" : header.getValue();
                logToCategory(category, "    " + header.getKey() + ": " + value);
            }
            logToCategory(category, "  请求体: " + requestBody);
        } else if ("headers_only".equals(apiRequestDetail)) {
            // 只记录请求头
            logToCategory(category, "  请求头数量: " + headers.size());
        }
        // minimal 模式不记录详细信息
    }

    public void logApiResponse(String playerName, String modelName, int statusCode,
                              String responseBody, String extractedContent) {
        if (!loggingEnabled || !logApiResponses) return;

        String category = separateFiles ? "api" : "general";

        logToCategory(category, "[API响应] 玩家:" + playerName + " 模型:" + modelName + " 状态码:" + statusCode);

        if ("full".equals(apiResponseDetail)) {
            // 记录完整响应
            logToCategory(category, "  完整响应: " + responseBody);
            logToCategory(category, "  提取内容: " + extractedContent);
        } else if ("content_only".equals(apiResponseDetail)) {
            // 只记录提取的内容
            logToCategory(category, "  AI回复: " + extractedContent);
        }
        // minimal 模式不记录详细信息
    }

    private void logPerformanceData(String playerName, String modelName, long responseTime, boolean success) {
        if (!logPerformance) return;

        String category = separateFiles ? "performance" : "general";

        if ("full".equals(performanceDetail)) {
            logToCategory(category, String.format("[性能] 玩家:%s 模型:%s 响应时间:%dms 成功:%s 时间戳:%d",
                playerName, modelName, responseTime, success, System.currentTimeMillis()));
        } else if ("summary".equals(performanceDetail)) {
            // 每100次调用输出一次汇总
            if (totalApiCalls.get() % 100 == 0) {
                logPerformanceSummary();
            }
        }
    }

    private void logPerformanceSummary() {
        String category = separateFiles ? "performance" : "general";

        long totalCalls = totalApiCalls.get();
        long totalErrorCount = totalErrors.get();
        double successRate = totalCalls > 0 ? ((double)(totalCalls - totalErrorCount) / totalCalls) * 100 : 0;

        // 计算平均响应时间
        long avgResponseTime = 0;
        if (!apiResponseTimes.isEmpty()) {
            avgResponseTime = apiResponseTimes.values().stream()
                .mapToLong(Long::longValue)
                .sum() / apiResponseTimes.size();
        }

        logToCategory(category, String.format("[性能汇总] 总调用:%d 成功率:%.2f%% 平均响应时间:%dms",
            totalCalls, successRate, avgResponseTime));
    }

    // 玩家使用统计
    public void logPlayerStats() {
        if (!loggingEnabled || !logPlayerStats) return;

        String category = separateFiles ? "stats" : "general";

        logToCategory(category, "=== 玩家使用统计 ===");
        for (Map.Entry<String, AtomicLong> entry : playerCallCounts.entrySet()) {
            logToCategory(category, "玩家 " + entry.getKey() + ": " + entry.getValue().get() + " 次调用");
        }

        logToCategory(category, "=== 模型使用统计 ===");
        for (Map.Entry<String, AtomicLong> entry : modelUsageCounts.entrySet()) {
            logToCategory(category, "模型 " + entry.getKey() + ": " + entry.getValue().get() + " 次使用");
        }
    }

    private void logFinalStats() {
        String category = separateFiles ? "stats" : "general";

        logToCategory(category, "=== 最终统计信息 ===");
        logToCategory(category, "总API调用次数: " + totalApiCalls.get());
        logToCategory(category, "总错误次数: " + totalErrors.get());

        if (totalApiCalls.get() > 0) {
            double successRate = ((double)(totalApiCalls.get() - totalErrors.get()) / totalApiCalls.get()) * 100;
            logToCategory(category, "成功率: " + String.format("%.2f%%", successRate));
        }

        logToCategory(category, "活跃玩家数: " + playerCallCounts.size());
        logToCategory(category, "使用的模型数: " + modelUsageCounts.size());
    }

    private void logToCategory(String category, String message) {
        PrintWriter writer = logWriters.get(category);
        if (writer != null) {
            String timestamp = timestampFormat.format(new Date());
            writer.println("[" + timestamp + "] " + message);
            writer.flush();
        }
    }

    // 清理旧日志文件
    private void cleanOldLogs() {
        if (retentionDays <= 0) return;

        File[] logFiles = logDirectory.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles == null) return;

        long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L);
        int deletedCount = 0;

        for (File file : logFiles) {
            if (file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    deletedCount++;
                }
            }
        }

        if (deletedCount > 0) {
            plugin.getLogger().info("已清理 " + deletedCount + " 个过期日志文件");
        }
    }

    public void reloadConfig() {
        FileConfiguration config = plugin.getConfig();
        boolean newLoggingEnabled = config.getBoolean("logging.enabled", true);

        if (newLoggingEnabled && logWriters.isEmpty()) {
            initializeLogging();
        } else if (!newLoggingEnabled && !logWriters.isEmpty()) {
            closeLog();
        }
    }

    // 获取统计信息的方法
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalApiCalls", totalApiCalls.get());
        stats.put("totalErrors", totalErrors.get());
        stats.put("activePlayerCount", playerCallCounts.size());
        stats.put("modelCount", modelUsageCounts.size());

        if (totalApiCalls.get() > 0) {
            double successRate = ((double)(totalApiCalls.get() - totalErrors.get()) / totalApiCalls.get()) * 100;
            stats.put("successRate", successRate);
        }

        // 计算平均响应时间
        if (!apiResponseTimes.isEmpty()) {
            long avgResponseTime = apiResponseTimes.values().stream()
                .mapToLong(Long::longValue)
                .sum() / apiResponseTimes.size();
            stats.put("avgResponseTime", avgResponseTime);
        }

        return stats;
    }

    // 重置统计数据
    public void resetStats() {
        playerCallCounts.clear();
        modelUsageCounts.clear();
        apiResponseTimes.clear();
        totalApiCalls.set(0);
        totalErrors.set(0);

        String category = separateFiles ? "stats" : "general";
        logToCategory(category, "[统计] 统计数据已重置");
    }
} 