package cn.zhuobing.testPlugin.game;

import cn.zhuobing.testPlugin.anni.RespawnDataManager;
import cn.zhuobing.testPlugin.boss.BossDataManager;
import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.kit.kits.Berserker;
import cn.zhuobing.testPlugin.map.LobbyManager;
import cn.zhuobing.testPlugin.nexus.NexusInfoBoard;
import cn.zhuobing.testPlugin.specialitem.items.*;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import cn.zhuobing.testPlugin.utils.BungeeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class GamePlayerJoinListener implements Listener {
    private final GameManager gameManager;
    private final LobbyManager lobbyManager;
    private final NexusInfoBoard nexusInfoBoard;
    private final TeamManager teamManager;
    private final RespawnDataManager respawnDataManager;
    private final BossDataManager bossDataManager;
    private final KitManager kitManager;
    private final Plugin plugin;

    /** 全局最大血量：两排心 = 40 滴血 */
    private static final double DEFAULT_MAX_HEALTH = 40.0;

    // 关闭任务变量
    private BukkitTask shutdownTask;

    public GamePlayerJoinListener(LobbyManager lobbyManager, TeamManager teamManager, GameManager gameManager,
                                  NexusInfoBoard nexusInfoBoard, RespawnDataManager respawnDataManager,
                                  BossDataManager bossDataManager, KitManager kitManager, Plugin plugin) {
        this.lobbyManager = lobbyManager;
        this.gameManager = gameManager;
        this.nexusInfoBoard = nexusInfoBoard;
        this.teamManager = teamManager;
        this.respawnDataManager = respawnDataManager;
        this.bossDataManager = bossDataManager;
        this.kitManager = kitManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String joinMessage = ChatColor.GOLD + "[核心战争] " + ChatColor.AQUA + player.getName() + ChatColor.YELLOW + " 加入了游戏！";
        event.setJoinMessage(joinMessage);

        if (teamManager.isInTeam(player)) {
            cancelShutdownTask();
        }

        // 传送、背包、计分板、BossBar、物品发放等全部延后 1 tick，避免阻塞 Join 事件导致进服卡顿
        Bukkit.getScheduler().runTaskLater(plugin, () -> runDeferredJoinLogic(player), 1L);
    }

    /** 延后执行的进服逻辑：传送、清包、发物品、计分板、BossBar、开局检测。 */
    private void runDeferredJoinLogic(Player player) {
        if (!player.isOnline()) return;

        if (!teamManager.isInTeam(player) || !gameManager.isGameStarted()) {
            if (!lobbyManager.teleportToLobby(player)) {
                if (AnniConfigManager.BUNGEE_ENABLED) {
                    BungeeUtil.sendToLobby(player);
                } else {
                    player.kickPlayer(ChatColor.RED + "你已被踢出服务器\n\n" + ChatColor.YELLOW + "大厅传送失败，请联系管理员！");
                }
                return;
            }
            // 清空玩家背包与状态
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.setExp(0);
            player.setMaxHealth(DEFAULT_MAX_HEALTH);
            player.setHealth(DEFAULT_MAX_HEALTH);
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.setExhaustion(0);
            List<PotionEffectType> toRemove = new ArrayList<>();
            for (PotionEffect effect : player.getActivePotionEffects()) {
                if (effect != null && effect.getType() != null) toRemove.add(effect.getType());
            }
            for (PotionEffectType type : toRemove) {
                player.removePotionEffect(type);
            }
            player.setGameMode(GameMode.SURVIVAL);

            player.sendMessage(ChatColor.GOLD + "[核心战争] " + ChatColor.AQUA + "欢迎回到核心战争！");
            player.sendMessage(ChatColor.GOLD + "[核心战争] " + ChatColor.LIGHT_PURPLE + "当前插件正处于测试阶段，游戏内会有少量日志提示");

            Inventory inventory = player.getInventory();
            inventory.setItem(1, TeamSelectorItem.createTeamStar());
            inventory.setItem(2, KitSelectorItem.createKitSelector());
            inventory.setItem(3, GuideBook.createGameGuideBook());
            if (gameManager.getCurrentPhase() == 0) {
                inventory.setItem(4, MapSelectorItem.createMapSelector());
            }
            if (player.isOp()) {
                inventory.setItem(7, MapConfigurerItem.createMapConfigurer());
            }
        }

        // 对局中重连：恢复两排血（40），狂战士由复活时 applyKit 再设
        if (gameManager.isGameStarted() && teamManager.isInTeam(player)) {
            Kit kit = kitManager.getPlayerKit(player.getUniqueId());
            if (kit == null || !(kit instanceof Berserker)) {
                player.setMaxHealth(DEFAULT_MAX_HEALTH);
                player.setHealth(DEFAULT_MAX_HEALTH);
            }
        }

        gameManager.getBossBar().addPlayer(player);
        teamManager.applyScoreboardToPlayer(player);
        nexusInfoBoard.updateInfoBoard();

        int currentPhase = gameManager.getCurrentPhase();
        if (currentPhase != 5 && currentPhase != 0) {
            gameManager.updateBossBar(currentPhase, gameManager.getRemainingTime());
        }

        if (currentPhase == 0) {
            Bukkit.getScheduler().runTaskLater(plugin, gameManager::checkAndStartGame, 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 将退出消息设置为 null，这样就不会有退出信息提示
        event.setQuitMessage(null);

        Player player = event.getPlayer();
        plugin.getLogger().info("[玩家退出] " + player.getName() + " 离开了游戏");
        plugin.getLogger().info("[玩家退出] 队伍状态: " + (teamManager.isInTeam(player) ? "已加入队伍" : "未加入队伍"));

        int currentPhase = gameManager.getCurrentPhase();
        plugin.getLogger().info("[关闭检测] 当前游戏阶段: " + currentPhase);

        // 仅当游戏阶段 >= 3 时才做关服检测（避免大厅/对局中误触关服）
        if (currentPhase >= 3) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cancelShutdownTask();

                boolean anyTeamPlayerOnline = false;
                int onlineCount = 0;
                int teamPlayerCount = 0;

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlineCount++;
                    if (teamManager.isInTeam(onlinePlayer)) {
                        anyTeamPlayerOnline = true;
                        teamPlayerCount++;
                    }
                }

                plugin.getLogger().info("[延迟关闭检测] 在线玩家: " + onlineCount +
                        " | 队伍玩家: " + teamPlayerCount +
                        " | 是否有队伍玩家: " + anyTeamPlayerOnline);

                if (!anyTeamPlayerOnline) {
                    plugin.getLogger().warning("[延迟关闭检测] 没有队伍玩家在线，启动关闭倒计时");
                    startShutdownCountdown();
                } else {
                    plugin.getLogger().info("[延迟关闭检测] 仍有队伍玩家在线，不启动关闭");
                }
            }, 100L); // 延迟100 ticks (5秒) 执行检测
        }

        if(!gameManager.isGameStarted()){
            gameManager.updatePlayerCountOnBossBar(); // 更新 BossBar 上的玩家人数
            gameManager.updateBossBar(gameManager.getCurrentPhase(), gameManager.getRemainingTime());
        }
    }

    /**
     * 启动关闭倒计时
     */
    private void startShutdownCountdown() {
        // 先取消可能存在的关闭任务
        cancelShutdownTask();

        // 创建关闭任务（2分钟倒计时）
        shutdownTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            shutdownTask = null; // 任务已执行，释放引用
            Bukkit.broadcastMessage(ChatColor.RED + "所有队伍玩家已离开，服务器即将关闭! ");
            Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 100L); // 5秒延迟
        }, 2400L); // 2分钟倒计时 (2400 ticks = 120秒)
    }

    /**
     * 取消关闭任务
     */
    private void cancelShutdownTask() {
        if (shutdownTask != null) {
            shutdownTask.cancel();
            shutdownTask = null;
            plugin.getLogger().info("[关闭检测] 已取消关闭任务");
        }
    }
}