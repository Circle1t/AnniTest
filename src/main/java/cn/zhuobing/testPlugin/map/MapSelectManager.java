package cn.zhuobing.testPlugin.map;

import cn.zhuobing.testPlugin.anniPlayer.RespawnDataManager;
import cn.zhuobing.testPlugin.boss.BossDataManager;
import cn.zhuobing.testPlugin.boss.WitchDataManager;
import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.nexus.NexusInfoBoard;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.ore.DiamondDataManager;
import cn.zhuobing.testPlugin.store.StoreManager;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.Bukkit.getLogger;

public class MapSelectManager {
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private List<String> candidateMaps = new ArrayList<>();
    private final Map<String, Material> mapIcons = new HashMap<>();
    private final Map<String, Integer> voteCounts = new HashMap<>(); // 存储地图的投票数
    private final Map<UUID, String> playerVotedMap = new HashMap<>(); // 存储玩家UUID和他们投票的地图名称
    private String gameMap;
    private boolean votingLocked = false;

    private final BossDataManager bossDataManager;
    private final BorderManager borderManager;
    private final DiamondDataManager diamondDataManager;
    private final NexusManager nexusManager;
    private final RespawnDataManager respawnDataManager;
    private final StoreManager storeManager;
    private final WitchDataManager witchDataManager;
    private final NexusInfoBoard nexusInfoBoard;

    public MapSelectManager(BossDataManager bossDataManager, BorderManager borderManager,
                            NexusManager nexusManager, DiamondDataManager diamondDataManager, RespawnDataManager respawnDataManager,
                            StoreManager storeManager, WitchDataManager witchDataManager, GameManager gameManager, NexusInfoBoard nexusInfoBoard, Plugin plugin) {
        this.bossDataManager = bossDataManager;
        this.borderManager = borderManager;
        this.diamondDataManager = diamondDataManager;
        this.nexusManager = nexusManager;
        this.respawnDataManager = respawnDataManager;
        this.storeManager = storeManager;
        this.witchDataManager = witchDataManager;
        this.nexusInfoBoard = nexusInfoBoard;
        gameManager.setMapSelectManager(this);
        nexusInfoBoard.setMapSelectManager(this);
        this.plugin = plugin;

        loadConfig();
    }

    private void loadConfig() {
        File dataFolder = plugin.getDataFolder();
        configFile = new File(dataFolder, "maps-config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        if (config.contains("gameMaps")) {
            candidateMaps = config.getStringList("gameMaps");
        }

        if (config.contains("mapIcons")) {
            for (String mapName : candidateMaps) {
                String iconName = config.getString("mapIcons." + mapName);
                try {
                    Material icon = Material.valueOf(iconName);
                    mapIcons.put(mapName, icon);
                } catch (IllegalArgumentException e) {
                    // 处理无效的材质名称
                    System.err.println("无效的地图材质图标" + mapName + ": " + iconName);
                }
            }
        }

        // 初始化投票计数
        for (String mapName : candidateMaps) {
            voteCounts.put(mapName, 0);
        }
    }

    public void saveConfig() {
        config.set("gameMaps", candidateMaps);
        for (Map.Entry<String, Material> entry : mapIcons.entrySet()) {
            config.set("mapIcons." + entry.getKey(), entry.getValue().name());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getCandidateMaps() {
        return candidateMaps;
    }

    public Material getMapIcon(String mapName) {
        return mapIcons.getOrDefault(mapName, Material.GRASS_BLOCK);
    }

    public void setMapIcon(String mapName, Material icon) {
        mapIcons.put(mapName, icon);
        saveConfig();
    }

    // 记录投票
    public void recordVote(UUID playerUUID, String mapName) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (votingLocked) {
            if (player != null) {
                player.sendMessage(ChatColor.RED + "投票已结束，无法再投票！");
            }
            return;
        }
        if (playerVotedMap.containsKey(playerUUID)) {
            String previousMap = playerVotedMap.get(playerUUID);
            removeVote(previousMap);
        }
        if (voteCounts.containsKey(mapName)) {
            voteCounts.put(mapName, voteCounts.get(mapName) + 1);
            playerVotedMap.put(playerUUID, mapName);
        }
        nexusInfoBoard.updateInfoBoard();
        if (player != null) {
            player.sendMessage(ChatColor.GREEN + "你已为 " + ChatColor.YELLOW + mapName + ChatColor.GREEN + " 地图投票！");
        }
    }

    public void removeVote(String mapName) {
        if (voteCounts.containsKey(mapName)) {
            voteCounts.put(mapName, voteCounts.get(mapName) - 1);
        }
    }

    public void lockVoting() {
        votingLocked = true;
        // 锁定当前地图并加载
        this.loadHighestVotedMap();
        nexusInfoBoard.setVoteFlag(false);
        nexusInfoBoard.updateInfoBoard();
    }

    // 获取地图的投票数
    public int getVoteCount(String mapName) {
        return voteCounts.getOrDefault(mapName, 0);
    }

    // 检查玩家是否已为特定地图投票
    public boolean hasVoted(UUID playerUUID, String mapName) {
        return playerVotedMap.containsKey(playerUUID) && playerVotedMap.get(playerUUID).equals(mapName);
    }

    // 获取得票最高的地图
    public String getHighestVotedMap() {
        String highestVotedMap = null;
        int highestVoteCount = -1;
        for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > highestVoteCount) {
                highestVoteCount = entry.getValue();
                highestVotedMap = entry.getKey();
            }
        }
        if (highestVotedMap == null) {
            highestVotedMap = candidateMaps.get(0);
        }
        return highestVotedMap;
    }

    // 加载得票最高的地图到游戏中
    public void loadHighestVotedMap() {
        if (candidateMaps.isEmpty()) {
            getLogger().severe("没有可用的候选地图！");
            return;
        }

        gameMap = getHighestVotedMap();
        getLogger().info("正在加载地图: " + gameMap);

        // 添加世界存在检查
        World existing = plugin.getServer().getWorld(gameMap);
        if (existing != null) {
            getLogger().warning("检测到已存在同名世界，正在卸载...");
            plugin.getServer().unloadWorld(existing, false);
        }

        if (gameMap != null) {
            // 定义服务器根目录下的worlds文件夹路径
            File worldsDir = new File("."); // 这里的"."代表当前目录，即服务器根目录
            File targetWorldDir = new File(worldsDir, gameMap);

            // 创建worlds文件夹（如果不存在）
            if (!worldsDir.exists()) {
                worldsDir.mkdirs();
            }

            // 1. 定义路径
            File templateDir = new File(plugin.getDataFolder(), "maps/" + gameMap);

            // 2. 验证模板
            if (!validateWorldTemplate(templateDir)) {
                getLogger().severe("游戏地图模板不完整: " + templateDir);
                return;
            }

            // 3. 创建副本到服务器根目录
            try {
                if (targetWorldDir.exists()) {
                    deleteDirectory(targetWorldDir);
                }
                copyDirectory(templateDir.toPath(), targetWorldDir.toPath());
            } catch (IOException e) {
                getLogger().severe("地图复制失败: " + e.getMessage());
                return;
            }

            // 4. 正确加载世界
            WorldCreator creator = new WorldCreator(gameMap);
            creator.environment(Environment.NORMAL);
            World world = plugin.getServer().createWorld(creator);

            if (world != null) {
                // 设置世界规则
                world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                world.setGameRule(GameRule.MOB_GRIEFING, false);
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);

                // 设置PVP
                world.setPVP(true);
            }
            if (world != null) {
                getLogger().info("地图 " + gameMap + " 加载成功");

                // 加载boss配置，从原地图文件夹读取配置文件
                bossDataManager.loadConfig(gameMap,world);
                getLogger().info("正在加载bossDataManager");
                // 加载border配置，从原地图文件夹读取配置文件
                borderManager.loadConfig(gameMap,world);
                getLogger().info("正在加载borderManager");
                // 加载diamond配置，从原地图文件夹读取配置文件
                diamondDataManager.loadConfig(gameMap,world);
                getLogger().info("正在加载diamondDataManager");
                // 加载nexus配置，从原地图文件夹读取配置文件
                nexusManager.loadConfig(gameMap,world);
                getLogger().info("正在加载nexusManager");
                // 加载respawn配置，从原地图文件夹读取配置文件
                respawnDataManager.loadConfig(gameMap,world);
                getLogger().info("正在加载respawnDataManager");
                // 加载store配置，从原地图文件夹读取配置文件
                storeManager.loadConfig(gameMap,world);
                getLogger().info("正在加载storeManager");
                // 加载witch配置，从原地图文件夹读取配置文件
                witchDataManager.loadConfig(gameMap,world);
                getLogger().info("正在加载witchDataManager");

                getLogger().info("游戏地图加载完成！地图：" + gameMap);
            } else {
                getLogger().severe("地图 " + gameMap + " 加载失败");
            }
        }
    }

    // 验证世界完整性的方法
    private boolean validateWorldTemplate(File dir) {
        return new File(dir, "level.dat").exists() &&
                new File(dir, "region").exists();
    }

    public String getGameMap() {
        return gameMap;
    }

    // 新增方法：删除游戏地图副本文件夹
    public void unloadGameWorld() {
        if (gameMap != null) {
            File gameWorldDir = new File(".", gameMap); // 使用服务器根目录
            deleteDirectory(gameWorldDir);
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