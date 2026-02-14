package cn.zhuobing.testPlugin.anni;

import cn.zhuobing.testPlugin.AnniTest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKManager implements Listener {

    private static final long INACTIVITY_TIMEOUT = 3 * 60 * 1000; // 3分钟
    private static final long WARNING_TIME = 2 * 60 * 1000; // 2分钟提醒
    private final Map<UUID, Long> playerLastActivityTime = new HashMap<>();
    private final AnniTest plugin;

    public AFKManager(AnniTest plugin) {
        this.plugin = plugin;
        startAFKCheckTask();
    }

    // 注册玩家加入事件
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerLastActivityTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // 玩家离开时清理，避免 Map 无限增长
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerLastActivityTime.remove(event.getPlayer().getUniqueId());
    }

    // 监听玩家移动事件
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        updatePlayerActivity(event.getPlayer());
    }

    // 监听玩家聊天事件
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        updatePlayerActivity(event.getPlayer());
    }

    // 监听玩家交互事件
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        updatePlayerActivity(event.getPlayer());
    }

    // 更新玩家最后活动时间
    private void updatePlayerActivity(Player player) {
        playerLastActivityTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // 定时任务，每分钟检查一次玩家是否挂机
    private void startAFKCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Map.Entry<UUID, Long> entry : playerLastActivityTime.entrySet()) {
                    long lastActivity = entry.getValue();
                    long timeInactive = currentTime - lastActivity;

                    if (timeInactive >= INACTIVITY_TIMEOUT) {
                        // 玩家超时没有活动，踢出服务器
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && player.isOnline() && !player.isOp()) {
                            player.kickPlayer(ChatColor.RED + "你因长时间未操作而被踢出游戏。");
                            Bukkit.getLogger().info("玩家 " + player.getName() + " 因挂机被踢出！");
                        }
                    } else if (timeInactive >= WARNING_TIME) {
                        // 给玩家发送提醒
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            player.sendMessage(ChatColor.RED + "你已长时间没有操作，若继续不活动将被踢出游戏！");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1200L); // 每隔1分钟执行一次（1200 ticks）
    }
}
