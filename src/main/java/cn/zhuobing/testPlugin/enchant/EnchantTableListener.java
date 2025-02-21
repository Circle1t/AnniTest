package cn.zhuobing.testPlugin.enchant;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

public class EnchantTableListener implements Listener {
    private final EnchantManager enchantManager;

    public EnchantTableListener(EnchantManager enchantManager) {
        this.enchantManager = enchantManager;
    }

    /**
     * 当玩家打开附魔台时触发此事件，自动填充 64 个青金石并标记玩家开始附魔流程
     *
     * @param event 库存打开事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            enchantManager.handleEnchantTableOpen(event.getInventory());
            if (event.getPlayer() instanceof Player) {
                Player player = (Player) event.getPlayer();
                enchantManager.startEnchanting(player);
            }
        }
    }

    /**
     * 当玩家进行附魔操作时触发此事件，使用正常附魔逻辑，附魔后重置青金石数量为 64
     *
     * @param event 附魔物品事件
     */
    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        // 让正常的附魔逻辑执行，不取消事件
        // 等待附魔完成后处理青金石数量

        // 附魔完成后，将青金石数量重新设置为 64
        Inventory inventory = event.getInventory();
        int lapisSlot = 1;
        ItemStack lapisStack = inventory.getItem(lapisSlot);
        if (lapisStack != null && lapisStack.getType() == Material.LAPIS_LAZULI) {
            lapisStack.setAmount(64);
            inventory.setItem(lapisSlot, lapisStack);
        }

        // 更新玩家库存
        event.getEnchanter().updateInventory();
    }

    /**
     * 当玩家关闭附魔台时触发此事件，标记玩家结束附魔流程
     *
     * @param event 库存关闭事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            // 附魔结束后销毁填充的青金石
            Inventory inventory = event.getInventory();
            int lapisSlot = 1;
            inventory.setItem(lapisSlot, null);

            if (event.getPlayer() instanceof Player) {
                enchantManager.endEnchanting((Player) event.getPlayer());
            }
        }
    }

    /**
     * 当玩家在库存中点击物品时触发此事件，阻止玩家拿取自动填充的青金石
     *
     * @param event 库存点击事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            int lapisSlot = 1;
            // 检查点击的是否是青金石槽位
            if (event.getRawSlot() == lapisSlot) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 当玩家在库存中拖动物品时触发此事件，阻止玩家拿取自动填充的青金石
     *
     * @param event 库存拖动事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            int lapisSlot = 1;
            for (int slot : event.getRawSlots()) {
                if (slot == lapisSlot) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    /**
     * 当玩家丢弃物品时触发此事件，阻止玩家丢弃附魔台自带的青金石
     * 若丢弃的是附魔台槽位 0 的附魔书，直接销毁
     *
     * @param event 玩家丢弃物品事件
     */
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        Inventory openInventory = player.getOpenInventory().getTopInventory();
        // 检查玩家是否正在使用附魔台
        if (openInventory.getType() != InventoryType.ENCHANTING) {
            return;
        }
        // 处理丢弃青金石的情况
        if (droppedItem.getType() == Material.LAPIS_LAZULI) {
            int lapisSlot = 1;
            ItemStack lapisInInventory = openInventory.getItem(lapisSlot);
            if (lapisInInventory != null && lapisInInventory.isSimilar(droppedItem)) {
                event.setCancelled(true); // 必须取消事件
                event.getItemDrop().remove();
                player.updateInventory();
                return;
            }
        }

        //处理附魔书丢弃 附魔过程中的附魔书一旦被丢弃就会直接销毁
        if(droppedItem.getType() != Material.ENCHANTED_BOOK) return;
        int bookSlot = 0;
        ItemStack bookInInventory = openInventory.getItem(bookSlot);
        event.getItemDrop().remove();
        openInventory.setItem(bookSlot, null);
        player.updateInventory();

    }
}