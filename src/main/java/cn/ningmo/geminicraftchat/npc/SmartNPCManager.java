package cn.ningmo.geminicraftchat.npc;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 智能NPC管理器
 * 实现基于玩家距离的动态调度和批量处理优化
 */
public class SmartNPCManager implements NpcService {
    private final GeminiCraftChat plugin;
    private final ConfigManager configManager;
    private final Map<String, AIControlledNPC> npcs;
    private final Map<UUID, AIControlledNPC> entityToNPC;
    private final NPCAIDecisionMaker aiDecisionMaker;
    
    // 智能调度相关
    private final ScheduledExecutorService smartScheduler;
    private final ExecutorService npcProcessor;
    private BukkitTask mainThreadTask;
    private boolean enabled;
    
    // 性能配置
    private final int maxActiveNPCs;
    private final int batchSize;
    private final long activeUpdateInterval;
    private final long inactiveUpdateInterval;
    private final double nearbyPlayerRange;
    private final long cleanupInterval;
    
    // 性能统计
    private final AtomicLong totalUpdates;
    private final AtomicLong avgUpdateTime;
    private final AtomicInteger currentActiveNPCs;
    
    // NPC状态缓存
    private final Map<String, NPCUpdateInfo> npcUpdateCache;
    
    public SmartNPCManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.npcs = new ConcurrentHashMap<>();
        this.entityToNPC = new ConcurrentHashMap<>();
        this.aiDecisionMaker = new NPCAIDecisionMaker(plugin);
        
        // 读取性能配置
        ConfigurationSection perfConfig = configManager.getConfig().getConfigurationSection("performance");
        this.maxActiveNPCs = perfConfig != null ? perfConfig.getInt("max_active_npcs", 20) : 20;
        this.batchSize = perfConfig != null ? perfConfig.getInt("npc_update_batch_size", 5) : 5;
        this.nearbyPlayerRange = configManager.getConfig().getDouble("npc.nearby_range", 20.0);
        this.cleanupInterval = perfConfig != null ? perfConfig.getLong("memory_cleanup_interval", 300000) : 300000;
        
        // NPC更新间隔配置
        ConfigurationSection npcConfig = configManager.getConfig().getConfigurationSection("npc");
        this.activeUpdateInterval = npcConfig != null ? npcConfig.getLong("update_interval_active", 20) : 20;
        this.inactiveUpdateInterval = npcConfig != null ? npcConfig.getLong("update_interval_inactive", 100) : 100;
        
        // 初始化异步组件
        this.smartScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "GCC-SmartNPC-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
        
        this.npcProcessor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread thread = new Thread(r, "GCC-NPCProcessor-" + System.currentTimeMillis());
                thread.setDaemon(true);
                return thread;
            }
        );
        
        // 初始化统计
        this.totalUpdates = new AtomicLong(0);
        this.avgUpdateTime = new AtomicLong(0);
        this.currentActiveNPCs = new AtomicInteger(0);
        this.npcUpdateCache = new ConcurrentHashMap<>();
        
        this.enabled = false;
    }
    
    /**
     * 初始化智能NPC管理器
     */
    public void initialize() {
        if (!configManager.getConfig().getBoolean("npc.enabled", false)) {
            plugin.getLogger().info("NPC功能已禁用");
            return;
        }
        
        this.enabled = true;
        loadNPCsFromConfig();
        startSmartScheduling();
        
        plugin.getLogger().info("智能NPC管理器已初始化 - 加载了 " + npcs.size() + " 个NPC，最大活跃数: " + maxActiveNPCs);
    }
    
    /**
     * 关闭智能NPC管理器
     */
    public void shutdown() {
        enabled = false;
        
        // 停止所有调度任务
        if (mainThreadTask != null) {
            mainThreadTask.cancel();
        }
        
        smartScheduler.shutdown();
        npcProcessor.shutdown();
        
        try {
            if (!smartScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                smartScheduler.shutdownNow();
            }
            if (!npcProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                npcProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 移除所有NPC实体
        for (AIControlledNPC npc : npcs.values()) {
            if (npc.getEntity() != null) {
                npc.getEntity().remove();
            }
        }
        
        npcs.clear();
        entityToNPC.clear();
        npcUpdateCache.clear();
        
        plugin.getLogger().info("智能NPC管理器已关闭 - 总更新次数: " + totalUpdates.get());
    }
    
    /**
     * 启动智能调度
     */
    private void startSmartScheduling() {
        // 高频更新：活跃NPC（有玩家附近）
        smartScheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                updateActiveNPCs();
            }
        }, 0, activeUpdateInterval * 50, TimeUnit.MILLISECONDS);
        
        // 低频更新：非活跃NPC
        smartScheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                updateInactiveNPCs();
            }
        }, 0, inactiveUpdateInterval * 50, TimeUnit.MILLISECONDS);
        
        // 定期清理和优化
        smartScheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                performMaintenance();
            }
        }, cleanupInterval, cleanupInterval, TimeUnit.MILLISECONDS);
        
        // 主线程任务：处理需要主线程的操作
        mainThreadTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (enabled) {
                    processMainThreadTasks();
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // 每5 tick执行一次
    }
    
    /**
     * 更新活跃NPC（有玩家附近）
     */
    private void updateActiveNPCs() {
        long startTime = System.currentTimeMillis();
        
        List<AIControlledNPC> activeNPCs = getActiveNPCs();
        currentActiveNPCs.set(activeNPCs.size());
        
        if (activeNPCs.isEmpty()) {
            return;
        }
        
        // 分批处理活跃NPC
        List<List<AIControlledNPC>> batches = partitionList(activeNPCs, batchSize);
        
        List<CompletableFuture<Void>> futures = batches.stream()
            .map(batch -> CompletableFuture.runAsync(() -> processBatch(batch, true), npcProcessor))
            .collect(Collectors.toList());
        
        // 等待所有批次完成（非阻塞）
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                long updateTime = System.currentTimeMillis() - startTime;
                updateStats(activeNPCs.size(), updateTime);
            });
    }
    
    /**
     * 更新非活跃NPC
     */
    private void updateInactiveNPCs() {
        List<AIControlledNPC> inactiveNPCs = getInactiveNPCs();
        
        if (inactiveNPCs.isEmpty()) {
            return;
        }
        
        // 非活跃NPC使用更简单的更新逻辑
        CompletableFuture.runAsync(() -> {
            for (AIControlledNPC npc : inactiveNPCs) {
                updateInactiveNPC(npc);
            }
        }, npcProcessor);
    }
    
    /**
     * 获取活跃NPC列表
     */
    private List<AIControlledNPC> getActiveNPCs() {
        return npcs.values().stream()
            .filter(npc -> npc.isActive() && npc.getEntity() != null && !npc.getEntity().isDead())
            .filter(this::hasNearbyPlayers)
            .limit(maxActiveNPCs) // 限制最大活跃NPC数量
            .collect(Collectors.toList());
    }
    
    /**
     * 获取非活跃NPC列表
     */
    private List<AIControlledNPC> getInactiveNPCs() {
        return npcs.values().stream()
            .filter(npc -> npc.isActive() && npc.getEntity() != null && !npc.getEntity().isDead())
            .filter(npc -> !hasNearbyPlayers(npc))
            .collect(Collectors.toList());
    }
    
    /**
     * 检查NPC是否有附近的玩家
     */
    private boolean hasNearbyPlayers(AIControlledNPC npc) {
        if (npc.getEntity() == null) {
            return false;
        }
        
        Location npcLoc = npc.getCurrentLocation();
        return npcLoc.getWorld().getPlayers().stream()
            .anyMatch(player -> player.getLocation().distance(npcLoc) <= nearbyPlayerRange);
    }
    
    /**
     * 处理NPC批次
     */
    private void processBatch(List<AIControlledNPC> batch, boolean isActive) {
        for (AIControlledNPC npc : batch) {
            try {
                if (isActive) {
                    updateActiveNPC(npc);
                } else {
                    updateInactiveNPC(npc);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("更新NPC " + npc.getNpcId() + " 时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 更新活跃NPC
     */
    private void updateActiveNPC(AIControlledNPC npc) {
        // 检查时间活跃性
        long worldTime = npc.getCurrentLocation().getWorld().getTime();
        if (!npc.getBehaviorConfig().shouldBeActiveAtTime(worldTime)) {
            if (npc.getCurrentState() != AIControlledNPC.NPCState.SLEEPING) {
                npc.setCurrentState(AIControlledNPC.NPCState.SLEEPING);
            }
            return;
        }
        
        // 检查是否需要做决策
        if (npc.shouldMakeDecision()) {
            aiDecisionMaker.makeDecision(npc);
        }
        
        // 执行当前状态的行为
        aiDecisionMaker.executeCurrentBehavior(npc);
        
        // 更新缓存信息
        updateNPCCache(npc, true);
    }
    
    /**
     * 更新非活跃NPC
     */
    @Override
    public void handlePlayerChat(Player player, AIControlledNPC npc, String message) {
        aiDecisionMaker.handleConversation(npc, player, message);
    }

    private void updateInactiveNPC(AIControlledNPC npc) {
        // 非活跃NPC只做基本的状态检查
        long worldTime = npc.getCurrentLocation().getWorld().getTime();
        if (!npc.getBehaviorConfig().shouldBeActiveAtTime(worldTime)) {
            if (npc.getCurrentState() != AIControlledNPC.NPCState.SLEEPING) {
                npc.setCurrentState(AIControlledNPC.NPCState.SLEEPING);
            }
        } else if (npc.getCurrentState() == AIControlledNPC.NPCState.SLEEPING) {
            npc.setCurrentState(AIControlledNPC.NPCState.IDLE);
        }
        
        updateNPCCache(npc, false);
    }
    
    /**
     * 更新NPC缓存信息
     */
    private void updateNPCCache(AIControlledNPC npc, boolean isActive) {
        NPCUpdateInfo info = npcUpdateCache.computeIfAbsent(npc.getNpcId(), k -> new NPCUpdateInfo());
        info.lastUpdate = System.currentTimeMillis();
        info.isActive = isActive;
        info.updateCount++;
    }
    
    /**
     * 处理主线程任务
     */
    private void processMainThreadTasks() {
        // 检查需要重生的NPC
        for (AIControlledNPC npc : npcs.values()) {
            if (npc.isActive() && (npc.getEntity() == null || npc.getEntity().isDead())) {
                respawnNPCAsync(npc);
            }
        }
    }
    
    /**
     * 异步重生NPC
     */
    private void respawnNPCAsync(AIControlledNPC npc) {
        if (npc.getEntity() != null) {
            entityToNPC.remove(npc.getEntity().getUniqueId());
        }
        
        // 延迟重生
        smartScheduler.schedule(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> spawnNPC(npc));
        }, npc.getBehaviorConfig().getRespawnDelay(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * 执行维护任务
     */
    private void performMaintenance() {
        // 清理过期的缓存
        long currentTime = System.currentTimeMillis();
        npcUpdateCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().lastUpdate > 600000); // 10分钟
        
        // 内存优化（可配置）
        if (configManager.getConfig().getBoolean("performance.allow_explicit_gc", false)) {
            System.gc();
        }
        
        plugin.getLogger().info("NPC维护完成 - 活跃NPC: " + currentActiveNPCs.get() + 
            ", 缓存大小: " + npcUpdateCache.size() + ", 总更新: " + totalUpdates.get());
    }
    
    /**
     * 更新统计信息
     */
    private void updateStats(int npcCount, long updateTime) {
        totalUpdates.addAndGet(npcCount);
        avgUpdateTime.set((avgUpdateTime.get() + updateTime) / 2);
    }
    
    /**
     * 分割列表
     */
    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
    
    // 从原NPCManager复制的方法（简化版本）
    private void loadNPCsFromConfig() {
        ConfigurationSection npcSection = configManager.getConfig().getConfigurationSection("npc.npcs");
        if (npcSection == null) {
            plugin.getLogger().warning("配置文件中未找到NPC配置");
            return;
        }
        
        for (String npcId : npcSection.getKeys(false)) {
            try {
                AIControlledNPC npc = loadNPCFromConfig(npcId, npcSection.getConfigurationSection(npcId));
                if (npc != null) {
                    npcs.put(npcId, npc);
                    spawnNPC(npc);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载NPC " + npcId + " 失败: " + e.getMessage());
            }
        }
    }
    
    private AIControlledNPC loadNPCFromConfig(String npcId, ConfigurationSection config) {
        // 简化的NPC加载逻辑（从原NPCManager复制）
        if (config == null) return null;
        
        String displayName = config.getString("display_name", npcId);
        String entityTypeName = config.getString("entity_type", "VILLAGER");
        String personality = config.getString("personality", "default");
        String apiModel = config.getString("api_model", "api1");
        
        // 解析位置
        String worldName = config.getString("spawn_location.world");
        double x = config.getDouble("spawn_location.x");
        double y = config.getDouble("spawn_location.y");
        double z = config.getDouble("spawn_location.z");
        float yaw = (float) config.getDouble("spawn_location.yaw", 0);
        float pitch = (float) config.getDouble("spawn_location.pitch", 0);
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("世界 " + worldName + " 不存在，跳过NPC " + npcId);
            return null;
        }
        
        Location spawnLocation = new Location(world, x, y, z, yaw, pitch);
        
        // 解析实体类型
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(entityTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的实体类型: " + entityTypeName + "，使用默认类型VILLAGER");
            entityType = EntityType.VILLAGER;
        }
        
        // 构建行为配置
        NPCBehaviorConfig behaviorConfig = buildBehaviorConfig(config.getConfigurationSection("behavior"));
        
        return new AIControlledNPC(npcId, displayName, entityType, personality, apiModel, spawnLocation, behaviorConfig);
    }
    
    private NPCBehaviorConfig buildBehaviorConfig(ConfigurationSection behaviorSection) {
        // 简化的行为配置构建（从原NPCManager复制）
        NPCBehaviorConfig.Builder builder = new NPCBehaviorConfig.Builder();
        
        if (behaviorSection != null) {
            builder.maxWanderDistance(behaviorSection.getDouble("max_wander_distance", 20.0))
                   .interactionRange(behaviorSection.getDouble("interaction_range", 5.0))
                   .movementSpeed(behaviorSection.getDouble("movement_speed", 0.3))
                   .decisionInterval(behaviorSection.getLong("decision_interval", 3000))
                   .maxConversationHistory(behaviorSection.getInt("max_conversation_history", 10));
        }
        
        return builder.build();
    }
    
    @Override
    public boolean spawnNPC(AIControlledNPC npc) {
        // 简化的NPC生成逻辑
        try {
            Location spawnLoc = npc.getSpawnLocation();
            LivingEntity entity = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, npc.getEntityType());

            entity.setCustomName(npc.getDisplayName());
            entity.setCustomNameVisible(true);
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true);

            npc.setEntity(entity);
            npc.setActive(true);
            entityToNPC.put(entity.getUniqueId(), npc);

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("生成NPC失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removeNPC(String npcId) {
        AIControlledNPC npc = npcs.get(npcId);
        if (npc == null) return false;

        if (npc.getEntity() != null) {
            entityToNPC.remove(npc.getEntity().getUniqueId());
            npc.getEntity().remove();
        }

        npcs.remove(npcId);
        return true;
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public Map<String, AIControlledNPC> getNPCs() { return new HashMap<>(npcs); }
    public AIControlledNPC getNPC(String npcId) { return npcs.get(npcId); }
    public AIControlledNPC getNPCByEntity(UUID entityId) { return entityToNPC.get(entityId); }
    public NPCAIDecisionMaker getAIDecisionMaker() { return aiDecisionMaker; }

    @Override
    public List<AIControlledNPC> getNearbyNPCs(Player player, double range) {
        List<AIControlledNPC> nearbyNPCs = new ArrayList<>();
        Location playerLoc = player.getLocation();
        for (AIControlledNPC npc : npcs.values()) {
            if (npc.isActive() && npc.getEntity() != null) {
                if (npc.getCurrentLocation().distance(playerLoc) <= range) {
                    nearbyNPCs.add(npc);
                }
            }
        }
        return nearbyNPCs;
    }
    
    public SmartNPCStats getStats() {
        return new SmartNPCStats(
            totalUpdates.get(),
            avgUpdateTime.get(),
            currentActiveNPCs.get(),
            npcs.size(),
            npcUpdateCache.size()
        );
    }

    @Override
    public void reloadConfig() {
        shutdown();
        initialize();
    }
    
    /**
     * NPC更新信息缓存
     */
    private static class NPCUpdateInfo {
        long lastUpdate;
        boolean isActive;
        int updateCount;
    }
    
    /**
     * 智能NPC统计
     */
    public static class SmartNPCStats {
        private final long totalUpdates;
        private final long avgUpdateTime;
        private final int activeNPCs;
        private final int totalNPCs;
        private final int cacheSize;
        
        public SmartNPCStats(long totalUpdates, long avgUpdateTime, int activeNPCs, int totalNPCs, int cacheSize) {
            this.totalUpdates = totalUpdates;
            this.avgUpdateTime = avgUpdateTime;
            this.activeNPCs = activeNPCs;
            this.totalNPCs = totalNPCs;
            this.cacheSize = cacheSize;
        }
        
        public long getTotalUpdates() { return totalUpdates; }
        public long getAvgUpdateTime() { return avgUpdateTime; }
        public int getActiveNPCs() { return activeNPCs; }
        public int getTotalNPCs() { return totalNPCs; }
        public int getCacheSize() { return cacheSize; }
    }
}
