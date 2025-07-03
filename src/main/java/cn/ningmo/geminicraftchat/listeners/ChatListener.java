package cn.ningmo.geminicraftchat.listeners;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.chat.ChatManager;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.ChatColor;

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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        // 检查是否是中文命令
        if (handleChineseCommand(player, message)) {
            event.setCancelled(true);
            return;
        }

        // 检查是否是AI触发消息
        if (isAITrigger(message)) {
            event.setCancelled(true);
            
            // 检查权限
            if (!player.hasPermission(configManager.getPermission("use_command"))) {
                player.sendMessage(configManager.getChatFormat("no_permission"));
                return;
            }

            // 提取问题内容
            int spaceIndex = message.indexOf(" ");
            if (spaceIndex <= 0) {
                player.sendMessage(configManager.getChatFormat("empty_question"));
                return;
            }

            String question = message.substring(spaceIndex + 1).trim();
            if (question.isEmpty()) {
                player.sendMessage(configManager.getChatFormat("empty_question"));
                return;
            }

            // 处理聊天
            chatManager.handleChat(player, question);
        }
    }

    private boolean handleChineseCommand(Player player, String message) {
        List<String> clearMemoryCommands = configManager.getConfig().getStringList("commands.chinese.clear_memory");
        List<String> switchPersonaCommands = configManager.getConfig().getStringList("commands.chinese.switch_persona");
        List<String> listPersonasCommands = configManager.getConfig().getStringList("commands.chinese.list_personas");
        List<String> helpCommands = configManager.getConfig().getStringList("commands.chinese.help");

        String command = message.split(" ")[0];

        if (clearMemoryCommands.contains(command)) {
            chatManager.clearHistory(player.getName());
            player.sendMessage(ChatColor.GREEN + "已清除你的对话记忆");
            return true;
        }

        if (switchPersonaCommands.contains(command)) {
            String personaName = message.substring(command.length()).trim();
            if (personaName.isEmpty()) {
                player.sendMessage(ChatColor.RED + "请指定要切换的人设名称");
                return true;
            }
            if (chatManager.switchPersona(player, personaName)) {
                player.sendMessage(ChatColor.GREEN + "已切换到人设: " + personaName);
            } else {
                player.sendMessage(ChatColor.RED + "找不到指定的人设: " + personaName);
            }
            return true;
        }

        if (listPersonasCommands.contains(command)) {
            List<String> personas = chatManager.getAvailablePersonas();
            player.sendMessage(ChatColor.GREEN + "可用的人设列表:");
            for (String persona : personas) {
                player.sendMessage(ChatColor.GRAY + "- " + persona);
            }
            return true;
        }

        if (helpCommands.contains(command)) {
            sendHelpMessage(player);
            return true;
        }

        return false;
    }

    /**
     * 检查消息是否是AI触发词
     * @param message 消息内容
     * @return 是否是AI触发消息
     */
    private boolean isAITrigger(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        String defaultTrigger = configManager.getDefaultTrigger();
        List<String> triggerWords = configManager.getTriggerWords();

        // 检查默认触发词
        if (message.toLowerCase().startsWith(defaultTrigger.toLowerCase())) {
            return true;
        }

        // 检查其他触发词
        for (String trigger : triggerWords) {
            if (message.toLowerCase().startsWith(trigger.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== GeminiCraftChat 帮助 ===");
        player.sendMessage(ChatColor.GRAY + "基本命令:");
        player.sendMessage(ChatColor.YELLOW + "ai <消息> " + ChatColor.WHITE + "- 与AI对话");
        player.sendMessage(ChatColor.YELLOW + "清除记忆 " + ChatColor.WHITE + "- 清除对话历史");
        player.sendMessage(ChatColor.YELLOW + "切换人设 <名称> " + ChatColor.WHITE + "- 切换AI人设");
        player.sendMessage(ChatColor.YELLOW + "查看人设 " + ChatColor.WHITE + "- 显示所有可用人设");
        player.sendMessage(ChatColor.YELLOW + "帮助 " + ChatColor.WHITE + "- 显示此帮助信息");
    }
} 