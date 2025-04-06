package cn.zhuobing.testPlugin.specialitem.listener;

import cn.zhuobing.testPlugin.map.MapSelectManager;
import cn.zhuobing.testPlugin.specialitem.manager.MapSelectorManager;
import cn.zhuobing.testPlugin.specialitem.items.MapSelectorItem;
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

// 地图选择监听器类，用于处理地图选择器物品的相关事件
public class MapSelectorListener implements Listener {
    // 地图选择管理器
    private final MapSelectorManager mapSelectorManager;
    // 地图选择配置管理器
    private final MapSelectManager mapSelectManager;

    public MapSelectorListener(MapSelectorManager mapSelectorManager, MapSelectManager mapSelectManager) {
        this.mapSelectorManager = mapSelectorManager;
        this.mapSelectManager = mapSelectManager;
        initSlotToMapMap();
    }

    // 初始化槽位到地图名称的映射
    private void initSlotToMapMap() {
        List<String> candidateMaps = mapSelectManager.getCandidateMaps();
        // 这里可以根据需要添加更复杂的映射逻辑，目前简单地按顺序映射
        for (int slot = 0; slot < candidateMaps.size(); slot++) {
            String mapName = candidateMaps.get(slot);
            // 这里可以使用一个Map来存储槽位和地图名称的对应关系，
            // 但暂时未使用，若后续有需求可以添加
        }
    }

    // 处理玩家右键点击物品的事件
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (MapSelectorItem.isMapSelector(item)) {
                event.setCancelled(true);
                Player player = event.getPlayer();

                // 检查是否存在候选地图
                if (mapSelectManager.getCandidateMaps().isEmpty()) {
                    player.sendMessage(ChatColor.RED + "当前没有可用的地图！");
                    return;
                }

                player.openInventory(mapSelectorManager.createMapSelectorGUI(player));
            }
        }
    }

    // 处理玩家在库存中点击物品的事件
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("地图投票")) {
            // 如果是在地图选择界面点击，则取消该事件
            event.setCancelled(true);
            // 获取点击的玩家对象
            Player player = (Player) event.getWhoClicked();
            UUID playerUUID = player.getUniqueId();

            // 获取点击的槽位
            int slot = event.getRawSlot();
            List<String> candidateMaps = mapSelectManager.getCandidateMaps();
            if (slot < 0 || slot >= candidateMaps.size()) {
                return;
            }

            // 根据槽位获取对应的地图名称
            String mapName = candidateMaps.get(slot);
            if (mapName != null) {
                mapSelectManager.recordVote(playerUUID, mapName);
                // 关闭玩家的库存界面
                player.closeInventory();
            }
        }
    }
}