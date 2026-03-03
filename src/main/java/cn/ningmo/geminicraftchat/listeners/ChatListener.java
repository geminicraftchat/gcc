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

    public ChatListener(GeminiCraftChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).trim();

        boolean chineseCommand = isChineseCommand(message);
        boolean aiTrigger = isAITrigger(message);
        if (!chineseCommand && !aiTrigger) {
            return;
        }

        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> processChatMessage(player, message, aiTrigger));
    }

    private String extractQuestion(String message) {
        String matchedTrigger = findMatchedTrigger(message);
        if (matchedTrigger == null) {
            return "";
        }
        return message.substring(matchedTrigger.length()).trim();
    }

    private boolean isAITrigger(String message) {
        return findMatchedTrigger(message) != null;
    }

    private String findMatchedTrigger(String message) {
        ConfigManager configManager = getConfigManager();
        String lowerMessage = message.toLowerCase();
        String defaultTrigger = configManager.getDefaultTrigger();
        if (lowerMessage.startsWith(defaultTrigger.toLowerCase())) {
            return defaultTrigger;
        }

        List<String> triggerWords = configManager.getTriggerWords();
        for (String trigger : triggerWords) {
            if (lowerMessage.startsWith(trigger.toLowerCase())) {
                return trigger;
            }
        }
        return null;
    }

    private void processChatMessage(Player player, String message, boolean aiTrigger) {
        if (!player.isOnline()) {
            return;
        }

        if (handleChineseCommand(player, message)) {
            return;
        }

        if (!aiTrigger) {
            return;
        }

        ConfigManager configManager = getConfigManager();
        if (!player.hasPermission(configManager.getPermission("use_command"))) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(configManager.getChatFormat("no_permission")));
            return;
        }

        String question = extractQuestion(message);
        if (question.isEmpty()) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(configManager.getChatFormat("empty_question")));
            return;
        }

        getChatManager().handleChat(player, question);
    }

    private boolean isChineseCommand(String message) {
        if (message.isEmpty()) {
            return false;
        }

        String command = message.split(" ")[0];
        ConfigManager configManager = getConfigManager();
        return configManager.getConfig().getStringList("commands.chinese.clear_memory").contains(command)
            || configManager.getConfig().getStringList("commands.chinese.switch_persona").contains(command)
            || configManager.getConfig().getStringList("commands.chinese.list_personas").contains(command)
            || configManager.getConfig().getStringList("commands.chinese.help").contains(command);
    }

    private boolean handleChineseCommand(Player player, String message) {
        ConfigManager configManager = getConfigManager();
        List<String> clearMemoryCommands = configManager.getConfig().getStringList("commands.chinese.clear_memory");
        List<String> switchPersonaCommands = configManager.getConfig().getStringList("commands.chinese.switch_persona");
        List<String> listPersonasCommands = configManager.getConfig().getStringList("commands.chinese.list_personas");
        List<String> helpCommands = configManager.getConfig().getStringList("commands.chinese.help");

        String command = message.split(" ")[0];

        if (clearMemoryCommands.contains(command)) {
            getChatManager().clearHistory(player.getName());
            player.sendMessage(Component.text("已清除你的对话记忆").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
            return true;
        }

        if (switchPersonaCommands.contains(command)) {
            String personaName = message.substring(command.length()).trim();
            if (personaName.isEmpty()) {
                player.sendMessage(Component.text("请指定要切换的人设名称").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                return true;
            }
            if (getChatManager().switchPersona(player, personaName)) {
                player.sendMessage(Component.text("已切换到人设: " + personaName).color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("找不到指定的人设: " + personaName).color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
            return true;
        }

        if (listPersonasCommands.contains(command)) {
            List<String> personas = getChatManager().getAvailablePersonas();
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

    private ConfigManager getConfigManager() {
        return plugin.getConfigManager();
    }

    private ChatManager getChatManager() {
        return plugin.getChatManager();
    }
}
