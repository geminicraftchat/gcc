package cn.ningmo.geminicraftchat.metrics;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SingleLineChart;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * bStats统计管理器
 * 收集匿名使用统计数据以帮助改进插件
 */
public class MetricsManager {
    private final GeminiCraftChat plugin;
    private final ConfigManager configManager;
    private Metrics metrics;
    
    // 插件ID来自bStats网站
    private static final int PLUGIN_ID = 26354;
    
    public MetricsManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    
    /**
     * 初始化bStats统计
     */
    public void initializeMetrics() {
        // 检查配置文件中是否启用了bStats
        if (!configManager.getConfig().getBoolean("bstats.enabled", true)) {
            plugin.getLogger().info("bStats统计已在配置中禁用");
            return;
        }

        try {
            metrics = new Metrics(plugin, PLUGIN_ID);

            // 添加自定义图表
            addCustomCharts();

            plugin.getLogger().info("bStats统计已启用 - 插件ID: " + PLUGIN_ID);
            plugin.getLogger().info("统计数据完全匿名，用于改进插件功能");
        } catch (Exception e) {
            plugin.getLogger().warning("bStats统计初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加自定义统计图表
     */
    private void addCustomCharts() {
        // 当前使用的AI模型
        metrics.addCustomChart(new SimplePie("current_ai_model", new Callable<String>() {
            @Override
            public String call() throws Exception {
                String currentModel = configManager.getCurrentModel();
                String modelName = configManager.getModelDisplayName(currentModel);
                
                // 简化模型名称以便统计
                if (modelName.toLowerCase().contains("gpt")) {
                    return "OpenAI GPT";
                } else if (modelName.toLowerCase().contains("claude")) {
                    return "Anthropic Claude";
                } else if (modelName.toLowerCase().contains("gemini")) {
                    return "Google Gemini";
                } else if (modelName.toLowerCase().contains("deepseek")) {
                    return "DeepSeek";
                } else {
                    return "Custom API";
                }
            }
        }));
        
        // 配置的API数量
        metrics.addCustomChart(new SimplePie("configured_apis", new Callable<String>() {
            @Override
            public String call() throws Exception {
                int apiCount = configManager.getAvailableModels().size();
                if (apiCount == 1) {
                    return "1 API";
                } else if (apiCount <= 3) {
                    return "2-3 APIs";
                } else if (apiCount <= 5) {
                    return "4-5 APIs";
                } else {
                    return "5+ APIs";
                }
            }
        }));
        
        // 是否启用长思考模式
        metrics.addCustomChart(new SimplePie("long_thinking_enabled", new Callable<String>() {
            @Override
            public String call() throws Exception {
                // 检查是否有任何模型启用了长思考模式
                for (String modelKey : configManager.getAvailableModels()) {
                    if (configManager.getConfig().getBoolean("api.models." + modelKey + ".timeout.long_thinking", false)) {
                        return "Enabled";
                    }
                }
                return "Disabled";
            }
        }));
        
        // 是否启用代理
        metrics.addCustomChart(new SimplePie("proxy_enabled", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return configManager.isHttpProxyEnabled() ? "Enabled" : "Disabled";
            }
        }));
        
        // 是否启用敏感词过滤
        metrics.addCustomChart(new SimplePie("word_filter_enabled", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return configManager.isFilterEnabled() ? "Enabled" : "Disabled";
            }
        }));
        
        // 配置的人设数量
        metrics.addCustomChart(new SimplePie("persona_count", new Callable<String>() {
            @Override
            public String call() throws Exception {
                int personaCount = configManager.getConfig().getConfigurationSection("personas").getKeys(false).size();
                if (personaCount <= 3) {
                    return "1-3 Personas";
                } else if (personaCount <= 5) {
                    return "4-5 Personas";
                } else if (personaCount <= 10) {
                    return "6-10 Personas";
                } else {
                    return "10+ Personas";
                }
            }
        }));
        
        // 是否启用日志记录
        metrics.addCustomChart(new SimplePie("logging_enabled", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return configManager.getConfig().getBoolean("logging.enabled", true) ? "Enabled" : "Disabled";
            }
        }));
        
        // Java版本
        metrics.addCustomChart(new SimplePie("java_version", new Callable<String>() {
            @Override
            public String call() throws Exception {
                String version = System.getProperty("java.version");
                if (version.startsWith("1.8")) {
                    return "Java 8";
                } else if (version.startsWith("11")) {
                    return "Java 11";
                } else if (version.startsWith("17")) {
                    return "Java 17";
                } else if (version.startsWith("21")) {
                    return "Java 21";
                } else {
                    return "Other";
                }
            }
        }));
        
        // 服务器软件类型
        metrics.addCustomChart(new SimplePie("server_software", new Callable<String>() {
            @Override
            public String call() throws Exception {
                String serverName = plugin.getServer().getName().toLowerCase();
                if (serverName.contains("paper")) {
                    return "Paper";
                } else if (serverName.contains("spigot")) {
                    return "Spigot";
                } else if (serverName.contains("bukkit")) {
                    return "Bukkit";
                } else if (serverName.contains("folia")) {
                    return "Folia";
                } else {
                    return "Other";
                }
            }
        }));
        
        // 触发词数量
        metrics.addCustomChart(new SimplePie("trigger_words_count", new Callable<String>() {
            @Override
            public String call() throws Exception {
                int triggerCount = 1 + configManager.getTriggerWords().size(); // 默认触发词 + 额外触发词
                if (triggerCount == 1) {
                    return "1 Trigger";
                } else if (triggerCount <= 3) {
                    return "2-3 Triggers";
                } else if (triggerCount <= 5) {
                    return "4-5 Triggers";
                } else {
                    return "5+ Triggers";
                }
            }
        }));

        // API调用统计（如果日志管理器可用）
        metrics.addCustomChart(new SingleLineChart("api_calls_per_day", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                try {
                    if (plugin.getLogManager() != null) {
                        Map<String, Object> stats = plugin.getLogManager().getStats();
                        Object apiCalls = stats.get("totalApiCalls");
                        if (apiCalls instanceof Number) {
                            // 返回每日平均API调用次数（简化计算）
                            return Math.min(((Number) apiCalls).intValue(), 1000);
                        }
                    }
                } catch (Exception e) {
                    // 忽略错误，返回0
                }
                return 0;
            }
        }));
    }
    
    /**
     * 获取统计实例
     */
    public Metrics getMetrics() {
        return metrics;
    }
    
    /**
     * 关闭统计
     */
    public void shutdown() {
        if (metrics != null) {
            // bStats会自动处理关闭
            plugin.getLogger().info("bStats统计已关闭");
        }
    }
}
