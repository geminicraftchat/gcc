package cn.ningmo.geminicraftchat;

import org.bukkit.plugin.java.JavaPlugin;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.commands.MainCommand;
import cn.ningmo.geminicraftchat.listeners.ChatListener;
import cn.ningmo.geminicraftchat.chat.ChatManager;

public class GeminiCraftChat extends JavaPlugin {
    private static GeminiCraftChat instance;
    private ConfigManager configManager;
    private ChatManager chatManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化配置管理器
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();
        
        // 初始化聊天管理器
        this.chatManager = new ChatManager(this);
        
        // 注册命令
        getCommand("gcc").setExecutor(new MainCommand(this));
        
        // 注册监听器
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        getLogger().info("GeminiCraftChat 插件已启动!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GeminiCraftChat 插件已关闭!");
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
} 