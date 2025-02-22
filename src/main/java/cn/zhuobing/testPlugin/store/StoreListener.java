package cn.zhuobing.testPlugin.store;

import cn.zhuobing.testPlugin.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class StoreListener implements Listener {
    private final StoreManager storeManager;
    private final GameManager gameManager;

    public StoreListener(StoreManager storeManager, GameManager gameManager) {
        this.storeManager = storeManager;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null
                && event.getClickedBlock().getState() instanceof Sign) {

            Location signLocation = event.getClickedBlock().getLocation();
            Player player = event.getPlayer();
            event.setCancelled(true);

            if (storeManager.isBrewSignLocation(signLocation)) {
                if (gameManager.getCurrentPhase() >= 4) {
                    storeManager.setPhase4(true); // 设置阶段四标志
                }else{
                    storeManager.setPhase4(false);
                }
                storeManager.openBrewStoreInterface(player);
            } else if (storeManager.isWeaponSignLocation(signLocation)) {
                storeManager.openWeaponStoreInterface(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title.equals("酿造商店") || title.equals("武器商店")) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.hasItemMeta()) {
                    Material itemMaterial = clickedItem.getType();
                    if (storeManager.canPlayerAfford(player, itemMaterial)) {
                        storeManager.purchaseItem(player, itemMaterial);
                    } else {
                        player.sendMessage(ChatColor.RED + "你没有足够的金锭！");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign)) return;

        Location loc = block.getLocation();
        if (!storeManager.isBrewSignLocation(loc) && !storeManager.isWeaponSignLocation(loc)) return;

        event.setCancelled(true);
        refreshChunk(block);
    }

    private void refreshChunk(Block block) {
        Bukkit.getScheduler().runTaskLater(storeManager.getPlugin(), () -> {
            block.getWorld().refreshChunk(
                    block.getChunk().getX(),
                    block.getChunk().getZ()
            );
        }, 2L);
    }
}