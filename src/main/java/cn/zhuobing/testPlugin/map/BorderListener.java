package cn.zhuobing.testPlugin.map;

import org.bukkit.Location;
import org.bukkit.Material;
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (borderManager == null) return;

        String gameMap = mapSelectManager.getGameMap();
        if (gameMap == null || !loc.getWorld().getName().equals(gameMap)) return;

        if (!borderManager.isInsideBorder(loc)) {
            event.setCancelled(true);
            player.sendMessage("你不能在边界外放置方块！");
            return;
        }

        // 禁止床在边界线上放置（床占两格，任一方格在边界上则禁止）
        if (isBed(block.getType())) {
            if (borderManager.isOnBorder(loc)) {
                event.setCancelled(true);
                player.sendMessage("你不能在边界上放置床！");
                return;
            }
            Block other = block.getRelative(player.getFacing());
            if (borderManager.isOnBorder(other.getLocation())) {
                event.setCancelled(true);
                player.sendMessage("你不能在边界上放置床！");
            }
        }
    }

    private static boolean isBed(Material type) {
        return type == Material.RED_BED || type == Material.BLACK_BED || type == Material.BLUE_BED
                || type == Material.BROWN_BED || type == Material.CYAN_BED || type == Material.GRAY_BED
                || type == Material.GREEN_BED || type == Material.LIGHT_BLUE_BED || type == Material.LIGHT_GRAY_BED
                || type == Material.LIME_BED || type == Material.MAGENTA_BED || type == Material.ORANGE_BED
                || type == Material.PINK_BED || type == Material.PURPLE_BED || type == Material.WHITE_BED
                || type == Material.YELLOW_BED;
    }
}
