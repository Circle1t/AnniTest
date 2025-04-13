package cn.zhuobing.testPlugin.game;

import cn.zhuobing.testPlugin.anniPlayer.RespawnDataManager;
import cn.zhuobing.testPlugin.boss.BossDataManager;
import cn.zhuobing.testPlugin.map.LobbyManager;
import cn.zhuobing.testPlugin.nexus.NexusInfoBoard;
import cn.zhuobing.testPlugin.specialitem.items.KitSelectorItem;
import cn.zhuobing.testPlugin.specialitem.items.MapConfigurerItem;
import cn.zhuobing.testPlugin.specialitem.items.MapSelectorItem;
import cn.zhuobing.testPlugin.specialitem.items.TeamSelectorItem;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class GamePlayerJoinListener implements Listener {
    private final GameManager gameManager;
    private final LobbyManager lobbyManager;
    private final NexusInfoBoard nexusInfoBoard;
    private final TeamManager teamManager;
    private final RespawnDataManager respawnDataManager;
    private final BossDataManager bossDataManager;
    private final Plugin plugin;

    public GamePlayerJoinListener(LobbyManager lobbyManager,TeamManager teamManager, GameManager gameManager, NexusInfoBoard nexusInfoBoard, RespawnDataManager respawnDataManager, BossDataManager bossDataManager,Plugin plugin) {
        this.lobbyManager = lobbyManager;
        this.gameManager = gameManager;
        this.nexusInfoBoard = nexusInfoBoard;
        this.teamManager = teamManager;
        this.respawnDataManager = respawnDataManager;
        this.bossDataManager = bossDataManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 获取玩家对象
        Player player = event.getPlayer();

        // 自定义玩家加入消息
        String joinMessage = ChatColor.GOLD + "[核心战争] " + ChatColor.AQUA + player.getName() + ChatColor.YELLOW + " 加入了游戏！";
        event.setJoinMessage(joinMessage);

        // 检查玩家是否选择了队伍
        if (!teamManager.isInTeam(player) || !gameManager.isGameStarted()) {
            // 玩家没有选择队伍，尝试传送到大厅重生点
            if (!lobbyManager.teleportToLobby(player)) {
                // 大厅传送失败，将玩家踢出服务器并解释原因
                String kickMessage = ChatColor.RED + "你已被踢出服务器\n\n" + ChatColor.YELLOW + "大厅传送失败，请联系管理员！";
                player.kickPlayer(kickMessage);
            } else {
                // 清空玩家背包
                player.getInventory().clear();
                // 清空玩家装备栏
                player.getInventory().setArmorContents(null);

                player.sendMessage(ChatColor.GOLD + "[核心战争] " + ChatColor.AQUA + "欢迎回到核心战争！");
                Inventory inventory = player.getInventory();

                // 未加入队伍的玩家获得特殊选择物品
                ItemStack teamStar = TeamSelectorItem.createTeamStar();
                // 物品栏索引从 0 开始，第二格的索引为 1
                inventory.setItem(1, teamStar);
                // 职业选择物品
                ItemStack kitSelector = KitSelectorItem.createKitSelector();
                inventory.setItem(2, kitSelector);
                // 游戏未开始就给玩家地图选择器
                if(gameManager.getCurrentPhase() == 0){
                    ItemStack mapSelector = MapSelectorItem.createMapSelector();
                    inventory.setItem(3, mapSelector);
                }

                if(player.isOp()){
                    // 地图配置物品
                    ItemStack mapConfigurer = MapConfigurerItem.createMapConfigurer();
                    inventory.setItem(7, mapConfigurer);
                }

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

        if(currentPhase == 0){
            gameManager.checkAndStartGame(); // 检查是否满足启动条件
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 将退出消息设置为 null，这样就不会有退出信息提示
        event.setQuitMessage(null);
        if(!gameManager.isGameStarted()){
            gameManager.updatePlayerCountOnBossBar(); // 更新 BossBar 上的玩家人数
        }
    }
}