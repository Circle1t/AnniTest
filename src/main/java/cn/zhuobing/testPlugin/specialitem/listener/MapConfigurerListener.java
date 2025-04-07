package cn.zhuobing.testPlugin.specialitem.listener;

import cn.zhuobing.testPlugin.map.MapSelectManager;
import cn.zhuobing.testPlugin.specialitem.manager.MapConfigurerManager;
import cn.zhuobing.testPlugin.specialitem.items.MapConfigurerItem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class MapConfigurerListener implements Listener {
    private final MapConfigurerManager mapConfigurerManager;
    private final MapSelectManager mapSelectManager;

    public MapConfigurerListener(MapConfigurerManager mapConfigurerManager, MapSelectManager mapSelectManager) {
        this.mapConfigurerManager = mapConfigurerManager;
        this.mapSelectManager = mapSelectManager;
        initSlotToMapMap();
    }

    private void initSlotToMapMap() {
        List<String> candidateMaps = mapSelectManager.getCandidateMaps();
        List<String> originalMaps = mapSelectManager.getOriginalMaps();
        // 这里可以根据需要添加更复杂的映射逻辑，目前简单地按顺序映射
        for (int slot = 0; slot < candidateMaps.size() + originalMaps.size(); slot++) {
            String mapName;
            if (slot < candidateMaps.size()) {
                mapName = candidateMaps.get(slot);
            } else {
                mapName = originalMaps.get(slot - candidateMaps.size());
            }
            // 这里可以使用一个Map来存储槽位和地图名称的对应关系，
            // 但暂时未使用，若后续有需求可以添加
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (MapConfigurerItem.isMapConfigurer(item)) {
                Player player = event.getPlayer();
                if (!player.hasPermission("op")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用此物品！");
                    return;
                }
                event.setCancelled(true);
                if (mapSelectManager.getCandidateMaps().isEmpty() && mapSelectManager.getOriginalMaps().isEmpty()) {
                    player.sendMessage(ChatColor.RED + "当前没有可用的地图！");
                    return;
                }
                player.openInventory(mapConfigurerManager.createMapConfigurerGUI(player));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("地图配置")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            UUID playerUUID = player.getUniqueId();

            int slot = event.getRawSlot();
            List<String> candidateMaps = mapSelectManager.getCandidateMaps();
            List<String> originalMaps = mapSelectManager.getOriginalMaps();
            if (slot < 0 || slot >= candidateMaps.size() + originalMaps.size()) {
                return;
            }

            String mapName;
            if (slot < candidateMaps.size()) {
                mapName = candidateMaps.get(slot);
            } else {
                mapName = originalMaps.get(slot - candidateMaps.size());
            }

            if (mapName != null) {
                mapSelectManager.enterMap(player, mapName);
                player.closeInventory();
            }
        }
    }
}