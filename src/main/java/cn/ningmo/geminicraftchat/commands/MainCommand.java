package cn.ningmo.geminicraftchat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.persona.PersonaManager;
import org.bukkit.entity.Player;

public class MainCommand implements CommandExecutor {
    private final GeminiCraftChat plugin;

    public MainCommand(GeminiCraftChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("gcc.use")) {
            sender.sendMessage("§c你没有权限使用此命令！");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        // 处理子命令
        switch (args[0].toLowerCase()) {
            case "reload":
                if (sender.hasPermission("gcc.admin")) {
                    plugin.getConfigManager().loadConfig();
                    sender.sendMessage("§a配置已重新加载！");
                } else {
                    sender.sendMessage("§c你没有权限执行此命令！");
                }
                break;
            case "persona":
                handlePersonaCommand(sender, args);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6=== GeminiCraftChat 帮助 ===");
        sender.sendMessage("§f/gcc reload §7- 重新加载配置");
        sender.sendMessage("§f/gcc persona §7- 人设命令");
        // 添加更多帮助信息
    }

    private void handlePersonaCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家使用！");
            return;
        }
        
        Player player = (Player) sender;
        if (args.length < 2) {
            sendPersonaHelpMessage(player);
            return;
        }

        PersonaManager personaManager = plugin.getChatManager().getPersonaManager();
        
        switch (args[1].toLowerCase()) {
            case "list":
                sendPersonaList(player, personaManager);
                break;
            case "set":
                if (args.length < 3) {
                    player.sendMessage("§c用法: /gcc persona set <人设名称>");
                    return;
                }
                setPlayerPersona(player, args[2], personaManager);
                break;
            case "clear":
                personaManager.clearPlayerPersona(player);
                player.sendMessage("§a已清除当前人设！");
                break;
            default:
                sendPersonaHelpMessage(player);
                break;
        }
    }

    private void sendPersonaList(Player player, PersonaManager personaManager) {
        player.sendMessage("§6=== 可用人设列表 ===");
        personaManager.getAllPersonas().forEach((key, persona) -> {
            player.sendMessage(String.format("§f%s §7- §f%s", key, persona.getDescription()));
        });
    }

    private void setPlayerPersona(Player player, String personaKey, PersonaManager personaManager) {
        if (personaManager.getPersona(personaKey).isPresent()) {
            personaManager.setPlayerPersona(player, personaKey);
            player.sendMessage("§a已设置人设为: " + personaKey);
        } else {
            player.sendMessage("§c找不到该人设！");
        }
    }

    private void sendPersonaHelpMessage(Player player) {
        player.sendMessage("§6=== 人设命令帮助 ===");
        player.sendMessage("§f/gcc persona list §7- 查看可用人设列表");
        player.sendMessage("§f/gcc persona set <名称> §7- 设置当前人设");
        player.sendMessage("§f/gcc persona clear §7- 清除当前人设");
    }
} 