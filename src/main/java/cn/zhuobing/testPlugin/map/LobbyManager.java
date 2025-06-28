package cn.zhuobing.testPlugin.map;

import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import cn.zhuobing.testPlugin.utils.BungeeUtil;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.World.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static cn.zhuobing.testPlugin.utils.DirectoryUtil.copyDirectory;
import static cn.zhuobing.testPlugin.utils.DirectoryUtil.deleteDirectory;

public class LobbyManager {
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private World lobbyWorld;
    private final List<Location> lobbyRespawns = new ArrayList<>();

    public LobbyManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File dataFolder = plugin.getDataFolder();
        configFile = new File(dataFolder, "lobby-config.yml");
        if (!configFile.exists()) {
            // 如果文件不存在，尝试创建
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try {
                if (configFile.createNewFile()) {
                    plugin.getLogger().info("lobby-config.yml 文件创建成功");
                    // 写入默认配置
                    InputStream defaultConfigStream = plugin.getResource("lobby-config.yml");
                    if (defaultConfigStream != null) {
                        config = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
                        config.save(configFile);
                    } else {
                        // 如果没有默认配置资源，写入默认内容
                        String defaultContent = "# lobby-config.yml\n" +
                                "lobbyMap: \"defaultLobby\"  # 大厅地图模板名称（需存放在 plugins/插件名/maps/ 目录下）\n" +
                                "respawnPoints:            # 大厅重生点坐标列表（自动生成，请勿手动编辑）\n" +
                                "  '0':                      # 第一个重生点，可以添加多个\n" +
                                "    ==: org.bukkit.Location    \n" +
                                "    x: 100.5              # X坐标（你可以在本地游戏中获取 X Y Z 坐标，并配置在此处）\n" +
                                "    y: 64.0               # Y坐标\n" +
                                "    z: 200.5              # Z坐标\n" +
                                "    yaw: 0.0              # 水平朝向角度\n" +
                                "    pitch: 0.0            # 垂直俯仰角度";
                        try (FileWriter writer = new FileWriter(configFile)) {
                            writer.write(defaultContent);
                        }
                        config = YamlConfiguration.loadConfiguration(configFile);
                    }
                } else {
                    plugin.getLogger().severe("无法创建 lobby-config.yml 文件");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("创建 lobby-config.yml 文件时出错: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // 加载大厅地图名称
        String worldName = config.getString("lobbyMap");
        if (worldName != null) {
            plugin.getLogger().info("正在加载大厅地图: " + worldName);
            loadLobbyWorld(worldName);
        } else {
            plugin.getLogger().info("配置文件中 'lobbyMap' 键不存在或值为 null");
        }
    }

    public void saveConfig() {
        config.set("respawnPoints", null);
        for (int i = 0; i < lobbyRespawns.size(); i++) {
            Location loc = lobbyRespawns.get(i);
            loc.setWorld(null);
            config.set("respawnPoints." + i, loc);
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLobbyWorld(String templateName) {
        try {
            // 1. 检查模板路径
            File templateDir = new File(plugin.getDataFolder(), "maps/" + templateName);
            if (!templateDir.exists()) {
                plugin.getLogger().info("大厅模板目录不存在: " + templateDir.getAbsolutePath());
                return;
            }

            // 2. 删除旧世界
            File targetWorldDir = new File(plugin.getServer().getWorldContainer(), "AnniLobby");
            if (targetWorldDir.exists()) {
                plugin.getLogger().info("正在删除旧大厅世界...");
                deleteDirectory(targetWorldDir);
            }

            // 3. 复制模板（捕获异常）
            plugin.getLogger().info("正在复制大厅模板...");
            copyDirectory(templateDir.toPath(), targetWorldDir.toPath());

            // 4. 加载世界（添加环境参数）
            WorldCreator creator = new WorldCreator("AnniLobby");
            creator.environment(Environment.NORMAL);
            creator.type(WorldType.FLAT); // 根据模板类型调整
            lobbyWorld = plugin.getServer().createWorld(creator);

            if (lobbyWorld == null) {
                plugin.getLogger().info("大厅世界加载失败！");
                return;
            }

            // ===== 大厅规则配置 =====
            lobbyWorld.setPVP(false); // 禁用大厅PVP
            lobbyWorld.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false); // 隐藏死亡消息
            lobbyWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            lobbyWorld.setDifficulty(Difficulty.EASY); // 和平模式

            // ===== 大厅天气控制 =====
            lobbyWorld.setStorm(false);                // 强制关闭下雨
            lobbyWorld.setWeatherDuration(Integer.MAX_VALUE); // 永久晴朗
            lobbyWorld.setThundering(false);           // 关闭雷暴

            // 加载大厅重生点
            if (config.contains("respawnPoints")) {
                for (String key : config.getConfigurationSection("respawnPoints").getKeys(false)) {
                    Location loc = config.getLocation("respawnPoints." + key);
                    if (loc != null) {
                        loc.setWorld(lobbyWorld);
                        lobbyRespawns.add(loc);
                    }
                }
            }
            if(!lobbyRespawns.isEmpty()){
                plugin.getLogger().info("大厅世界加载成功！");
            }else{
                plugin.getLogger().info("大厅世界重生点列表为空！大厅无法进入...");
            }
        } catch (IOException e) {
            plugin.getLogger().info("大厅世界加载过程中发生IO异常: " + e.getMessage());
        }
    }

    public void addRespawnPoint(Location location) {
        lobbyRespawns.add(location);
        saveConfig();
    }

    public boolean teleportToLobby(Player player) {
        if (!lobbyRespawns.isEmpty()) {
            Location spawn = getRandomRespawn();
            spawn.setWorld(lobbyWorld);
            return player.teleport(spawn);
        }
        return false;
    }

    public World getLobbyWorld() {
        return lobbyWorld;
    }

    public List<Location> getLobbyRespawns() {
        return lobbyRespawns;
    }

    public Location getRandomRespawn() {
        if (lobbyRespawns.isEmpty()) {
            // 添加默认重生点（大厅出生点）
            Location defaultSpawn = lobbyWorld.getSpawnLocation();
            lobbyRespawns.add(defaultSpawn);
            plugin.getLogger().warning("重生点列表为空，已添加默认出生点！");
        }
        return lobbyRespawns.get(new Random().nextInt(lobbyRespawns.size()));
    }

    // 删除大厅地图副本文件夹
    public void unloadLobbyWorld() {
        if (lobbyWorld != null) {
            // 1. 传送所有玩家
            for (Player p : lobbyWorld.getPlayers()) {
                if (AnniConfigManager.BUNGEE_ENABLED) {
                    BungeeUtil.sendToLobby(p);
                } else {
                    String kickMessage = ChatColor.RED + "你已被踢出服务器\n\n" + ChatColor.YELLOW + "服务器已关闭";
                    p.kickPlayer(kickMessage);
                }
            }
            // 2. 卸载世界
            plugin.getServer().unloadWorld(lobbyWorld, false);

            // 3. 删除副本
            File worldDir = lobbyWorld.getWorldFolder();
            deleteDirectory(worldDir);
        }
    }
}