package cn.zhuobing.testPlugin.utils;

import cn.zhuobing.testPlugin.game.GamePhase;
import cn.zhuobing.testPlugin.xp.XPManager; // 新增导入
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AnniConfigManager {
    // 配置项
    public static int MIN_PLAYERS_TO_START = 4;
    public static int BOSS_HEALTH = 500;
    public static String MAP_CONFIG_FOLDER = "AnniMapConfig";
    public static double APPLE_DROP_RATE = 3.0;
    public static boolean BUNGEE_ENABLED = false;
    public static String BUNGEE_LOBBY_SERVER = "lobby";
    public static List<GamePhase> GAME_PHASES = new ArrayList<>();
    // XP系统开关配置（默认开启）
    public static boolean XP_SYSTEM_ENABLED = true;

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

        // 读取基础配置值
        MIN_PLAYERS_TO_START = config.getInt("settings.min-players-to-start", 4);
        BOSS_HEALTH = config.getInt("settings.boss-health", 500);
        MAP_CONFIG_FOLDER = config.getString("paths.map-config-folder", "AnniMapConfig");
        APPLE_DROP_RATE = config.getDouble("settings.apple-drop-rate", 3.0);

        // 读取 BungeeCord 配置
        BUNGEE_ENABLED = config.getBoolean("bungeecord.enabled", false);
        BUNGEE_LOBBY_SERVER = config.getString("bungeecord.lobby-server", "lobby");

        // 读取XP系统开关配置
        XP_SYSTEM_ENABLED = config.getBoolean("xp-system.enabled", true);

        // 读取 Header 和 Footer 配置
        if (config.contains("header")) {
            String headerValue;
            if (config.isList("header")) {
                List<String> headerLines = config.getStringList("header");
                headerValue = String.join("\n", headerLines);
            } else {
                headerValue = config.getString("header", HEADER);
            }
            HEADER = ChatColor.translateAlternateColorCodes('&', headerValue);
        }

        if (config.contains("footer")) {
            String footerValue;
            if (config.isList("footer")) {
                List<String> footerLines = config.getStringList("footer");
                footerValue = String.join("\n", footerLines);
            } else {
                footerValue = config.getString("footer", FOOTER);
            }
            FOOTER = ChatColor.translateAlternateColorCodes('&', footerValue);
        }

        // 加载游戏阶段配置
        GAME_PHASES.clear();
        if (config.contains("phases")) {
            List<Map<?, ?>> phaseMaps = (List<Map<?, ?>>) config.getList("phases");
            if (phaseMaps != null) {
                for (Map<?, ?> phaseMap : phaseMaps) {
                    String name = (String) phaseMap.get("name");
                    int duration = (int) phaseMap.get("duration");
                    String colorStr = (String) phaseMap.get("color");
                    BarColor color;
                    try {
                        color = BarColor.valueOf(colorStr);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("无效的Boss血条颜色: " + colorStr + "，使用默认颜色 BLUE");
                        color = BarColor.BLUE;
                    }
                    GAME_PHASES.add(new GamePhase(name, duration, color));
                }
            }
        } else {
            // 创建默认阶段配置
            createDefaultPhases();
            plugin.getLogger().info("未找到阶段配置，使用默认阶段配置");
        }

        // 同步XP系统开关状态到XPManager
        XPManager.getInstance().setXpSystemEnabled(XP_SYSTEM_ENABLED);
        plugin.getLogger().info("XP系统状态: " + (XP_SYSTEM_ENABLED ? "已启用" : "已禁用"));

        // 保存可能更新后的配置
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
                    "# XP系统配置\n" +
                    "# ==============================\n" +
                    "xp-system:\n" +
                    "  enabled: true    # 是否启用XP系统（默认开启）\n" +
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
                    "  - \"&a🐱  GitHub Circle1t/AnniTest  插件已开源\"\n" +
                    "\n" +
                    "# ==============================\n" +
                    "# 游戏阶段配置\n" +
                    "# ==============================\n" +
                    "# 每个阶段包含以下属性：\n" +
                    "#   name: 阶段显示名称\n" +
                    "#   duration: 阶段持续时间(秒)\n" +
                    "#   color: Boss血条颜色(可选值: PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE)\n" +
                    "phases:\n" +
                    "  - name: \"游戏即将开始 请为地图投票\"\n" +
                    "    duration: 30\n" +
                    "    color: BLUE\n" +
                    "  - name: \"阶段一\"\n" +
                    "    duration: 600\n" +
                    "    color: BLUE\n" +
                    "  - name: \"阶段二\"\n" +
                    "    duration: 600\n" +
                    "    color: BLUE\n" +
                    "  - name: \"阶段三\"\n" +
                    "    duration: 600\n" +
                    "    color: BLUE\n" +
                    "  - name: \"阶段四\"\n" +
                    "    duration: 600\n" +
                    "    color: PURPLE\n" +
                    "  - name: \"阶段五\"\n" +
                    "    duration: 0\n" +
                    "    color: WHITE";

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(defaultConfig);
                plugin.getLogger().info("已创建默认配置文件: config.yml");
            }
            createDefaultPhases(); // 初始化默认阶段
        } catch (IOException e) {
            plugin.getLogger().severe("创建配置文件失败: " + e.getMessage());
        }
    }

    private static void createDefaultPhases() {
        GAME_PHASES.clear();
        GAME_PHASES.add(new GamePhase("游戏即将开始 请为地图投票", 30, BarColor.BLUE));
        GAME_PHASES.add(new GamePhase("阶段一", 600, BarColor.BLUE));
        GAME_PHASES.add(new GamePhase("阶段二", 600, BarColor.BLUE));
        GAME_PHASES.add(new GamePhase("阶段三", 600, BarColor.BLUE));
        GAME_PHASES.add(new GamePhase("阶段四", 600, BarColor.PURPLE));
        GAME_PHASES.add(new GamePhase("阶段五", 0, BarColor.WHITE));
    }

    public static void saveConfig(Plugin plugin, FileConfiguration config) {
        // 更新基础配置值
        config.set("settings.min-players-to-start", MIN_PLAYERS_TO_START);
        config.set("settings.boss-health", BOSS_HEALTH);
        config.set("settings.apple-drop-rate", APPLE_DROP_RATE);
        config.set("paths.map-config-folder", MAP_CONFIG_FOLDER);
        config.set("bungeecord.enabled", BUNGEE_ENABLED);
        config.set("bungeecord.lobby-server", BUNGEE_LOBBY_SERVER);

        // 保存XP系统开关配置
        config.set("xp-system.enabled", XP_SYSTEM_ENABLED);

        // 保存 Header 和 Footer
        config.set("header", Arrays.asList(HEADER.replace(ChatColor.COLOR_CHAR, '&').split("\n")));
        config.set("footer", Arrays.asList(FOOTER.replace(ChatColor.COLOR_CHAR, '&').split("\n")));

        // 保存阶段配置
        List<Map<String, Object>> phaseList = new ArrayList<>();
        for (GamePhase phase : GAME_PHASES) {
            Map<String, Object> phaseMap = new LinkedHashMap<>();
            phaseMap.put("name", phase.getName());
            phaseMap.put("duration", phase.getDuration());
            phaseMap.put("color", phase.getColor().name());
            phaseList.add(phaseMap);
        }
        config.set("phases", phaseList);

        // 保存配置
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("保存配置文件失败: " + e.getMessage());
        }
    }

    public static void reloadConfig(Plugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        loadConfig(plugin);
    }
}