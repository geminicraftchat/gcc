package cn.ningmo.geminicraftchat.commands;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.chat.ChatManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MainCommand implements CommandExecutor {
    private final GeminiCraftChat plugin;
    private final ChatManager chatManager;

    public MainCommand(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getChatManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!player.hasPermission("gcc.admin")) {
                    player.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                    return true;
                }
                plugin.getConfigManager().loadConfig();
                player.sendMessage(ChatColor.GREEN + "配置已重新加载！");
                break;

            case "clear":
                if (args.length == 2 && args[1].equalsIgnoreCase("all")) {
                    if (!player.hasPermission("gcc.admin")) {
                        player.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                        return true;
                    }
                    chatManager.clearAllHistory();
                    player.sendMessage(ChatColor.GREEN + "已清除所有玩家的对话历史！");
                } else {
                    chatManager.clearHistory(player.getName());
                    player.sendMessage(ChatColor.GREEN + "已清除你的对话历史！");
                }
                break;

            case "persona":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "用法: /gcc persona <list|switch> [人设名称]");
                    return true;
                }
                handlePersonaCommand(player, args);
                break;

            case "debug":
                if (!player.hasPermission("gcc.admin")) {
                    player.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                    return true;
                }
                boolean debugEnabled = plugin.getConfigManager().getConfig().getBoolean("debug", false);
                plugin.getConfigManager().getConfig().set("debug", !debugEnabled);
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + "调试模式已" + (!debugEnabled ? "启用" : "禁用"));
                break;

            default:
                sendHelpMessage(player);
                break;
        }

        return true;
    }

    private void handlePersonaCommand(Player player, String[] args) {
        switch (args[1].toLowerCase()) {
            case "list":
                List<String> personas = chatManager.getAvailablePersonas();
                player.sendMessage(ChatColor.GREEN + "可用的人设列表:");
                for (String persona : personas) {
                    String name = plugin.getConfigManager().getConfig().getString("personas." + persona + ".name", persona);
                    String desc = plugin.getConfigManager().getConfig().getString("personas." + persona + ".description", "");
                    player.sendMessage(ChatColor.YELLOW + "- " + name + ChatColor.GRAY + ": " + desc);
                }
                break;

            case "switch":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "请指定要切换的人设名称！");
                    return;
                }
                String personaName = args[2].toLowerCase();
                if (chatManager.switchPersona(player, personaName)) {
                    player.sendMessage(ChatColor.GREEN + "已切换到人设: " + personaName);
                } else {
                    player.sendMessage(ChatColor.RED + "找不到指定的人设: " + personaName);
                }
                break;

            default:
                player.sendMessage(ChatColor.RED + "未知的人设命令！用法: /gcc persona <list|switch> [人设名称]");
                break;
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== GeminiCraftChat 命令帮助 ===");
        player.sendMessage(ChatColor.YELLOW + "/gcc reload " + ChatColor.GRAY + "- 重新加载配置文件");
        player.sendMessage(ChatColor.YELLOW + "/gcc clear " + ChatColor.GRAY + "- 清除你的对话历史");
        player.sendMessage(ChatColor.YELLOW + "/gcc clear all " + ChatColor.GRAY + "- 清除所有玩家的对话历史");
        player.sendMessage(ChatColor.YELLOW + "/gcc persona list " + ChatColor.GRAY + "- 查看可用的人设列表");
        player.sendMessage(ChatColor.YELLOW + "/gcc persona switch <名称> " + ChatColor.GRAY + "- 切换到指定人设");
        if (player.hasPermission("gcc.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/gcc debug " + ChatColor.GRAY + "- 切换调试模式");
        }
    }
} 