package cn.ningmo.geminicraftchat.npc;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI控制的NPC实体
 * 支持AI驱动的移动、对话和行为
 */
public class AIControlledNPC {
    private final String npcId;
    private final String displayName;
    private final EntityType entityType;
    private final String personality;
    private final String apiModel;
    private final Location spawnLocation;
    private final NPCBehaviorConfig behaviorConfig;
    
    private LivingEntity entity;
    private boolean isActive;
    private long lastActionTime;
    private NPCState currentState;
    private Location targetLocation;
    private Player currentTarget;
    
    // 对话历史 - 每个玩家独立的对话记录
    private final Map<UUID, List<String>> conversationHistory;
    
    // NPC状态枚举
    public enum NPCState {
        IDLE,           // 空闲状态
        WANDERING,      // 随机游荡
        FOLLOWING,      // 跟随玩家
        TALKING,        // 对话中
        MOVING_TO,      // 移动到指定位置
        FLEEING,        // 逃跑
        ATTACKING,      // 攻击（如果配置允许）
        SLEEPING,       // 休息状态
        PATROLLING      // 巡逻
    }
    
    public AIControlledNPC(String npcId, String displayName, EntityType entityType, 
                          String personality, String apiModel, Location spawnLocation,
                          NPCBehaviorConfig behaviorConfig) {
        this.npcId = npcId;
        this.displayName = displayName;
        this.entityType = entityType;
        this.personality = personality;
        this.apiModel = apiModel;
        this.spawnLocation = spawnLocation.clone();
        this.behaviorConfig = behaviorConfig;
        this.conversationHistory = new ConcurrentHashMap<>();
        this.currentState = NPCState.IDLE;
        this.isActive = false;
        this.lastActionTime = System.currentTimeMillis();
    }
    
    // Getters
    public String getNpcId() { return npcId; }
    public String getDisplayName() { return displayName; }
    public EntityType getEntityType() { return entityType; }
    public String getPersonality() { return personality; }
    public String getApiModel() { return apiModel; }
    public Location getSpawnLocation() { return spawnLocation; }
    public NPCBehaviorConfig getBehaviorConfig() { return behaviorConfig; }
    public LivingEntity getEntity() { return entity; }
    public boolean isActive() { return isActive; }
    public NPCState getCurrentState() { return currentState; }
    public Location getTargetLocation() { return targetLocation; }
    public Player getCurrentTarget() { return currentTarget; }
    
    // Setters
    public void setEntity(LivingEntity entity) { this.entity = entity; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setCurrentState(NPCState state) { 
        this.currentState = state;
        this.lastActionTime = System.currentTimeMillis();
    }
    public void setTargetLocation(Location location) { this.targetLocation = location; }
    public void setCurrentTarget(Player player) { this.currentTarget = player; }
    
    /**
     * 获取玩家的对话历史
     */
    public List<String> getConversationHistory(UUID playerId) {
        return conversationHistory.get(playerId);
    }
    
    /**
     * 添加对话记录
     */
    public void addConversationHistory(UUID playerId, String message) {
        conversationHistory.computeIfAbsent(playerId, k -> new java.util.ArrayList<>()).add(message);
        
        // 限制历史记录长度
        List<String> history = conversationHistory.get(playerId);
        if (history.size() > behaviorConfig.getMaxConversationHistory()) {
            history.remove(0);
        }
    }
    
    /**
     * 清除玩家的对话历史
     */
    public void clearConversationHistory(UUID playerId) {
        conversationHistory.remove(playerId);
    }
    
    /**
     * 检查是否需要执行AI决策
     */
    public boolean shouldMakeDecision() {
        long currentTime = System.currentTimeMillis();
        return currentTime - lastActionTime >= behaviorConfig.getDecisionInterval();
    }
    
    /**
     * 获取当前位置
     */
    public Location getCurrentLocation() {
        return entity != null ? entity.getLocation() : spawnLocation;
    }
    
    /**
     * 检查NPC是否在有效范围内
     */
    public boolean isInValidRange() {
        if (entity == null) return false;
        
        Location current = entity.getLocation();
        double maxDistance = behaviorConfig.getMaxWanderDistance();
        
        return spawnLocation.distance(current) <= maxDistance;
    }
    
    /**
     * 获取附近的玩家
     */
    public List<Player> getNearbyPlayers() {
        if (entity == null) return new java.util.ArrayList<>();
        
        return entity.getLocation().getWorld().getPlayers().stream()
            .filter(player -> player.getLocation().distance(entity.getLocation()) <= behaviorConfig.getInteractionRange())
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 检查是否可以与玩家互动
     */
    public boolean canInteractWith(Player player) {
        if (entity == null || player == null) return false;
        
        double distance = entity.getLocation().distance(player.getLocation());
        return distance <= behaviorConfig.getInteractionRange();
    }
    
    /**
     * 重置到出生点
     */
    public void resetToSpawn() {
        if (entity != null) {
            entity.teleport(spawnLocation);
            setCurrentState(NPCState.IDLE);
            setTargetLocation(null);
            setCurrentTarget(null);
        }
    }
    
    /**
     * 获取NPC信息摘要
     */
    public String getInfoSummary() {
        return String.format("NPC[%s] %s (%s) - State: %s, Active: %s", 
            npcId, displayName, entityType.name(), currentState, isActive);
    }
    
    @Override
    public String toString() {
        return String.format("AIControlledNPC{id='%s', name='%s', type=%s, state=%s}", 
            npcId, displayName, entityType, currentState);
    }
}
