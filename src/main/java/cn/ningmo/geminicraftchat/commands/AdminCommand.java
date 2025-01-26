package cn.ningmo.geminicraftchat.commands;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class AdminCommand implements CommandExecutor {
    private final GeminiCraftChat plugin;

    public AdminCommand(GeminiCraftChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("gcc.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "debug":
                handleDebug(sender);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        try {
            sender.sendMessage(ChatColor.YELLOW + "正在重载插件...");
            
            // 执行重载
            plugin.reloadPlugin();
            
            sender.sendMessage(ChatColor.GREEN + "插件重载成功！");
            plugin.getLogger().info("插件已通过后台命令重新加载");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "插件重载失败: " + e.getMessage());
            plugin.getLogger().severe("插件重载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDebug(CommandSender sender) {
        boolean newState = !plugin.isDebugEnabled();
        plugin.getConfig().set("debug", newState);
        plugin.saveConfig();
        
        String status = newState ? "启用" : "禁用";
        sender.sendMessage(ChatColor.GREEN + "调试模式已" + status);
        plugin.getLogger().info("调试模式已" + status);
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== GeminiCraftChat 后台命令 ===");
        sender.sendMessage(ChatColor.YELLOW + "/gccadmin reload " + ChatColor.GRAY + "- 重新加载配置文件");
        sender.sendMessage(ChatColor.YELLOW + "/gccadmin debug " + ChatColor.GRAY + "- 切换调试模式");
    }
} 