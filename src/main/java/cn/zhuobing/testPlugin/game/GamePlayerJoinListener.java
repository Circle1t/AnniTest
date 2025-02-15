package cn.zhuobing.testPlugin.game;

import cn.zhuobing.testPlugin.anniPlayer.RespawnDataManager;
import cn.zhuobing.testPlugin.boss.BossDataManager;
import cn.zhuobing.testPlugin.nexus.NexusInfoBoard;
import cn.zhuobing.testPlugin.specialitem.items.TeamSelectorItem;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GamePlayerJoinListener implements Listener {
    private final GameManager gameManager;
    private final NexusInfoBoard nexusInfoBoard;
    private final TeamManager teamManager;
    private final RespawnDataManager respawnDataManager;
    private final BossDataManager bossDataManager;

    public GamePlayerJoinListener(TeamManager teamManager, GameManager gameManager, NexusInfoBoard nexusInfoBoard, RespawnDataManager respawnDataManager, BossDataManager bossDataManager) {
        this.gameManager = gameManager;
        this.nexusInfoBoard = nexusInfoBoard;
        this.teamManager = teamManager;
        this.respawnDataManager = respawnDataManager;
        this.bossDataManager = bossDataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 获取玩家对象
        Player player = event.getPlayer();

        // 自定义玩家加入消息
        String joinMessage = ChatColor.GOLD + "[核心战争] " + ChatColor.AQUA + player.getName() + ChatColor.YELLOW + " 加入了游戏！";
        event.setJoinMessage(joinMessage);

        // 检查玩家是否选择了队伍
        if (!teamManager.isInTeam(player)) {
            // 玩家没有选择队伍，尝试传送到大厅重生点
            if (!respawnDataManager.teleportPlayerToRandomRespawnLocation(player, "lobby")) {
                player.sendMessage(ChatColor.RED + "未设置大厅重生点，无法传送到大厅！");
            } else {
                player.sendMessage(ChatColor.GOLD + "[核心战争] " + ChatColor.AQUA + "欢迎回到核心战争！");
                // 未加入队伍的玩家获得团队选择物品
                Inventory inventory = player.getInventory();
                ItemStack teamStar = TeamSelectorItem.createTeamStar();
                // 物品栏索引从 0 开始，第二格的索引为 1
                inventory.setItem(1, teamStar);
            }
        }

        // 设置计分板 BossBar 事项
        gameManager.getBossBar().addPlayer(player);
        bossDataManager.clearOriginalBossBar();
        teamManager.applyScoreboardToPlayer(player);
        nexusInfoBoard.updateInfoBoard();
        int currentPhase = gameManager.getCurrentPhase();
        if (currentPhase != 5 && currentPhase != 0) {
            gameManager.updateBossBar(currentPhase, gameManager.getRemainingTime());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 将退出消息设置为 null，这样就不会有退出信息提示
        event.setQuitMessage(null);
    }

}