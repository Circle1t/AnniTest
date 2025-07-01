package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static cn.zhuobing.testPlugin.enchant.SoulBoundListener.getSoulBoundLevel;
import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;
import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.isSoulBoundItem;

public class Pyro extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack woodSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack firestormItem;
    private ItemStack healthPotion;

    // 冷却相关字段
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final int FIRESTORM_COOLDOWN = 40 * 1000; // 40秒冷却
    private final String FIRESTORM_ITEM_NAME = ChatColor.AQUA + "烈焰风暴 " + ChatColor.GREEN + "准备就绪";
    private final String FIRESTORM_COOLDOWN_PREFIX = ChatColor.RED + "冷却中 ";
    private final String FIRESTORM_COOLDOWN_SUFFIX = " 秒";
    private final HashMap<UUID, BukkitTask> cooldownTasks = new HashMap<>();
    private final int SOUL_BOUND_LEVEL = 4; // 灵魂绑定IV级

    public Pyro(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "火法师";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.RED + "火法师";
    }

    @Override
    public String getDescription() {
        return "操控火焰的大师。免疫火焰与岩浆伤害，攻击有概率点燃敌人，箭矢会自动点燃。特殊技能可对附近敌人造成范围燃烧效果。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Pyro",
                "",
                ChatColor.YELLOW + "你是火焰的掌控者。",
                "",
                ChatColor.AQUA + "免疫火焰与岩浆伤害，",
                ChatColor.AQUA + "攻击有37%概率点燃敌人，",
                ChatColor.AQUA + "箭矢会自动点燃目标，",
                ChatColor.AQUA + "特殊技能对附近敌人造成燃烧效果。",
                " "
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public List<ItemStack> getKitArmors(Player player) {
        String teamColor = teamManager.getPlayerTeamName(player);

        return Arrays.asList(
                SpecialArmor.createArmor(Material.LEATHER_HELMET, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_BOOTS, teamColor)
        );
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // 应用职业专属装甲套装
        for (ItemStack armor : getKitArmors(player)) {
            if (armor != null) {
                switch (armor.getType()) {
                    case LEATHER_HELMET:
                        inv.setHelmet(armor);
                        break;
                    case LEATHER_CHESTPLATE:
                        inv.setChestplate(armor);
                        break;
                    case LEATHER_LEGGINGS:
                        inv.setLeggings(armor);
                        break;
                    case LEATHER_BOOTS:
                        inv.setBoots(armor);
                        break;
                    default:
                        inv.addItem(armor);
                }
            }
        }

        for (ItemStack item : kitItems) {
            inv.addItem(item);
        }

        // 添加火焰抗性 (适配1.20)
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
    }

    // 岩浆和火焰免疫效果 (新增事件监听)
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isThisKit(player) && (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                    event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK)) {
                event.setCancelled(true);
            }
        }
    }

    private void setUp() {

        // 木剑
        woodSword = createSoulBoundItem(Material.WOODEN_SWORD, null, 1, 1, false);
        kitItems.add(woodSword.clone());
        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());
        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());
        // 烈焰风暴物品 (灵魂绑定IV级)
        firestormItem = createSoulBoundItem(Material.FIRE_CHARGE, null, 1, SOUL_BOUND_LEVEL, true);
        ItemMeta itemMeta = firestormItem.getItemMeta();
        itemMeta.setDisplayName(FIRESTORM_ITEM_NAME);
        firestormItem.setItemMeta(itemMeta);
        kitItems.add(firestormItem.clone());

        // 治疗药水
        ItemStack soulBoundHealthPotion = createSoulBoundItem(Material.POTION, null, 1, 3, true);
        PotionMeta potionMeta = (PotionMeta) soulBoundHealthPotion.getItemMeta();
        // 设置药水效果为立即治疗 I
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0), true); // 1 tick, 等级 0（I 级）
        // 设置药水名称和描述
        potionMeta.setDisplayName(ChatColor.RESET + ChatColor.LIGHT_PURPLE.toString() + "治疗药水");
        soulBoundHealthPotion.setItemMeta(potionMeta);
        // 复制药水元数据
        kitItems.add(soulBoundHealthPotion);

        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 执行烈焰风暴操作
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();

            // 检查是否为灵魂绑定IV的烈焰弹
            if (item != null && isFirestormItem(item) && isThisKit(player)) {
                // 阻止烈焰弹的原版使用效果
                event.setCancelled(true);

                if (!performSpecialAction(player)) {
                    // 技能使用失败，需要处理冷却中的提示
                    if (isOnCooldown(player)) {
                        long secondsLeft = getCooldownSecondsLeft(player);
                        player.sendMessage(ChatColor.RED + "烈焰风暴冷却中，剩余 " + secondsLeft + " 秒");
                    } else {
                        player.sendMessage(ChatColor.RED + "未找到有效目标或不在队伍中，无法使用技能");
                    }
                }
            }
        }
    }

    // 冷却检查方法
    private boolean isOnCooldown(Player player) {
        return cooldowns.containsKey(player.getUniqueId()) &&
                cooldowns.get(player.getUniqueId()) > System.currentTimeMillis();
    }

    private long getCooldownSecondsLeft(Player player) {
        if (cooldowns.containsKey(player.getUniqueId())) {
            return (cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
        }
        return 0;
    }

    private void startCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + FIRESTORM_COOLDOWN);
        startCooldownCheckTask(player);
        updateFirestormItem(player);
    }

    // 统一 isFirestormItem 方法逻辑
    private boolean isFirestormItem(ItemStack stack) {
        return stack != null && isSoulBoundItem(stack, Material.FIRE_CHARGE) &&
                getSoulBoundLevel(stack) == SOUL_BOUND_LEVEL;
    }

    // 新增物品更新方法
    private void updateFirestormItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack heldItem = inv.getItemInMainHand();

        if (isFirestormItem(heldItem) && isThisKit(player)) {
            ItemMeta meta = heldItem.getItemMeta();
            long secondsLeft = getCooldownSecondsLeft(player);

            if (isOnCooldown(player)) {
                meta.setDisplayName(FIRESTORM_COOLDOWN_PREFIX + secondsLeft + FIRESTORM_COOLDOWN_SUFFIX);
            } else {
                meta.setDisplayName(FIRESTORM_ITEM_NAME);
            }

            heldItem.setItemMeta(meta);
            player.updateInventory();
        }
    }

    // 新增冷却检查任务
    private void startCooldownCheckTask(Player player) {
        Plugin plugin = kitManager.getPlugin();
        if (plugin == null) {
            throw new IllegalStateException("Plugin instance in KitManager is null!");
        }
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                if (!isOnCooldown(player)) {
                    if(isThisKit(player)){
                        // 冷却完成音效
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.8f, 1.0f);
                        player.sendMessage(ChatColor.GREEN + "你的技能 " + ChatColor.YELLOW + "烈焰风暴 " + ChatColor.GREEN + "准备就绪！");
                        updateFirestormItemsInInventory(player);
                    }
                    cooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                } else {
                    updateFirestormItem(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        cooldownTasks.put(player.getUniqueId(), task);
    }

    private void updateFirestormItemsInInventory(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (isFirestormItem(item)) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(FIRESTORM_ITEM_NAME);
                item.setItemMeta(meta);
            }
        }
        player.updateInventory();
    }

    private boolean performSpecialAction(Player player) {
        if (isOnCooldown(player)) {
            return false;
        }

        String playerTeam = teamManager.getPlayerTeamName(player);
        if (playerTeam == null) {
            return false;
        }

        // 对附近敌人施加燃烧效果 (6秒燃烧)
        boolean hasTarget = false;
        for (Entity e : player.getNearbyEntities(5, 5, 5)) {
            if (e instanceof Player) {
                Player target = (Player) e;
                String targetTeam = teamManager.getPlayerTeamName(target);
                if (targetTeam != null && !targetTeam.equals(playerTeam)) {
                    target.setFireTicks(120); // 6秒燃烧 (120 ticks)
                    hasTarget = true;
                }
            }
        }

        if (!hasTarget) {
            return false;
        }

        // 播放音效 (1.20版本)
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.0f, 0.8f);

        // 播放粒子效果 (1.20版本)
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);

        player.sendMessage(ChatColor.DARK_RED + "烈焰风暴！");
        startCooldown(player);
        return true;
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)) {
            updateFirestormItem(player);
        }
    }

    // 箭发射时点燃
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArrowLaunch(ProjectileLaunchEvent event) {
        if (event.getEntityType() == EntityType.ARROW) {
            ProjectileSource shooter = event.getEntity().getShooter();
            if (shooter instanceof Player) {
                Player player = (Player) shooter;
                if (isThisKit(player)) {
                    event.getEntity().setFireTicks(999999); // 长时间燃烧
                }
            }
        }
    }

    // 攻击时有概率点燃敌人
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player && event.getEntity() instanceof Player) {
            Player player = (Player) damager;
            if (isThisKit(player)) {
                // 37%概率点燃
                if (ThreadLocalRandom.current().nextInt(100) < 37) {
                    event.getEntity().setFireTicks(60); // 3秒燃烧 (60 ticks)
                }
            }
        }
    }

    @Override
    public void onKitUnset(Player player) {
        super.onKitUnset(player);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Pyro;
    }
}