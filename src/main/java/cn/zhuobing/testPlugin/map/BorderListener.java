package cn.zhuobing.testPlugin.map;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BorderListener implements Listener {
    private final BorderManager borderManager;
    private final MapSelectManager mapSelectManager;

    public BorderListener(BorderManager borderManager, MapSelectManager mapSelectManager) {
        this.borderManager = borderManager;
        this.mapSelectManager = mapSelectManager;
    }

    /**
     * 处理方块放置事件
     * @param event 方块放置事件
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        // 添加边界管理器空值检查
        if (borderManager == null) return;

        String gameMap = mapSelectManager.getGameMap();
        if (gameMap != null && loc.getWorld().getName().equals(gameMap)) {
            // 添加调试信息
            if (!borderManager.isInsideBorder(loc)) {
                event.setCancelled(true);
                player.sendMessage("你不能在边界外放置方块！");
            }
        }
    }


}
