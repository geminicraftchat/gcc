package cn.ningmo.geminicraftchat.npc;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.persona.Persona;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.api.GeminiService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;

/**
 * NPC AI决策制定器
 * 负责NPC的智能决策和行为执行
 */
public class NPCAIDecisionMaker {
    private final GeminiCraftChat plugin;
    private final ConfigManager configManager;
    private final GeminiService geminiService;
    private final Random random;
    
    // 线程安全的NPC状态管理
    private final Map<String, ReentrantLock> npcLocks;
    private final Map<String, AtomicBoolean> npcProcessing;
    private final Map<String, Long> lastDecisionTime;
    
    // 性能监控和限流
    private final AtomicInteger activeAIRequests;
    private final AtomicLong totalAIRequests;
    private final AtomicLong failedAIRequests;
    private final Semaphore aiRequestSemaphore;
    private final int maxConcurrentAIRequests;
    private final long requestTimeoutMs;
    
    // AI提示词模板
    private static final String MOVEMENT_DECISION_PROMPT = """
        你是一个在Minecraft世界中的智能NPC，名字是{npc_name}，性格是{personality}。
        
        当前状态信息：
        - 当前位置：{current_location}
        - 当前状态：{current_state}
        - 附近玩家：{nearby_players}
        - 世界时间：{world_time}
        - 生物群系：{biome}
        
        可选择的行为：
        1. IDLE - 保持原地不动，观察周围
        2. WANDER - 随机游荡探索
        3. FOLLOW - 跟随最近的玩家
        4. FLEE - 远离威胁或玩家
        5. EXPLORE - 探索新的区域
        6. PATROL - 在固定路线巡逻
        7. SLEEP - 休息（如果是夜晚）
        
        移动方向选项：
        - NORTH (北方)
        - SOUTH (南方) 
        - EAST (东方)
        - WEST (西方)
        - NORTHEAST (东北)
        - NORTHWEST (西北)
        - SOUTHEAST (东南)
        - SOUTHWEST (西南)
        - UP (向上，如果可以飞行)
        - DOWN (向下)
        - STAY (保持原地)
        
        请根据你的性格和当前情况，选择最合适的行为和移动方向。
        回复格式：ACTION:行为名称|DIRECTION:方向|REASON:原因
        例如：ACTION:WANDER|DIRECTION:NORTH|REASON:我想去北边看看有什么有趣的东西
        """;
    
    private static final String CONVERSATION_PROMPT = """
        你是一个在Minecraft世界中的智能NPC，名字是{npc_name}，性格是{personality}。
        
        玩家 {player_name} 对你说："{player_message}"
        
        对话历史：
        {conversation_history}
        
        当前环境：
        - 位置：{current_location}
        - 时间：{world_time}
        - 天气：{weather}
        
        请以你的性格回应玩家。你可以：
        - 正常对话
        - 询问玩家问题
        - 分享关于世界的信息
        - 表达情感和想法
        - 如果合适，可以建议一起做某事
        
        请保持角色一致性，回复要自然友好。回复长度控制在1-3句话。
        """;
    
    public NPCAIDecisionMaker(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        // 使用ChatManager中的GeminiService实例，避免重复创建
        this.geminiService = plugin.getChatManager().getGeminiService();
        this.random = new Random();
        
        // 初始化线程安全组件
        this.npcLocks = new ConcurrentHashMap<>();
        this.npcProcessing = new ConcurrentHashMap<>();
        this.lastDecisionTime = new ConcurrentHashMap<>();
        
        // 初始化性能监控和限流组件
        this.activeAIRequests = new AtomicInteger(0);
        this.totalAIRequests = new AtomicLong(0);
        this.failedAIRequests = new AtomicLong(0);
        this.maxConcurrentAIRequests = configManager.getConfig().getInt("npc.ai.max-concurrent-requests", 5);
        this.requestTimeoutMs = configManager.getConfig().getLong("npc.ai.request-timeout-ms", 10000);
        this.aiRequestSemaphore = new Semaphore(maxConcurrentAIRequests, true);
    }
    
    /**
     * 为NPC做出决策（线程安全版本）
     */
    public void makeDecision(AIControlledNPC npc) {
        if (!npc.isActive() || npc.getEntity() == null) return;
        
        String npcId = npc.getNpcId();
        
        // 检查是否正在处理中
        AtomicBoolean processing = npcProcessing.computeIfAbsent(npcId, k -> new AtomicBoolean(false));
        if (!processing.compareAndSet(false, true)) {
            return; // 已经在处理中，跳过
        }
        
        try {
            // 检查决策间隔
            Long lastTime = lastDecisionTime.get(npcId);
            long currentTime = System.currentTimeMillis();
            if (lastTime != null && (currentTime - lastTime) < 3000) { // 3秒间隔
                return;
            }
            
            // 如果正在对话中，不做移动决策
            if (npc.getCurrentState() == AIControlledNPC.NPCState.TALKING) {
                return;
            }
            
            // 更新最后决策时间
            lastDecisionTime.put(npcId, currentTime);
            
            // 检查AI请求限流
            if (!aiRequestSemaphore.tryAcquire()) {
                plugin.debug("NPC " + npcId + " AI请求被限流，当前活跃请求: " + activeAIRequests.get());
                processing.set(false);
                return;
            }
            
            // 增加活跃请求计数
            activeAIRequests.incrementAndGet();
            totalAIRequests.incrementAndGet();
            
            // 构建决策提示词
            String prompt = buildMovementPrompt(npc);
            
            // 创建一个虚拟玩家ID用于NPC决策
            String npcPlayerId = "npc_" + npc.getNpcId();

            // 调用真实AI服务
            CompletableFuture<String> future = geminiService.sendMessage(
                npcPlayerId,
                prompt,
                getPersonaForNPC(npc)
            );
            
            future.thenAccept(response -> {
                // 在主线程中执行决策
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ReentrantLock lock = npcLocks.computeIfAbsent(npcId, k -> new ReentrantLock());
                    lock.lock();
                    try {
                        // 再次检查NPC状态
                        if (npc.isActive() && npc.getEntity() != null) {
                            parseAndExecuteDecision(npc, response);
                        }
                    } catch (Exception e) {
                        plugin.debug("NPC决策执行失败: " + e.getMessage());
                        executeDefaultBehavior(npc);
                        failedAIRequests.incrementAndGet();
                    } finally {
                        lock.unlock();
                        processing.set(false);
                        activeAIRequests.decrementAndGet();
                        aiRequestSemaphore.release();
                    }
                });
            }).exceptionally(throwable -> {
                plugin.debug("NPC决策AI请求失败: " + throwable.getMessage());
                failedAIRequests.incrementAndGet();
                // 在主线程中执行默认行为
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        executeDefaultBehavior(npc);
                    } finally {
                        processing.set(false);
                        activeAIRequests.decrementAndGet();
                        aiRequestSemaphore.release();
                    }
                });
                return null;
            });
            
        } catch (Exception e) {
            plugin.debug("NPC决策准备失败: " + e.getMessage());
            processing.set(false);
        }
    }
    
    /**
     * 构建移动决策提示词
     */
    private String buildMovementPrompt(AIControlledNPC npc) {
        Location loc = npc.getCurrentLocation();
        List<Player> nearbyPlayers = npc.getNearbyPlayers();
        
        String nearbyPlayersStr = nearbyPlayers.isEmpty() ? "无" : 
            nearbyPlayers.stream()
                .map(Player::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("无");
        
        long worldTime = loc.getWorld().getTime();
        String timeStr = getTimeDescription(worldTime);
        String biome = loc.getBlock().getBiome().name();
        
        return MOVEMENT_DECISION_PROMPT
            .replace("{npc_name}", npc.getDisplayName())
            .replace("{personality}", npc.getPersonality())
            .replace("{current_location}", String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()))
            .replace("{current_state}", npc.getCurrentState().name())
            .replace("{nearby_players}", nearbyPlayersStr)
            .replace("{world_time}", timeStr)
            .replace("{biome}", biome);
    }
    
    /**
     * 解析并执行AI决策
     */
    private void parseAndExecuteDecision(AIControlledNPC npc, String response) {
        plugin.debug("NPC " + npc.getNpcId() + " AI决策: " + response);
        
        // 解析响应格式：ACTION:行为|DIRECTION:方向|REASON:原因
        String[] parts = response.split("\\|");
        String action = "IDLE";
        String direction = "STAY";
        String reason = "默认行为";
        
        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                switch (key.toUpperCase()) {
                    case "ACTION":
                        action = value.toUpperCase();
                        break;
                    case "DIRECTION":
                        direction = value.toUpperCase();
                        break;
                    case "REASON":
                        reason = value;
                        break;
                }
            }
        }
        
        // 执行决策
        executeDecision(npc, action, direction, reason);
    }
    
    /**
     * 执行决策
     */
    private void executeDecision(AIControlledNPC npc, String action, String direction, String reason) {
        plugin.debug("NPC " + npc.getDisplayName() + " 决定: " + action + " 方向: " + direction + " 原因: " + reason);
        
        // 设置NPC状态（线程安全）
        try {
            AIControlledNPC.NPCState newState = AIControlledNPC.NPCState.valueOf(action);
            AIControlledNPC.NPCState currentState = npc.getCurrentState();
            
            // 使用compareAndSet确保状态更新的原子性
            if (!npc.compareAndSetState(currentState, newState)) {
                plugin.debug("NPC " + npc.getNpcId() + " 状态更新失败，可能被其他线程修改");
                return; // 如果状态已被其他线程修改，则跳过此次决策
            }
        } catch (IllegalArgumentException e) {
            AIControlledNPC.NPCState currentState = npc.getCurrentState();
            npc.compareAndSetState(currentState, AIControlledNPC.NPCState.IDLE);
        }
        
        // 计算移动目标
        if (!direction.equals("STAY")) {
            Location targetLocation = calculateTargetLocation(npc.getCurrentLocation(), direction);
            if (targetLocation != null && isValidLocation(npc, targetLocation)) {
                npc.setTargetLocation(targetLocation);
            }
        }
    }
    
    /**
     * 计算目标位置
     */
    private Location calculateTargetLocation(Location current, String direction) {
        Vector moveVector = new Vector(0, 0, 0);
        double distance = 5.0; // 移动距离
        
        switch (direction.toUpperCase()) {
            case "NORTH":
                moveVector = new Vector(0, 0, -distance);
                break;
            case "SOUTH":
                moveVector = new Vector(0, 0, distance);
                break;
            case "EAST":
                moveVector = new Vector(distance, 0, 0);
                break;
            case "WEST":
                moveVector = new Vector(-distance, 0, 0);
                break;
            case "NORTHEAST":
                moveVector = new Vector(distance * 0.7, 0, -distance * 0.7);
                break;
            case "NORTHWEST":
                moveVector = new Vector(-distance * 0.7, 0, -distance * 0.7);
                break;
            case "SOUTHEAST":
                moveVector = new Vector(distance * 0.7, 0, distance * 0.7);
                break;
            case "SOUTHWEST":
                moveVector = new Vector(-distance * 0.7, 0, distance * 0.7);
                break;
            case "UP":
                moveVector = new Vector(0, distance, 0);
                break;
            case "DOWN":
                moveVector = new Vector(0, -distance, 0);
                break;
            default:
                return null;
        }
        
        return current.clone().add(moveVector);
    }
    
    /**
     * 检查位置是否有效
     */
    private boolean isValidLocation(AIControlledNPC npc, Location location) {
        // 检查是否在允许的游荡范围内
        double distance = location.distance(npc.getSpawnLocation());
        if (distance > npc.getBehaviorConfig().getMaxWanderDistance()) {
            return false;
        }
        
        // 检查是否是安全的位置（不在虚空中，不在岩浆中等）
        if (location.getY() < 0 || location.getY() > 256) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 执行当前行为
     */
    public void executeCurrentBehavior(AIControlledNPC npc) {
        if (npc.getEntity() == null || npc.getEntity().isDead()) return;
        
        switch (npc.getCurrentState()) {
            case MOVING_TO:
                executeMoveToTarget(npc);
                break;
            case FOLLOWING:
                executeFollowPlayer(npc);
                break;
            case FLEEING:
                executeFlee(npc);
                break;
            case WANDERING:
                executeWander(npc);
                break;
            case PATROLLING:
                executePatrol(npc);
                break;
            case IDLE:
            case SLEEPING:
            case TALKING:
            default:
                // 这些状态不需要特殊的移动行为
                break;
        }
    }
    
    /**
     * 执行移动到目标位置
     */
    private void executeMoveToTarget(AIControlledNPC npc) {
        Location target = npc.getTargetLocation();
        if (target == null) {
            npc.setCurrentState(AIControlledNPC.NPCState.IDLE);
            return;
        }
        
        Location current = npc.getCurrentLocation();
        if (current.distance(target) < 1.0) {
            // 到达目标
            npc.setCurrentState(AIControlledNPC.NPCState.IDLE);
            npc.setTargetLocation(null);
            return;
        }
        
        // 移动向目标
        moveTowardsLocation(npc, target);
    }
    
    /**
     * 执行跟随玩家
     */
    private void executeFollowPlayer(AIControlledNPC npc) {
        Player target = npc.getCurrentTarget();
        if (target == null || !target.isOnline()) {
            // 寻找最近的玩家
            List<Player> nearbyPlayers = npc.getNearbyPlayers();
            if (!nearbyPlayers.isEmpty()) {
                target = nearbyPlayers.get(0);
                npc.setCurrentTarget(target);
            } else {
                npc.setCurrentState(AIControlledNPC.NPCState.IDLE);
                return;
            }
        }
        
        Location targetLoc = target.getLocation();
        Location current = npc.getCurrentLocation();
        double distance = current.distance(targetLoc);
        
        if (distance > npc.getBehaviorConfig().getFollowDistance()) {
            moveTowardsLocation(npc, targetLoc);
        }
    }
    
    /**
     * 执行逃跑
     */
    private void executeFlee(AIControlledNPC npc) {
        List<Player> nearbyPlayers = npc.getNearbyPlayers();
        if (nearbyPlayers.isEmpty()) {
            npc.setCurrentState(AIControlledNPC.NPCState.IDLE);
            return;
        }
        
        // 计算远离所有玩家的方向
        Location current = npc.getCurrentLocation();
        Vector fleeDirection = new Vector(0, 0, 0);
        
        for (Player player : nearbyPlayers) {
            Vector awayFromPlayer = current.toVector().subtract(player.getLocation().toVector()).normalize();
            fleeDirection.add(awayFromPlayer);
        }
        
        fleeDirection.normalize().multiply(5.0);
        Location fleeTarget = current.clone().add(fleeDirection);
        
        if (isValidLocation(npc, fleeTarget)) {
            moveTowardsLocation(npc, fleeTarget);
        }
    }
    
    /**
     * 执行游荡
     */
    private void executeWander(AIControlledNPC npc) {
        if (npc.getTargetLocation() == null || npc.getCurrentLocation().distance(npc.getTargetLocation()) < 2.0) {
            // 选择新的随机目标
            Location randomTarget = generateRandomLocation(npc);
            npc.setTargetLocation(randomTarget);
        }
        
        moveTowardsLocation(npc, npc.getTargetLocation());
    }
    
    /**
     * 执行巡逻
     */
    private void executePatrol(AIControlledNPC npc) {
        // 简单的巡逻逻辑：在出生点周围巡逻
        if (npc.getTargetLocation() == null || npc.getCurrentLocation().distance(npc.getTargetLocation()) < 2.0) {
            Location patrolTarget = generatePatrolLocation(npc);
            npc.setTargetLocation(patrolTarget);
        }
        
        moveTowardsLocation(npc, npc.getTargetLocation());
    }
    
    /**
     * 移动向指定位置
     */
    private void moveTowardsLocation(AIControlledNPC npc, Location target) {
        if (npc.getEntity() == null || target == null) return;
        
        Location current = npc.getCurrentLocation();
        Vector direction = target.toVector().subtract(current.toVector()).normalize();
        direction.multiply(npc.getBehaviorConfig().getMovementSpeed());
        
        Location newLocation = current.clone().add(direction);
        
        // 确保Y坐标合理
        newLocation.setY(target.getY());
        
        // 设置朝向
        Vector lookDirection = target.toVector().subtract(current.toVector());
        Location lookAt = current.clone().setDirection(lookDirection);
        newLocation.setYaw(lookAt.getYaw());
        newLocation.setPitch(lookAt.getPitch());
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (npc.getEntity() != null) {
                npc.getEntity().teleport(newLocation);
            }
        });
    }
    
    /**
     * 生成随机位置
     */
    private Location generateRandomLocation(AIControlledNPC npc) {
        Location spawn = npc.getSpawnLocation();
        double maxDistance = npc.getBehaviorConfig().getMaxWanderDistance();
        
        double x = spawn.getX() + (random.nextDouble() - 0.5) * 2 * maxDistance;
        double z = spawn.getZ() + (random.nextDouble() - 0.5) * 2 * maxDistance;
        double y = spawn.getY();
        
        return new Location(spawn.getWorld(), x, y, z);
    }
    
    /**
     * 生成巡逻位置
     */
    private Location generatePatrolLocation(AIControlledNPC npc) {
        Location spawn = npc.getSpawnLocation();
        double patrolRadius = Math.min(npc.getBehaviorConfig().getMaxWanderDistance(), 10.0);
        
        double angle = random.nextDouble() * 2 * Math.PI;
        double x = spawn.getX() + Math.cos(angle) * patrolRadius;
        double z = spawn.getZ() + Math.sin(angle) * patrolRadius;
        
        return new Location(spawn.getWorld(), x, spawn.getY(), z);
    }
    
    /**
     * 执行默认行为
     */
    private void executeDefaultBehavior(AIControlledNPC npc) {
        // 简单的默认行为：随机选择一个状态
        AIControlledNPC.NPCState[] states = {
            AIControlledNPC.NPCState.IDLE,
            AIControlledNPC.NPCState.WANDERING,
            AIControlledNPC.NPCState.IDLE
        };
        
        AIControlledNPC.NPCState newState = states[random.nextInt(states.length)];
        npc.setCurrentState(newState);
        
        if (newState == AIControlledNPC.NPCState.WANDERING) {
            npc.setTargetLocation(generateRandomLocation(npc));
        }
    }
    
    /**
     * 获取NPC的人设
     */
    private Optional<Persona> getPersonaForNPC(AIControlledNPC npc) {
        String personaName = npc.getPersonality();

        if (configManager.getConfig().contains("personas." + personaName)) {
            String name = configManager.getConfig().getString("personas." + personaName + ".name");
            String description = configManager.getConfig().getString("personas." + personaName + ".description");
            String context = configManager.getConfig().getString("personas." + personaName + ".context");
            return Optional.of(new Persona(name, description, context));
        }

        return Optional.empty();
    }
    
    /**
     * 获取时间描述
     */
    private String getTimeDescription(long worldTime) {
        if (worldTime >= 0 && worldTime < 6000) {
            return "早晨";
        } else if (worldTime >= 6000 && worldTime < 12000) {
            return "中午";
        } else if (worldTime >= 12000 && worldTime < 18000) {
            return "下午";
        } else {
            return "夜晚";
        }
    }
    
    /**
     * 处理对话
     */
    public void handleConversation(AIControlledNPC npc, Player player, String message) {
        String npcId = npc.getNpcId();
        
        // 检查是否正在处理对话
        AtomicBoolean processing = npcProcessing.computeIfAbsent(npcId + "_conv", k -> new AtomicBoolean(false));
        if (!processing.compareAndSet(false, true)) {
            player.sendMessage("§e" + npc.getDisplayName() + "§f: 请稍等，我还在思考上一个问题...");
            return;
        }
        
        try {
            // 设置NPC状态为对话中
            ReentrantLock lock = npcLocks.computeIfAbsent(npcId, k -> new ReentrantLock());
            lock.lock();
            try {
                npc.setCurrentState(AIControlledNPC.NPCState.TALKING);
                npc.setCurrentTarget(player);
            } finally {
                lock.unlock();
            }
            
            // 检查AI请求限流
            if (!aiRequestSemaphore.tryAcquire()) {
                player.sendMessage("§e" + npc.getDisplayName() + "§f: 请稍等，我还在思考上一个问题...");
                processing.set(false);
                return;
            }
            
            // 增加活跃请求计数
            activeAIRequests.incrementAndGet();
            totalAIRequests.incrementAndGet();
            
            // 构建对话提示词
            String prompt = buildConversationPrompt(npc, player, message);
            
            // 创建一个唯一的对话ID
            String conversationId = "npc_" + npcId + "_" + player.getUniqueId();

            // 调用真实AI服务
            CompletableFuture<String> future = geminiService.sendMessage(
                conversationId,
                prompt,
                getPersonaForNPC(npc)
            );
            
            future.thenAccept(response -> {
                // 在主线程中处理回复
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        // 发送回复给玩家
                        if (player.isOnline()) {
                            player.sendMessage("§e" + npc.getDisplayName() + "§f: " + response);
                        }
                        
                        // 线程安全地记录对话历史
                        lock.lock();
                        try {
                            npc.addConversationHistory(player.getUniqueId(), "玩家: " + message);
                            npc.addConversationHistory(player.getUniqueId(), "NPC: " + response);
                        } finally {
                            lock.unlock();
                        }
                        failedAIRequests.incrementAndGet();
                        
                        // 一段时间后回到空闲状态
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            lock.lock();
                            try {
                                if (npc.getCurrentState() == AIControlledNPC.NPCState.TALKING) {
                                    npc.setCurrentState(AIControlledNPC.NPCState.IDLE);
                                    npc.setCurrentTarget(null);
                                }
                            } finally {
                            lock.unlock();
                            processing.set(false);
                            activeAIRequests.decrementAndGet();
                            aiRequestSemaphore.release();
                        }
                        }, 100L); // 5秒后
                        
                    } catch (Exception e) {
                        plugin.debug("处理NPC对话回复时发生错误: " + e.getMessage());
                        processing.set(false);
                    }
                });
            }).exceptionally(throwable -> {
                // 在主线程中处理错误
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (player.isOnline()) {
                            player.sendMessage("§c" + npc.getDisplayName() + " 似乎在思考中...");
                        }
                        plugin.debug("NPC对话AI请求失败: " + throwable.getMessage());
                        
                        // 重置NPC状态
                        lock.lock();
                        try {
                            npc.setCurrentState(AIControlledNPC.NPCState.IDLE);
                            npc.setCurrentTarget(null);
                        } finally {
                            lock.unlock();
                        }
                    } finally {
                        processing.set(false);
                        activeAIRequests.decrementAndGet();
                        aiRequestSemaphore.release();
                    }
                });
                return null;
            });
            
        } catch (Exception e) {
            plugin.debug("NPC对话准备失败: " + e.getMessage());
            processing.set(false);
        }
    }
    
    /**
     * 构建对话提示词
     */
    private String buildConversationPrompt(AIControlledNPC npc, Player player, String message) {
        Location loc = npc.getCurrentLocation();
        long worldTime = loc.getWorld().getTime();
        String timeStr = getTimeDescription(worldTime);
        String weather = loc.getWorld().hasStorm() ? "下雨" : "晴朗";
        
        // 获取对话历史
        List<String> history = npc.getConversationHistory(player.getUniqueId());
        String historyStr = history == null || history.isEmpty() ? "这是你们的第一次对话" :
            String.join("\n", history.subList(Math.max(0, history.size() - 6), history.size()));
        
        return CONVERSATION_PROMPT
            .replace("{npc_name}", npc.getDisplayName())
            .replace("{personality}", npc.getPersonality())
            .replace("{player_name}", player.getName())
            .replace("{player_message}", message)
            .replace("{conversation_history}", historyStr)
            .replace("{current_location}", String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()))
            .replace("{world_time}", timeStr)
            .replace("{weather}", weather);
    }
    
    /**
     * 清理不再使用的NPC资源
     */
    public void cleanupNPCResources(String npcId) {
        npcLocks.remove(npcId);
        npcProcessing.remove(npcId);
        npcProcessing.remove(npcId + "_conv");
        lastDecisionTime.remove(npcId);
    }
    
    /**
     * 定期清理过期的资源
     */
    public void performMaintenance() {
        long currentTime = System.currentTimeMillis();
        
        // 清理超过10分钟未使用的决策时间记录
        lastDecisionTime.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > 600000);
        
        // 清理孤立的处理状态
        npcProcessing.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            if (key.endsWith("_conv")) {
                String npcId = key.substring(0, key.length() - 5);
                return !lastDecisionTime.containsKey(npcId);
            }
            return !lastDecisionTime.containsKey(key);
        });
        
        // 清理孤立的锁
        npcLocks.entrySet().removeIf(entry -> 
            !lastDecisionTime.containsKey(entry.getKey()));
    }
    
    /**
     * 获取AI请求性能统计
     */
    public String getPerformanceStats() {
        long total = totalAIRequests.get();
        long failed = failedAIRequests.get();
        int active = activeAIRequests.get();
        int available = aiRequestSemaphore.availablePermits();
        
        double successRate = total > 0 ? ((double)(total - failed) / total) * 100 : 0;
        
        return String.format("AI请求统计 - 总计: %d, 失败: %d, 成功率: %.1f%%, 活跃: %d, 可用槽位: %d",
            total, failed, successRate, active, available);
    }
    
    /**
     * 重置性能统计
     */
    public void resetPerformanceStats() {
        totalAIRequests.set(0);
        failedAIRequests.set(0);
    }
    
    /**
     * 检查系统是否过载
     */
    public boolean isSystemOverloaded() {
        return aiRequestSemaphore.availablePermits() == 0;
    }
}
