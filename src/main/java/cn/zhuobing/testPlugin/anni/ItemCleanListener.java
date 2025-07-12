package cn.zhuobing.testPlugin.anni;

import cn.zhuobing.testPlugin.map.BossWorldManager;
import cn.zhuobing.testPlugin.map.MapSelectManager;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import cn.zhuobing.testPlugin.AnniTest;

import java.util.UUID;

public class ItemCleanListener implements Listener {
    private final AnniTest plugin;
    private final MapSelectManager mapSelectManager;
    private final BossWorldManager bossWorldManager;

    public ItemCleanListener(AnniTest plugin, MapSelectManager mapSelectManager, BossWorldManager bossWorldManager) {
        this.plugin = plugin;
        this.mapSelectManager = mapSelectManager;
        this.bossWorldManager = bossWorldManager;
    }

    // 记录新生成的掉落物
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Item)) return;

        // 处理 游戏世界 和 boss世界 中的掉落物
        if (mapSelectManager.isGameWorld(event.getLocation().getWorld())
        || bossWorldManager.isBossWorld(event.getLocation().getWorld())) {
            plugin.getItemSpawnTimes().put(event.getEntity().getUniqueId(), System.currentTimeMillis());
        }
    }

    // 玩家拾取时移除记录
    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        plugin.getItemSpawnTimes().remove(event.getItem().getUniqueId());
    }

    // 自然消失时移除记录
    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        plugin.getItemSpawnTimes().remove(event.getEntity().getUniqueId());
    }
}