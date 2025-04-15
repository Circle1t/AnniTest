package cn.zhuobing.testPlugin.map;

import cn.zhuobing.testPlugin.anniPlayer.RespawnDataManager;
import cn.zhuobing.testPlugin.boss.BossDataManager;
import cn.zhuobing.testPlugin.boss.WitchDataManager;
import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.nexus.NexusInfoBoard;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.ore.DiamondDataManager;
import cn.zhuobing.testPlugin.ore.OreManager;
import cn.zhuobing.testPlugin.store.StoreManager;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.World.Environment;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static cn.zhuobing.testPlugin.utils.DirectoryUtil.copyDirectory;
import static cn.zhuobing.testPlugin.utils.DirectoryUtil.deleteDirectory;
import static org.bukkit.Bukkit.getLogger;

public class MapSelectManager {
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private List<String> candidateMaps = new ArrayList<>();
    private List<String> originalMaps = new ArrayList<>();
    private final Map<String, String> mapFolderNameMapping = new HashMap<>(); // 存储地图文件夹名与地图名的映射
    private final Map<String, Material> mapIcons = new HashMap<>();
    private final Map<String, Integer> voteCounts = new HashMap<>(); // 存储地图的投票数
    private final Map<UUID, String> playerVotedMap = new HashMap<>(); // 存储玩家UUID和他们投票的地图名称
    private final List<UUID> editingPlayers = new ArrayList<>(); // 储存处于编辑地图状态的玩家
    private String gameMap;
    private boolean votingLocked = false;

    private final BossDataManager bossDataManager;
    private final BorderManager borderManager;
    private final DiamondDataManager diamondDataManager;
    private final OreManager oreManager;
    private final NexusManager nexusManager;
    private final RespawnDataManager respawnDataManager;
    private final StoreManager storeManager;
    private final WitchDataManager witchDataManager;
    private final NexusInfoBoard nexusInfoBoard;

    public MapSelectManager(BossDataManager bossDataManager, BorderManager borderManager,
                            NexusManager nexusManager, DiamondDataManager diamondDataManager, OreManager oreManager, RespawnDataManager respawnDataManager,
                            StoreManager storeManager, WitchDataManager witchDataManager, GameManager gameManager, NexusInfoBoard nexusInfoBoard, Plugin plugin) {
        this.bossDataManager = bossDataManager;
        this.borderManager = borderManager;
        this.diamondDataManager = diamondDataManager;
        this.oreManager = oreManager;
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
            // 如果文件不存在，尝试创建
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try {
                if (configFile.createNewFile()) {
                    plugin.getLogger().info("maps-config.yml 文件创建成功");
                    // 写入默认配置
                    InputStream defaultConfigStream = plugin.getResource("maps-config.yml");
                    if (defaultConfigStream != null) {
                        config = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
                        config.save(configFile);
                    } else {
                        // 如果没有默认配置资源，设置一些基本的默认值
                        String defaultConfigContent = "# maps-config.yml\n" +
                                "gameMaps:                 # 游戏候选地图列表（文件夹名称，需存放在 plugins/插件名/maps/ 目录下）\n" +
                                "  - \"map1\"                # 地图模板1\n" +
                                "  - \"map2\"                # 地图模板2\n" +
                                "\n" +
                                "originalMaps:             # 原始地图列表（还未被配置的地图）\n" +
                                "  - \"original_map1\"       # 原始地图1\n" +
                                "  - \"original_map2\"       # 原始地图2\n" +
                                "\n" +
                                "mapIcons:                 # 地图投票图标配置\n" +
                                "  map1: GRASS_BLOCK       # 地图1的展示材质（必须使用有效的材质名称）\n" +
                                "  map2: NETHERRACK        # 地图2的展示材质\n" +
                                "\n" +
                                "mapFolderNameMapping:     # 地图文件夹名与显示名称映射\n" +
                                "  map1: \"草原地图\"        # 显示在投票界面的地图名称\n" +
                                "  map2: \"地狱岩地图\"";
                        try (FileWriter writer = new FileWriter(configFile)) {
                            writer.write(defaultConfigContent);
                        }
                        config = YamlConfiguration.loadConfiguration(configFile);
                    }
                } else {
                    plugin.getLogger().severe("无法创建 maps-config.yml 文件");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("创建 maps-config.yml 文件时出错: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        if (config.contains("gameMaps")) {
            candidateMaps = config.getStringList("gameMaps");
        }

        if (config.contains("mapIcons")) {
            for (String mapName : candidateMaps) {
                String iconName = config.getString("mapIcons." + mapName);
                if (iconName != null) {
                    try {
                        Material icon = Material.valueOf(iconName);
                        mapIcons.put(mapName, icon);
                    } catch (IllegalArgumentException e) {
                        // 处理无效的材质名称
                        plugin.getLogger().info("无效的地图材质图标" + mapName + ": " + iconName);
                    }
                } else {
                    // 处理图标名称为 null 的情况
                    plugin.getLogger().info("地图 " + mapName + " 的图标名称为 null，请检查配置文件。");
                }
            }
        }

        if (config.contains("originalMap")) {
            originalMaps = config.getStringList("originalMap");
        }

        // 加载地图文件夹名与地图名的映射
        if (config.contains("mapFolderNameMapping")) {
            Map<?, ?> mapping = config.getConfigurationSection("mapFolderNameMapping").getValues(false);
            for (Map.Entry<?, ?> entry : mapping.entrySet()) {
                mapFolderNameMapping.put((String) entry.getKey(), (String) entry.getValue());
            }
        }

        // 初始化投票计数
        for (String mapName : candidateMaps) {
            voteCounts.put(mapName, 0);
        }
    }

    public void saveConfig() {
        config.set("gameMaps", candidateMaps);
        config.set("originalMap", originalMaps);

        // 保存地图图标设置
        for (Map.Entry<String, Material> entry : mapIcons.entrySet()) {
            config.set("mapIcons." + entry.getKey(), entry.getValue().name());
        }
        // 保存地图文件夹名与地图名的映射
        config.set("mapFolderNameMapping", mapFolderNameMapping);

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getCandidateMaps() {
        return candidateMaps;
    }

    public List<String> getOriginalMaps(){
        return originalMaps;
    }

    public Material getMapIcon(String mapName) {
        return mapIcons.getOrDefault(mapName, Material.GRASS_BLOCK);
    }

    public void setMappingName(String mapName,String mappingName) {
        mapFolderNameMapping.put(mapName,mappingName);
        saveConfig();
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
            player.sendMessage(ChatColor.GREEN + "你已为 " + ChatColor.YELLOW + mapFolderNameMapping.get(mapName) + ChatColor.GREEN + " 地图投票！");
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
                // ===== 世界规则配置 =====
                world.setGameRule(GameRule.DO_MOB_SPAWNING, false);       // 禁止自然生物生成（如怪物、动物）
                world.setGameRule(GameRule.MOB_GRIEFING, false);          // 禁止怪物破坏地形（如苦力怕炸方块）
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);      // 启用昼夜循环（时间正常流逝）
                world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false); // 禁用成就提示
                world.setDifficulty(Difficulty.NORMAL);

                // ===== 天气控制 =====
                world.setStorm(false);                // 关闭下雨/下雪
                world.setWeatherDuration(Integer.MAX_VALUE); // 设置天气持续时间最大值（几乎永久晴朗）

                // ===== PVP 设置 =====
                world.setPVP(true); // 启用玩家间战斗
            }
            if (world != null) {
                getLogger().info("地图 " + gameMap + " 加载成功");

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

                getLogger().info("配置文件加载完成！地图：" + gameMap);
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

    public String getGameMapMappingName(){
        return mapFolderNameMapping.getOrDefault(gameMap,gameMap);
    }

    public String getMapMappingName(String mapName){
        return mapFolderNameMapping.getOrDefault(mapName, mapName);
    }

    public void removeEditingPlayer(Player player) {
        editingPlayers.remove(player.getUniqueId());
    }

    // 进入地图的方法
    public void enterMap(Player player, String mapName) {
        if(editingPlayers.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "请退出当前地图后再加入其他地图！");
            return;
        }
        World existing = plugin.getServer().getWorld(mapName);
        if (existing != null) {
            player.teleport(existing.getSpawnLocation());
        } else {
            File templateDir = new File(plugin.getDataFolder(), "maps/" + mapName);
            if (!templateDir.exists()) {
                player.sendMessage(ChatColor.RED + "地图模板不存在！");
                return;
            }

            File worldsDir = new File(".");
            File targetWorldDir = new File(worldsDir, mapName);

            if (!worldsDir.exists()) {
                worldsDir.mkdirs();
            }

            try {
                if (targetWorldDir.exists()) {
                    deleteDirectory(targetWorldDir);
                }
                copyDirectory(templateDir.toPath(), targetWorldDir.toPath());
            } catch (IOException e) {
                player.sendMessage(ChatColor.RED + "地图复制失败！");
                return;
            }

            WorldCreator creator = new WorldCreator(mapName);
            creator.environment(Environment.NORMAL);
            World world = plugin.getServer().createWorld(creator);

            if (world != null) {
                // ===== 世界规则配置 =====
                world.setGameRule(GameRule.DO_MOB_SPAWNING, false);       // 禁止自然生物生成（如怪物、动物）
                world.setGameRule(GameRule.MOB_GRIEFING, false);          // 禁止怪物破坏地形（如苦力怕炸方块）
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);      // 启用昼夜循环（时间正常流逝）
                world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false); // 禁用成就提示

                // ===== 天气控制 =====
                world.setStorm(false);                // 关闭下雨/下雪
                world.setWeatherDuration(Integer.MAX_VALUE); // 设置天气持续时间最大值（几乎永久晴朗）

                // ===== PVP 设置 =====
                world.setPVP(false); // 不启用玩家间战斗

                player.teleport(world.getSpawnLocation());
                editingPlayers.add(player.getUniqueId());

                // 设置玩家游戏模式为创造模式
                player.setGameMode(GameMode.CREATIVE);
                // 开启玩家飞行能力
                player.setAllowFlight(true);
                // 让玩家开始浮空悬停
                player.setFlying(true);

                player.sendMessage(ChatColor.GOLD + "[管理员模式]"+ ChatColor.AQUA + "你的游戏模式已被设置为创造模式，你现在可以开始配置地图了！");

                getLogger().info("地图 " + mapName + " 加载成功");

                player.sendMessage(ChatColor.GREEN + "已进入地图: " + ChatColor.YELLOW + mapFolderNameMapping.getOrDefault(mapName,world.getName()));

                getLogger().info("正在加载bossDataManager");
                // 加载border配置，从原地图文件夹读取配置文件
                borderManager.loadConfig(mapName,world);
                getLogger().info("正在加载borderManager");
                // 加载diamond配置，从原地图文件夹读取配置文件
                diamondDataManager.loadConfig(mapName,world);
                getLogger().info("正在加载diamondDataManager");
                // 加载nexus配置，从原地图文件夹读取配置文件
                nexusManager.loadConfig(mapName,world);
                getLogger().info("正在加载nexusManager");
                // 加载respawn配置，从原地图文件夹读取配置文件
                respawnDataManager.loadConfig(mapName,world);
                getLogger().info("正在加载respawnDataManager");
                // 加载store配置，从原地图文件夹读取配置文件
                storeManager.loadConfig(mapName,world);
                getLogger().info("正在加载storeManager");
                // 加载witch配置，从原地图文件夹读取配置文件
                witchDataManager.loadConfig(mapName,world);
                getLogger().info("正在加载witchDataManager");

                getLogger().info("配置文件加载完成！地图：" + mapName);
            } else {
                player.sendMessage(ChatColor.RED + "地图加载失败！");
            }
        }
    }

    public void unloadMap(String mapName) {
        World world = plugin.getServer().getWorld(mapName);
        if (world != null) {
            getLogger().info("正在卸载地图: " + mapName);
            plugin.getServer().unloadWorld(world, false);
            File worldDir = new File(".", mapName); // 使用服务器根目录
            deleteDirectory(worldDir);
            getLogger().info("地图 " + mapName + " 已成功卸载");
        } else {
            getLogger().info("地图 " + mapName + " 不存在，无法卸载");
        }
    }


    // 删除游戏地图副本文件夹
    public void unloadGameWorld() {
        if (gameMap != null) {
            World world = plugin.getServer().getWorld(gameMap);
            if (world != null) {
                // 1. 传送所有玩家
                for (Player p : world.getPlayers()) {
                    String kickMessage = ChatColor.RED + "你已被踢出服务器\n\n" + ChatColor.YELLOW + "游戏地图已被卸载！";
                    p.kickPlayer(kickMessage);
                }

                // 2. 卸载世界
                plugin.getServer().unloadWorld(world, false);

                // 3. 删除副本
                File worldDir = world.getWorldFolder();
                deleteDirectory(worldDir);
            }
            gameMap = null;
        }
    }
}