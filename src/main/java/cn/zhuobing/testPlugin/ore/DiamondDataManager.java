package cn.zhuobing.testPlugin.ore;

import org.bukkit.Location;
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

    public DiamondDataManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        // 获取插件数据文件夹
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        // 创建配置文件
        configFile = new File(dataFolder, "diamond-config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 加载配置文件
        config = YamlConfiguration.loadConfiguration(configFile);
        // 加载钻石生成点位置
        if (config.contains("diamond.spawnLocations")) {
            for (String key : config.getConfigurationSection("diamond.spawnLocations").getKeys(false)) {
                Location location = config.getLocation("diamond.spawnLocations." + key);
                if (location != null) {
                    diamondSpawnLocations.add(location);
                }
            }
        }
    }

    public void saveConfig() {
        // 先清空旧的配置项
        config.set("diamond.spawnLocations", null);
        int index = 0;
        // 保存钻石生成点位置
        for (Location location : diamondSpawnLocations) {
            config.set("diamond.spawnLocations." + index, location);
            index++;
        }
        try {
            // 保存配置文件
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addDiamondSpawnLocation(Location location) {
        diamondSpawnLocations.add(location);
        saveConfig();
    }

    // 移除钻石生成点
    public void removeDiamondSpawnLocation(Location location) {
        diamondSpawnLocations.remove(location);
        saveConfig();
    }

    public Set<Location> getDiamondSpawnLocations() {
        return diamondSpawnLocations;
    }
}