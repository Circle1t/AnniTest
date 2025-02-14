package cn.zhuobing.testPlugin.boss;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BossDataManager {
    private final Map<String, Location> teamBossLocations = new HashMap<>();
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private Wither boss;
    private boolean bossAlive = false;
    private UUID lastAttackerUUID; // 使用 UUID 存储最后攻击者
    private BossBar bossBar;
    private BukkitRunnable bossRespawnTask; // 用于控制 Boss 重生的任务
    private final Map<UUID, String> killerTeamMap = new HashMap<>(); // 使用 UUID 作为键
    private final Set<UUID> bossPlayers = new HashSet<>(); // 储存进入 boss 点的玩家
    private final GameManager gameManager;
    private final TeamManager teamManager;

    public BossDataManager(Plugin plugin, GameManager gameManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.gameManager = gameManager; // 初始化 GameManager
        this.teamManager = teamManager;
        loadConfig();
        this.bossBar = Bukkit.createBossBar(ChatColor.RED + "凋零 Boss", BarColor.RED, BarStyle.SOLID);
        bossBar.setVisible(false);
        // 设置 gameManager 中的 BossDataManager
        setBossDataManager();
    }

    private void loadConfig() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        configFile = new File(dataFolder, "boss-config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // 加载队伍 Boss 点和 Boss 生成位置
        if (config.contains("boss")) {
            for (String teamName : config.getConfigurationSection("boss").getKeys(false)) {
                Location location = config.getLocation("boss." + teamName);
                if (location != null) {
                    teamBossLocations.put(teamName, location);
                }
            }
        }
    }

    public void saveConfig() {
        config.set("boss", null); // 清空原有数据
        for (Map.Entry<String, Location> entry : teamBossLocations.entrySet()) {
            config.set("boss." + entry.getKey(), entry.getValue());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setBossLocation(String teamName, Location location) {
        teamBossLocations.put(teamName, location);
        saveConfig();
    }

    public Location getBossLocation(String teamName) {
        return teamBossLocations.get(teamName);
    }

    public boolean hasBossLocation(String teamName) {
        return teamBossLocations.containsKey(teamName);
    }

    public void spawnBossManually(Player player) {
        if (gameManager.getCurrentPhase() >= 4) {
            spawnBoss();
        } else {
            player.sendMessage(ChatColor.RED + "当前游戏阶段不允许生成 Boss。");
        }
    }

    public void spawnBoss() {
        Location bossLocation = getBossLocation("boss");
        if (bossLocation == null || bossAlive) {
            return;
        }

        boss = (Wither) bossLocation.getWorld().spawnEntity(bossLocation, EntityType.WITHER);
        boss.setMetadata("customBoss", new FixedMetadataValue(plugin, true));

        // 清除原版自带 bossbar
        clearOriginalBossBar();

        // 设置血量和 BossBar
        AttributeInstance maxHealth = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(300);
            boss.setHealth(maxHealth.getBaseValue());
        }

        updateBossBar(); // 生成 Boss 时更新 BossBar
        bossBar.setVisible(false);
        bossAlive = true;

        // 固定 Boss 位置和攻击逻辑
        boss.setAI(false);
        boss.setRotation(0, 0);

        // 攻击任务
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bossAlive) {
                    attackNearestPlayer();
                    clearOriginalBossBar(); // 每次攻击时也清除原版 BossBar
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 200L); // 10 秒一次攻击
    }

    private void attackNearestPlayer() {
        double minDistance = Double.MAX_VALUE;
        Player nearestPlayer = null;
        for (UUID uuid : bossPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getWorld().equals(boss.getWorld())) {
                double distance = player.getLocation().distance(boss.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    if (minDistance < 15) {
                        nearestPlayer = player;
                    }
                }
            }
        }

        if (nearestPlayer != null) {
            // 给予玩家凋零效果
            nearestPlayer.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1)); // 3 秒凋零效果

            // 给予玩家冲量
            Vector direction = nearestPlayer.getLocation().toVector().subtract(boss.getLocation().toVector()).normalize();
            Vector knockback = direction.multiply(2).setY(1);
            nearestPlayer.setVelocity(knockback);
        }
    }

    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Wither && event.getEntity().hasMetadata("customBoss")) {
            Wither wither = (Wither) event.getEntity();
            if (event.getDamager() instanceof Player) {
                Player damager = (Player) event.getDamager();
                lastAttackerUUID = damager.getUniqueId(); // 存储攻击者的 UUID
            }
            // 更新 BossBar
            updateBossBar();
            clearOriginalBossBar(); // 受到攻击时清除原版 BossBar
        }
    }

    private void updateBossBar() {
        if (boss == null || !bossAlive) {
            bossBar.setVisible(false); // 如果 Boss 不存在或已死亡，隐藏 BossBar
            return;
        }

        AttributeInstance maxHealthAttribute = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute == null) return;

        double maxHealth = maxHealthAttribute.getBaseValue();
        double currentHealth = boss.getHealth();

        // 计算进度，处理最大生命值为0的情况
        double progress = (maxHealth > 0) ? currentHealth / maxHealth : 0.0;
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress))); // 确保进度在0-1之间

        // 设置标题
        bossBar.setTitle(ChatColor.RED + "凋零 Boss: " + ChatColor.WHITE + String.format("%.0f", currentHealth) + " / " + String.format("%.0f", maxHealth));

        // 仅对进入 Boss 点的玩家显示 BossBar
        for (UUID uuid : bossPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (!bossBar.getPlayers().contains(player)) {
                    bossBar.addPlayer(player);
                }
            }
        }
        // 移除不在 Boss 点的玩家
        for (Player player : new ArrayList<>(bossBar.getPlayers())) {
            if (!bossPlayers.contains(player.getUniqueId())) {
                bossBar.removePlayer(player);
            }
        }

        bossBar.setVisible(!bossBar.getPlayers().isEmpty()); // 如果有玩家在 Boss 点，显示 BossBar
    }

    public void onBossDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Wither && event.getEntity().hasMetadata("customBoss")) {
            bossAlive = false;
            bossBar.setVisible(false);

            // 清除当前 Boss 实例
            this.boss = null;

            if (lastAttackerUUID != null) {
                Player lastAttacker = Bukkit.getPlayer(lastAttackerUUID);
                if (lastAttacker != null) {
                    String teamName = teamManager.getPlayerTeamName(lastAttacker);
                    killerTeamMap.put(lastAttackerUUID, teamName);
                    announceBossKill(lastAttacker, teamName);
                }
            }

            // 取消之前的重生任务
            if (bossRespawnTask != null) {
                bossRespawnTask.cancel();
            }

            // 启动 15 分钟后重生
            bossRespawnTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (gameManager.getCurrentPhase() >= 4 && !bossAlive) {
                        spawnBoss();
                    }
                }
            };
            bossRespawnTask.runTaskLater(plugin, 15 * 60 * 20L); // 15 分钟
        }
    }

    private void announceBossKill(Player killer, String teamName) {
        String teamChineseName = teamManager.getTeamChineseName(teamName);
        String message = ChatColor.GOLD + killer.getName() + " 所在的 " + teamChineseName + "队 成功击杀了凋零 Boss！";
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public boolean isBossAlive() {
        return bossAlive;
    }

    public String getKillerTeam(UUID killerUUID) {
        return killerTeamMap.get(killerUUID);
    }

    public Player getLastAttacker() {
        return lastAttackerUUID != null ? Bukkit.getPlayer(lastAttackerUUID) : null;
    }

    // 清除 Boss 的方法
    public void clearBoss() {
        if (boss != null && bossAlive) {
            boss.remove();
            bossAlive = false;
            bossBar.setVisible(false);
        }
    }

    public void setBossDataManager() {
        gameManager.setBossDataManager(this);
    }

    public void addBossPlayer(Player player) {
        bossPlayers.add(player.getUniqueId());
        updateBossBar();
        clearOriginalBossBar(); // 玩家进入时清除原版 BossBar
    }

    public void removeBossPlayer(Player player) {
        bossPlayers.remove(player.getUniqueId());
        updateBossBar();
        clearOriginalBossBar(); // 玩家离开时清除原版 BossBar
    }

    public boolean isPlayerInBoss(Player player) {
        return bossPlayers.contains(player.getUniqueId());
    }

    public void clearOriginalBossBar() {
        if (boss != null) {
            boss.getBossBar().removeAll();
        }
    }
}