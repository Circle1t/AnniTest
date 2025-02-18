package cn.zhuobing.testPlugin.specialitem.listener;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.KitSelectorItem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class KitSelectorListener implements Listener {
    private final KitManager kitManager;

    public KitSelectorListener(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("选择你的职业")) {
            event.setCancelled(true); // 取消默认的点击行为
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem != null) {
                for (Kit kit : kitManager.getRegisteredKits().values()) {
                    ItemStack originalIcon = kit.getIcon();
                    if (clickedItem.getType() == originalIcon.getType() &&
                            ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName())
                                    .equals(ChatColor.stripColor(originalIcon.getItemMeta().getDisplayName()))) {
                        UUID playerId = player.getUniqueId();
                        kitManager.setPlayerKit(playerId, kit.getName());
                        player.closeInventory();
                        player.sendMessage(ChatColor.GREEN + "你已选择 " + kit.getNameWithColor() + ChatColor.GREEN + " 职业");
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 检查玩家右键点击的物品是否是职业选择器
        if (item != null && KitSelectorItem.isKitSelector(item)) {
            event.setCancelled(true); // 取消默认的右键行为
            kitManager.openKitSelection(player); // 打开职业选择界面
        }
    }
}