package cn.zhuobing.testPlugin.anniPlayer;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

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

    public RespawnDataManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        configFile = new File(dataFolder, "respawn-config.yml");
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
                        locations.add(location);
                    }
                }
                teamRespawnLocations.put(teamName, locations);
            }
        }
    }

    public void saveConfig() {
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
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        String targetTeam = teamName != null ? teamName : "lobby";
        Set<Location> respawnLocations = getRespawnLocations(targetTeam);
        if (respawnLocations.isEmpty()) {
            return;
        }
        List<Location> locationList = new ArrayList<>(respawnLocations);
        Random random = new Random();
        Location randomRespawnLocation = locationList.get(random.nextInt(locationList.size()));
        event.setRespawnLocation(randomRespawnLocation);
    }
}