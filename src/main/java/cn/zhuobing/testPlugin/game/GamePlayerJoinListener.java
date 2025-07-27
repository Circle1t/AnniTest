package cn.zhuobing.testPlugin.game;

import cn.zhuobing.testPlugin.anni.RespawnDataManager;
import cn.zhuobing.testPlugin.boss.BossDataManager;
import cn.zhuobing.testPlugin.map.LobbyManager;
import cn.zhuobing.testPlugin.nexus.NexusInfoBoard;
import cn.zhuobing.testPlugin.specialitem.items.KitSelectorItem;
import cn.zhuobing.testPlugin.specialitem.items.MapConfigurerItem;
import cn.zhuobing.testPlugin.specialitem.items.MapSelectorItem;
import cn.zhuobing.testPlugin.specialitem.items.TeamSelectorItem;
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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class GamePlayerJoinListener implements Listener {
    private final GameManager gameManager;
    private final LobbyManager lobbyManager;
    private final NexusInfoBoard nexusInfoBoard;
    private final TeamManager teamManager;
    private final RespawnDataManager respawnDataManager;
    private final BossDataManager bossDataManager;
    private final Plugin plugin;

    // 关闭任务变量
    private BukkitTask shutdownTask;

    public GamePlayerJoinListener(LobbyManager lobbyManager,TeamManager teamManager, GameManager gameManager,
                                  NexusInfoBoard nexusInfoBoard, RespawnDataManager respawnDataManager,
                                  BossDataManager bossDataManager,Plugin plugin) {
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
                if (AnniConfigManager.BUNGEE_ENABLED) {
                    BungeeUtil.sendToLobby(player);
                } else {
                    String kickMessage = ChatColor.RED + "你已被踢出服务器\n\n" + ChatColor.YELLOW + "大厅传送失败，请联系管理员！";
                    player.kickPlayer(kickMessage);
                }
            } else {
                // 清空玩家背包
                player.getInventory().clear();
                // 清空玩家装备栏
                player.getInventory().setArmorContents(null);
                // 设置玩家经验为0
                player.setExp(0);
                // 设置血量为满
                player.setMaxHealth(20.0);
                player.setHealth(20.0);
                // 设置饥饿值为满
                player.setFoodLevel(20);
                // 防止饥饿值降低
                player.setSaturation(20);
                player.setExhaustion(0);
                // 清楚玩家效果
                // 遍历所有可能的状态效果类型
                for (PotionEffectType effectType : PotionEffectType.values()) {
                    // 检查玩家是否拥有该效果
                    if (effectType != null && player.hasPotionEffect(effectType)) {
                        // 移除该效果
                        player.removePotionEffect(effectType);
                    }
                }

                // 设置玩家游戏模式为生存模式
                player.setGameMode(GameMode.SURVIVAL);

                player.sendMessage(ChatColor.GOLD + "[核心战争] " + ChatColor.AQUA + "欢迎回到核心战争！");
                player.sendMessage(ChatColor.GOLD + "[核心战争] " + ChatColor.LIGHT_PURPLE + "当前插件正处于测试阶段，游戏内会有少量日志提示");
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
        teamManager.applyScoreboardToPlayer(player);
        nexusInfoBoard.updateInfoBoard();
        int currentPhase = gameManager.getCurrentPhase();
        if (currentPhase != 5 && currentPhase != 0) {
            gameManager.updateBossBar(currentPhase, gameManager.getRemainingTime());
        }

        if (this.teamManager.isInTeam(player))
            cancelShutdownTask();

        if(currentPhase == 0){
            gameManager.checkAndStartGame(); // 检查是否满足启动条件
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

        // 如果不是游戏结束阶段，延迟检测是否需要关闭服务器
        if (currentPhase != 3) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cancelShutdownTask();

                boolean anyTeamPlayerOnline = false;
                int onlineCount = 0;
                int teamPlayerCount = 0;

                // 统计在线玩家信息
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

                // 如果没有队伍玩家在线，启动关闭倒计时
                if (!anyTeamPlayerOnline) {
                    plugin.getLogger().warning("[延迟关闭检测] 没有队伍玩家在线，启动关闭倒计时");
                    startShutdownCountdown();
                } else {
                    plugin.getLogger().info("[延迟关闭检测] 仍有队伍玩家在线，不启动关闭");
                }
            }, 10L); // 延迟10 ticks (0.5秒) 执行检测
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
            // 广播关闭消息
            Bukkit.broadcastMessage(ChatColor.RED + "所有队伍玩家已离开，服务器即将关闭! ");

            // 延迟5秒后关闭服务器
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