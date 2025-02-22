package cn.zhuobing.testPlugin.map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class MapListener implements Listener {
    private final MapManager mapManager;

    public MapListener(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (!mapManager.isInsideBorder(loc)) {
            event.setCancelled(true);
            //player.sendMessage("你不能在边界外放置方块！");
        }
    }
}