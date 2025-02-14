package cn.zhuobing.testPlugin.anniPlayer;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.specialitem.items.TeamSelectorItem;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class PlayerRespawnListener implements Listener {
    private final TeamManager teamManager;
    private final RespawnDataManager respawnDataManager;
    private final GameManager gameManager;
    private final NexusManager nexusManager;
    private final Plugin plugin;
    int currentPhase = 0;

    public PlayerRespawnListener(TeamManager teamManager, RespawnDataManager respawnDataManager, GameManager gameManager, NexusManager nexusManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.respawnDataManager = respawnDataManager;
        this.gameManager = gameManager;
        this.nexusManager = nexusManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String teamName = teamManager.getPlayerTeamName(player);
        currentPhase = gameManager.getCurrentPhase();

        //游戏还未开始，死亡后自动到大厅复活
        if(currentPhase < 1) {
            // 取消默认的死亡界面，让玩家自动复活
            event.setKeepInventory(false);
            event.setKeepLevel(false);
            event.setDroppedExp(0);

            // 让玩家在死亡后立即自动复活
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isDead()) {
                        player.spigot().respawn();
                    }
                }
            }.runTaskLater(plugin, 1L); // 1 tick 后执行
            return;
        }

        // 取消默认的死亡界面，让玩家自动复活
        event.setKeepInventory(false);
        event.setKeepLevel(false);
        event.setDroppedExp(0);

        // 让玩家在死亡后立即自动复活
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (player.isDead()) {
                    player.spigot().respawn();
                }
            }
        }.runTaskLater(plugin, 1L); // 1 tick 后执行
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String teamName = teamManager.getPlayerTeamName(player);
        currentPhase = gameManager.getCurrentPhase();

        // 游戏未开始，自动在大厅复活
        if(currentPhase < 1) {
            respawnDataManager.handlePlayerRespawn(player, null, event);
            // 获得初始物品
            // 团队选择之星
            Inventory inventory = player.getInventory();
            ItemStack teamStar = TeamSelectorItem.createTeamStar();
            // 物品栏索引从 0 开始，第二格的索引为 1
            inventory.setItem(1, teamStar);
            return;
        }

        // 核心已被摧毁
        if(nexusManager.getNexusHealth(teamName) <= 0) {
            respawnDataManager.handlePlayerRespawn(player, null, event);
        }

        respawnDataManager.handlePlayerRespawn(player, teamName, event);
    }
}