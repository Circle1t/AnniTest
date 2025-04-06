package cn.zhuobing.testPlugin.map;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

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
}