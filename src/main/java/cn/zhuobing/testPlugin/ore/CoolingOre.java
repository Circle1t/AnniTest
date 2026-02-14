package cn.zhuobing.testPlugin.ore;

import org.bukkit.Location;

/**
 * 处于冷却中的矿物。恢复时间由 OreManager 的全局 tick 统一检查，不单独创建 runTaskLater，避免大量矿物时产生大量定时任务。
 */
public class CoolingOre {
    private final Location location;
    private final OreType oreType;
    /** 恢复为原矿物的时间戳（毫秒），由全局任务在到达时恢复并移出 map */
    private final long restoreAtMillis;

    public CoolingOre(Location location, OreType oreType, long restoreAtMillis) {
        this.location = location;
        this.oreType = oreType;
        this.restoreAtMillis = restoreAtMillis;
    }

    public Location getLocation() { return location; }
    public OreType getOreType() { return oreType; }
    public long getRestoreAtMillis() { return restoreAtMillis; }
}
