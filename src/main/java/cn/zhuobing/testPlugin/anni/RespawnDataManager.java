package cn.zhuobing.testPlugin.anni;

import cn.zhuobing.testPlugin.map.LobbyManager;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class RespawnDataManager {
    private final Map<String, Set<Location>> teamRespawnLocations = new HashMap<>();
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private LobbyManager lobbyManager;
    private NexusManager nexusManager;
    private String mapFolderName;
    private static final long SAVE_DEBOUNCE_TICKS = 20L;
    private BukkitTask pendingSaveTask;

    public RespawnDataManager(LobbyManager lobbyManager, NexusManager nexusManager, Plugin plugin) {
        this.plugin = plugin;
        this.lobbyManager = lobbyManager;
        this.nexusManager = nexusManager;
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
        configFile = new File(configFolder, "respawn-config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        if (config.contains("team.respawnLocations")) {
            for (String teamName : config.getConfigurationSection("team.respawnLocations").getKeys(false)) {
                Set<Location> locations = new HashSet<>();
                for (String key : config.getConfigurationSection("team.respawnLocations." + teamName).getKeys(false)) {
                    Location location = config.getLocation("team.respawnLocations." + teamName + "." + key);
                    if (location != null) {
                        location.setWorld(world);
                        locations.add(location);
                    }
                }
                teamRespawnLocations.put(teamName, locations);
            }
        }
    }

    public void saveConfig() {
        if (config == null || configFile == null) return;
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
        configFile = new File(configFolder, "respawn-config.yml");

        config.set("team.respawnLocations", null);
        for (Map.Entry<String, Set<Location>> entry : teamRespawnLocations.entrySet()) {
            String teamName = entry.getKey();
            Set<Location> locations = entry.getValue();
            int index = 0;
            for (Location location : locations) {
                config.set("team.respawnLocations." + teamName + "." + index, location);
                index++;
            }
        }

        if (pendingSaveTask != null) pendingSaveTask.cancel();
        FileConfiguration toSave = new YamlConfiguration();
        toSave.set("team.respawnLocations", null);
        for (Map.Entry<String, Set<Location>> entry : teamRespawnLocations.entrySet()) {
            String teamName = entry.getKey();
            Set<Location> locations = entry.getValue();
            int index = 0;
            for (Location location : locations) {
                toSave.set("team.respawnLocations." + teamName + "." + index, location);
                index++;
            }
        }
        File fileToSave = configFile;
        pendingSaveTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingSaveTask = null;
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    toSave.save(fileToSave);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }, SAVE_DEBOUNCE_TICKS);
    }

    public void addRespawnLocation(String teamName, Location location) {
        teamRespawnLocations.computeIfAbsent(teamName, k -> new HashSet<>()).add(location);
        saveConfig();
    }

    public void removeRespawnLocations(String teamName) {
        teamRespawnLocations.remove(teamName);
        saveConfig();
    }

    public Set<Location> getRespawnLocations(String teamName) {
        return teamRespawnLocations.getOrDefault(teamName, new HashSet<>());
    }

    /**
     * 将玩家传送到指定队伍的随机复活点
     * @param player 要传送的玩家
     * @param teamName 队伍名称
     * @return 如果成功传送返回 true，否则返回 false
     */
    public boolean teleportPlayerToRandomRespawnLocation(Player player, String teamName) {
        Set<Location> respawnLocations = getRespawnLocations(teamName);
        if (respawnLocations.isEmpty()) {
            return false;
        }
        List<Location> locationList = new ArrayList<>(respawnLocations);
        Random random = new Random();
        Location randomRespawnLocation = locationList.get(random.nextInt(locationList.size()));
        return player.teleport(randomRespawnLocation);
    }

    /**
     * 处理玩家重生逻辑
     * @param player 重生的玩家
     * @param teamName 玩家所在队伍名称，如果为 null 表示无队伍
     * @param event 玩家重生事件，可以为 null
     */
    public void handlePlayerRespawn(Player player, String teamName, PlayerRespawnEvent event) {
        if (teamName == null || nexusManager.getNexusHealth(teamName) <= 0) {
            // 改为使用LobbyManager处理
            event.setRespawnLocation(lobbyManager.getRandomRespawn());
            return;
        }
        Set<Location> respawnLocations = getRespawnLocations(teamName);
        if (respawnLocations.isEmpty()) {
            return;
        }
        List<Location> locationList = new ArrayList<>(respawnLocations);
        Random random = new Random();
        Location randomRespawnLocation = locationList.get(random.nextInt(locationList.size()));
        event.setRespawnLocation(randomRespawnLocation);
    }
}