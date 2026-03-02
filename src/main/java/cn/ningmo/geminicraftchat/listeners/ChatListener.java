package cn.ningmo.geminicraftchat.listeners;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.chat.ChatManager;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class ChatListener implements Listener {
    private final GeminiCraftChat plugin;
    private final ChatManager chatManager;
    private final ConfigManager configManager;

    public ChatListener(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getChatManager();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).trim();

        // 检查是否是中文命令
        if (handleChineseCommand(player, message)) {
            event.setCancelled(true);
            return;
        }

        // 检查是否是AI触发消息
        String question = extractQuestion(message);
        if (!question.isEmpty()) { // If question is not empty, it means it was an AI trigger
            event.setCancelled(true);
            
            // 检查权限
            if (!player.hasPermission(configManager.getPermission("use_command"))) {
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(configManager.getChatFormat("no_permission")));
                return;
            }

            // 提取问题内容
            // question is already extracted by extractQuestion method
            // if (question.isEmpty()) is checked above
            if (question.isEmpty()) { // This check is redundant if extractQuestion is correctly implemented, but kept for safety
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(configManager.getChatFormat("empty_question")));
                return;
            }

            // 处理聊天
            chatManager.handleChat(player, question);
        }
    }

    private String extractQuestion(String message) {
        String defaultTrigger = configManager.getDefaultTrigger();
        List<String> triggerWords = configManager.getTriggerWords();
        
        String lowerMessage = message.toLowerCase();
        String matchedTrigger = null;
        
        if (lowerMessage.startsWith(defaultTrigger.toLowerCase())) {
            matchedTrigger = defaultTrigger;
        } else {
            for (String trigger : triggerWords) {
                if (lowerMessage.startsWith(trigger.toLowerCase())) {
                    matchedTrigger = trigger;
                    break;
                }
            }
        }
        
        if (matchedTrigger != null) {
            String question = message.substring(matchedTrigger.length()).trim();
            return question;
        }
        
        return "";
    }

    private boolean handleChineseCommand(Player player, String message) {
        List<String> clearMemoryCommands = configManager.getConfig().getStringList("commands.chinese.clear_memory");
        List<String> switchPersonaCommands = configManager.getConfig().getStringList("commands.chinese.switch_persona");
        List<String> listPersonasCommands = configManager.getConfig().getStringList("commands.chinese.list_personas");
        List<String> helpCommands = configManager.getConfig().getStringList("commands.chinese.help");

        String command = message.split(" ")[0];

        if (clearMemoryCommands.contains(command)) {
            chatManager.clearHistory(player.getName());
            player.sendMessage(Component.text("已清除你的对话记忆").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
            return true;
        }

        if (switchPersonaCommands.contains(command)) {
            String personaName = message.substring(command.length()).trim();
            if (personaName.isEmpty()) {
                player.sendMessage(Component.text("请指定要切换的人设名称").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                return true;
            }
            if (chatManager.switchPersona(player, personaName)) {
                player.sendMessage(Component.text("已切换到人设: " + personaName).color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("找不到指定的人设: " + personaName).color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
            return true;
        }

        if (listPersonasCommands.contains(command)) {
            List<String> personas = chatManager.getAvailablePersonas();
            player.sendMessage(Component.text("可用的人设列表:").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
            for (String persona : personas) {
                player.sendMessage(Component.text("- " + persona).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
            }
            return true;
        }

        if (helpCommands.contains(command)) {
            sendHelpMessage(player);
            return true;
        }

        return false;
    }

    // The isAITrigger method is removed as its logic is now part of extractQuestion
    // private boolean isAITrigger(String message) {
    //     if (message == null || message.isEmpty()) {
    //         return false;
    //     }

    //     String defaultTrigger = configManager.getDefaultTrigger();
    //     List<String> triggerWords = configManager.getTriggerWords();

    //     // 检查默认触发词
    //     if (message.toLowerCase().startsWith(defaultTrigger.toLowerCase())) {
    //         return true;
    //     }

    //     // 检查其他触发词
    //     for (String trigger : triggerWords) {
    //         if (message.toLowerCase().startsWith(trigger.toLowerCase())) {
    //             return true;
    //         }
    //     }

    //     return false;
    // }

    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("=== GeminiCraftChat 帮助 ===").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
        player.sendMessage(Component.text("基本命令:").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        player.sendMessage(Component.text("ai <消息> ").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(Component.text("- 与AI对话").color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
        player.sendMessage(Component.text("清除记忆 ").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(Component.text("- 清除对话历史").color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
        player.sendMessage(Component.text("切换人设 <名称> ").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(Component.text("- 切换AI人设").color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
        player.sendMessage(Component.text("查看人设 ").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(Component.text("- 显示所有可用人设").color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
        player.sendMessage(Component.text("帮助 ").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(Component.text("- 显示此帮助信息").color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
    }
}