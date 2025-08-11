package cn.ningmo.geminicraftchat.commands;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.chat.ChatManager;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.logging.LogManager;
import cn.ningmo.geminicraftchat.npc.NPCManager;
import cn.ningmo.geminicraftchat.npc.NpcService;
import cn.ningmo.geminicraftchat.npc.AIControlledNPC;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class MainCommand implements CommandExecutor {
    private final GeminiCraftChat plugin;
    private final ChatManager chatManager;
    private final ConfigManager configManager;
    private final LogManager logManager;
    private final NpcService npcManager;

    public MainCommand(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getChatManager();
        this.configManager = plugin.getConfigManager();
        this.logManager = plugin.getLogManager();
        this.npcManager = plugin.getNpcService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission(configManager.getPermission("use_command"))) {
                sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                return true;
            }
        }
        // 控制台默认有所有权限

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;

            case "clear":
                handleClear(sender, args);
                break;

            case "debug":
                handleDebug(sender);
                break;

            case "model":
                handleModelSwitch(sender, args);
                break;

            case "temp":
                handleTemperature(sender, args);
                break;

            case "persona":
                handlePersona(sender, args);
                break;

            case "logs":
                handleLogs(sender, args);
                break;

            case "timeout":
                handleTimeout(sender, args);
                break;

            case "npc":
                handleNPC(sender, args);
                break;

            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission(configManager.getPermission("admin"))) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                return;
            }
        }

        configManager.loadConfig();
        sender.sendMessage(ChatColor.GREEN + "配置已重新加载！");

        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        logManager.logCommand(senderName, "reload");
    }

    private void handleClear(CommandSender sender, String[] args) {
        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";

        if (args.length == 2 && args[1].equalsIgnoreCase("all")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission(configManager.getPermission("admin"))) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                    return;
                }
            }
            chatManager.clearAllHistory();
            sender.sendMessage(ChatColor.GREEN + "已清除所有玩家的对话历史！");
            logManager.logCommand(senderName, "clear all");
        } else {
            if (sender instanceof Player) {
                chatManager.clearHistory(sender.getName());
                sender.sendMessage(ChatColor.GREEN + "已清除你的对话历史！");
                logManager.logCommand(senderName, "clear");
            } else {
                sender.sendMessage(ChatColor.RED + "控制台无法清除个人历史记录，请使用 'clear all' 清除所有历史记录！");
            }
        }
    }

    private void handleDebug(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission(configManager.getPermission("admin"))) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                return;
            }
        }

        boolean debugEnabled = configManager.getConfig().getBoolean("debug", false);
        configManager.getConfig().set("debug", !debugEnabled);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "调试模式已" + (!debugEnabled ? "启用" : "禁用"));

        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        logManager.logCommand(senderName, "debug " + (!debugEnabled ? "enable" : "disable"));
    }

    private void handleModelSwitch(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission(configManager.getPermission("model_switch"))) {
                sender.sendMessage(ChatColor.RED + "你没有权限切换模型！");
                return;
            }
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "请指定要切换的模型名称！");
            sender.sendMessage(ChatColor.GRAY + "可用模型：" + String.join(", ", configManager.getAvailableModels()));
            return;
        }

        String modelName = args[1].toLowerCase();
        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";

        if (configManager.switchModel(modelName)) {
            String modelDisplayName = configManager.getModelDisplayName(modelName);
            sender.sendMessage(ChatColor.GREEN + "已切换到模型: " + modelDisplayName);
            logManager.logModelChange(senderName, modelName);
        } else {
            sender.sendMessage(ChatColor.RED + "找不到指定的模型: " + modelName);
        }
    }

    private void handleTemperature(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission(configManager.getPermission("temperature_adjust"))) {
                sender.sendMessage(ChatColor.RED + "你没有权限调整温度！");
                return;
            }
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "请指定要设置的温度值！(0.0-1.0)");
            return;
        }

        try {
            double temp = Double.parseDouble(args[1]);
            if (temp < 0.0 || temp > 1.0) {
                sender.sendMessage(ChatColor.RED + "温度值必须在0.0到1.0之间！");
                return;
            }

            configManager.setTemperature(temp);
            sender.sendMessage(ChatColor.GREEN + "已将温度设置为: " + temp);

            String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
            logManager.logTemperatureChange(senderName, temp);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的温度值！请输入0.0到1.0之间的数字。");
        }
    }

    private void handlePersona(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /gcc persona <list|switch> [人设名称]");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list":
                List<String> personas = chatManager.getAvailablePersonas();
                sender.sendMessage(ChatColor.GREEN + "可用的人设列表:");
                for (String persona : personas) {
                    String name = configManager.getConfig().getString("personas." + persona + ".name", persona);
                    String desc = configManager.getConfig().getString("personas." + persona + ".description", "");
                    sender.sendMessage(ChatColor.YELLOW + "- " + name + ChatColor.GRAY + ": " + desc);
                }
                break;

            case "switch":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "控制台无法切换人设！此功能仅限玩家使用。");
                    return;
                }

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "请指定要切换的人设名称！");
                    return;
                }

                Player player = (Player) sender;
                String personaName = args[2].toLowerCase();
                String senderName = sender.getName();

                if (chatManager.switchPersona(player, personaName)) {
                    sender.sendMessage(ChatColor.GREEN + "已切换到人设: " + personaName);
                    logManager.logCommand(senderName, "persona switch " + personaName);
                } else {
                    sender.sendMessage(ChatColor.RED + "找不到指定的人设: " + personaName);
                }
                break;

            default:
                sender.sendMessage(ChatColor.RED + "未知的人设命令！用法: /gcc persona <list|switch> [人设名称]");
                break;
        }
    }

    private void handleLogs(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission(configManager.getPermission("admin"))) {
                sender.sendMessage(ChatColor.RED + "你没有权限查看日志统计！");
                return;
            }
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /gcc logs <stats|reset|export>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "stats":
                showLogStats(sender);
                break;

            case "reset":
                resetLogStats(sender);
                break;

            case "export":
                exportPlayerStats(sender);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "未知的日志命令！用法: /gcc logs <stats|reset|export>");
                break;
        }
    }

    private void showLogStats(CommandSender sender) {
        Map<String, Object> stats = logManager.getStats();

        sender.sendMessage(ChatColor.GREEN + "=== 日志统计信息 ===");
        sender.sendMessage(ChatColor.YELLOW + "总API调用次数: " + ChatColor.WHITE + stats.getOrDefault("totalApiCalls", 0));
        sender.sendMessage(ChatColor.YELLOW + "总错误次数: " + ChatColor.WHITE + stats.getOrDefault("totalErrors", 0));
        sender.sendMessage(ChatColor.YELLOW + "活跃玩家数: " + ChatColor.WHITE + stats.getOrDefault("activePlayerCount", 0));
        sender.sendMessage(ChatColor.YELLOW + "使用的模型数: " + ChatColor.WHITE + stats.getOrDefault("modelCount", 0));

        if (stats.containsKey("successRate")) {
            double successRate = (Double) stats.get("successRate");
            sender.sendMessage(ChatColor.YELLOW + "成功率: " + ChatColor.WHITE + String.format("%.2f%%", successRate));
        }

        if (stats.containsKey("avgResponseTime")) {
            long avgResponseTime = (Long) stats.get("avgResponseTime");
            sender.sendMessage(ChatColor.YELLOW + "平均响应时间: " + ChatColor.WHITE + avgResponseTime + "ms");
        }

        // 显示日志配置状态
        boolean loggingEnabled = configManager.getConfig().getBoolean("logging.enabled", true);
        boolean separateFiles = configManager.getConfig().getBoolean("logging.separate_files", true);
        int retentionDays = configManager.getConfig().getInt("logging.retention_days", 30);

        sender.sendMessage(ChatColor.GRAY + "--- 日志配置 ---");
        sender.sendMessage(ChatColor.YELLOW + "日志状态: " + (loggingEnabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "分离文件: " + (separateFiles ? ChatColor.GREEN + "是" : ChatColor.RED + "否"));
        sender.sendMessage(ChatColor.YELLOW + "保留天数: " + ChatColor.WHITE + retentionDays + " 天");

        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        logManager.logCommand(senderName, "logs stats");
    }

    private void resetLogStats(CommandSender sender) {
        logManager.resetStats();
        sender.sendMessage(ChatColor.GREEN + "日志统计数据已重置！");

        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        logManager.logCommand(senderName, "logs reset");
    }

    private void exportPlayerStats(CommandSender sender) {
        // 触发玩家统计导出到日志文件
        logManager.logPlayerStats();
        sender.sendMessage(ChatColor.GREEN + "玩家统计信息已导出到日志文件！");
        sender.sendMessage(ChatColor.GRAY + "请查看 plugins/GeminiCraftChat/logs/ 目录下的统计文件");

        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        logManager.logCommand(senderName, "logs export");
    }

    private void handleTimeout(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission(configManager.getPermission("admin"))) {
                sender.sendMessage(ChatColor.RED + "你没有权限管理超时设置！");
                return;
            }
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /gcc timeout <list|info|toggle> [模型名称]");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list":
                showTimeoutList(sender);
                break;

            case "info":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "请指定模型名称！用法: /gcc timeout info <模型名称>");
                    return;
                }
                showTimeoutInfo(sender, args[2]);
                break;

            case "toggle":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "请指定模型名称！用法: /gcc timeout toggle <模型名称>");
                    return;
                }
                toggleLongThinking(sender, args[2]);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "未知的超时命令！用法: /gcc timeout <list|info|toggle> [模型名称]");
                break;
        }
    }

    private void showTimeoutList(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== 模型超时设置列表 ===");

        List<String> models = configManager.getAvailableModels();
        for (String modelKey : models) {
            String modelName = configManager.getModelDisplayName(modelKey);

            // 获取超时配置
            int connectTimeout = configManager.getConfig().getInt("api.models." + modelKey + ".timeout.connect", 30);
            int readTimeout = configManager.getConfig().getInt("api.models." + modelKey + ".timeout.read", 30);
            boolean longThinking = configManager.getConfig().getBoolean("api.models." + modelKey + ".timeout.long_thinking", false);

            String status = longThinking ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用";
            sender.sendMessage(ChatColor.YELLOW + "- " + modelName + " (" + modelKey + ")");
            sender.sendMessage(ChatColor.GRAY + "  连接:" + connectTimeout + "s, 读取:" + readTimeout + "s, 长思考:" + status);
        }

        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        logManager.logCommand(senderName, "timeout list");
    }

    private void showTimeoutInfo(CommandSender sender, String modelKey) {
        if (!configManager.getAvailableModels().contains(modelKey)) {
            sender.sendMessage(ChatColor.RED + "找不到指定的模型: " + modelKey);
            return;
        }

        String modelName = configManager.getModelDisplayName(modelKey);
        int connectTimeout = configManager.getConfig().getInt("api.models." + modelKey + ".timeout.connect", 30);
        int readTimeout = configManager.getConfig().getInt("api.models." + modelKey + ".timeout.read", 30);
        int writeTimeout = configManager.getConfig().getInt("api.models." + modelKey + ".timeout.write", 30);
        boolean longThinking = configManager.getConfig().getBoolean("api.models." + modelKey + ".timeout.long_thinking", false);

        sender.sendMessage(ChatColor.GREEN + "=== " + modelName + " 超时设置详情 ===");
        sender.sendMessage(ChatColor.YELLOW + "模型标识: " + ChatColor.WHITE + modelKey);
        sender.sendMessage(ChatColor.YELLOW + "连接超时: " + ChatColor.WHITE + connectTimeout + " 秒");
        sender.sendMessage(ChatColor.YELLOW + "读取超时: " + ChatColor.WHITE + readTimeout + " 秒");
        sender.sendMessage(ChatColor.YELLOW + "写入超时: " + ChatColor.WHITE + writeTimeout + " 秒");
        sender.sendMessage(ChatColor.YELLOW + "长思考模式: " + (longThinking ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));

        if (longThinking) {
            sender.sendMessage(ChatColor.GRAY + "长思考模式允许AI花费更多时间进行深度思考，适合复杂问题。");
        } else {
            sender.sendMessage(ChatColor.GRAY + "标准模式适合快速响应的场景。");
        }

        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        logManager.logCommand(senderName, "timeout info " + modelKey);
    }

    private void toggleLongThinking(CommandSender sender, String modelKey) {
        if (!configManager.getAvailableModels().contains(modelKey)) {
            sender.sendMessage(ChatColor.RED + "找不到指定的模型: " + modelKey);
            return;
        }

        String configPath = "api.models." + modelKey + ".timeout.long_thinking";
        boolean currentValue = configManager.getConfig().getBoolean(configPath, false);
        boolean newValue = !currentValue;

        configManager.getConfig().set(configPath, newValue);
        plugin.saveConfig();

        String modelName = configManager.getModelDisplayName(modelKey);
        String status = newValue ? "启用" : "禁用";
        sender.sendMessage(ChatColor.GREEN + "已" + status + "模型 " + modelName + " 的长思考模式！");

        if (newValue) {
            int readTimeout = configManager.getConfig().getInt("api.models." + modelKey + ".timeout.read", 30);
            sender.sendMessage(ChatColor.GRAY + "长思考模式已启用，读取超时时间: " + readTimeout + " 秒");
        } else {
            sender.sendMessage(ChatColor.GRAY + "长思考模式已禁用，将使用标准超时设置");
        }

        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        logManager.logCommand(senderName, "timeout toggle " + modelKey + " -> " + status);
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== GeminiCraftChat 命令帮助 ===");
        sender.sendMessage(ChatColor.YELLOW + "/gcc reload " + ChatColor.GRAY + "- 重新加载配置文件");

        if (sender instanceof Player) {
            sender.sendMessage(ChatColor.YELLOW + "/gcc clear " + ChatColor.GRAY + "- 清除你的对话历史");
        }
        sender.sendMessage(ChatColor.YELLOW + "/gcc clear all " + ChatColor.GRAY + "- 清除所有玩家的对话历史");
        sender.sendMessage(ChatColor.YELLOW + "/gcc model <名称> " + ChatColor.GRAY + "- 切换到指定模型");
        sender.sendMessage(ChatColor.YELLOW + "/gcc temp <数值> " + ChatColor.GRAY + "- 调整模型温度(0.0-1.0)");
        sender.sendMessage(ChatColor.YELLOW + "/gcc persona list " + ChatColor.GRAY + "- 查看可用的人设列表");

        if (sender instanceof Player) {
            sender.sendMessage(ChatColor.YELLOW + "/gcc persona switch <名称> " + ChatColor.GRAY + "- 切换到指定人设");
        }

        // 管理员命令 - 控制台默认有管理员权限
        boolean hasAdminPerm = true;
        if (sender instanceof Player) {
            Player player = (Player) sender;
            hasAdminPerm = player.hasPermission(configManager.getPermission("admin"));
        }

        if (hasAdminPerm) {
            sender.sendMessage(ChatColor.YELLOW + "/gcc debug " + ChatColor.GRAY + "- 切换调试模式");
            sender.sendMessage(ChatColor.YELLOW + "/gcc logs stats " + ChatColor.GRAY + "- 查看日志统计信息");
            sender.sendMessage(ChatColor.YELLOW + "/gcc logs reset " + ChatColor.GRAY + "- 重置统计数据");
            sender.sendMessage(ChatColor.YELLOW + "/gcc logs export " + ChatColor.GRAY + "- 导出玩家统计到日志");
            sender.sendMessage(ChatColor.YELLOW + "/gcc timeout list " + ChatColor.GRAY + "- 查看所有模型的超时设置");
            sender.sendMessage(ChatColor.YELLOW + "/gcc timeout info <模型> " + ChatColor.GRAY + "- 查看指定模型的详细超时信息");
            sender.sendMessage(ChatColor.YELLOW + "/gcc timeout toggle <模型> " + ChatColor.GRAY + "- 切换模型的长思考模式");
            sender.sendMessage(ChatColor.YELLOW + "/gcc npc list " + ChatColor.GRAY + "- 查看所有NPC");
            sender.sendMessage(ChatColor.YELLOW + "/gcc npc info <NPC ID> " + ChatColor.GRAY + "- 查看NPC详细信息");
            sender.sendMessage(ChatColor.YELLOW + "/gcc npc spawn <NPC ID> " + ChatColor.GRAY + "- 重新生成NPC");
            sender.sendMessage(ChatColor.YELLOW + "/gcc npc remove <NPC ID> " + ChatColor.GRAY + "- 移除NPC");
            sender.sendMessage(ChatColor.YELLOW + "/gcc npc reload " + ChatColor.GRAY + "- 重新加载NPC配置");
        }

        // NPC相关命令（普通玩家也可以使用部分）
        if (npcManager.isEnabled()) {
            sender.sendMessage(ChatColor.YELLOW + "/gcc npc chat <NPC ID> <消息> " + ChatColor.GRAY + "- 与NPC对话");
            sender.sendMessage(ChatColor.YELLOW + "/gcc npc nearby " + ChatColor.GRAY + "- 查看附近的NPC");
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.GRAY + "注意：控制台拥有所有管理员权限");
        }
    }

    /**
     * 处理NPC相关命令
     */
    private void handleNPC(CommandSender sender, String[] args) {
        if (!npcManager.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "NPC功能已禁用！");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /gcc npc <子命令>");
            sender.sendMessage(ChatColor.YELLOW + "可用子命令: list, info, chat, nearby, spawn, remove, reload");
            return;
        }

        String subCommand = args[1].toLowerCase();
        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";

        switch (subCommand) {
            case "list":
                handleNPCList(sender);
                break;

            case "info":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /gcc npc info <NPC ID>");
                    return;
                }
                handleNPCInfo(sender, args[2]);
                break;

            case "chat":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "只有玩家可以与NPC对话！");
                    return;
                }
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "用法: /gcc npc chat <NPC ID> <消息>");
                    return;
                }
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                handleNPCChat((Player) sender, args[2], message);
                break;

            case "nearby":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "只有玩家可以查看附近的NPC！");
                    return;
                }
                handleNPCNearby((Player) sender);
                break;

            case "spawn":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (!player.hasPermission(configManager.getPermission("admin"))) {
                        sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                        return;
                    }
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /gcc npc spawn <NPC ID>");
                    return;
                }
                handleNPCSpawn(sender, args[2]);
                break;

            case "remove":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (!player.hasPermission(configManager.getPermission("admin"))) {
                        sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                        return;
                    }
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /gcc npc remove <NPC ID>");
                    return;
                }
                handleNPCRemove(sender, args[2]);
                break;

            case "reload":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (!player.hasPermission(configManager.getPermission("admin"))) {
                        sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                        return;
                    }
                }
                handleNPCReload(sender);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "未知的NPC子命令: " + subCommand);
                sender.sendMessage(ChatColor.YELLOW + "可用子命令: list, info, chat, nearby, spawn, remove, reload");
                break;
        }

        logManager.logCommand(senderName, "npc " + subCommand);
    }

    /**
     * 处理NPC列表命令
     */
    private void handleNPCList(CommandSender sender) {
        Map<String, AIControlledNPC> npcs = npcManager.getNPCs();

        if (npcs.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "当前没有活跃的NPC");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "=== NPC列表 ===");
        for (AIControlledNPC npc : npcs.values()) {
            String status = npc.isActive() ? ChatColor.GREEN + "活跃" : ChatColor.RED + "非活跃";
            String location = String.format("%.1f, %.1f, %.1f",
                npc.getCurrentLocation().getX(),
                npc.getCurrentLocation().getY(),
                npc.getCurrentLocation().getZ());

            sender.sendMessage(String.format("%s%s %s- %s%s %s在 %s%s",
                ChatColor.YELLOW, npc.getNpcId(),
                ChatColor.GRAY, ChatColor.WHITE, npc.getDisplayName(),
                status, ChatColor.GRAY, location));
        }
    }

    /**
     * 处理NPC信息命令
     */
    private void handleNPCInfo(CommandSender sender, String npcId) {
        AIControlledNPC npc = npcManager.getNPC(npcId);
        if (npc == null) {
            sender.sendMessage(ChatColor.RED + "未找到NPC: " + npcId);
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "=== NPC信息: " + npc.getDisplayName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + npc.getNpcId());
        sender.sendMessage(ChatColor.YELLOW + "显示名称: " + ChatColor.WHITE + npc.getDisplayName());
        sender.sendMessage(ChatColor.YELLOW + "实体类型: " + ChatColor.WHITE + npc.getEntityType().name());
        sender.sendMessage(ChatColor.YELLOW + "人设: " + ChatColor.WHITE + npc.getPersonality());
        sender.sendMessage(ChatColor.YELLOW + "API模型: " + ChatColor.WHITE + npc.getApiModel());
        sender.sendMessage(ChatColor.YELLOW + "当前状态: " + ChatColor.WHITE + npc.getCurrentState().name());
        sender.sendMessage(ChatColor.YELLOW + "是否活跃: " + (npc.isActive() ? ChatColor.GREEN + "是" : ChatColor.RED + "否"));

        if (npc.getEntity() != null) {
            Location loc = npc.getCurrentLocation();
            sender.sendMessage(ChatColor.YELLOW + "当前位置: " + ChatColor.WHITE +
                String.format("%.1f, %.1f, %.1f (%s)", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName()));
        }

        if (npc.getCurrentTarget() != null) {
            sender.sendMessage(ChatColor.YELLOW + "当前目标: " + ChatColor.WHITE + npc.getCurrentTarget().getName());
        }
    }

    /**
     * 处理NPC对话命令
     */
    private void handleNPCChat(Player player, String npcId, String message) {
        AIControlledNPC npc = npcManager.getNPC(npcId);
        if (npc == null) {
            player.sendMessage(ChatColor.RED + "未找到NPC: " + npcId);
            return;
        }

        if (!player.hasPermission("gcc.npc.chat")) {
            player.sendMessage(ChatColor.RED + "你没有权限与NPC对话！");
            return;
        }

        if (!npc.canInteractWith(player)) {
            player.sendMessage(ChatColor.RED + "你离 " + npc.getDisplayName() + " 太远了！");
            return;
        }

        player.sendMessage(ChatColor.GRAY + "你对 " + ChatColor.YELLOW + npc.getDisplayName() + ChatColor.GRAY + " 说: " + ChatColor.WHITE + message);
        npcManager.handlePlayerChat(player, npc, message);
    }

    /**
     * 处理附近NPC命令
     */
    private void handleNPCNearby(Player player) {
        if (!player.hasPermission("gcc.npc.nearby")) {
            player.sendMessage(ChatColor.RED + "你没有权限查看附近的NPC！");
            return;
        }

        double range = configManager.getConfig().getDouble("npc.nearby_range", 20.0);
        java.util.List<AIControlledNPC> nearbyNPCs = npcManager.getNearbyNPCs(player, range);

        if (nearbyNPCs.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "附近 " + range + " 格范围内没有NPC");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "=== 附近的NPC (" + range + "格范围) ===");
        for (AIControlledNPC npc : nearbyNPCs) {
            double distance = player.getLocation().distance(npc.getCurrentLocation());
            String canInteract = npc.canInteractWith(player) ? ChatColor.GREEN + "可交互" : ChatColor.RED + "太远";

            player.sendMessage(String.format("%s%s %s- %s%s %s距离: %.1f格 %s",
                ChatColor.YELLOW, npc.getNpcId(),
                ChatColor.GRAY, ChatColor.WHITE, npc.getDisplayName(),
                ChatColor.GRAY, distance, canInteract));
        }
    }

    /**
     * 处理NPC生成命令
     */
    private void handleNPCSpawn(CommandSender sender, String npcId) {
        AIControlledNPC npc = npcManager.getNPC(npcId);
        if (npc == null) {
            sender.sendMessage(ChatColor.RED + "未找到NPC配置: " + npcId);
            return;
        }

        if (npc.isActive() && npc.getEntity() != null && !npc.getEntity().isDead()) {
            sender.sendMessage(ChatColor.YELLOW + "NPC " + npc.getDisplayName() + " 已经存在");
            return;
        }

        boolean success = npcManager.spawnNPC(npc);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "成功生成NPC: " + npc.getDisplayName());
        } else {
            sender.sendMessage(ChatColor.RED + "生成NPC失败: " + npc.getDisplayName());
        }
    }

    /**
     * 处理NPC移除命令
     */
    private void handleNPCRemove(CommandSender sender, String npcId) {
        AIControlledNPC npc = npcManager.getNPC(npcId);
        if (npc == null) {
            sender.sendMessage(ChatColor.RED + "未找到NPC: " + npcId);
            return;
        }

        boolean success = npcManager.removeNPC(npcId);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "成功移除NPC: " + npc.getDisplayName());
        } else {
            sender.sendMessage(ChatColor.RED + "移除NPC失败: " + npc.getDisplayName());
        }
    }

    /**
     * 处理NPC重新加载命令
     */
    private void handleNPCReload(CommandSender sender) {
        try {
            npcManager.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "NPC配置已重新加载");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "重新加载NPC配置失败: " + e.getMessage());
            plugin.getLogger().warning("重新加载NPC配置失败: " + e.getMessage());
        }
    }
}