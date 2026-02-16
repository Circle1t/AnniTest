package cn.zhuobing.testPlugin.enderfurnance;

import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderFurnaceManager {
    private Location virtualFurnaceArea;
    private final Map<String, Location> teamFurnaces = new HashMap<>();
    private final Map<UUID, Location> playerFurnaces = new HashMap<>();
    private final Map<Location, FurnaceInventory> furnaceInventories = new HashMap<>();

    // 配置相关字段
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private String currentMapName;
    private World currentWorld;

    public EnderFurnaceManager(Plugin plugin) {
        this.plugin = plugin;
    }

    // 加载配置（在MapSelectManager加载地图时调用）
    public void loadConfig(String mapFolderName, World world) {
        this.currentMapName = mapFolderName;
        this.currentWorld = world;
        virtualFurnaceArea = new Location(world, 100000, 100, 100000);

        File mapsDir = new File(plugin.getDataFolder(), "maps");
        File mapDir = new File(mapsDir, mapFolderName);
        File configDir = new File(mapDir, AnniConfigManager.MAP_CONFIG_FOLDER);

        if (!configDir.exists() && !configDir.mkdirs()) {
            plugin.getLogger().warning("无法创建末影高炉配置目录");
            return;
        }

        configFile = new File(configDir, "enderfurnace-config.yml");
        if (!configFile.exists()) {
            try {
                if (configFile.createNewFile()) {
                    // 初始化默认配置文件结构
                    config = YamlConfiguration.loadConfiguration(configFile);
                    config.createSection("team-furnaces");
                    config.save(configFile);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("创建末影高炉配置文件失败: " + e.getMessage());
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadTeamFurnaces(world);
        setChunkAlwaysLoad(world);
    }

    // 保存配置（在地图卸载或插件禁用时调用）
    public void saveConfig() {
        if (configFile == null || currentMapName == null || currentWorld == null) return;

        try {
            // 清空现有配置
            config.set("team-furnaces", null);

            // 序列化队伍高炉数据
            for (Map.Entry<String, Location> entry : teamFurnaces.entrySet()) {
                Location loc = entry.getValue();
                // 确保保存时使用当前世界
                Location worldLoc = new Location(currentWorld, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                config.set("team-furnaces." + entry.getKey(), worldLoc);
            }

            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存末影高炉配置失败: " + e.getMessage());
        }
    }

    // 加载队伍高炉位置
    private void loadTeamFurnaces(World world) {
        teamFurnaces.clear();
        ConfigurationSection section = config.getConfigurationSection("team-furnaces");
        if (section == null) return;

        for (String teamName : section.getKeys(false)) {
            Location loc = section.getLocation(teamName);
            if (loc != null) {
                // 确保位置属于当前世界
                loc.setWorld(world);
                // 验证高炉存在
                if (loc.getBlock().getType() == Material.FURNACE) {
                    teamFurnaces.put(teamName, loc);
                } else {
                    plugin.getLogger().warning("末影高炉配置错误: " + teamName + " 位置不存在高炉");
                }
            }
        }
    }

    // 修改原有方法
    public void setTeamFurnace(String teamName, Location location) {
        // 标准化坐标到方块中心
        Location standardizedLoc = location.clone();
        standardizedLoc.setX(location.getBlockX() + 0.5);
        standardizedLoc.setZ(location.getBlockZ() + 0.5);
        standardizedLoc.setWorld(currentWorld); // 确保使用当前世界

        teamFurnaces.put(teamName, standardizedLoc);
        saveConfig(); // 自动保存
    }

    public boolean removeFurnace(Location location) {
        boolean removed = teamFurnaces.values().removeIf(loc -> loc.equals(location));
        if (removed) saveConfig(); // 自动保存
        return removed;
    }

    /** 强制保持虚拟熔炉所在区块加载，否则熔炉不 tick、烧不了东西 */
    public void setChunkAlwaysLoad(World world) {
        if (virtualFurnaceArea == null || virtualFurnaceArea.getWorld() == null) return;
        Chunk chunk = virtualFurnaceArea.getChunk();
        chunk.setForceLoaded(true);
        if (!chunk.isLoaded()) {
            chunk.load(true);
        }
    }

    private void ensureFurnaceChunkLoaded(Location furnaceLoc) {
        if (furnaceLoc == null || furnaceLoc.getWorld() == null) return;
        Chunk chunk = furnaceLoc.getChunk();
        chunk.setForceLoaded(true);
        if (!chunk.isLoaded()) {
            chunk.load(true);
        }
    }

    public boolean isTeamFurnace(Location loc) {
        for (Location furnaceLoc : teamFurnaces.values()) {
            if (furnaceLoc.getBlockX() == loc.getBlockX() &&
                    furnaceLoc.getBlockY() == loc.getBlockY() &&
                    furnaceLoc.getBlockZ() == loc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    public String getTeamByFurnace(Location loc) {
        for (Map.Entry<String, Location> entry : teamFurnaces.entrySet()) {
            Location furnaceLoc = entry.getValue();
            if (furnaceLoc.getBlockX() == loc.getBlockX() &&
                    furnaceLoc.getBlockY() == loc.getBlockY() &&
                    furnaceLoc.getBlockZ() == loc.getBlockZ()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void createVirtualFurnace(Player player) {
        if (virtualFurnaceArea == null || currentWorld == null) return;
        Location baseLoc = virtualFurnaceArea.clone();
        int offset = playerFurnaces.size() * 3;
        Location furnaceLoc = baseLoc.add(offset, 0, 0);
        furnaceLoc.setWorld(currentWorld);

        // 先强制加载区块，否则熔炉不会 tick、烧不了东西（服务器上该区块可能未加载）
        ensureFurnaceChunkLoaded(furnaceLoc);

        Block block = furnaceLoc.getBlock();
        block.setType(Material.FURNACE);
        Furnace furnace = (Furnace) block.getState();
        String title = ChatColor.DARK_PURPLE + "末影高炉";
        furnace.setCustomName(title);

        Location key = furnaceLoc.clone();
        furnaceInventories.put(key, furnace.getInventory());
        playerFurnaces.put(player.getUniqueId(), key);

        furnace.update(true);
    }

    public FurnaceInventory getPlayerInventory(Player player) {
        Location loc = playerFurnaces.get(player.getUniqueId());
        return loc != null ? furnaceInventories.get(loc) : null;
    }

    public boolean hasVirtualFurnace(Player player) {
        return playerFurnaces.containsKey(player.getUniqueId());
    }

    /** 打开前确保熔炉所在区块已加载，避免服务器上区块被卸载导致无法烧炼 */
    public void ensurePlayerFurnaceChunkLoaded(Player player) {
        Location loc = playerFurnaces.get(player.getUniqueId());
        if (loc != null) ensureFurnaceChunkLoaded(loc);
    }
}