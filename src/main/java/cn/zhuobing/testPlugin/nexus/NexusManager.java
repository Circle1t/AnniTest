package cn.zhuobing.testPlugin.nexus;

import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NexusManager {
    private final Map<String, Location> teamNexusLocations = new HashMap<>();
    private final Map<String, Integer> teamNexusHealth = new HashMap<>();
    private final Map<String, Location> borderFirst = new HashMap<>();
    private final Map<String, Location> borderSecond = new HashMap<>();
    private static final int DEFAULT_HEALTH = 75;
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private String mapFolderName;

    public NexusManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig(String mapFolderName, World world) {
        this.mapFolderName = mapFolderName;
        File mapsFolder = new File(plugin.getDataFolder(), "maps");
        if (!mapsFolder.exists()) {
            mapsFolder.mkdirs();
        }
        File mapFolder = new File(mapsFolder, mapFolderName);
        if (!mapFolder.exists()) {
            mapFolder.mkdirs();
        }
        File configFolder = new File(mapFolder, AnniConfigManager.MAP_CONFIG_FOLDER);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        configFile = new File(configFolder, "nexus-config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        if (config.contains("nexus.locations")) {
            for (String teamName : config.getConfigurationSection("nexus.locations").getKeys(false)) {
                Location location = config.getLocation("nexus.locations." + teamName);
                if (world == null) {
                    plugin.getLogger().warning("未获取到游戏地图world！");
                    return;
                }
                if (location != null) {
                    location.setWorld(world);
                }
                teamNexusLocations.put(teamName, location);
                world.getBlockAt(location).setType(Material.END_STONE);

            }
        }
        if (config.contains("nexus.health")) {
            for (String teamName : config.getConfigurationSection("nexus.health").getKeys(false)) {
                int health = config.getInt("nexus.health." + teamName);
                teamNexusHealth.put(teamName, health);
            }
        }
        if (config.contains("nexus.borders")) {
            for (String team : config.getConfigurationSection("nexus.borders").getKeys(false)) {
                Location first = config.getLocation("nexus.borders." + team + ".first");
                Location second = config.getLocation("nexus.borders." + team + ".second");
                if (first == null || second == null) {
                    break;
                }
                first.setWorld(world);
                second.setWorld(world);
                borderFirst.put(team, first);
                borderSecond.put(team, second);
            }
        }
    }

    public void saveConfig() {
        File mapsFolder = new File(plugin.getDataFolder(), "maps");
        if (!mapsFolder.exists()) {
            mapsFolder.mkdirs();
        }
        File mapFolder = new File(mapsFolder, mapFolderName);
        if (!mapFolder.exists()) {
            mapFolder.mkdirs();
        }
        File configFolder = new File(mapFolder, AnniConfigManager.MAP_CONFIG_FOLDER);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        configFile = new File(configFolder, "nexus-config.yml");

        for (Map.Entry<String, Location> entry : teamNexusLocations.entrySet()) {
            String teamName = entry.getKey();
            Location location = entry.getValue();
            config.set("nexus.locations." + teamName, location);
        }
        for (Map.Entry<String, Integer> entry : teamNexusHealth.entrySet()) {
            String teamName = entry.getKey();
            int health = entry.getValue();
            config.set("nexus.health." + teamName, health);
        }
        for (String team : borderFirst.keySet()) {
            config.set("nexus.borders." + team + ".first", borderFirst.get(team));
        }
        for (String team : borderSecond.keySet()) {
            config.set("nexus.borders." + team + ".second", borderSecond.get(team));
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setNexusLocation(String teamName, Location location) {
        teamNexusLocations.put(teamName, location);
        teamNexusHealth.put(teamName, DEFAULT_HEALTH);
    }

    public void setNexusHealth(String teamName, int health) {
        teamNexusHealth.put(teamName, health);
        Location location = teamNexusLocations.get(teamName);
        if(health > 0){
            if(location != null){
                location.getBlock().setType(Material.END_STONE);
            }
        }else{
            location.getBlock().setType(Material.BEDROCK);
        }
    }

    public Location getTeamNexusLocation(String teamName) {
        return teamNexusLocations.get(teamName);
    }

    public Map<String, Location> getNexusLocations() {
        return teamNexusLocations;
    }

    public int getNexusHealth(String teamName) {
        return teamNexusHealth.getOrDefault(teamName, 0);
    }

    public Map<String, Integer> getNexusHealthOfAllTeam() {
        return teamNexusHealth;
    }

    public boolean hasNexus(String teamName) {
        return teamNexusLocations.containsKey(teamName);
    }

    public void removeNexus(String teamName) {
        teamNexusLocations.remove(teamName);
        teamNexusHealth.remove(teamName);
    }

    // 保护区域设置方法
    public void setBorder(String team, String position, Location loc) {
        if (position.equalsIgnoreCase("first")) {
            borderFirst.put(team, loc);
        } else {
            borderSecond.put(team, loc);
        }
    }

    // 保护区域检查方法
    public boolean isInProtectedArea(Location loc) {
        for (String team : borderFirst.keySet()) {
            Location first = borderFirst.get(team);
            Location second = borderSecond.get(team);
            if (first == null || second == null) continue;

            int minX = Math.min(first.getBlockX(), second.getBlockX());
            int maxX = Math.max(first.getBlockX(), second.getBlockX());
            int minZ = Math.min(first.getBlockZ(), second.getBlockZ());
            int maxZ = Math.max(first.getBlockZ(), second.getBlockZ());

            if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                    loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                return true;
            }
        }
        return false;
    }

    // 检测玩家是否在某个队伍的核心保护区域
    public boolean isPlayerInTeamProtectionArea(Player player, String teamName) {
        // 验证队伍名称有效性
        if (teamName == null || !borderFirst.containsKey(teamName) || !borderSecond.containsKey(teamName)) {
            return false;
        }

        Location first = borderFirst.get(teamName);
        Location second = borderSecond.get(teamName);
        if (first == null || second == null) {
            return false;
        }

        Location loc = player.getLocation();

        // 计算保护区域边界
        int minX = Math.min(first.getBlockX(), second.getBlockX());
        int maxX = Math.max(first.getBlockX(), second.getBlockX());
        int minZ = Math.min(first.getBlockZ(), second.getBlockZ());
        int maxZ = Math.max(first.getBlockZ(), second.getBlockZ());

        // 可选：添加高度检测（如果需要）
        // int minY = Math.min(first.getBlockY(), second.getBlockY());
        // int maxY = Math.max(first.getBlockY(), second.getBlockY());

        // 检查玩家位置是否在矩形区域内
        boolean inX = loc.getBlockX() >= minX && loc.getBlockX() <= maxX;
        boolean inZ = loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
        // boolean inY = loc.getBlockY() >= minY && loc.getBlockY() <= maxY;

        return inX && inZ; // && inY; // 如果启用高度检测
    }
}