package cn.zhuobing.testPlugin.enderfurnance;

import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.FurnaceInventory;

public class EnderFurnaceListener implements Listener {
    private final EnderFurnaceManager furnaceManager;
    private final TeamManager teamManager;

    public EnderFurnaceListener(EnderFurnaceManager furnaceManager, TeamManager teamManager) {
        this.furnaceManager = furnaceManager;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onFurnaceInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.FURNACE) return;

        Player player = event.getPlayer();
        String team = furnaceManager.getTeamByFurnace(block.getLocation());

        // 检查是否队伍高炉
        if (team == null) return;

        // 验证玩家队伍
        String playerTeam = teamManager.getPlayerTeamName(player);
        if (playerTeam == null || !playerTeam.equals(team)) {
            player.sendMessage(ChatColor.RED + "你无法打开其他队伍的末影高炉！");
            event.setCancelled(true);
            return;
        }

        // 创建/获取虚拟高炉
        if (!furnaceManager.hasVirtualFurnace(player)) {
            furnaceManager.createVirtualFurnace(player);
        }

        // 打开库存
        FurnaceInventory inventory = furnaceManager.getPlayerInventory(player);
        if (inventory != null) {
            player.openInventory(inventory);
            player.sendMessage(ChatColor.DARK_PURPLE + "你已打开末影高炉！");
        } else {
            player.sendMessage(ChatColor.RED + "末影高炉加载失败，请联系管理员！");
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // 检查是否为末影高炉
        if (block.getType() == Material.FURNACE && furnaceManager.isTeamFurnace(block.getLocation())) {
            player.sendMessage(ChatColor.RED + "你不能破坏末影高炉！");
            event.setCancelled(true);
        }
    }
}