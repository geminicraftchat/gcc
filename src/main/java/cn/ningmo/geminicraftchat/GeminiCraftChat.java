package cn.ningmo.geminicraftchat;

import org.bukkit.plugin.java.JavaPlugin;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.commands.MainCommand;
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
            
            // 验证必要的配置项
            validateConfig();
        } catch (Exception e) {
            pluginLogger.severe("配置加载失败: " + e.getMessage());
            pluginLogger.severe("插件将被禁用");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化聊天管理器
        try {
            this.chatManager = new ChatManager(this);
            pluginLogger.info("聊天管理器初始化成功");
        } catch (Exception e) {
            pluginLogger.severe("聊天管理器初始化失败: " + e.getMessage());
            pluginLogger.severe("插件将被禁用");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 注册命令
        try {
            getCommand("gcc").setExecutor(new MainCommand(this));
            pluginLogger.info("命令注册成功");
        } catch (Exception e) {
            pluginLogger.severe("命令注册失败: " + e.getMessage());
            pluginLogger.severe("插件将被禁用");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 注册监听器
        try {
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
            pluginLogger.info("事件监听器注册成功");
        } catch (Exception e) {
            pluginLogger.severe("事件监听器注册失败: " + e.getMessage());
            pluginLogger.severe("插件将被禁用");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 检查代理设置
        if (configManager.isProxyEnabled()) {
            pluginLogger.info("代理已启用 - " + configManager.getProxyHost() + ":" + configManager.getProxyPort());
        }
        
        // 输出触发词信息
        pluginLogger.info("默认触发词: " + configManager.getDefaultTrigger());
        pluginLogger.info("其他触发词: " + String.join(", ", configManager.getTriggerWords()));
        
        // 输出人设信息
        int personaCount = configManager.getConfig().getConfigurationSection("personas").getKeys(false).size();
        pluginLogger.info("已加载 " + personaCount + " 个人设");
        
        // 输出敏感词过滤信息
        if (configManager.isFilterEnabled()) {
            int filterWordCount = configManager.getFilterWords().size();
            pluginLogger.info("敏感词过滤已启用，共 " + filterWordCount + " 个敏感词");
        }
        
        pluginLogger.info("GeminiCraftChat v" + getDescription().getVersion() + " 插件已成功启动!");
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
        String apiKey = configManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            throw new IllegalStateException("API密钥未设置");
        }
        
        String model = configManager.getModel();
        if (model == null || model.isEmpty()) {
            throw new IllegalStateException("模型名称未设置");
        }
        
        if (configManager.getCooldown() < 0) {
            throw new IllegalStateException("冷却时间不能为负数");
        }
        
        if (configManager.getMaxHistory() <= 0) {
            throw new IllegalStateException("历史记录长度必须大于0");
        }
        
        if (configManager.isFilterEnabled()) {
            List<String> filterWords = configManager.getFilterWords();
            if (filterWords == null) {
                pluginLogger.warning("敏感词过滤已启用但未配置敏感词列表");
            }
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
} 