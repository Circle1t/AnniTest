package cn.zhuobing.testPlugin.boss;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.map.BossWorldManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import cn.zhuobing.testPlugin.utils.MessageRenderer;
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
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class BossDataManager implements Listener {
    private Location bossSpawnLocation; // Boss生成点字段
    private final Map<String, Location> teamTpLocations = new HashMap<>();
    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private Wither boss;
    private boolean bossAlive = false;
    private UUID lastAttackerUUID; // 使用 UUID 存储最后攻击者
    private BukkitRunnable bossRespawnTask; // 用于控制 Boss 重生的任务
    private final Map<UUID, String> killerTeamMap = new HashMap<>(); // 使用 UUID 作为键
    private final Set<UUID> bossPlayers = new HashSet<>(); // 储存进入 boss 点的玩家
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final BossWorldManager bossWorldManager;
    private final MessageRenderer messageRenderer;
    private final String BOSS_MESSAGE = ChatColor.DARK_PURPLE + " BOSS " + ChatColor.GRAY +"已经生成";

    public BossDataManager(Plugin plugin, GameManager gameManager, TeamManager teamManager, BossWorldManager bossWorldManager, MessageRenderer messageRenderer) {
        this.plugin = plugin;
        this.gameManager = gameManager; // 初始化 GameManager
        this.teamManager = teamManager;
        this.bossWorldManager = bossWorldManager;
        this.messageRenderer = messageRenderer;
        // 设置 gameManager 中的 BossDataManager
        setBossDataManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadConfig();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "boss-config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        // 加载Boss生成点
        if (config.contains("bossSpawn")) {
            Location loc = config.getLocation("bossSpawn");
            if (loc != null) loc.setWorld(bossWorldManager.getBossWorld());
            bossSpawnLocation = loc;
        }

        // 加载队伍传送点
        if (config.contains("teamTpLocations")) {
            for (String team : config.getConfigurationSection("teamTpLocations").getKeys(false)) {
                Location loc = config.getLocation("teamTpLocations." + team);
                if (loc != null) loc.setWorld(bossWorldManager.getBossWorld());
                teamTpLocations.put(team, loc);
            }
        }
    }

    public void saveConfig() {
        config.set("bossSpawn", bossSpawnLocation);
        config.set("teamTpLocations", null);
        for (Map.Entry<String, Location> entry : teamTpLocations.entrySet()) {
            config.set("teamTpLocations." + entry.getKey(), entry.getValue());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadConfig();
    }

    public void enterBossMap(Player player) {
        addBossPlayer(player); // 添加到Boss玩家列表

        // 判断玩家是否有队伍
        String teamName = teamManager.getPlayerTeamName(player);
        if (teamName != null) {
            Location teamSpawn = teamTpLocations.get(teamName);
            if (teamSpawn != null) {
                player.teleport(teamSpawn);
            }
        }else{
            bossWorldManager.enterBossMap(player);
        }
    }

    public void leaveBossMap(Player player) {
        bossWorldManager.leaveBossMap(player);
        removeBossPlayer(player); // 移除Boss玩家列表
    }

    // 设置Boss生成点的方法
    public void setBossSpawnLocation(Location location) {
        this.bossSpawnLocation = location;
        saveConfig();
    }


    public void setBossTeamTpLocation(String teamName, Location location) {
        teamTpLocations.put(teamName, location);
        saveConfig();
    }

    public Location getBossSpawnLocation() {
        return bossSpawnLocation;
    }


    public boolean hasBossLocation(String teamName) {
        return teamTpLocations.containsKey(teamName);
    }

    // 清除 Boss 的方法
    public void clearBoss() {
        if (boss != null && bossAlive) {
            boss.remove();
            bossAlive = false;
        }
    }


    public Location getTeamTpLocation(String teamName) {
        return teamTpLocations.get(teamName);
    }

    public void spawnBossManually(Player player) {
        if (gameManager.getCurrentPhase() >= 4) {
            spawnBoss();
        } else {
            player.sendMessage(ChatColor.RED + "当前游戏阶段不允许生成 Boss。");
        }
    }

    public void spawnBoss() {
        Location bossLocation = getBossSpawnLocation();
        if (bossLocation == null || bossAlive) {
            return;
        }

        boss = (Wither) bossLocation.getWorld().spawnEntity(bossLocation, EntityType.WITHER);
        boss.setMetadata("customBoss", new FixedMetadataValue(plugin, true));

        // 设置原生凋零 BossBar
        updateBossBar(); // 初始化时设置一次标题

        // 设置血量和属性
        AttributeInstance maxHealth = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(AnniConfigManager.BOSS_HEALTH);
            boss.setHealth(maxHealth.getBaseValue());
        }

        // 确保凋零不会破坏方块
        boss.setCollidable(false);
        boss.setCustomName(ChatColor.RED + "凋零 Boss");
        boss.setCustomNameVisible(true);

        bossAlive = true;

        // 广播 Boss 生成消息给全服玩家
        String message = ChatColor.LIGHT_PURPLE + "Boss 已生成！击败Boss将会获得特殊奖励";
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            // 播放音效
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }

        // 固定 Boss 位置和攻击逻辑
        boss.setAI(false);
        boss.setRotation(0, 0);

        // 每 20 秒对周围玩家发起冲击波
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bossAlive) {
                    attackNearbyPlayers();
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20 * 20L); // 20 秒一次

        // 每 8 秒发射凋零头颅（带音效）
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bossAlive) {
                    shootWitherSkull();
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 8 * 20L); // 8 秒一次

        // 发送boss消息
        Bukkit.broadcastMessage(" ");
        List<String> bossMessage = messageRenderer.formatMessage(
                messageRenderer.getBossMessage("Wither"),
                BOSS_MESSAGE
        );
        broadcastMessage(bossMessage);
    }

    // 更新 BossBar 的标题
    private void updateBossBar() {
        if (boss == null || !bossAlive) {
            return;
        }

        AttributeInstance maxHealthAttribute = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute == null) return;

        double maxHealth = maxHealthAttribute.getBaseValue();
        double currentHealth = boss.getHealth();
        BossBar bossBar = boss.getBossBar();

        // 设置标题显示具体血量
        bossBar.setTitle(ChatColor.RED + "凋零 Boss: " +
                ChatColor.WHITE + String.format("%.0f", currentHealth) +
                " / " + String.format("%.0f", maxHealth));
        bossBar.setProgress(maxHealth == 0? 0.0 : currentHealth/maxHealth);
    }

    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Wither && event.getEntity().hasMetadata("customBoss")) {
            Wither wither = (Wither) event.getEntity();
            if (event.getDamager() instanceof Player) {
                Player damager = (Player) event.getDamager();
                lastAttackerUUID = damager.getUniqueId(); // 存储攻击者的 UUID
            }

            // 更新 BossBar 标题
            updateBossBar();
        }
    }

    private void broadcastMessage(List<String> lines) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (String line : lines) {
                p.sendMessage(line);
            }
        }
    }

    private void attackNearbyPlayers() {
        for (UUID uuid : bossPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getWorld().equals(boss.getWorld()) && player.getLocation().distance(boss.getLocation()) <= 3) {
                // 给予玩家斜向上的向量
                Vector direction = player.getLocation().toVector().subtract(boss.getLocation().toVector()).normalize();
                Vector knockback = direction.multiply(2).setY(1.732); // 60 度角的 y 分量，tan(60) = √3 ≈ 1.732
                player.setVelocity(knockback);

                // 给予玩家 2 秒凋零效果
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 2 * 20, 1));
            }
        }
    }

    private void shootWitherSkull() {
        // 寻找最近的玩家（在 15 格范围内）
        Player nearestPlayer = null;
        double minDistance = Double.MAX_VALUE;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!bossPlayers.contains(player.getUniqueId())) continue;

            if (player.getWorld().equals(boss.getWorld())) {
                double distance = player.getLocation().distance(boss.getLocation());
                if (distance < minDistance && distance <= 15) { // 只检测 15 格内的玩家
                    minDistance = distance;
                    nearestPlayer = player;
                }
            }
        }

        if (nearestPlayer != null) {
            // 播放音效（凋零攻击音）
            boss.getWorld().playSound(boss.getLocation(),
                    Sound.ENTITY_WITHER_SHOOT,
                    2.0f,
                    0.8f + (float) Math.random() * 0.4f);

            // 计算方向向量
            Location targetLoc = nearestPlayer.getEyeLocation();
            Location bossEyeLoc = boss.getEyeLocation();
            Vector direction = targetLoc.toVector()
                    .subtract(bossEyeLoc.toVector())
                    .normalize();

            // 添加随机散布（±5 度）
            direction = direction.rotateAroundX(Math.toRadians((Math.random() - 0.5) * 10))
                    .rotateAroundY(Math.toRadians((Math.random() - 0.5) * 10));

            // 发射凋零头
            WitherSkull skull = boss.launchProjectile(WitherSkull.class, direction.multiply(1.8));
            skull.setCharged(false); // 普通头颅
        }
    }

    public void onBossDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Wither && event.getEntity().hasMetadata("customBoss")) {
            // 清空掉落物列表
            event.getDrops().clear();

            bossAlive = false;

            // 清除当前 Boss 实例
            this.boss = null;

            if (lastAttackerUUID != null) {
                Player lastAttacker = Bukkit.getPlayer(lastAttackerUUID);
                if (lastAttacker != null) {
                    String teamName = teamManager.getPlayerTeamName(lastAttacker);
                    killerTeamMap.put(lastAttackerUUID, teamName);
                    announceBossKill(lastAttacker, teamName);

                    // 为击杀队伍的所有玩家发放 Boss 奖励
                    ItemStack bossStar = BossStarItem.createBossStar();
                    List<Player> teamPlayers = teamManager.getPlayersInTeam(teamName);
                    for (Player player : teamPlayers) {
                        if (player != null) {
                            player.getInventory().addItem(bossStar.clone());
                            player.sendMessage(ChatColor.LIGHT_PURPLE + "你所在的队伍击杀了 Boss，获得了 Boss 之星！");
                        }
                    }
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
    public void setBossDataManager() {
        gameManager.setBossDataManager(this);
    }

    public void addBossPlayer(Player player) {
        bossPlayers.add(player.getUniqueId());
    }

    public void removeBossPlayer(Player player) {
        bossPlayers.remove(player.getUniqueId());
    }

    public boolean isPlayerInBoss(Player player) {
        return bossPlayers.contains(player.getUniqueId());
    }

    // 监听爆炸事件，防止凋零爆炸破坏方块
    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof Wither || event.getEntity() instanceof WitherSkull) {
            event.setFire(false);
            event.setRadius(0);
        }
    }
}