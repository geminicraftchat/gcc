package cn.ningmo.geminicraftchat;

import org.bukkit.plugin.java.JavaPlugin;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.commands.MainCommand;
import cn.ningmo.geminicraftchat.commands.AdminCommand;
import cn.ningmo.geminicraftchat.listeners.ChatListener;
import cn.ningmo.geminicraftchat.chat.ChatManager;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

public class GeminiCraftChat extends JavaPlugin {
    private static GeminiCraftChat instance;
    private ConfigManager configManager;
    private ChatManager chatManager;
    private Logger pluginLogger;

    @Override
    public void onEnable() {
        instance = this;
        this.pluginLogger = getLogger();
        
        pluginLogger.info("正在初始化 GeminiCraftChat 插件...");
        
        // 初始化配置管理器
        try {
            this.configManager = new ConfigManager(this);
            this.configManager.loadConfig();
            pluginLogger.info("配置加载成功");
        } catch (Exception e) {
            pluginLogger.severe("配置加载失败: " + e.getMessage());
            pluginLogger.severe("将使用默认配置");
        }
        
        // 验证配置并提供警告而不是直接禁用
        validateConfig();
        
        // 初始化聊天管理器
        try {
            this.chatManager = new ChatManager(this);
            pluginLogger.info("聊天管理器初始化成功");
        } catch (Exception e) {
            pluginLogger.severe("聊天管理器初始化失败: " + e.getMessage());
            pluginLogger.warning("AI聊天功能将不可用，但其他功能仍可使用");
        }
        
        // 注册命令
        try {
            getCommand("gcc").setExecutor(new MainCommand(this));
            getCommand("gccadmin").setExecutor(new AdminCommand(this));
            pluginLogger.info("命令注册成功");
        } catch (Exception e) {
            pluginLogger.severe("命令注册失败: " + e.getMessage());
            pluginLogger.warning("命令功能将不可用");
        }
        
        // 注册监听器
        try {
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
            pluginLogger.info("事件监听器注册成功");
        } catch (Exception e) {
            pluginLogger.severe("事件监听器注册失败: " + e.getMessage());
            pluginLogger.warning("聊天监听功能将不可用");
        }
        
        // 输出配置信息
        logConfigInfo();
        
        pluginLogger.info("GeminiCraftChat v" + getDescription().getVersion() + " 插件已启动!");
    }

    @Override
    public void onDisable() {
        if (chatManager != null) {
            try {
                chatManager.clearAllHistory();
                pluginLogger.info("已清理所有聊天历史记录");
            } catch (Exception e) {
                pluginLogger.warning("清理聊天历史记录时发生错误: " + e.getMessage());
            }
        }
        
        pluginLogger.info("GeminiCraftChat 插件已关闭!");
    }

    private void validateConfig() {
        boolean hasWarnings = false;
        
        // 检查API密钥
        String apiKey = configManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            if (configManager.isProxyApi()) {
                pluginLogger.warning("中转API密钥未设置，AI聊天功能将不可用");
            } else {
                pluginLogger.warning("Gemini API密钥未设置，AI聊天功能将不可用");
            }
            hasWarnings = true;
        }
        
        // 检查模型设置
        String model = configManager.getModel();
        if (model == null || model.isEmpty()) {
            pluginLogger.warning("模型名称未设置，将使用默认模型 gemini-pro");
            hasWarnings = true;
        }
        
        // 检查冷却时间
        if (configManager.getCooldown() < 0) {
            pluginLogger.warning("冷却时间不能为负数，将使用默认值 10 秒");
            hasWarnings = true;
        }
        
        // 检查历史记录长度
        if (configManager.getMaxHistory() <= 0) {
            pluginLogger.warning("历史记录长度必须大于0，将使用默认值 10");
            hasWarnings = true;
        }
        
        // 检查敏感词过滤
        if (configManager.isFilterEnabled()) {
            List<String> filterWords = configManager.getFilterWords();
            if (filterWords == null || filterWords.isEmpty()) {
                pluginLogger.warning("敏感词过滤已启用但未配置敏感词列表");
                hasWarnings = true;
            }
        }
        
        if (hasWarnings) {
            pluginLogger.warning("配置验证完成，存在一些警告，请检查配置文件");
        } else {
            pluginLogger.info("配置验证完成，一切正常");
        }
    }

    private void logConfigInfo() {
        // 检查代理设置
        if (configManager.isHttpProxyEnabled()) {
            pluginLogger.info("代理已启用 - " + configManager.getHttpProxyHost() + ":" + configManager.getHttpProxyPort() + 
                " (" + configManager.getProxyType() + ")");
        }
        
        // 输出API模式
        String apiType = configManager.getConfig().getString("api.type", "direct");
        pluginLogger.info("API模式: " + apiType.toUpperCase());
        
        // 输出触发词信息
        pluginLogger.info("默认触发词: " + configManager.getDefaultTrigger());
        List<String> triggers = configManager.getTriggerWords();
        if (!triggers.isEmpty()) {
            pluginLogger.info("其他触发词: " + String.join(", ", triggers));
        }
        
        // 输出人设信息
        try {
            int personaCount = configManager.getConfig().getConfigurationSection("personas").getKeys(false).size();
            pluginLogger.info("已加载 " + personaCount + " 个人设");
        } catch (Exception e) {
            pluginLogger.warning("人设配置加载失败，将使用默认人设");
        }
        
        // 输出敏感词过滤信息
        if (configManager.isFilterEnabled()) {
            int filterWordCount = configManager.getFilterWords().size();
            pluginLogger.info("敏感词过滤已启用，共 " + filterWordCount + " 个敏感词");
        }
        
        // 输出调试状态
        if (isDebugEnabled()) {
            pluginLogger.info("调试模式已启用");
        }
    }

    public static GeminiCraftChat getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public void log(Level level, String message) {
        pluginLogger.log(level, message);
    }

    public void debug(String message) {
        if (configManager.getConfig().getBoolean("debug", false)) {
            pluginLogger.info("[DEBUG] " + message);
        }
    }

    public boolean isDebugEnabled() {
        return configManager.getConfig().getBoolean("debug", false);
    }

    public void validateAndLogConfig() {
        validateConfig();
        logConfigInfo();
    }
} 