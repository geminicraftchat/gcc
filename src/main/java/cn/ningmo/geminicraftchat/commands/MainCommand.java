package cn.ningmo.geminicraftchat.commands;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.chat.ChatManager;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.logging.LogManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MainCommand implements CommandExecutor {
    private final GeminiCraftChat plugin;
    private final ChatManager chatManager;
    private final ConfigManager configManager;
    private final LogManager logManager;

    public MainCommand(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getChatManager();
        this.configManager = plugin.getConfigManager();
        this.logManager = plugin.getLogManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        // 检查基本使用权限
        if (!player.hasPermission(configManager.getPermission("use_command"))) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(player);
                break;

            case "clear":
                handleClear(player, args);
                break;

            case "debug":
                handleDebug(player);
                break;

            case "model":
                handleModelSwitch(player, args);
                break;

            case "temp":
                handleTemperature(player, args);
                break;

            case "persona":
                handlePersona(player, args);
                break;

            default:
                sendHelpMessage(player);
                break;
        }

        return true;
    }

    private void handleReload(Player player) {
        if (!player.hasPermission(configManager.getPermission("admin"))) {
            player.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return;
        }
        configManager.loadConfig();
        player.sendMessage(ChatColor.GREEN + "配置已重新加载！");
        logManager.logCommand(player.getName(), "reload");
    }

    private void handleClear(Player player, String[] args) {
        if (args.length == 2 && args[1].equalsIgnoreCase("all")) {
            if (!player.hasPermission(configManager.getPermission("admin"))) {
                player.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                return;
            }
            chatManager.clearAllHistory();
            player.sendMessage(ChatColor.GREEN + "已清除所有玩家的对话历史！");
            logManager.logCommand(player.getName(), "clear all");
        } else {
            chatManager.clearHistory(player.getName());
            player.sendMessage(ChatColor.GREEN + "已清除你的对话历史！");
            logManager.logCommand(player.getName(), "clear");
        }
    }

    private void handleDebug(Player player) {
        if (!player.hasPermission(configManager.getPermission("admin"))) {
            player.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return;
        }
        boolean debugEnabled = configManager.getConfig().getBoolean("debug", false);
        configManager.getConfig().set("debug", !debugEnabled);
        plugin.saveConfig();
        player.sendMessage(ChatColor.GREEN + "调试模式已" + (!debugEnabled ? "启用" : "禁用"));
        logManager.logCommand(player.getName(), "debug " + (!debugEnabled ? "enable" : "disable"));
    }

    private void handleModelSwitch(Player player, String[] args) {
        if (!player.hasPermission(configManager.getPermission("model_switch"))) {
            player.sendMessage(ChatColor.RED + "你没有权限切换模型！");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "请指定要切换的模型名称！");
            player.sendMessage(ChatColor.GRAY + "可用模型：" + String.join(", ", configManager.getAvailableModels()));
            return;
        }

        String modelName = args[1].toLowerCase();
        if (configManager.switchModel(modelName)) {
            String modelDisplayName = configManager.getModelDisplayName(modelName);
            player.sendMessage(ChatColor.GREEN + "已切换到模型: " + modelDisplayName);
            logManager.logModelChange(player.getName(), modelName);
        } else {
            player.sendMessage(ChatColor.RED + "找不到指定的模型: " + modelName);
        }
    }

    private void handleTemperature(Player player, String[] args) {
        if (!player.hasPermission(configManager.getPermission("temperature_adjust"))) {
            player.sendMessage(ChatColor.RED + "你没有权限调整温度！");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "请指定要设置的温度值！(0.0-1.0)");
            return;
        }

        try {
            double temp = Double.parseDouble(args[1]);
            if (temp < 0.0 || temp > 1.0) {
                player.sendMessage(ChatColor.RED + "温度值必须在0.0到1.0之间！");
                return;
            }

            configManager.setTemperature(temp);
            player.sendMessage(ChatColor.GREEN + "已将温度设置为: " + temp);
            logManager.logTemperatureChange(player.getName(), temp);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的温度值！请输入0.0到1.0之间的数字。");
        }
    }

    private void handlePersona(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /gcc persona <list|switch> [人设名称]");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list":
                List<String> personas = chatManager.getAvailablePersonas();
                player.sendMessage(ChatColor.GREEN + "可用的人设列表:");
                for (String persona : personas) {
                    String name = configManager.getConfig().getString("personas." + persona + ".name", persona);
                    String desc = configManager.getConfig().getString("personas." + persona + ".description", "");
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
                    logManager.logCommand(player.getName(), "persona switch " + personaName);
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
        player.sendMessage(ChatColor.YELLOW + "/gcc model <名称> " + ChatColor.GRAY + "- 切换到指定模型");
        player.sendMessage(ChatColor.YELLOW + "/gcc temp <数值> " + ChatColor.GRAY + "- 调整模型温度(0.0-1.0)");
        player.sendMessage(ChatColor.YELLOW + "/gcc persona list " + ChatColor.GRAY + "- 查看可用的人设列表");
        player.sendMessage(ChatColor.YELLOW + "/gcc persona switch <名称> " + ChatColor.GRAY + "- 切换到指定人设");
        if (player.hasPermission(configManager.getPermission("admin"))) {
            player.sendMessage(ChatColor.YELLOW + "/gcc debug " + ChatColor.GRAY + "- 切换调试模式");
        }
    }
} 