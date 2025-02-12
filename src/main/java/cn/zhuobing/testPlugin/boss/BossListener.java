package cn.zhuobing.testPlugin.boss;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

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
}