// AnniConfigManager.java
package cn.zhuobing.testPlugin.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AnniConfigManager {
    private static FileConfiguration config;
    private static File configFile;

    // 配置项默认值
    public static int MIN_PLAYERS_TO_START = 4;
    public static int BOSS_HEALTH = 500;
    public static String MAP_CONFIG_FOLDER = "AnniMapConfig";

    public static void loadConfig(Plugin plugin) {
        configFile = new File(plugin.getDataFolder(), "anni-config.yml");

        // 创建默认配置
        if (!configFile.exists()) {
            createDefaultConfig(plugin);
        }

        // 加载配置
        reloadConfig();

        // 读取配置值
        MIN_PLAYERS_TO_START = config.getInt("settings.min-players-to-start", 4);
        BOSS_HEALTH = config.getInt("settings.boss-health", 500);
        MAP_CONFIG_FOLDER = config.getString("paths.map-config-folder", "AnniMapConfig");

        // 保存可能更新后的配置（用于添加缺失的配置项）
        saveConfig();
    }

    private static void createDefaultConfig(Plugin plugin) {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            String defaultConfig = "# anni-config.yml - 核心配置文件\n" +
                    "\n" +
                    "# 游戏设置\n" +
                    "settings:\n" +
                    "  min-players-to-start: 4  # 启动游戏需要的最小玩家数\n" +
                    "  boss-health: 500        # Boss的基础血量\n" +
                    "\n" +
                    "# 路径配置\n" +
                    "paths:\n" +
                    "  map-config-folder: 'AnniMapConfig'  # 地图配置文件夹名称\n";

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(defaultConfig);
                plugin.getLogger().info("已创建默认配置文件: anni-config.yml");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("创建配置文件失败: " + e.getMessage());
        }
    }

    public static void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static void saveConfig() {
        try {
            // 更新当前内存中的配置值
            config.set("settings.min-players-to-start", MIN_PLAYERS_TO_START);
            config.set("settings.boss-health", BOSS_HEALTH);
            config.set("paths.map-config-folder", MAP_CONFIG_FOLDER);

            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}