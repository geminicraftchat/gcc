package cn.ningmo.geminicraftchat.chat;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.api.GeminiService;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.persona.Persona;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {
    private final GeminiCraftChat plugin;
    private final GeminiService geminiService;
    private final ConfigManager configManager;
    private final Map<String, String> playerPersonas;
    private final Map<String, Long> cooldowns;

    public ChatManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.geminiService = new GeminiService(plugin);
        this.configManager = plugin.getConfigManager();
        this.playerPersonas = new ConcurrentHashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public void handleChat(Player player, String message) {
        String playerId = player.getName();
        
        // 检查冷却时间
        if (isOnCooldown(playerId)) {
            long remainingTime = getRemainingCooldown(playerId);
            player.sendMessage(ChatColor.RED + "请等待 " + (remainingTime / 1000) + " 秒后再次发送消息");
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

        // 发送请求
        geminiService.sendMessage(playerId, message, persona)
            .thenAccept(response -> {
                String formattedResponse = String.format(configManager.getResponseFormat(), response);
                player.sendMessage(formattedResponse);
                
                // 广播回答
                if (shouldBroadcast(player, persona)) {
                    broadcastAnswer(player, response);
                }
            })
            .exceptionally(throwable -> {
                String error = String.format(configManager.getErrorFormat(), throwable.getMessage());
                player.sendMessage(error);
                return null;
            });

        // 设置冷却时间
        setCooldown(playerId);
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
        geminiService.clearHistory(playerId);
    }

    public void clearAllHistory() {
        geminiService.clearAllHistory();
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
} 