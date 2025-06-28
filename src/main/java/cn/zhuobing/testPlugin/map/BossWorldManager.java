package cn.zhuobing.testPlugin.map;

import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import cn.zhuobing.testPlugin.utils.BungeeUtil;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;

import static cn.zhuobing.testPlugin.utils.DirectoryUtil.copyDirectory;
import static cn.zhuobing.testPlugin.utils.DirectoryUtil.deleteDirectory;

public class BossWorldManager {
    private final LobbyManager lobbyManager;
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private World bossWorld;

    public BossWorldManager(LobbyManager lobbyManager, Plugin plugin) {
        this.lobbyManager = lobbyManager;
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "boss-config.yml");
        if (!configFile.exists()) {
            // 如果文件不存在，尝试创建
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try {
                if (configFile.createNewFile()) {
                    plugin.getLogger().info("boss-config.yml 文件创建成功");
                    // 写入默认配置
                    InputStream defaultConfigStream = plugin.getResource("boss-config.yml");
                    if (defaultConfigStream != null) {
                        config = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
                        config.save(configFile);
                    } else {
                        // 如果没有默认配置资源，写入默认内容
                        String defaultConfigContent = "# boss-config.yml\n" +
                                "bossMap: \"BossTemplate\"    # Boss地图模板名称（需存放在 plugins/插件名/maps/ 目录下）\n" +
                                "bossSpawn:                 # Boss生成点坐标（建议参数都在游戏里配置）\n" +
                                "  world: AnniBoss          # 必须为 AnniBoss 世界\n" +
                                "  x: 100.5\n" +
                                "  y: 64.0\n" +
                                "  z: 200.5\n" +
                                "  yaw: 0.0\n" +
                                "  pitch: 0.0\n" +
                                "teamTpLocations:           # 队伍传送点坐标\n" +
                                "  red:                     # 红队传送点\n" +
                                "    world: AnniBoss\n" +
                                "    x: -150.5\n" +
                                "    y: 64.0\n" +
                                "    z: 300.5\n" +
                                "    yaw: 90.0\n" +
                                "    pitch: 0.0\n" +
                                "  blue:                    # 蓝队传送点\n" +
                                "    world: AnniBoss\n" +
                                "    x: 200.5\n" +
                                "    y: 64.0\n" +
                                "    z: -100.5\n" +
                                "    yaw: -90.0\n" +
                                "    pitch: 0.0";
                        try (FileWriter writer = new FileWriter(configFile)) {
                            writer.write(defaultConfigContent);
                        } catch (IOException e) {
                            plugin.getLogger().severe("写入默认配置到文件时出错: " + e.getMessage());
                        }
                        config = YamlConfiguration.loadConfiguration(configFile);
                    }
                } else {
                    plugin.getLogger().severe("无法创建 boss-config.yml 文件");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("创建 boss-config.yml 文件时出错: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadBossWorld();
    }

    private void loadBossWorld() {
        String templateName = config.getString("bossMap");
        File templateDir = new File(plugin.getDataFolder(), "maps/" + templateName);

        try {
            // 复制模板到服务器目录
            File targetDir = new File(plugin.getServer().getWorldContainer(), "AnniBoss");
            if (targetDir.exists()) deleteDirectory(targetDir);
            copyDirectory(templateDir.toPath(), targetDir.toPath());

            plugin.getLogger().info("Boss世界加载中...");

            // 加载世界
            WorldCreator creator = new WorldCreator("AnniBoss");
            creator.environment(World.Environment.NORMAL);
            bossWorld = plugin.getServer().createWorld(creator);

            // 设置世界规则
            if (bossWorld != null) {
                bossWorld.setPVP(true);
                bossWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);       // 禁止自然生物生成（如怪物、动物）
                bossWorld.setGameRule(GameRule.MOB_GRIEFING, false);          // 禁止怪物破坏地形（如苦力怕炸方块）
                bossWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false); // 禁用成就提示
                bossWorld.setDifficulty(Difficulty.NORMAL);
            }

            plugin.getLogger().info("Boss世界加载成功！");
        } catch (IOException e) {
            plugin.getLogger().warning("Boss世界加载失败: " + e.getMessage());
        }
    }

    public void enterBossMap(Player player) {
        if (bossWorld == null) loadBossWorld();
        Location spawn = bossWorld.getSpawnLocation();
        spawn.setY(spawn.getY() + 2); // 防止卡在方块中
        player.teleport(spawn);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "你已进入boss世界！");

        // 设置玩家游戏模式为创造模式
        player.setGameMode(GameMode.CREATIVE);
        // 开启玩家飞行能力
        player.setAllowFlight(true);
        // 让玩家开始浮空悬停
        player.setFlying(true);

        player.sendMessage(ChatColor.GOLD + "[管理员模式]"+ ChatColor.AQUA + "你的游戏模式已被设置为创造模式！");
    }

    public void leaveBossMap(Player player) {
        lobbyManager.teleportToLobby(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "你已离开boss世界！");
    }

    public World getBossWorld() {
        return bossWorld;
    }

    public void unloadBossWorld() {
        if (bossWorld != null) {
            // 1. 传送所有玩家
            for (Player p : bossWorld.getPlayers()) {
                if(AnniConfigManager.BUNGEE_ENABLED){
                    BungeeUtil.sendToLobby(p);
                }else{
                    String kickMessage = ChatColor.RED + "你已被踢出服务器\n\n" + ChatColor.YELLOW + "Boss 世界地图已被卸载！";
                    p.kickPlayer(kickMessage);
                }
            }

            // 2. 卸载世界
            plugin.getServer().unloadWorld(bossWorld, false);

            // 3. 删除副本
            File worldDir = bossWorld.getWorldFolder();
            deleteDirectory(worldDir);

            // 4. 将 bossWorld 置为 null
            bossWorld = null;
        }
    }
}