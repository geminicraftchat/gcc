package cn.ningmo.geminicraftchat.listeners;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.npc.AIControlledNPC;
import cn.ningmo.geminicraftchat.npc.NPCManager;
import cn.ningmo.geminicraftchat.npc.NpcService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * NPC相关事件监听器
 */
public class NPCListener implements Listener {
    private final GeminiCraftChat plugin;
    private final NpcService npcManager;

    public NPCListener(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.npcManager = plugin.getNpcService();
    }
    
    /**
     * 处理玩家与NPC的交互
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!npcManager.isEnabled()) return;
        
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();
        
        AIControlledNPC npc = npcManager.getNPCByEntity(entity.getUniqueId());
        if (npc == null) return;
        
        event.setCancelled(true);
        
        // 检查玩家是否有权限与NPC交互
        if (!player.hasPermission("gcc.npc.interact")) {
            player.sendMessage("§c你没有权限与NPC交互！");
            return;
        }
        
        // 检查距离
        if (!npc.canInteractWith(player)) {
            player.sendMessage("§c你离 " + npc.getDisplayName() + " 太远了！");
            return;
        }
        
        // 发送交互提示
        player.sendMessage("§a你正在与 " + npc.getDisplayName() + " 交互");
        player.sendMessage("§7在聊天中输入消息来与NPC对话，或使用 §e/gcc npc chat " + npc.getNpcId() + " <消息>§7 命令");
        
        // 设置NPC注意玩家
        npc.setCurrentTarget(player);
        if (npc.getCurrentState() == AIControlledNPC.NPCState.IDLE || 
            npc.getCurrentState() == AIControlledNPC.NPCState.WANDERING) {
            npc.setCurrentState(AIControlledNPC.NPCState.TALKING);
        }
    }
    
    /**
     * 处理NPC受到伤害
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!npcManager.isEnabled()) return;
        
        Entity entity = event.getEntity();
        AIControlledNPC npc = npcManager.getNPCByEntity(entity.getUniqueId());
        if (npc == null) return;
        
        // 如果NPC是无敌的，取消伤害
        if (npc.getBehaviorConfig().isInvulnerable()) {
            event.setCancelled(true);
            return;
        }
        
        // 如果是玩家攻击，检查权限
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
            if (damageEvent.getDamager() instanceof Player) {
                Player attacker = (Player) damageEvent.getDamager();
                
                if (!attacker.hasPermission("gcc.npc.damage")) {
                    event.setCancelled(true);
                    attacker.sendMessage("§c你没有权限伤害NPC！");
                    return;
                }
                
                // 如果NPC不是攻击性的，让它逃跑
                if (!npc.getBehaviorConfig().isAggressive()) {
                    npc.setCurrentState(AIControlledNPC.NPCState.FLEEING);
                    npc.setCurrentTarget(attacker);
                }
            }
        }
    }
    
    /**
     * 处理NPC死亡
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!npcManager.isEnabled()) return;
        
        Entity entity = event.getEntity();
        AIControlledNPC npc = npcManager.getNPCByEntity(entity.getUniqueId());
        if (npc == null) return;
        
        plugin.debug("NPC " + npc.getDisplayName() + " 死亡");
        
        // 清除掉落物（NPC不应该掉落物品）
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // 广播NPC死亡消息（如果配置允许）
        if (plugin.getConfigManager().getConfig().getBoolean("npc.broadcast_death", false)) {
            plugin.getServer().broadcastMessage("§c" + npc.getDisplayName() + " 已死亡，将在 " + 
                (npc.getBehaviorConfig().getRespawnDelay() / 1000) + " 秒后重生");
        }
        
        // NPC将由NPCManager自动重生
    }
    
    /**
     * 处理玩家退出
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!npcManager.isEnabled()) return;
        
        Player player = event.getPlayer();
        
        // 清理所有与该玩家相关的NPC状态
        for (AIControlledNPC npc : npcManager.getNPCs().values()) {
            if (npc.getCurrentTarget() == player) {
                npc.setCurrentTarget(null);
                if (npc.getCurrentState() == AIControlledNPC.NPCState.TALKING ||
                    npc.getCurrentState() == AIControlledNPC.NPCState.FOLLOWING) {
                    npc.setCurrentState(AIControlledNPC.NPCState.IDLE);
                }
            }
        }
    }
}
