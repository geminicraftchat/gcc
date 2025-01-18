package cn.ningmo.geminicraftchat.chat;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.api.GeminiService;
import cn.ningmo.geminicraftchat.persona.PersonaManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {
    private final GeminiCraftChat plugin;
    private final GeminiService geminiService;
    private final PersonaManager personaManager;
    private final Map<String, Long> cooldowns;

    public ChatManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.geminiService = new GeminiService(plugin);
        this.personaManager = new PersonaManager(plugin);
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public void handleChat(Player player, String message) {
        if (!checkCooldown(player)) {
            player.sendMessage("§c请稍等片刻再发送下一条消息！");
            return;
        }

        // 移除触发词
        String cleanMessage = removePrefix(message);
        
        // 发送思考中消息
        player.sendMessage(plugin.getConfigManager().getConfig()
            .getString("chat.format.thinking", "§7[AI] §f正在思考中..."));

        // 异步处理AI响应
        geminiService.sendMessage(
            player.getUniqueId().toString(), 
            cleanMessage,
            personaManager.getPlayerPersona(player)
        ).thenAccept(response -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                String format = plugin.getConfigManager().getConfig()
                    .getString("chat.format.response", "§7[AI] §f%s");
                player.sendMessage(String.format(format, response));
            });
        }).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                String format = plugin.getConfigManager().getConfig()
                    .getString("chat.format.error", "§c[AI] 发生错误：%s");
                player.sendMessage(String.format(format, throwable.getMessage()));
            });
            return null;
        });

        updateCooldown(player);
    }

    private boolean checkCooldown(Player player) {
        if (player.hasPermission("gcc.bypass_cooldown")) {
            return true;
        }

        long cooldownTime = plugin.getConfigManager().getConfig()
            .getLong("chat.cooldown", 10000);
        long lastUse = cooldowns.getOrDefault(player.getUniqueId().toString(), 0L);
        return System.currentTimeMillis() - lastUse >= cooldownTime;
    }

    private void updateCooldown(Player player) {
        cooldowns.put(player.getUniqueId().toString(), System.currentTimeMillis());
    }

    private String removePrefix(String message) {
        String trigger = plugin.getConfigManager().getDefaultTrigger();
        if (message.startsWith(trigger)) {
            return message.substring(trigger.length()).trim();
        }
        
        for (String word : plugin.getConfigManager().getTriggerWords()) {
            if (message.startsWith(word)) {
                return message.substring(word.length()).trim();
            }
        }
        
        return message;
    }

    public void clearHistory(Player player) {
        geminiService.clearHistory(player.getUniqueId().toString());
    }

    public void clearAllHistory() {
        geminiService.clearAllHistory();
    }

    public PersonaManager getPersonaManager() {
        return personaManager;
    }
} 