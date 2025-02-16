package cn.zhuobing.testPlugin.boss;

import cn.zhuobing.testPlugin.AnniTest;
import cn.zhuobing.testPlugin.anniPlayer.RespawnDataManager;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

import java.util.Comparator;
import java.util.Map;

public class HellPortalListener implements Listener {
    private final TeamManager teamManager;
    private final NexusManager nexusManager;
    private final RespawnDataManager respawnDataManager;
    private final BossDataManager bossDataManager;
    private final KitManager kitManager;

    public HellPortalListener(TeamManager teamManager, NexusManager nexusManager, RespawnDataManager respawnDataManager, BossDataManager bossDataManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.nexusManager = nexusManager;
        this.respawnDataManager = respawnDataManager;
        this.bossDataManager = bossDataManager;
        this.kitManager = kitManager;
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() == org.bukkit.event.player.PlayerPortalEvent.TeleportCause.NETHER_PORTAL) {
            Player player = event.getPlayer();
            String playerTeam = teamManager.getPlayerTeamName(player);

            // 如果玩家在boss点
            if (bossDataManager.isPlayerInBoss(player)) {
                respawnDataManager.teleportPlayerToRandomRespawnLocation(player, playerTeam);
                bossDataManager.removeBossPlayer(player);
                event.setCancelled(true);
                return;
            }

            if (playerTeam != null) {
                // 获取距离玩家最近的核心
                String nearestNexusTeam = getNearestNexusTeam(player.getLocation());
                if (nearestNexusTeam != null && nearestNexusTeam.equals(playerTeam)) {
                    respawnDataManager.teleportPlayerToRandomRespawnLocation(player, playerTeam);
                    player.sendMessage(ChatColor.RED + "战场上改变职业会受到死亡惩罚！");
                    // 延迟 2 tick 后打开职业选择界面
                    Bukkit.getScheduler().runTaskLater(AnniTest.getInstance(), () -> {
                        kitManager.openKitSelection(player);
                    }, 2L);
                }
            }
            event.setCancelled(true); // 取消默认的地狱门传送事件
        }
    }

    private String getNearestNexusTeam(Location playerLocation) {
        Map<String, Location> nexusLocations = nexusManager.getNexusLocations();
        if (nexusLocations.isEmpty()) {
            return null;
        }

        return nexusLocations.entrySet().stream()
                .min(Comparator.comparingDouble(entry -> entry.getValue().distance(playerLocation)))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}