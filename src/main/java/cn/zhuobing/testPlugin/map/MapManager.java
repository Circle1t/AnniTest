package cn.zhuobing.testPlugin.map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapManager {
    private final List<Location> mapBorders = new ArrayList<>();
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;

    public MapManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        configFile = new File(dataFolder, "map-config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        if (config.contains("map.borders")) {
            for (String key : config.getConfigurationSection("map.borders").getKeys(false)) {
                Location border = config.getLocation("map.borders." + key);
                if (border != null) {
                    mapBorders.add(border);
                }
            }
        }
    }

    public void saveConfig() {
        config.set("map.borders", null);
        for (int i = 0; i < mapBorders.size(); i++) {
            config.set("map.borders.border" + (i + 1), mapBorders.get(i));
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setBorder(int borderNumber, Location location) {
        if (borderNumber >= 1 && borderNumber <= 4) {
            if (borderNumber > mapBorders.size()) {
                for (int i = mapBorders.size(); i < borderNumber; i++) {
                    mapBorders.add(null);
                }
            }
            mapBorders.set(borderNumber - 1, location);
            saveConfig();
        }
    }

    public Location getBorder(int borderNumber) {
        if (borderNumber >= 1 && borderNumber <= mapBorders.size()) {
            return mapBorders.get(borderNumber - 1);
        }
        return null;
    }

    public boolean isInsideBorder(Location loc) {
        if (mapBorders.size() != 4) {
            return false;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (Location border : mapBorders) {
            minX = Math.min(minX, border.getBlockX());
            maxX = Math.max(maxX, border.getBlockX());
            minZ = Math.min(minZ, border.getBlockZ());
            maxZ = Math.max(maxZ, border.getBlockZ());
        }

        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }
}