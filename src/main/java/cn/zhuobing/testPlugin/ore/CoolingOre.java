package cn.zhuobing.testPlugin.ore;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

public class CoolingOre {
    private final Location location;
    private final OreType oreType;
    private BukkitTask restoreTask;

    public CoolingOre(Location location, OreType oreType) {
        this.location = location;
        this.oreType = oreType;
    }

    // Getter和Setter
    public Location getLocation() { return location; }
    public OreType getOreType() { return oreType; }
    public BukkitTask getRestoreTask() { return restoreTask; }
    public void setRestoreTask(BukkitTask task) { this.restoreTask = task; }

    public void cancelTask() {
        if (restoreTask != null) {
            restoreTask.cancel();
        }
    }
}