package cn.ningmo.geminicraftchat.npc;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NpcService {
    boolean isEnabled();
    Map<String, AIControlledNPC> getNPCs();
    AIControlledNPC getNPC(String npcId);
    AIControlledNPC getNPCByEntity(UUID entityId);
    List<AIControlledNPC> getNearbyNPCs(Player player, double range);
    boolean spawnNPC(AIControlledNPC npc);
    boolean removeNPC(String npcId);
    void reloadConfig();
    void handlePlayerChat(Player player, AIControlledNPC npc, String message);
}
