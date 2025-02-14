package cn.zhuobing.testPlugin.anniPlayer;

import cn.zhuobing.testPlugin.ore.OreType;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PlayerListener implements Listener {
    private final TeamManager teamManager;
    private final Set<Material> prohibitedMaterials;

    public PlayerListener(TeamManager teamManager) {
        this.teamManager = teamManager;
        // 初始化禁止放置的方块集合
        this.prohibitedMaterials = new HashSet<>();
        // 遍历 OreType 枚举
        for (OreType oreType : OreType.values()) {
            // 排除羊毛类和泥土类
            if (oreType != OreType.WOOL && oreType != OreType.DIRT) {
                // 将对应方块类型添加到禁止集合中
                prohibitedMaterials.addAll(Arrays.asList(oreType.getSourceBlocks()));
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // 检查攻击者和被攻击者是否都是玩家
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            // 获取攻击者和被攻击者所在的队伍名称
            String attackerTeamName = teamManager.getPlayerTeamName(attacker);
            String victimTeamName = teamManager.getPlayerTeamName(victim);

            // 检查攻击者和被攻击者是否属于同一队伍
            if (attackerTeamName != null && attackerTeamName.equals(victimTeamName)) {
                // 阻止攻击行为
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material placedMaterial = event.getBlock().getType();

        // 检查放置的方块是否在禁止放置的方块集合中
        if (prohibitedMaterials.contains(placedMaterial)) {
            // 取消方块放置事件
            event.setCancelled(true);
        }
    }
}