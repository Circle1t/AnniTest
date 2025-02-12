package cn.zhuobing.testPlugin.specialitem.listener;

import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class CompassListener implements Listener {
    private final TeamManager teamManager;
    private final NexusManager nexusManager;
    private final List<String> teamNames;
    private int currentIndex = 0;
    private final Plugin plugin;

    public CompassListener(TeamManager teamManager, NexusManager nexusManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.nexusManager = nexusManager;
        this.teamNames = new ArrayList<>(teamManager.getEnglishToChineseMap().keySet());
        this.plugin = plugin;
        startRefreshTask();
    }

    // 启动每两秒刷新任务
    private void startRefreshTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    ItemStack mainHandItem = player.getInventory().getItemInMainHand();
                    if (CompassItem.isCompass(mainHandItem)) {
                        updateCompassInfo(player, false);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每一秒执行一次
    }

    // 更新指南针信息
    private void updateCompassInfo(Player player, boolean switchTeam) {
        String playerTeam = teamManager.getPlayerTeamName(player);

        if (playerTeam == null) {
            player.sendMessage(ChatColor.RED + "你不在任何队伍中！");
            return;
        }

        // 初始指向玩家所在队伍的核心
        if (currentIndex == 0) {
            for (int i = 0; i < teamNames.size(); i++) {
                if (teamNames.get(i).equals(playerTeam)) {
                    currentIndex = i;
                    break;
                }
            }
        }

        String targetTeam = teamNames.get(currentIndex);
        Location targetLocation = nexusManager.getTeamNexusLocation(targetTeam);

        if (targetLocation != null) {
            player.setCompassTarget(targetLocation);
            double distance = player.getLocation().distance(targetLocation);
            ChatColor teamColor = teamManager.getTeamColor(targetTeam);
            String message = ChatColor.GOLD + "距离 " + teamColor + teamManager.getEnglishToChineseMap().get(targetTeam) + "队" + ChatColor.GOLD + " 核心: " + teamColor + String.format("%.2f", distance) + ChatColor.GOLD + " 米";
            MessageUtil.sendActionBarMessage(player, message);
        } else {
            player.sendMessage(ChatColor.RED + "未找到 " + teamManager.getEnglishToChineseMap().get(targetTeam) + "队 的核心位置！");
        }

        // 如果需要切换队伍
        if (switchTeam) {
            currentIndex = (currentIndex + 1) % teamNames.size();
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (CompassItem.isCompass(item)) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                updateCompassInfo(player, true);
            }
        }
    }
}