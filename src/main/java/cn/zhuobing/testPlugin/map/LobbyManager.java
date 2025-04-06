package cn.zhuobing.testPlugin.map;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.World.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
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
    }

    public void saveConfig() {
        // 保存重生点
        config.set("respawnPoints", null);
        for (int i = 0; i < lobbyRespawns.size(); i++) {
            config.set("respawnPoints." + i, lobbyRespawns.get(i));
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

            // 5. 配置规则（确保PVP关闭）
            lobbyWorld.setPVP(false);
            lobbyWorld.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            plugin.getLogger().info("大厅世界加载成功！");

        } catch (IOException e) {
            plugin.getLogger().info("大厅世界加载过程中发生IO异常: " + e.getMessage());
        }
    }

    // 验证世界完整性的方法
    private boolean validateWorldTemplate(File dir) {
        return new File(dir, "level.dat").exists() &&
                new File(dir, "region").exists();
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
        return lobbyRespawns.get(new Random().nextInt(lobbyRespawns.size()));
    }

    // 删除大厅地图副本文件夹
    public void unloadLobbyWorld() {
        if (lobbyWorld != null) {
            // 1. 传送所有玩家
            for (Player p : lobbyWorld.getPlayers()) {
                p.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
            }

            // 2. 卸载世界
            plugin.getServer().unloadWorld(lobbyWorld, false);

            // 3. 删除副本
            File worldDir = lobbyWorld.getWorldFolder();
            deleteDirectory(worldDir);
        }
    }

    // 辅助方法：递归删除文件夹
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    // 辅助方法：复制目录
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source)
                .forEach(sourcePath -> {
                    try {
                        Path targetPath = target.resolve(source.relativize(sourcePath));
                        if (Files.isDirectory(sourcePath)) {
                            Files.createDirectories(targetPath);
                        } else {
                            Files.copy(sourcePath, targetPath);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}