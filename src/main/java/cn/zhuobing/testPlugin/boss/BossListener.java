package cn.zhuobing.testPlugin.boss;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

public class BossListener implements Listener {
    private final BossDataManager bossDataManager;

    public BossListener(BossDataManager bossDataManager) {
        this.bossDataManager = bossDataManager;
    }

    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent event) {
        bossDataManager.onBossDamage(event);
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        bossDataManager.onBossDeath(event);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Wither) {
            Wither wither = (Wither) event.getEntity();
            if (wither.hasMetadata("customBoss")) {
                wither.getBossBar().removeAll(); // 移除原生 BossBar
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if(bossDataManager.isPlayerInBoss(player)){
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "你不能在boss区域内破坏方块！");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if(bossDataManager.isPlayerInBoss(player)){
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "你不能在boss区域内放置方块！");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if(bossDataManager.isPlayerInBoss(player)){
            bossDataManager.removeBossPlayer(player);
        }
    }

}