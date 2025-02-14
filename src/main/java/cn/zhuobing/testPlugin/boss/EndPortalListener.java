package cn.zhuobing.testPlugin.boss;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.HashSet;
import java.util.Set;

public class EndPortalListener implements Listener {
    private final TeamManager teamManager;
    private final BossDataManager bossDataManager;
    private final GameManager gameManager;

    public EndPortalListener(TeamManager teamManager, BossDataManager bossDataManager, GameManager gameManager) {
        this.teamManager = teamManager;
        this.bossDataManager = bossDataManager;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() == TeleportCause.END_PORTAL) {
            // 检查当前游戏阶段是否为第四阶段
            if (gameManager.getCurrentPhase() < 4) {
                event.setCancelled(true);
                return;
            }

            Player player = event.getPlayer();
            String teamName = teamManager.getPlayerTeamName(player);

            if (teamName == null) {
                event.setCancelled(true);
                return;
            }

            Location bossLocation = bossDataManager.getBossLocation(teamName);
            if (bossLocation == null) {
                player.sendMessage(ChatColor.RED + "你所在队伍的Boss点尚未设置！");
                event.setCancelled(true);
                return;
            }

            // 设置传送位置并取消原事件
            event.setTo(bossLocation);
            //将玩家添加到boss点玩家集合中
            bossDataManager.addBossPlayer(player);
            event.setCancelled(false); // 允许传送

            // 将成功进入传送门的玩家添加到哈希集合中
            bossDataManager.addPlayerWhoEnteredPortal(player);
        }
    }

}