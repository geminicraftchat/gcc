package cn.ningmo.geminicraftchat.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.security.KeyEncryption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.nio.file.Paths;

public class ConfigManager {
    private final GeminiCraftChat plugin;
    private FileConfiguration config;
    private KeyEncryption keyEncryption;

    public ConfigManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.keyEncryption = new KeyEncryption(Paths.get(plugin.getDataFolder().getAbsolutePath()));
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        migrateApiKeys();
    }

    private void migrateApiKeys() {
        ConfigurationSection modelsSection = config.getConfigurationSection("api.models");
        if (modelsSection == null) {
            return;
        }
        
        boolean configChanged = false;
        for (String modelName : modelsSection.getKeys(false)) {
            String apiKeyPath = "api.models." + modelName + ".api_key";
            String apiKey = config.getString(apiKeyPath);
            
            if (apiKey != null && !apiKey.isEmpty() && !keyEncryption.isEncrypted(apiKey)) {
                try {
                    String encryptedKey = keyEncryption.encrypt(apiKey);
                    config.set(apiKeyPath, encryptedKey);
                    configChanged = true;
                    plugin.getLogger().info("已加密模型 " + modelName + " 的API密钥");
                } catch (Exception e) {
                    plugin.getLogger().severe("加密模型 " + modelName + " 的API密钥失败: " + e.getMessage());
                }
            }
        }
        
        if (configChanged) {
            plugin.saveConfig();
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getPermission(String key) {
        return config.getString("permissions." + key, "gcc." + key);
    }

    public List<String> getAvailableModels() {
        ConfigurationSection modelsSection = config.getConfigurationSection("api.models");
        return modelsSection != null ? new ArrayList<>(modelsSection.getKeys(false)) : new ArrayList<>();
    }

    public String getCurrentModel() {
        return config.getString("api.current_model", "gemini");
    }

    public String getModelDisplayName(String modelName) {
        return config.getString("api.models." + modelName + ".name", modelName);
    }

    public boolean switchModel(String modelName) {
        if (!getAvailableModels().contains(modelName)) {
            return false;
        }
        config.set("api.current_model", modelName);
        plugin.saveConfig();
        return true;
    }

    public double getTemperature() {
        String currentModel = getCurrentModel();
        return config.getDouble("api.models." + currentModel + ".temperature", 0.7);
    }

    public void setTemperature(double temperature) {
        String currentModel = getCurrentModel();
        config.set("api.models." + currentModel + ".temperature", temperature);
        plugin.saveConfig();
    }

    public String getApiKey() {
        String currentModel = getCurrentModel();
        String encryptedKey = config.getString("api.models." + currentModel + ".api_key");
        if (encryptedKey == null || encryptedKey.isEmpty()) {
            return "";
        }
        
        try {
            // 如果是加密的密钥，解密后返回
            if (keyEncryption.isEncrypted(encryptedKey)) {
                return keyEncryption.decrypt(encryptedKey);
            } else {
                // 明文密钥，记录警告并返回
                plugin.getLogger().warning("检测到明文API密钥，建议重新加载配置以启用加密存储");
                return encryptedKey;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("解密API密钥失败: " + e.getMessage());
            return "";
        }
    }

    public String getModel() {
        String currentModel = getCurrentModel();
        return config.getString("api.models." + currentModel + ".model");
    }

    public String getBaseUrl() {
        String currentModel = getCurrentModel();
        return config.getString("api.models." + currentModel + ".base_url");
    }

    public int getMaxTokens() {
        String currentModel = getCurrentModel();
        return config.getInt("api.models." + currentModel + ".max_tokens", 4096);
    }

    public String getDefaultTrigger() {
        return config.getString("chat.trigger", "ai");
    }

    public List<String> getTriggerWords() {
        List<String> words = config.getStringList("chat.trigger_words");
        return words != null ? words : new ArrayList<>();
    }

    public int getMaxHistory() {
        return config.getInt("chat.max_history", 10);
    }
    
    public int getChatHistoryLimit() {
        return getMaxHistory();
    }

    public String getThinkingFormat() {
        return config.getString("chat.format.thinking", "§7[AI] §f正在思考中...");
    }

    public String getResponseFormat() {
        return config.getString("chat.format.response", "§7[AI] §f%s");
    }

    public String getErrorFormat() {
        return config.getString("chat.format.error", "§c[AI] 发生错误：%s");
    }

    public long getCooldown() {
        return config.getLong("chat.cooldown", 10000);
    }

    public boolean isFilterEnabled() {
        return config.getBoolean("filter.enabled", true);
    }

    public List<String> getFilterWords() {
        List<String> words = config.getStringList("filter.words");
        return words != null ? words : new ArrayList<>();
    }

    public String getFilterReplacement() {
        return config.getString("filter.replacement", "***");
    }

    public boolean isHttpProxyEnabled() {
        return config.getBoolean("api.http_proxy.enabled", false);
    }

    public String getHttpProxyHost() {
        return config.getString("api.http_proxy.host", "127.0.0.1");
    }

    public int getHttpProxyPort() {
        return config.getInt("api.http_proxy.port", 7890);
    }

    public String getHttpProxyType() {
        return config.getString("api.http_proxy.type", "SOCKS");
    }

    /**
     * 获取聊天消息格式
     * @param key 消息类型键值
     * @return 格式化的消息字符串
     */
    public String getChatFormat(String key) {
        return config.getString("chat.format." + key, "§c[AI] 未知的消息格式: " + key);
    }

    /**
     * 获取聊天消息格式并应用参数
     * @param key 消息类型键值
     * @param args 格式化参数
     * @return 格式化的消息字符串
     */
    public String getChatFormat(String key, Object... args) {
        String format = getChatFormat(key);
        return String.format(format, args);
    }
}