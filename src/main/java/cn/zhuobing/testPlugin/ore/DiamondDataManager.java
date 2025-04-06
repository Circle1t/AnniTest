package cn.zhuobing.testPlugin.ore;

import cn.zhuobing.testPlugin.utils.AnniConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DiamondDataManager {
    private final Set<Location> diamondSpawnLocations = new HashSet<>();
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private String mapFolderName;

    public DiamondDataManager(Plugin plugin) {
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
        File configFolder = new File(mapFolder, AnniConfig.ANNI_MAP_CONFIG);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        configFile = new File(configFolder, "diamond-config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        if (config.contains("diamond.spawnLocations")) {
            for (String key : config.getConfigurationSection("diamond.spawnLocations").getKeys(false)) {
                Location location = config.getLocation("diamond.spawnLocations." + key);
                if (location != null) {
                    location.setWorld(world);
                    diamondSpawnLocations.add(location);
                }
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
        File configFolder = new File(mapFolder, AnniConfig.ANNI_MAP_CONFIG);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        configFile = new File(configFolder, "diamond-config.yml");

        config.set("diamond.spawnLocations", null);
        int index = 0;
        for (Location location : diamondSpawnLocations) {
            config.set("diamond.spawnLocations." + index, location);
            index++;
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addDiamondSpawnLocation(Location location) {
        diamondSpawnLocations.add(location);
        saveConfig();
    }

    public void removeDiamondSpawnLocation(Location location) {
        diamondSpawnLocations.remove(location);
        saveConfig();
    }

    public Set<Location> getDiamondSpawnLocations() {
        return diamondSpawnLocations;
    }
}