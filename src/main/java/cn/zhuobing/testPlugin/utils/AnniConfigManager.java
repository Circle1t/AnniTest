package cn.zhuobing.testPlugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AnniConfigManager {
    // 配置项
    public static int MIN_PLAYERS_TO_START = 4;
    public static int BOSS_HEALTH = 500;
    public static String MAP_CONFIG_FOLDER = "AnniMapConfig";
    public static double APPLE_DROP_RATE = 3.0;
    public static boolean BUNGEE_ENABLED = false;
    public static String BUNGEE_LOBBY_SERVER = "lobby";

    // Header 和 Footer 配置
    public static String HEADER = ChatColor.GOLD + "核心战争\n" + ChatColor.YELLOW + "欢迎体验一个全新的核心战争！";
    public static String FOOTER = ChatColor.AQUA + "📺 Bilibili 烧烤蒸馏水\n" + ChatColor.GREEN + "🐱  GitHub Circle1t/AnniTest  插件已开源";

    public static void loadConfig(Plugin plugin) {
        // 确保插件有默认配置文件
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            createDefaultConfig(plugin);
        }

        // 加载配置
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // 读取配置值
        MIN_PLAYERS_TO_START = config.getInt("settings.min-players-to-start", 4);
        BOSS_HEALTH = config.getInt("settings.boss-health", 500);
        MAP_CONFIG_FOLDER = config.getString("paths.map-config-folder", "AnniMapConfig");
        APPLE_DROP_RATE = config.getDouble("settings.apple-drop-rate", 3.0);

        // 读取 BungeeCord 配置
        BUNGEE_ENABLED = config.getBoolean("bungeecord.enabled", false);
        BUNGEE_LOBBY_SERVER = config.getString("bungeecord.lobby-server", "lobby");

        // 读取 Header 和 Footer 配置
        if (config.contains("header")) {
            String headerValue;
            if (config.isList("header")) {
                // 多行格式
                List<String> headerLines = config.getStringList("header");
                headerValue = String.join("\n", headerLines);
            } else {
                // 单行格式
                headerValue = config.getString("header", HEADER);
            }
            // 转换颜色代码
            HEADER = ChatColor.translateAlternateColorCodes('&', headerValue);
        }

        if (config.contains("footer")) {
            String footerValue;
            if (config.isList("footer")) {
                // 多行格式
                List<String> footerLines = config.getStringList("footer");
                footerValue = String.join("\n", footerLines);
            } else {
                // 单行格式
                footerValue = config.getString("footer", FOOTER);
            }
            // 转换颜色代码
            FOOTER = ChatColor.translateAlternateColorCodes('&', footerValue);
        }

        // 保存可能更新后的配置（用于添加缺失的配置项）
        saveConfig(plugin, config);
    }

    private static void createDefaultConfig(Plugin plugin) {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            String defaultConfig = "# config.yml - 核心配置文件\n" +
                    "\n" +
                    "# ==============================\n" +
                    "# BungeeCord 跨服配置\n" +
                    "# ==============================\n" +
                    "bungeecord:\n" +
                    "  enabled: true    # 是否启用 BungeeCord 跨服传送功能\n" +
                    "  lobby-server: \"lobby\"  # BungeeCord 大厅服务器名称（必须与 BungeeCord 配置匹配）\n" +
                    "\n" +
                    "# ==============================\n" +
                    "# 游戏设置\n" +
                    "# ==============================\n" +
                    "settings:\n" +
                    "  min-players-to-start: 4 # 启动游戏需要的最小玩家数\n" +
                    "  boss-health: 500 # Boss的基础血量\n" +
                    "  apple-drop-rate: 3.0 # 玩家破坏树叶时苹果的掉落率（百分比）\n" +
                    "\n" +
                    "# ==============================\n" +
                    "# 路径配置\n" +
                    "# ==============================\n" +
                    "paths:\n" +
                    "  map-config-folder: \"AnniMapConfig\"  # 地图配置文件夹名称\n" +
                    "\n" +
                    "# ==============================\n" +
                    "# 界面显示配置\n" +
                    "# ==============================\n" +
                    "# Header 和 Footer 支持多行格式，使用 & 符号作为颜色代码\n" +
                    "# 单行格式: header: \"&6核心战争\\n&e欢迎体验一个全新的核心战争！\"\n" +
                    "# 多行格式: \n" +
                    "header:\n" +
                    "  - \"&6核心战争\"\n" +
                    "  - \"&e欢迎体验一个全新的核心战争！\"\n" +
                    "\n" +
                    "footer:\n" +
                    "  - \"&b📺 Bilibili 烧烤蒸馏水\"\n" +
                    "  - \"&a🐱  GitHub Circle1t/AnniTest  插件已开源\"";

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(defaultConfig);
                plugin.getLogger().info("已创建默认配置文件: config.yml");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("创建配置文件失败: " + e.getMessage());
        }
    }

    public static void saveConfig(Plugin plugin, FileConfiguration config) {
        // 更新配置值
        config.set("settings.min-players-to-start", MIN_PLAYERS_TO_START);
        config.set("settings.boss-health", BOSS_HEALTH);
        config.set("settings.apple-drop-rate", APPLE_DROP_RATE);
        config.set("paths.map-config-folder", MAP_CONFIG_FOLDER);
        config.set("bungeecord.enabled", BUNGEE_ENABLED);
        config.set("bungeecord.lobby-server", BUNGEE_LOBBY_SERVER);

        // 保存 Header 和 Footer 配置（使用多行格式）
        // 注意：保存时使用原始格式（带&符号），不保存转换后的颜色代码
        config.set("header", Arrays.asList(HEADER.replace(ChatColor.COLOR_CHAR, '&').split("\n")));
        config.set("footer", Arrays.asList(FOOTER.replace(ChatColor.COLOR_CHAR, '&').split("\n")));

        // 保存配置
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("保存配置文件失败: " + e.getMessage());
        }
    }

    // 添加重载配置的方法
    public static void reloadConfig(Plugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        loadConfig(plugin);
    }
}