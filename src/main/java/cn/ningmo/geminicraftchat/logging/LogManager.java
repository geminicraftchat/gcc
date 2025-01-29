package cn.ningmo.geminicraftchat.logging;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class LogManager {
    private final GeminiCraftChat plugin;
    private final File logDirectory;
    private PrintWriter logWriter;
    private String currentLogFile;
    private final SimpleDateFormat dateFormat;
    private final boolean loggingEnabled;
    private final boolean logChat;
    private final boolean logCommands;
    private final boolean logErrors;
    private final boolean logModelChanges;
    private final boolean logTemperatureChanges;

    public LogManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        
        this.loggingEnabled = config.getBoolean("logging.enabled", true);
        this.logChat = config.getBoolean("logging.include.chat", true);
        this.logCommands = config.getBoolean("logging.include.commands", true);
        this.logErrors = config.getBoolean("logging.include.errors", true);
        this.logModelChanges = config.getBoolean("logging.include.model_changes", true);
        this.logTemperatureChanges = config.getBoolean("logging.include.temperature_changes", true);
        
        this.dateFormat = new SimpleDateFormat(config.getString("logging.format", "yyyy-MM-dd_HH-mm-ss"));
        this.logDirectory = new File(plugin.getDataFolder(), config.getString("logging.directory", "logs"));
        
        if (loggingEnabled) {
            initializeLogging();
        }
    }

    private void initializeLogging() {
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            plugin.getLogger().warning("无法创建日志目录！");
            return;
        }

        String timestamp = dateFormat.format(new Date());
        currentLogFile = "log_" + timestamp + ".txt";
        File logFile = new File(logDirectory, currentLogFile);

        try {
            logWriter = new PrintWriter(new FileWriter(logFile, true));
            logMessage("=== 日志会话开始 ===");
            logMessage("服务器版本: " + plugin.getServer().getVersion());
            logMessage("插件版本: " + plugin.getDescription().getVersion());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法创建日志文件: " + e.getMessage());
        }
    }

    public void closeLog() {
        if (logWriter != null) {
            logMessage("=== 日志会话结束 ===");
            logWriter.close();
        }
    }

    public void logChat(String playerName, String message, String response) {
        if (!loggingEnabled || !logChat || logWriter == null) return;
        logMessage("[聊天] " + playerName + " > " + message);
        logMessage("[回复] " + response);
    }

    public void logCommand(String playerName, String command) {
        if (!loggingEnabled || !logCommands || logWriter == null) return;
        logMessage("[命令] " + playerName + " 执行: " + command);
    }

    public void logError(String playerName, String error) {
        if (!loggingEnabled || !logErrors || logWriter == null) return;
        logMessage("[错误] " + playerName + " - " + error);
    }

    public void logModelChange(String playerName, String newModel) {
        if (!loggingEnabled || !logModelChanges || logWriter == null) return;
        logMessage("[模型] " + playerName + " 切换到: " + newModel);
    }

    public void logTemperatureChange(String playerName, double newTemp) {
        if (!loggingEnabled || !logTemperatureChanges || logWriter == null) return;
        logMessage("[温度] " + playerName + " 设置为: " + newTemp);
    }

    private void logMessage(String message) {
        if (logWriter != null) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            logWriter.println("[" + timestamp + "] " + message);
            logWriter.flush();
        }
    }

    public void reloadConfig() {
        FileConfiguration config = plugin.getConfig();
        if (loggingEnabled && logWriter == null) {
            initializeLogging();
        } else if (!loggingEnabled && logWriter != null) {
            closeLog();
        }
    }
} 