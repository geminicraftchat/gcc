package cn.ningmo.geminicraftchat.npc;

import java.util.List;
import java.util.Map;

/**
 * NPC行为配置类
 * 定义NPC的各种行为参数和限制
 */
public class NPCBehaviorConfig {
    private final double maxWanderDistance;
    private final double interactionRange;
    private final double movementSpeed;
    private final long decisionInterval;
    private final int maxConversationHistory;
    private final boolean canFly;
    private final boolean canSwim;
    private final boolean canClimb;
    private final boolean isAggressive;
    private final boolean canTeleport;
    private final List<String> allowedBiomes;
    private final Map<String, Double> behaviorWeights;
    private final List<String> avoidedBlocks;
    private final int maxHealth;
    private final boolean invulnerable;
    private final long respawnDelay;
    private final boolean persistentMemory;
    
    // 时间相关配置
    private final boolean activeAtNight;
    private final boolean activeAtDay;
    private final boolean sleepAtNight;
    
    // 社交配置
    private final boolean friendlyToPlayers;
    private final boolean canFollowPlayers;
    private final double followDistance;
    private final long maxFollowTime;
    
    // AI决策权重
    private final double idleWeight;
    private final double wanderWeight;
    private final double followWeight;
    private final double fleeWeight;
    private final double exploreWeight;
    
    public NPCBehaviorConfig(Builder builder) {
        this.maxWanderDistance = builder.maxWanderDistance;
        this.interactionRange = builder.interactionRange;
        this.movementSpeed = builder.movementSpeed;
        this.decisionInterval = builder.decisionInterval;
        this.maxConversationHistory = builder.maxConversationHistory;
        this.canFly = builder.canFly;
        this.canSwim = builder.canSwim;
        this.canClimb = builder.canClimb;
        this.isAggressive = builder.isAggressive;
        this.canTeleport = builder.canTeleport;
        this.allowedBiomes = builder.allowedBiomes;
        this.behaviorWeights = builder.behaviorWeights;
        this.avoidedBlocks = builder.avoidedBlocks;
        this.maxHealth = builder.maxHealth;
        this.invulnerable = builder.invulnerable;
        this.respawnDelay = builder.respawnDelay;
        this.persistentMemory = builder.persistentMemory;
        this.activeAtNight = builder.activeAtNight;
        this.activeAtDay = builder.activeAtDay;
        this.sleepAtNight = builder.sleepAtNight;
        this.friendlyToPlayers = builder.friendlyToPlayers;
        this.canFollowPlayers = builder.canFollowPlayers;
        this.followDistance = builder.followDistance;
        this.maxFollowTime = builder.maxFollowTime;
        this.idleWeight = builder.idleWeight;
        this.wanderWeight = builder.wanderWeight;
        this.followWeight = builder.followWeight;
        this.fleeWeight = builder.fleeWeight;
        this.exploreWeight = builder.exploreWeight;
    }
    
    // Getters
    public double getMaxWanderDistance() { return maxWanderDistance; }
    public double getInteractionRange() { return interactionRange; }
    public double getMovementSpeed() { return movementSpeed; }
    public long getDecisionInterval() { return decisionInterval; }
    public int getMaxConversationHistory() { return maxConversationHistory; }
    public boolean canFly() { return canFly; }
    public boolean canSwim() { return canSwim; }
    public boolean canClimb() { return canClimb; }
    public boolean isAggressive() { return isAggressive; }
    public boolean canTeleport() { return canTeleport; }
    public List<String> getAllowedBiomes() { return allowedBiomes; }
    public Map<String, Double> getBehaviorWeights() { return behaviorWeights; }
    public List<String> getAvoidedBlocks() { return avoidedBlocks; }
    public int getMaxHealth() { return maxHealth; }
    public boolean isInvulnerable() { return invulnerable; }
    public long getRespawnDelay() { return respawnDelay; }
    public boolean hasPersistentMemory() { return persistentMemory; }
    public boolean isActiveAtNight() { return activeAtNight; }
    public boolean isActiveAtDay() { return activeAtDay; }
    public boolean sleepsAtNight() { return sleepAtNight; }
    public boolean isFriendlyToPlayers() { return friendlyToPlayers; }
    public boolean canFollowPlayers() { return canFollowPlayers; }
    public double getFollowDistance() { return followDistance; }
    public long getMaxFollowTime() { return maxFollowTime; }
    public double getIdleWeight() { return idleWeight; }
    public double getWanderWeight() { return wanderWeight; }
    public double getFollowWeight() { return followWeight; }
    public double getFleeWeight() { return fleeWeight; }
    public double getExploreWeight() { return exploreWeight; }
    
    /**
     * 获取行为权重
     */
    public double getBehaviorWeight(String behavior) {
        return behaviorWeights.getOrDefault(behavior, 1.0);
    }
    
    /**
     * 检查是否应该在当前时间活跃
     */
    public boolean shouldBeActiveAtTime(long worldTime) {
        boolean isNight = worldTime >= 13000 && worldTime <= 23000;
        
        if (isNight) {
            return activeAtNight && !sleepAtNight;
        } else {
            return activeAtDay;
        }
    }
    
    /**
     * Builder模式构建配置
     */
    public static class Builder {
        private double maxWanderDistance = 20.0;
        private double interactionRange = 5.0;
        private double movementSpeed = 0.3;
        private long decisionInterval = 3000; // 3秒
        private int maxConversationHistory = 10;
        private boolean canFly = false;
        private boolean canSwim = true;
        private boolean canClimb = false;
        private boolean isAggressive = false;
        private boolean canTeleport = false;
        private List<String> allowedBiomes = new java.util.ArrayList<>();
        private Map<String, Double> behaviorWeights = new java.util.HashMap<>();
        private List<String> avoidedBlocks = new java.util.ArrayList<>();
        private int maxHealth = 20;
        private boolean invulnerable = false;
        private long respawnDelay = 30000; // 30秒
        private boolean persistentMemory = true;
        private boolean activeAtNight = true;
        private boolean activeAtDay = true;
        private boolean sleepAtNight = false;
        private boolean friendlyToPlayers = true;
        private boolean canFollowPlayers = true;
        private double followDistance = 3.0;
        private long maxFollowTime = 300000; // 5分钟
        private double idleWeight = 1.0;
        private double wanderWeight = 1.0;
        private double followWeight = 1.5;
        private double fleeWeight = 0.5;
        private double exploreWeight = 0.8;
        
        public Builder maxWanderDistance(double distance) { this.maxWanderDistance = distance; return this; }
        public Builder interactionRange(double range) { this.interactionRange = range; return this; }
        public Builder movementSpeed(double speed) { this.movementSpeed = speed; return this; }
        public Builder decisionInterval(long interval) { this.decisionInterval = interval; return this; }
        public Builder maxConversationHistory(int max) { this.maxConversationHistory = max; return this; }
        public Builder canFly(boolean fly) { this.canFly = fly; return this; }
        public Builder canSwim(boolean swim) { this.canSwim = swim; return this; }
        public Builder canClimb(boolean climb) { this.canClimb = climb; return this; }
        public Builder isAggressive(boolean aggressive) { this.isAggressive = aggressive; return this; }
        public Builder canTeleport(boolean teleport) { this.canTeleport = teleport; return this; }
        public Builder allowedBiomes(List<String> biomes) { this.allowedBiomes = biomes; return this; }
        public Builder behaviorWeights(Map<String, Double> weights) { this.behaviorWeights = weights; return this; }
        public Builder avoidedBlocks(List<String> blocks) { this.avoidedBlocks = blocks; return this; }
        public Builder maxHealth(int health) { this.maxHealth = health; return this; }
        public Builder invulnerable(boolean invul) { this.invulnerable = invul; return this; }
        public Builder respawnDelay(long delay) { this.respawnDelay = delay; return this; }
        public Builder persistentMemory(boolean persistent) { this.persistentMemory = persistent; return this; }
        public Builder activeAtNight(boolean active) { this.activeAtNight = active; return this; }
        public Builder activeAtDay(boolean active) { this.activeAtDay = active; return this; }
        public Builder sleepAtNight(boolean sleep) { this.sleepAtNight = sleep; return this; }
        public Builder friendlyToPlayers(boolean friendly) { this.friendlyToPlayers = friendly; return this; }
        public Builder canFollowPlayers(boolean follow) { this.canFollowPlayers = follow; return this; }
        public Builder followDistance(double distance) { this.followDistance = distance; return this; }
        public Builder maxFollowTime(long time) { this.maxFollowTime = time; return this; }
        public Builder idleWeight(double weight) { this.idleWeight = weight; return this; }
        public Builder wanderWeight(double weight) { this.wanderWeight = weight; return this; }
        public Builder followWeight(double weight) { this.followWeight = weight; return this; }
        public Builder fleeWeight(double weight) { this.fleeWeight = weight; return this; }
        public Builder exploreWeight(double weight) { this.exploreWeight = weight; return this; }
        
        public NPCBehaviorConfig build() {
            return new NPCBehaviorConfig(this);
        }
    }
}
