package cn.ningmo.geminicraftchat.config;

import org.bukkit.configuration.file.FileConfiguration;
import cn.ningmo.geminicraftchat.GeminiCraftChat;

public class ConfigManager {
    private final GeminiCraftChat plugin;
    private FileConfiguration config;

    public ConfigManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public String getApiKey() {
        String envKey = System.getenv("GEMINI_API_KEY");
        return envKey != null ? envKey : config.getString("api.key");
    }

    public String getModel() {
        return config.getString("api.model", "gemini-1.5-pro");
    }

    public boolean isProxyEnabled() {
        return config.getBoolean("api.proxy.enabled", false);
    }

    public String getDefaultTrigger() {
        return config.getString("chat.trigger", "ai");
    }

    public java.util.List<String> getTriggerWords() {
        return config.getStringList("chat.trigger_words");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getProjectId() {
        return config.getString("api.project_id");
    }

    public String getLocation() {
        return config.getString("api.location", "us-central1");
    }
} 