package cn.zhuobing.testPlugin.enchant;

import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 炼药台：玩家打开时若燃料槽为空则填满燃料条，不放入任何物品，
 * 玩家无法拿到、摧毁炼药台时也不会掉落烈焰粉。
 */
public class BrewingStandListener implements Listener {

    private static final int MAX_BREWING_FUEL = 20;
    /** 炼药台物品栏中燃料槽的索引 */
    private static final int FUEL_SLOT = 3;

    @EventHandler
    public void onBrewingStandOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getType() != InventoryType.BREWING) {
            return;
        }

        if (!(inv.getHolder() instanceof BrewingStand brewingStand)) {
            return;
        }

        // 仅当燃料槽为空时填满燃料条，不放入物品
        ItemStack fuel = inv.getItem(FUEL_SLOT);
        if (fuel == null || fuel.getType() == Material.AIR || fuel.getAmount() <= 0) {
            brewingStand.setFuelLevel(MAX_BREWING_FUEL);
            brewingStand.update(true);
        }
    }
}
