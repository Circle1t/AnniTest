package cn.zhuobing.testPlugin.boss;

import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
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

}