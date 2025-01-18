package cn.ningmo.geminicraftchat.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.chat.ChatManager;

public class ChatListener implements Listener {
    private final GeminiCraftChat plugin;
    private final ChatManager chatManager;

    public ChatListener(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.chatManager = new ChatManager(plugin);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        String trigger = plugin.getConfigManager().getDefaultTrigger();
        
        // 检查是否触发AI对话
        if (message.startsWith(trigger) || plugin.getConfigManager().getTriggerWords().stream()
                .anyMatch(word -> message.startsWith(word))) {
            event.setCancelled(true);
            chatManager.handleChat(event.getPlayer(), message);
        }
    }
} 