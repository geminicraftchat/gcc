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
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC管理器
 * 负责管理所有AI控制的NPC
 */
public class NPCManager implements NpcService {
    private final GeminiCraftChat plugin;
    private final ConfigManager configManager;
    private final Map<String, AIControlledNPC> npcs;
    private final Map<UUID, AIControlledNPC> entityToNPC;
    private final NPCAIDecisionMaker aiDecisionMaker;
    private BukkitTask aiUpdateTask;
    private boolean enabled;
    
    public NPCManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.npcs = new ConcurrentHashMap<>();
        this.entityToNPC = new ConcurrentHashMap<>();
        this.aiDecisionMaker = new NPCAIDecisionMaker(plugin);
        this.enabled = false;
    }
    
    /**
     * 初始化NPC管理器
     */
    public void initialize() {
        if (!configManager.getConfig().getBoolean("npc.enabled", false)) {
            plugin.getLogger().info("NPC功能已禁用");
            return;
        }
        
        this.enabled = true;
        loadNPCsFromConfig();
        startAIUpdateTask();
        
        plugin.getLogger().info("NPC管理器已初始化，加载了 " + npcs.size() + " 个NPC");
    }
    
    /**
     * 关闭NPC管理器
     */
    public void shutdown() {
        if (aiUpdateTask != null) {
            aiUpdateTask.cancel();
            aiUpdateTask = null;
        }
        
        // 移除所有NPC实体
        for (AIControlledNPC npc : npcs.values()) {
            if (npc.getEntity() != null) {
                npc.getEntity().remove();
            }
        }
        
        npcs.clear();
        entityToNPC.clear();
        enabled = false;
        
        plugin.getLogger().info("NPC管理器已关闭");
    }
    
    /**
     * 从配置文件加载NPC
     */
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
    
    /**
     * 从配置加载单个NPC
     */
    private AIControlledNPC loadNPCFromConfig(String npcId, ConfigurationSection config) {
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
    
    /**
     * 构建行为配置
     */
    private NPCBehaviorConfig buildBehaviorConfig(ConfigurationSection behaviorSection) {
        NPCBehaviorConfig.Builder builder = new NPCBehaviorConfig.Builder();
        
        if (behaviorSection != null) {
            builder.maxWanderDistance(behaviorSection.getDouble("max_wander_distance", 20.0))
                   .interactionRange(behaviorSection.getDouble("interaction_range", 5.0))
                   .movementSpeed(behaviorSection.getDouble("movement_speed", 0.3))
                   .decisionInterval(behaviorSection.getLong("decision_interval", 3000))
                   .maxConversationHistory(behaviorSection.getInt("max_conversation_history", 10))
                   .canFly(behaviorSection.getBoolean("can_fly", false))
                   .canSwim(behaviorSection.getBoolean("can_swim", true))
                   .canClimb(behaviorSection.getBoolean("can_climb", false))
                   .isAggressive(behaviorSection.getBoolean("is_aggressive", false))
                   .canTeleport(behaviorSection.getBoolean("can_teleport", false))
                   .maxHealth(behaviorSection.getInt("max_health", 20))
                   .invulnerable(behaviorSection.getBoolean("invulnerable", false))
                   .respawnDelay(behaviorSection.getLong("respawn_delay", 30000))
                   .persistentMemory(behaviorSection.getBoolean("persistent_memory", true))
                   .activeAtNight(behaviorSection.getBoolean("active_at_night", true))
                   .activeAtDay(behaviorSection.getBoolean("active_at_day", true))
                   .sleepAtNight(behaviorSection.getBoolean("sleep_at_night", false))
                   .friendlyToPlayers(behaviorSection.getBoolean("friendly_to_players", true))
                   .canFollowPlayers(behaviorSection.getBoolean("can_follow_players", true))
                   .followDistance(behaviorSection.getDouble("follow_distance", 3.0))
                   .maxFollowTime(behaviorSection.getLong("max_follow_time", 300000));
            
            // 加载行为权重
            ConfigurationSection weightsSection = behaviorSection.getConfigurationSection("behavior_weights");
            if (weightsSection != null) {
                Map<String, Double> weights = new HashMap<>();
                for (String key : weightsSection.getKeys(false)) {
                    weights.put(key, weightsSection.getDouble(key));
                }
                builder.behaviorWeights(weights);
            }
            
            // 加载避免的方块
            List<String> avoidedBlocks = behaviorSection.getStringList("avoided_blocks");
            builder.avoidedBlocks(avoidedBlocks);
            
            // 加载允许的生物群系
            List<String> allowedBiomes = behaviorSection.getStringList("allowed_biomes");
            builder.allowedBiomes(allowedBiomes);
        }
        
        return builder.build();
    }
    
    /**
     * 生成NPC
     */
    public boolean spawnNPC(AIControlledNPC npc) {
        try {
            Location spawnLoc = npc.getSpawnLocation();
            LivingEntity entity = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, npc.getEntityType());
            
            // 设置NPC属性
            entity.setCustomName(npc.getDisplayName());
            entity.setCustomNameVisible(true);
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true);
            
            if (npc.getBehaviorConfig().isInvulnerable()) {
                entity.setInvulnerable(true);
            }
            
            entity.setMaxHealth(npc.getBehaviorConfig().getMaxHealth());
            entity.setHealth(entity.getMaxHealth());
            
            npc.setEntity(entity);
            npc.setActive(true);
            entityToNPC.put(entity.getUniqueId(), npc);
            
            plugin.debug("成功生成NPC: " + npc.getDisplayName() + " 在 " + spawnLoc);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("生成NPC失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 移除NPC
     */
    public boolean removeNPC(String npcId) {
        AIControlledNPC npc = npcs.get(npcId);
        if (npc == null) return false;
        
        if (npc.getEntity() != null) {
            entityToNPC.remove(npc.getEntity().getUniqueId());
            npc.getEntity().remove();
        }
        
        npcs.remove(npcId);
        plugin.debug("移除NPC: " + npcId);
        return true;
    }
    
    /**
     * 启动AI更新任务
     */
    private void startAIUpdateTask() {
        long updateInterval = configManager.getConfig().getLong("npc.ai_update_interval", 20); // 1秒
        
        aiUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllNPCs();
            }
        }.runTaskTimer(plugin, 0L, updateInterval);
    }
    
    /**
     * 更新所有NPC
     */
    private void updateAllNPCs() {
        for (AIControlledNPC npc : npcs.values()) {
            if (npc.isActive() && npc.getEntity() != null && !npc.getEntity().isDead()) {
                updateNPC(npc);
            } else if (npc.getEntity() == null || npc.getEntity().isDead()) {
                // 重新生成死亡的NPC
                respawnNPC(npc);
            }
        }
    }
    
    /**
     * 更新单个NPC
     */
    private void updateNPC(AIControlledNPC npc) {
        try {
            // 检查是否应该在当前时间活跃
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
            
        } catch (Exception e) {
            plugin.debug("更新NPC " + npc.getNpcId() + " 时出错: " + e.getMessage());
        }
    }
    
    /**
     * 重新生成NPC
     */
    private void respawnNPC(AIControlledNPC npc) {
        if (npc.getEntity() != null) {
            entityToNPC.remove(npc.getEntity().getUniqueId());
        }
        
        // 延迟重生
        new BukkitRunnable() {
            @Override
            public void run() {
                spawnNPC(npc);
            }
        }.runTaskLater(plugin, npc.getBehaviorConfig().getRespawnDelay() / 50); // 转换为tick
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public Map<String, AIControlledNPC> getNPCs() { return new HashMap<>(npcs); }
    public AIControlledNPC getNPC(String npcId) { return npcs.get(npcId); }
    public AIControlledNPC getNPCByEntity(UUID entityId) { return entityToNPC.get(entityId); }
    public NPCAIDecisionMaker getAIDecisionMaker() { return aiDecisionMaker; }
    
    /**
     * 获取玩家附近的NPC
     */
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
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        shutdown();
        initialize();
    }

    /**
     * 处理玩家与NPC的对话
     */
    public void handlePlayerChat(Player player, AIControlledNPC npc, String message) {
        if (!npc.canInteractWith(player)) {
            player.sendMessage("§c你离 " + npc.getDisplayName() + " 太远了！");
            return;
        }

        // 设置NPC状态为对话中
        npc.setCurrentState(AIControlledNPC.NPCState.TALKING);
        npc.setCurrentTarget(player);

        // 委托给AI决策器处理对话
        aiDecisionMaker.handleConversation(npc, player, message);
    }
}
