package cn.ningmo.geminicraftchat.chat;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.api.GeminiService;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.persona.Persona;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChatManager {
    private final GeminiCraftChat plugin;
    private final List<GeminiService> apiServices;
    private final Random random;
    private int currentApiIndex;
    private final ConfigManager configManager;
    private final Map<String, String> playerPersonas;
    private final Map<String, Long> cooldowns;
    private final Map<GeminiService, ServiceStatus> serviceStatuses;
    private final Map<String, List<Long>> requestTimes = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    // 添加服务状态内部类
    private static class ServiceStatus {
        int errorCount;
        long disabledUntil;
        
        boolean isAvailable() {
            return System.currentTimeMillis() >= disabledUntil;
        }
        
        void recordError() {
            errorCount++;
        }
        
        void reset() {
            errorCount = 0;
            disabledUntil = 0;
        }
    }

    public ChatManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.apiServices = new ArrayList<>();
        this.random = new Random();
        this.currentApiIndex = 0;
        this.configManager = plugin.getConfigManager();
        this.playerPersonas = new ConcurrentHashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
        this.serviceStatuses = new ConcurrentHashMap<>();
        
        // 初始化API服务列表和状态
        List<Map<?, ?>> items = plugin.getConfig().getMapList("api.items");
        
        for (Map<?, ?> item : items) {
            try {
                // 安全地检查是否启用
                Object enabledObj = item.get("enabled");
                boolean enabled = true; // 默认启用
                if (enabledObj instanceof Boolean) {
                    enabled = (Boolean) enabledObj;
                }
                
                if (!enabled) {
                    plugin.debug("跳过禁用的API节点: " + item.get("name"));
                    continue;
                }

                // 安全地获取节点类型
                Object typeObj = item.get("type");
                String nodeType = typeObj != null ? typeObj.toString() : "proxy";
                
                // 创建服务实例
                GeminiService service = new GeminiService(plugin, item, nodeType);
                apiServices.add(service);
                serviceStatuses.put(service, new ServiceStatus());
                
                plugin.debug("成功加载API节点: " + service.getEndpointInfo());
            } catch (Exception e) {
                plugin.getLogger().warning("加载API节点失败 [" + item.get("name") + "]: " + e.getMessage());
            }
        }

        if (apiServices.isEmpty()) {
            plugin.getLogger().severe("没有可用的API节点！");
        } else {
            plugin.getLogger().info("成功加载 " + apiServices.size() + " 个API节点");
        }
    }

    public void handleChat(Player player, String message) {
        try {
            String playerId = player.getName();
            
            // 检查消息长度
            int maxLength = configManager.getConfig().getInt("security.limits.max_message_length", 500);
            if (message.length() > maxLength) {
                player.sendMessage(ChatColor.RED + "消息长度超过限制 (" + maxLength + " 字符)");
                return;
            }
            
            // 检查速率限制
            if (!checkRateLimit(playerId)) {
                player.sendMessage(ChatColor.RED + "发送消息太快，请稍后再试");
                return;
            }
            
            // 检查冷却时间
            if (isOnCooldown(playerId)) {
                long remainingTime = getRemainingCooldown(playerId);
                player.sendMessage(ChatColor.RED + "请等待 " + (remainingTime / 1000) + " 秒后再次发送消息");
                return;
            }

            // 添加安全检查
            if (containsCommandSyntax(message)) {
                player.sendMessage(ChatColor.RED + "为了安全考虑，不允许在对话中包含命令语法");
                return;
            }

            // 发送思考消息
            player.sendMessage(String.format(configManager.getThinkingFormat()));

            // 获取当前人设
            Optional<Persona> persona = getCurrentPersona(playerId);

            // 广播问题
            if (shouldBroadcast(player, persona)) {
                broadcastQuestion(player, message);
            }

            // 获取API服务
            GeminiService service = selectApiService();
            
            // 发送请求
            sendMessageWithRetry(player, message, persona, service, 0);

            // 设置冷却时间
            setCooldown(playerId);
        } catch (Exception e) {
            plugin.getLogger().severe("处理聊天消息时发生错误: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "处理消息时发生错误，请稍后重试");
        }
    }

    public boolean switchPersona(Player player, String personaName) {
        if (configManager.getConfig().contains("personas." + personaName)) {
            playerPersonas.put(player.getName(), personaName);
            // 切换人设时清除历史记录
            clearHistory(player.getName());
            return true;
        }
        return false;
    }

    public List<String> getAvailablePersonas() {
        return new ArrayList<>(configManager.getConfig().getConfigurationSection("personas").getKeys(false));
    }

    private Optional<Persona> getCurrentPersona(String playerId) {
        String personaName = playerPersonas.get(playerId);
        if (personaName == null) {
            personaName = "default";
        }
        
        if (configManager.getConfig().contains("personas." + personaName)) {
            String name = configManager.getConfig().getString("personas." + personaName + ".name");
            String description = configManager.getConfig().getString("personas." + personaName + ".description");
            String context = configManager.getConfig().getString("personas." + personaName + ".context");
            return Optional.of(new Persona(name, description, context));
        }
        
        return Optional.empty();
    }

    private boolean isOnCooldown(String playerId) {
        Long lastUse = cooldowns.get(playerId);
        return lastUse != null && System.currentTimeMillis() - lastUse < configManager.getCooldown();
    }

    private long getRemainingCooldown(String playerId) {
        Long lastUse = cooldowns.get(playerId);
        if (lastUse == null) return 0;
        return Math.max(0, configManager.getCooldown() - (System.currentTimeMillis() - lastUse));
    }

    private void setCooldown(String playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    public void clearHistory(String playerId) {
        for (GeminiService service : apiServices) {
            service.clearHistory(playerId);
        }
    }

    public void clearAllHistory() {
        for (GeminiService service : apiServices) {
            service.clearAllHistory();
        }
    }

    private boolean shouldBroadcast(Player player, Optional<Persona> persona) {
        // 检查是否启用广播
        if (!configManager.getConfig().getBoolean("chat.broadcast.enabled", true)) {
            return false;
        }

        // 检查玩家是否有跳过广播的权限
        if (player.hasPermission("gcc.broadcast.bypass")) {
            return false;
        }

        // 检查当前人设是否在忽略列表中
        if (persona.isPresent()) {
            List<String> ignorePersonas = configManager.getConfig().getStringList("chat.broadcast.ignore_personas");
            if (ignorePersonas.contains(playerPersonas.get(player.getName()))) {
                return false;
            }
        }

        return true;
    }

    private void broadcastQuestion(Player player, String message) {
        String format = configManager.getConfig().getString("chat.broadcast.format.question", 
            "§8[AI] §7{player} §f问: §7{message}");
        String broadcast = format
            .replace("{player}", player.getName())
            .replace("{message}", message);

        // 广播给其他玩家
        if (configManager.getConfig().getBoolean("chat.broadcast.to_players", true)) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p != player && p.hasPermission("gcc.broadcast.receive")) {
                    p.sendMessage(broadcast);
                }
            }
        }

        // 广播给控制台
        if (configManager.getConfig().getBoolean("chat.broadcast.to_console", true)) {
            plugin.getLogger().info(ChatColor.stripColor(broadcast));
        }
    }

    private void broadcastAnswer(Player player, String response) {
        String format = configManager.getConfig().getString("chat.broadcast.format.answer", 
            "§8[AI] §7回答 §f{player}: §7{message}");
        String broadcast = format
            .replace("{player}", player.getName())
            .replace("{message}", response);

        // 广播给其他玩家
        if (configManager.getConfig().getBoolean("chat.broadcast.to_players", true)) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p != player && p.hasPermission("gcc.broadcast.receive")) {
                    p.sendMessage(broadcast);
                }
            }
        }

        // 广播给控制台
        if (configManager.getConfig().getBoolean("chat.broadcast.to_console", true)) {
            plugin.getLogger().info(ChatColor.stripColor(broadcast));
        }
    }

    private boolean containsCommandSyntax(String text) {
        if (!configManager.getConfig().getBoolean("security.command_check.enabled", true)) {
            return false;
        }

        String lowerText = text.toLowerCase();
        
        // 检查命令
        List<String> blockedCommands = configManager.getConfig().getStringList("security.command_check.blocked_commands");
        for (String command : blockedCommands) {
            if (lowerText.contains(command.toLowerCase())) {
                return true;
            }
        }
        
        // 检查关键词
        List<String> blockedKeywords = configManager.getConfig().getStringList("security.command_check.blocked_keywords");
        for (String keyword : blockedKeywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }

    private GeminiService selectApiService() {
        String mode = plugin.getConfig().getString("api.mode", "single");
        List<GeminiService> availableServices = getAvailableServices();
        
        if (availableServices.isEmpty()) {
            plugin.debug("没有可用的API节点，尝试重置所有节点状态");
            resetAllServices();
            availableServices = new ArrayList<>(apiServices);
        }
        
        if (availableServices.isEmpty()) {
            plugin.getLogger().severe("所有API节点都不可用！");
            return null;
        }
        
        switch (mode.toLowerCase()) {
            case "random":
                return selectRandomService(availableServices);
            case "failover":
                return selectFailoverService(availableServices);
            case "single":
            default:
                return availableServices.get(0);
        }
    }

    private List<GeminiService> getAvailableServices() {
        return apiServices.stream()
            .filter(service -> {
                ServiceStatus status = serviceStatuses.get(service);
                return status != null && status.isAvailable();
            })
            .collect(Collectors.toList());
    }

    private void resetAllServices() {
        serviceStatuses.values().forEach(ServiceStatus::reset);
        plugin.debug("已重置所有API服务状态");
    }

    private GeminiService selectRandomService(List<GeminiService> availableServices) {
        int totalWeight = availableServices.stream()
            .mapToInt(GeminiService::getWeight)
            .sum();
            
        if (totalWeight == 0) return availableServices.get(0);
        
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (GeminiService service : availableServices) {
            currentWeight += service.getWeight();
            if (randomWeight < currentWeight) {
                return service;
            }
        }
        
        return availableServices.get(0);
    }

    private GeminiService selectFailoverService(List<GeminiService> availableServices) {
        if (availableServices.isEmpty()) return apiServices.get(0);
        currentApiIndex = (currentApiIndex + 1) % availableServices.size();
        return availableServices.get(currentApiIndex);
    }

    private void handleServiceError(GeminiService service) {
        ServiceStatus status = serviceStatuses.get(service);
        if (status != null) {
            status.recordError();
            
            int errorThreshold = plugin.getConfig().getInt("api.failover.error_threshold", 5);
            long recoveryTime = plugin.getConfig().getLong("api.failover.recovery_time", 300000);
            
            if (status.errorCount >= errorThreshold) {
                status.disabledUntil = System.currentTimeMillis() + recoveryTime;
                plugin.debug("API节点暂时禁用: " + service.getEndpointInfo() + 
                    ", 将在 " + (recoveryTime / 1000) + " 秒后重试");
            }
        }
    }

    private void sendMessageWithRetry(Player player, String message, Optional<Persona> persona, 
                                    GeminiService service, int retryCount) {
        if (service == null) {
            String error = String.format(configManager.getErrorFormat(), "没有可用的API节点");
            player.sendMessage(error);
            return;
        }

        int maxRetries = plugin.getConfig().getInt("api.failover.max_retries", 3);
        long retryDelay = plugin.getConfig().getLong("api.failover.retry_delay", 1000);

        service.sendMessage(player.getName(), message, persona)
            .thenAccept(response -> {
                if (containsCommandSyntax(response)) {
                    player.sendMessage(ChatColor.RED + "AI 回复包含不安全内容，已被过滤");
                    return;
                }
                
                // 成功响应，重置服务错误计数
                ServiceStatus status = serviceStatuses.get(service);
                if (status != null) {
                    status.reset();
                }
                
                String formattedResponse = String.format(configManager.getResponseFormat(), response);
                player.sendMessage(formattedResponse);
                
                if (shouldBroadcast(player, persona)) {
                    broadcastAnswer(player, response);
                }
            })
            .exceptionally(throwable -> {
                plugin.debug("API调用失败 [" + service.getEndpointInfo() + "]: " + throwable.getMessage());
                handleServiceError(service);
                
                if (retryCount < maxRetries) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        GeminiService nextService = selectApiService();
                        if (nextService != null) {
                            sendMessageWithRetry(player, message, persona, nextService, retryCount + 1);
                        } else {
                            String error = String.format(configManager.getErrorFormat(), "所有API节点不可用");
                            player.sendMessage(error);
                        }
                    }, retryDelay / 50);
                } else {
                    String error = String.format(configManager.getErrorFormat(), "所有重试均失败");
                    player.sendMessage(error);
                }
                return null;
            });
    }

    private boolean checkRateLimit(String playerId) {
        int rateLimit = configManager.getConfig().getInt("security.limits.rate_limit", 5);
        long oneMinute = 60000L;
        
        List<Long> times = requestTimes.computeIfAbsent(playerId, k -> new ArrayList<>());
        long now = System.currentTimeMillis();
        
        // 清理旧的记录
        times.removeIf(time -> now - time > oneMinute);
        
        // 检查是否超过限制
        if (times.size() >= rateLimit) {
            return false;
        }
        
        // 添加新的请求时间
        times.add(now);
        return true;
    }

    public void saveAllHistory() {
        // 如果需要持久化历史记录，可以在这里实现
        plugin.debug("保存所有对话历史");
    }

    public void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
            plugin.debug("清理任务已停止");
        }
    }

    public void startCleanupTask() {
        if (cleanupTask != null) {
            stopCleanupTask();
        }
        
        cleanupTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            
            try {
                // 清理请求时间记录
                requestTimes.entrySet().removeIf(entry -> {
                    entry.getValue().removeIf(time -> now - time > 60000L);
                    return entry.getValue().isEmpty();
                });
                
                // 清理冷却时间记录
                cooldowns.entrySet().removeIf(entry -> 
                    now - entry.getValue() > configManager.getCooldown() * 2);
                
                // 清理玩家人设记录
                for (String playerId : new ArrayList<>(playerPersonas.keySet())) {
                    if (plugin.getServer().getPlayer(playerId) == null) {
                        playerPersonas.remove(playerId);
                    }
                }
                
                // 清理服务状态
                for (ServiceStatus status : serviceStatuses.values()) {
                    if (status.disabledUntil < now) {
                        status.reset();
                    }
                }
                
                plugin.debug("执行定期清理任务");
            } catch (Exception e) {
                plugin.getLogger().warning("清理任务执行失败: " + e.getMessage());
            }
        }, 1200L, 1200L); // 每分钟执行一次
        
        plugin.debug("清理任务已启动");
    }
} 