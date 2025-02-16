package cn.zhuobing.testPlugin.anniPlayer;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.ore.OreType;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final TeamManager teamManager;
    private final GameManager gameManager;
    private final KitManager kitManager;
    private final Set<Material> prohibitedMaterials;

    public PlayerListener(TeamManager teamManager, GameManager gameManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.kitManager = kitManager;
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

            // 游戏未开始，未开启pvp
            if(gameManager.getCurrentPhase() < 1){
                // 阻止攻击行为
                event.setCancelled(true);
                return;
            }
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            // 获取攻击者和被攻击者所在的队伍名称
            String attackerTeamName = teamManager.getPlayerTeamName(attacker);
            String victimTeamName = teamManager.getPlayerTeamName(victim);

            //不允许没有队伍的玩家攻击/被攻击
            if(attackerTeamName == null || victimTeamName == null) {
                event.setCancelled(true);
                return;
            }

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            // 玩家被其他玩家杀死
            String victimTeamName = teamManager.getPlayerTeamName(victim);
            String killerTeamName = teamManager.getPlayerTeamName(killer);

            ChatColor victimColor = teamManager.getTeamColor(victimTeamName);
            ChatColor killerColor = teamManager.getTeamColor(killerTeamName);

            double killerHealth = killer.getHealth();
            // 使用 String.format 方法格式化健康值为保留两位小数的字符串
            String formattedHealth = String.format("%.2f", killerHealth);

            String deathMessage = victimColor + victim.getName() + ChatColor.GRAY + " 被击杀因为 " +
                    killerColor + killer.getName() + "[" + ChatColor.GOLD + formattedHealth + ChatColor.RED + "❤" + killerColor + "]("
                    + ChatColor.LIGHT_PURPLE + kitManager.getPlayerKitName(killer.getUniqueId()) + killerColor + ")";
            event.getEntity().getServer().broadcastMessage(deathMessage);
        }
        // 取消默认的死亡信息显示
        event.setDeathMessage(null);
    }
}