package cn.zhuobing.testPlugin.boss;

import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Witch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WitchDataManager implements Listener{
    private final Map<String, Location> teamWitchLocations = new HashMap<>();
    private final Map<UUID, Witch> activeWitches = new HashMap<>();
    private final Map<UUID, String> witchTeamMapping = new HashMap<>();
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private final TeamManager teamManager;

    public WitchDataManager(Plugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        loadConfig();

        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfig() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        configFile = new File(dataFolder, "witch-config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        if (config.contains("witch")) {
            for (String teamName : config.getConfigurationSection("witch").getKeys(false)) {
                Location location = config.getLocation("witch." + teamName);
                if (location != null) {
                    teamWitchLocations.put(teamName, location);
                }
            }
        }
    }

    public void saveConfig() {
        config.set("witch", null);
        for (Map.Entry<String, Location> entry : teamWitchLocations.entrySet()) {
            config.set("witch." + entry.getKey(), entry.getValue());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setWitchLocation(String teamName, Location location) {
        teamWitchLocations.put(teamName, location);
        saveConfig();
    }

    public void removeWitchLocation(String teamName) {
        teamWitchLocations.remove(teamName);
        saveConfig();
    }

    public Location getWitchLocation(String teamName) {
        return teamWitchLocations.get(teamName);
    }

    public boolean hasWitchLocation(String teamName) {
        return teamWitchLocations.containsKey(teamName);
    }

    public void startWitchesSpawn() {
        spawnWitches();
        startWitchPositionCheck();
    }

    private void spawnWitches() {
        for (String teamName : teamWitchLocations.keySet()) {
            Location location = teamWitchLocations.get(teamName);
            if (location != null) {
                spawnWitch(teamName, location);
            }
        }
    }

    private void spawnWitch(String teamName, Location location) {
        Witch witch = (Witch) location.getWorld().spawnEntity(location, EntityType.WITCH);
        witch.setMetadata("customWitch", new FixedMetadataValue(plugin, true));
        witch.setMetadata("witchTeam", new FixedMetadataValue(plugin, teamName));

        AttributeInstance maxHealth = witch.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20);
            witch.setHealth(maxHealth.getBaseValue());
        }
        activeWitches.put(witch.getUniqueId(), witch);
        witchTeamMapping.put(witch.getUniqueId(), teamName);

    }

    @EventHandler
    public void onWitchDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Witch)) return;
        Witch witch = (Witch) event.getEntity();

        // 检查是否为自定义女巫
        if (!witch.hasMetadata("customWitch") || !witch.hasMetadata("witchTeam")) return;

        // 处理掉落物
        event.getDrops().clear();
        generateCustomDrops(event.getDrops()); // 生成自定义掉落物

        // 处理重生逻辑
        String teamName = witch.getMetadata("witchTeam").get(0).asString();
        UUID witchUUID = witch.getUniqueId();

        activeWitches.remove(witchUUID);
        witchTeamMapping.remove(witchUUID);

        // 延迟3分钟重生
        new BukkitRunnable() {
            @Override
            public void run() {
                if (teamWitchLocations.containsKey(teamName)) {
                    Location loc = teamWitchLocations.get(teamName);
                    if (loc != null && loc.getWorld() != null) {
                        spawnWitch(teamName, loc);
                    }
                }
            }
        }.runTaskLater(plugin, 3 * 60 * 20L); // 3分钟 = 3*60*20 ticks
    }

    // 自定义掉落物生成方法
    private void generateCustomDrops(List<ItemStack> drops) {
        // 创建一个随机数生成器
        java.util.Random random = ThreadLocalRandom.current();
        // 生成一个 0 到 1 之间的随机数
        double randomValue = random.nextDouble();

        // 定义每种物品的掉落概率
        double fermentedSpiderEyeChance = 0.3;
        double gunpowderChance = 0.2;
        double glowstoneDustChance = 0.2;
        double sugarChance = 0.3;

        // 计算累计概率
        double cumulativeChance1 = fermentedSpiderEyeChance;
        double cumulativeChance2 = cumulativeChance1 + gunpowderChance;
        double cumulativeChance3 = cumulativeChance2 + glowstoneDustChance;

        // 根据随机数的值决定掉落哪种物品
        if (randomValue < cumulativeChance1) {
            drops.add(new ItemStack(Material.FERMENTED_SPIDER_EYE));
        } else if (randomValue < cumulativeChance2) {
            drops.add(new ItemStack(Material.GUNPOWDER));
        } else if (randomValue < cumulativeChance3) {
            drops.add(new ItemStack(Material.GLOWSTONE_DUST));
        } else {
            drops.add(new ItemStack(Material.SUGAR));
        }
    }


    private void startWitchPositionCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Witch> entry : new HashMap<>(activeWitches).entrySet()) {
                    Witch witch = entry.getValue();
                    String teamName = witchTeamMapping.get(entry.getKey());
                    Location spawnPoint = teamWitchLocations.get(teamName);

                    if (spawnPoint != null && witch.getLocation().distance(spawnPoint) > 15) {
                        witch.teleport(spawnPoint);
                        witch.setAI(false);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> witch.setAI(true), 20L);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}