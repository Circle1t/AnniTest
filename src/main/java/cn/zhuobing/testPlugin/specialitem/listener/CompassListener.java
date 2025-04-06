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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class CompassListener implements Listener {
    private final TeamManager teamManager;
    private final NexusManager nexusManager;
    private final List<String> teamNames;
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
        }.runTaskTimer(plugin, 0L, 40L); // 每两秒执行一次
    }

    // 更新指南针信息
    private void updateCompassInfo(Player player, boolean switchTeam) {
        String playerTeam = teamManager.getPlayerTeamName(player);

        if (playerTeam == null) {
            player.sendMessage(ChatColor.RED + "你不在任何队伍中！");
            return;
        }

        // 获取当前指向的队伍索引
        int currentIndex = getCurrentIndex(player);

        // 如果需要切换队伍
        if (switchTeam) {
            currentIndex = (currentIndex + 1) % teamNames.size();
            setCurrentIndex(player, currentIndex);
        }

        String targetTeam = teamNames.get(currentIndex);
        Location targetLocation = nexusManager.getTeamNexusLocation(targetTeam);

        if (targetLocation != null) {
            player.setCompassTarget(targetLocation);
            Location playerLocation = player.getLocation();
            if (playerLocation.getWorld() != null && targetLocation.getWorld() != null && playerLocation.getWorld().equals(targetLocation.getWorld())) {
                double distance = playerLocation.distance(targetLocation);
                ChatColor teamColor = teamManager.getTeamColor(targetTeam);
                String message = ChatColor.GOLD + "距离 " + teamColor + teamManager.getEnglishToChineseMap().get(targetTeam) + "队" + ChatColor.GOLD + " 核心: " + teamColor + String.format("%.0f", distance) + ChatColor.GOLD + " 米";
                MessageUtil.sendActionBarMessage(player, message);
            }
//            else {
//                player.sendMessage(ChatColor.RED + "目标核心与你不在同一世界，无法计算距离！");
//            }
        } else {
            player.sendMessage(ChatColor.RED + "未找到 " + teamManager.getEnglishToChineseMap().get(targetTeam) + "队 的核心位置！");
        }
    }

    // 获取玩家当前指向的队伍索引
    private int getCurrentIndex(Player player) {
        Object indexObj = player.getMetadata("compassIndex").stream()
                .filter(meta -> meta.getOwningPlugin().equals(plugin))
                .findFirst()
                .map(meta -> meta.asInt())
                .orElse(0);
        return (int) indexObj;
    }

    // 设置玩家当前指向的队伍索引
    private void setCurrentIndex(Player player, int index) {
        player.setMetadata("compassIndex", new FixedMetadataValue(plugin, index));
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