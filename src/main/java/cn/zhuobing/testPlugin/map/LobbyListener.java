package cn.zhuobing.testPlugin.map;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class LobbyListener implements Listener {
    private final LobbyManager lobbyManager;

    public LobbyListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        World lobbyWorld = lobbyManager.getLobbyWorld();
        if (lobbyWorld != null && event.getBlock().getWorld().equals(lobbyWorld)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        World lobbyWorld = lobbyManager.getLobbyWorld();
        if (lobbyWorld != null && event.getBlock().getWorld().equals(lobbyWorld)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        // 检查实体是否为玩家
        if (event.getEntity() instanceof Player player) {
            World lobbyWorld = lobbyManager.getLobbyWorld();

            // 检查玩家是否在大厅世界且伤害类型是摔落
            if (lobbyWorld != null && player.getWorld().equals(lobbyWorld) &&
                    event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                // 取消摔落伤害
                event.setCancelled(true);
            }
        }
    }
}