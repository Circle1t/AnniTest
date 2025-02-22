package cn.zhuobing.testPlugin.enchant;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class BrewingStandListener implements Listener {

    private final Plugin plugin;

    public BrewingStandListener(Plugin plugin) {
        this.plugin = plugin;
    }

    // 当玩家打开炼药台时，自动将燃料槽设置为满状态，5 tick 后清空燃料槽
    @EventHandler
    public void onBrewingStandOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.BREWING) {
            BrewerInventory inventory = (BrewerInventory) event.getInventory();
            setFuelToFull(inventory);

            // 使用 Bukkit 调度器在 5 tick 后清空燃料槽
            new BukkitRunnable() {
                @Override
                public void run() {
                    inventory.setFuel(null);
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    // 阻止玩家操作燃料槽
    @EventHandler
    public void onBrewingStandClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.BREWING) {
            int fuelSlot = 4; // 燃料槽位索引（1.14+版本）
            if (event.getRawSlot() == fuelSlot) {
                event.setCancelled(true); // 取消所有交互
                event.getWhoClicked().setItemOnCursor(null); // 清空玩家光标物品
            }
        }
    }

    // 酿造开始前确保燃料满状态
    @EventHandler
    public void onBrewStart(InventoryMoveItemEvent event) {
        if (event.getDestination().getType() == InventoryType.BREWING) {
            BrewerInventory inventory = (BrewerInventory) event.getDestination();
            setFuelToFull(inventory);
        }
    }

    // 酿造完成后重置燃料为满状态
    @EventHandler
    public void onBrew(BrewEvent event) {
        BrewerInventory inventory = event.getContents();
        setFuelToFull(inventory);
    }

    // 破坏炼药台时清除燃料防止掉落
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.BREWING_STAND) {
            BrewingStand brewingStand = (BrewingStand) block.getState();
            BrewerInventory inventory = brewingStand.getInventory();

            // 强制清空燃料槽
            inventory.setFuel(null);

            // 更新方块状态确保修改生效，不向周边方块发送更新通知
            brewingStand.update(true, false);
        }
    }

    // 将炼药台燃料设置为满状态的辅助方法
    private void setFuelToFull(BrewerInventory inventory) {
        ItemStack fuel = new ItemStack(Material.BLAZE_POWDER, 1);
        inventory.setFuel(fuel);
    }
}