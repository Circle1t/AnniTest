package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.MessageUtil;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Sova extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    // 冷却相关字段
    private final HashMap<UUID, Long> scanCooldowns = new HashMap<>();
    private final HashMap<UUID, Long> furyCooldowns = new HashMap<>();
    private final int SCAN_COOLDOWN = 60 * 1000; // 60秒冷却
    private final int FURY_COOLDOWN = 120 * 1000; // 120秒冷却
    private final String SCAN_ITEM_NAME = ChatColor.AQUA + "寻敌弓 " + ChatColor.GREEN + "准备就绪";
    private final String LIGHT_ARROW = ChatColor.AQUA + "光灵箭";
    private final String HELMET = ChatColor.AQUA + "头盔";
    private final String SCAN_COOLDOWN_PREFIX = ChatColor.RED + "冷却中 ";
    private final String SCAN_COOLDOWN_SUFFIX = " 秒";
    private final String FURY_ITEM_NAME = ChatColor.YELLOW + "狂猎之怒 " + ChatColor.GREEN + "准备就绪";
    private final String FURY_COOLDOWN_PREFIX = ChatColor.RED + "冷却中 ";
    private final String FURY_COOLDOWN_SUFFIX = " 秒";
    private final HashMap<UUID, BukkitTask> scanCooldownTasks = new HashMap<>();
    private final HashMap<UUID, BukkitTask> furyCooldownTasks = new HashMap<>();

    // 标记相关
    private final Map<UUID, Set<UUID>> markedPlayers = new HashMap<>(); // 标记者 -> 被标记玩家
    private final Map<UUID, BukkitTask> markTasks = new HashMap<>(); // 标记结束任务
    private final Map<UUID, UUID> markerMap = new HashMap<>(); // 被标记者 -> 标记者

    public Sova(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "猎枭";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.AQUA + "猎枭";
    }

    @Override
    public String getDescription() {
        return "追踪专家，使用寻敌弓标记敌人，然后用狂猎之怒发动致命雷击。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.ARROW);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Sova",
                "",
                ChatColor.YELLOW + "你是战场上的猎手，追踪敌人的专家。",
                "",
                ChatColor.AQUA + "每 60 秒可发射一支" + ChatColor.AQUA + "寻敌箭",
                ChatColor.AQUA + "标记 8 格范围内的敌人，持续 3 秒",
                ChatColor.AQUA + "被标记的敌人会显示" + ChatColor.AQUA + "队伍描边" + ChatColor.AQUA + "并听到警报",
                ChatColor.AQUA + "右键" + ChatColor.YELLOW + "狂猎之怒" + ChatColor.AQUA + "对标记的敌人",
                ChatColor.AQUA + "造成其生命值 20% 的" + ChatColor.GOLD + "雷击伤害",
                " "
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public List<ItemStack> getKitArmors(Player player) {
        String teamColor = teamManager.getPlayerTeamName(player);

        return Arrays.asList(
                createSoulBoundItem(Material.CHAINMAIL_HELMET, HELMET, 1, 4, false),
                SpecialArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_BOOTS, teamColor)
        );
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // 装备护甲
        List<ItemStack> armors = getKitArmors(player);
        for (ItemStack armor : armors) {
            if (armor != null) {
                switch (armor.getType()) {
                    case CHAINMAIL_HELMET:
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
    }

    private void setUp() {
        // 木剑
        ItemStack woodSword = createSoulBoundItem(Material.WOODEN_SWORD, null, 1, 1, false);
        kitItems.add(woodSword);

        // 木镐
        ItemStack woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe);

        // 木斧
        ItemStack woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe);

        // 寻敌弓（技能物品，带无限附魔）
        ItemStack scanBow = createSoulBoundItem(Material.BOW, null, 1, 4, true);
        ItemMeta bowMeta = scanBow.getItemMeta();
        bowMeta.setDisplayName(SCAN_ITEM_NAME);
        bowMeta.setUnbreakable(true);
        bowMeta.addEnchant(Enchantment.INFINITY, 1, true);
        bowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        scanBow.setItemMeta(bowMeta);
        kitItems.add(scanBow);

        // 狂猎之怒（改为避雷针）
        ItemStack furyItem = createSoulBoundItem(Material.LIGHTNING_ROD, null, 1, 4, true);
        ItemMeta furyMeta = furyItem.getItemMeta();
        furyMeta.setDisplayName(FURY_ITEM_NAME);
        furyItem.setItemMeta(furyMeta);
        kitItems.add(furyItem);

        // 给玩家一支普通箭（配合无限弓）
        ItemStack arrow = createSoulBoundItem(Material.ARROW, LIGHT_ARROW, 1, 4, true);
        kitItems.add(arrow);

        // 指南针
        kitItems.add(CompassItem.createCompass());

    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 检查冷却
    private boolean isScanOnCooldown(Player player) {
        return scanCooldowns.containsKey(player.getUniqueId()) &&
                scanCooldowns.get(player.getUniqueId()) > System.currentTimeMillis();
    }

    private boolean isFuryOnCooldown(Player player) {
        return furyCooldowns.containsKey(player.getUniqueId()) &&
                furyCooldowns.get(player.getUniqueId()) > System.currentTimeMillis();
    }

    private long getScanCooldownSecondsLeft(Player player) {
        if (scanCooldowns.containsKey(player.getUniqueId())) {
            return (scanCooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
        }
        return 0;
    }

    private long getFuryCooldownSecondsLeft(Player player) {
        if (furyCooldowns.containsKey(player.getUniqueId())) {
            return (furyCooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
        }
        return 0;
    }

    private void startScanCooldown(Player player) {
        scanCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + SCAN_COOLDOWN);
        startScanCooldownCheckTask(player);
        updateBowItem(player);
    }

    private void startFuryCooldown(Player player) {
        furyCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + FURY_COOLDOWN);
        startFuryCooldownCheckTask(player);
        updateFuryItem(player);
    }

    private void startScanCooldownCheckTask(Player player) {
        Plugin plugin = kitManager.getPlugin();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    scanCooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                if (!isScanOnCooldown(player)) {
                    if (isThisKit(player)) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.0f);
                        player.sendMessage(ChatColor.GREEN + "你的技能 " + ChatColor.AQUA + "寻敌弓 " + ChatColor.GREEN + "准备就绪！");
                        updateBowItemsInInventory(player);
                    }
                    scanCooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                } else {
                    updateBowItem(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        scanCooldownTasks.put(player.getUniqueId(), task);
    }

    private void startFuryCooldownCheckTask(Player player) {
        Plugin plugin = kitManager.getPlugin();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    furyCooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                if (!isFuryOnCooldown(player)) {
                    if (isThisKit(player)) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.0f);
                        player.sendMessage(ChatColor.GOLD + "你的技能 " + ChatColor.YELLOW + "狂猎之怒 " + ChatColor.GREEN + "准备就绪！");
                        updateFuryItemsInInventory(player);
                    }
                    furyCooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                } else {
                    updateFuryItem(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        furyCooldownTasks.put(player.getUniqueId(), task);
    }

    private void updateBowItem(Player player) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (isScanBow(item)) {
                ItemMeta meta = item.getItemMeta();
                long secondsLeft = getScanCooldownSecondsLeft(player);

                if (isScanOnCooldown(player)) {
                    meta.setDisplayName(SCAN_COOLDOWN_PREFIX + secondsLeft + SCAN_COOLDOWN_SUFFIX);
                } else {
                    meta.setDisplayName(SCAN_ITEM_NAME);
                }

                item.setItemMeta(meta);
            }
        }
        player.updateInventory();
    }

    private void updateBowItemsInInventory(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (isScanBow(item)) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(SCAN_ITEM_NAME);
                item.setItemMeta(meta);
            }
        }
        player.updateInventory();
    }

    private void updateFuryItem(Player player) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (isFuryItem(item)) {
                ItemMeta meta = item.getItemMeta();
                long secondsLeft = getFuryCooldownSecondsLeft(player);

                if (isFuryOnCooldown(player)) {
                    meta.setDisplayName(FURY_COOLDOWN_PREFIX + secondsLeft + FURY_COOLDOWN_SUFFIX);
                } else {
                    meta.setDisplayName(FURY_ITEM_NAME);
                }

                item.setItemMeta(meta);
            }
        }
        player.updateInventory();
    }

    private void updateFuryItemsInInventory(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (isFuryItem(item)) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(FURY_ITEM_NAME);
                item.setItemMeta(meta);
            }
        }
        player.updateInventory();
    }

    private boolean isScanBow(ItemStack stack) {
        return stack != null && stack.getType() == Material.BOW &&
                SoulBoundUtil.isSoulBoundItem(stack, Material.BOW);
    }

    private boolean isFuryItem(ItemStack stack) {
        return stack != null && stack.getType() == Material.LIGHTNING_ROD &&
                stack.hasItemMeta() && stack.getItemMeta().hasDisplayName() &&
                SoulBoundUtil.isSoulBoundItem(stack, Material.LIGHTNING_ROD);
    }

    // 处理弓的使用
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Action action = event.getAction();

        // 检查是否是寻敌弓
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) &&
                item != null && isScanBow(item) && isThisKit(player)) {

            if (isScanOnCooldown(player)) {
                long secondsLeft = getScanCooldownSecondsLeft(player);
                player.sendMessage(ChatColor.RED + "技能冷却中，剩余 " + secondsLeft + " 秒");
                event.setCancelled(true);
                return;
            }

            // 检查是否有普通箭
            if (!hasArrow(player)) {
                player.sendMessage(ChatColor.RED + "你需要" + ChatColor.AQUA + " 光灵箭 " + ChatColor.RED + "来使用寻敌弓！");
                event.setCancelled(true);
                return;
            }
        }

        // 处理狂猎之怒右键
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                && item != null && isFuryItem(item) && isThisKit(player)) {

            // 防止放置避雷针
            event.setCancelled(true);

            if (isFuryOnCooldown(player)) {
                long secondsLeft = getFuryCooldownSecondsLeft(player);
                player.sendMessage(ChatColor.RED + "狂猎之怒冷却中，剩余 " + secondsLeft + " 秒");
                return;
            }

            UUID playerId = player.getUniqueId();
            if (!markedPlayers.containsKey(playerId)) {
                player.sendMessage(ChatColor.RED + "没有标记的敌人！");
                return;
            }

            // 对所有标记的敌人造成伤害
            strikeMarkedEnemies(player);

            // 清除标记
            removeMarkEffect(playerId);
            BukkitTask task = markTasks.remove(playerId);
            if (task != null) task.cancel();

            // 开始冷却
            startFuryCooldown(player);
        }
    }

    // 检查玩家是否有普通箭
    private boolean hasArrow(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (SoulBoundUtil.isSoulBoundItemWithDisplayName(item, Material.ARROW,LIGHT_ARROW)) {
                return true;
            }
        }
        return false;
    }

    // 处理箭矢命中
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getEntity();
        if (!(arrow.getShooter() instanceof Player)) return;

        Player shooter = (Player) arrow.getShooter();
        if (!isThisKit(shooter)) return;

        ItemStack bow = shooter.getInventory().getItemInMainHand();
        if (!isScanBow(bow)) return;

        // 检查冷却是否就绪
        if (isScanOnCooldown(shooter)) return;

        // 移除箭矢
        arrow.remove();

        // 标记敌人
        markNearbyEnemies(shooter, arrow.getLocation());

        // 开始冷却
        startScanCooldown(shooter);
    }

    // 标记附近敌人
    private void markNearbyEnemies(Player shooter, Location location) {
        UUID shooterId = shooter.getUniqueId();
        World world = location.getWorld();

        // 粒子效果
        world.spawnParticle(Particle.GLOW, location, 50, 3, 3, 3, 0.1);
        world.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);

        // 查找8格范围内的敌人
        Set<UUID> marked = new HashSet<>();
        for (Entity entity : world.getNearbyEntities(location, 8, 8, 8)) {
            if (!(entity instanceof Player)) continue;

            Player target = (Player) entity;
            if (target.equals(shooter)) continue;
            if (isSameTeam(shooter, target)) continue;

            // 添加描边效果
            applyMarkEffect(shooter, target);
            marked.add(target.getUniqueId());

            // 警报声
            target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.0f);
            target.sendMessage(ChatColor.YELLOW + "警告！你被 " + teamManager.getTeamColor(teamManager.getPlayerTeamName(shooter)) + shooter.getName() + ChatColor.YELLOW +  " 标记了！");
        }

        if (!marked.isEmpty()) {
            markedPlayers.put(shooterId, marked);

            // 3秒后清除标记
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    removeMarkEffect(shooterId);
                    markTasks.remove(shooterId);
                }
            }.runTaskLater(kitManager.getPlugin(), 3 * 20);

            markTasks.put(shooterId, task);

            shooter.sendMessage(ChatColor.AQUA + "成功标记了 " + marked.size() + " 名敌人！");
        } else {
            shooter.sendMessage(ChatColor.YELLOW + "没有发现可标记的敌人");
        }
    }

    // 应用标记效果
    private void applyMarkEffect(Player shooter, Player target) {
        UUID targetId = target.getUniqueId();
        UUID shooterId = shooter.getUniqueId();

        // 移除现有标记
        if (markerMap.containsKey(targetId)) {
            Player oldMarker = Bukkit.getPlayer(markerMap.get(targetId));
            if (oldMarker != null && markedPlayers.containsKey(oldMarker.getUniqueId())) {
                markedPlayers.get(oldMarker.getUniqueId()).remove(targetId);
            }
        }

        // 添加新标记
        markerMap.put(targetId, shooterId);

        // 添加描边效果
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING,
                3 * 20, // 3秒
                0,
                false,
                false,
                false
        ));

        // 添加队伍颜色粒子
        startMarkParticles(target, shooter);
    }

    // 创建标记粒子效果
    private void startMarkParticles(Player target, Player shooter) {
        String teamName = teamManager.getPlayerTeamName(shooter);
        ChatColor teamColor = ChatColor.valueOf(teamName.toUpperCase());
        Color particleColor = getColorFromChatColor(teamColor);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!target.isOnline() || !markerMap.containsKey(target.getUniqueId()) || ticks > 60) {
                    this.cancel();
                    return;
                }

                Location loc = target.getLocation().add(0, 1, 0);
                target.getWorld().spawnParticle(
                        Particle.PORTAL,
                        loc,
                        10,
                        0.5, 0.5, 0.5,
                        new Particle.DustOptions(particleColor, 1.5f)
                );
                ticks += 5;
            }
        }.runTaskTimer(kitManager.getPlugin(), 0L, 5L);
    }

    // 移除标记效果
    private void removeMarkEffect(UUID shooterId) {
        Set<UUID> marked = markedPlayers.remove(shooterId);
        if (marked == null) return;

        for (UUID targetId : marked) {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                target.removePotionEffect(PotionEffectType.GLOWING);
            }
            markerMap.remove(targetId);
        }
    }

    // 对标记的敌人造成雷击伤害
    private void strikeMarkedEnemies(Player shooter) {
        Set<UUID> marked = markedPlayers.get(shooter.getUniqueId());
        if (marked == null || marked.isEmpty()) return;

        World world = shooter.getWorld();
        int count = 0;

        for (UUID targetId : marked) {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) continue;

            // 计算伤害（最大生命值的20%，至少0.5颗心）
            double maxHealth = target.getMaxHealth();
            double damage = Math.max(1, maxHealth * 0.20); // 至少造成0.5颗心伤害

            // 雷击效果
            Location loc = target.getLocation();
            world.strikeLightningEffect(loc);
            world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

            // 造成伤害
            target.damage(damage, shooter);

            // 击退效果
            Vector direction = target.getLocation().toVector()
                    .subtract(shooter.getLocation().toVector())
                    .normalize()
                    .multiply(0.5)
                    .setY(0.2);
            target.setVelocity(direction);

            count++;
        }

        shooter.sendMessage(ChatColor.GOLD + "狂猎之怒！对 " + count + " 名敌人造成雷击伤害！");
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
    }

    // 防止被标记的玩家受到多次伤害
    @EventHandler
    public void onMarkedPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (markerMap.containsKey(player.getUniqueId())) {
            // 添加额外视觉效果
            player.getWorld().spawnParticle(
                    Particle.CRIT,
                    player.getLocation().add(0, 1, 0),
                    10,
                    0.5, 0.5, 0.5,
                    0.1
            );
        }
    }

    // 将ChatColor转换为Color
    private Color getColorFromChatColor(ChatColor color) {
        switch (color) {
            case RED: return Color.RED;
            case BLUE: return Color.BLUE;
            case GREEN: return Color.GREEN;
            case YELLOW: return Color.YELLOW;
            case AQUA: return Color.AQUA;
            case LIGHT_PURPLE: return Color.PURPLE;
            case WHITE: return Color.WHITE;
            case GRAY: return Color.GRAY;
            case DARK_GRAY: return Color.GRAY;
            case BLACK: return Color.BLACK;
            case DARK_RED: return Color.MAROON;
            case DARK_BLUE: return Color.NAVY;
            case DARK_GREEN: return Color.OLIVE;
            case DARK_AQUA: return Color.TEAL;
            case DARK_PURPLE: return Color.PURPLE;
            case GOLD: return Color.ORANGE;
            default: return Color.WHITE;
        }
    }

    private boolean isSameTeam(Player p1, Player p2) {
        return teamManager.getPlayerTeamName(p1).equals(teamManager.getPlayerTeamName(p2));
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Sova;
    }
}