package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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

public class Scorpio extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack stoneSword;
    private ItemStack hookItem;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack compass;

    // 冷却相关字段
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final int SKILL_COOLDOWN = 5 * 1000; // 5秒冷却
    private final String HOOK_ITEM_NAME = ChatColor.YELLOW + "钩子 " + ChatColor.GREEN + "准备就绪";
    private final String HOOK_COOLDOWN_PREFIX = ChatColor.RED + "冷却中 ";
    private final String HOOK_COOLDOWN_SUFFIX = " 秒";
    private final HashMap<UUID, BukkitTask> cooldownTasks = new HashMap<>();
    // 新增字段记录免疫坠落伤害的玩家
    private final Map<UUID, Long> fallDamageImmunePlayers = new HashMap<>();

    public Scorpio(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "天蝎";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.AQUA + "天蝎";
    }

    @Override
    public String getDescription() {
        return "自带一把石剑，一个名为“钩子”的下界之星，木镐，木斧，指南针。左/右键激活技能。当玩家激活技能后，会投掷出一个拥有合理的抛物线且无法被拾取的下界之星，并且会有轨迹上的白色粒子效果，通过投掷下界之星向命中的队友飞去（左键投掷）/将命中的敌人瞬移到自身面前（右键投掷）。左键只会生效于队友，右键只会生效于敌人。冷却5s。天蝎前方必须有3格以上的空间右键技能才能生效。被勾中的敌人10s内免疫坠落伤害。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Scorpio",
                "",
                ChatColor.YELLOW + "你是战场的掌控者。",
                "",
                ChatColor.AQUA + "左/右键激活技能，投掷钩子。",
                ChatColor.AQUA + "左键飞向队友，右键拉取敌人。",
                ChatColor.AQUA + "冷却5s，右键需前方3格空间。",
                ChatColor.AQUA + "被勾中的敌人10s内免疫坠落伤害。",
                " "
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // 皮革护甲
        String teamColor = teamManager.getPlayerTeamName(player);
        inv.setHelmet(SpecialLeatherArmor.createArmor(Material.LEATHER_HELMET, teamColor));
        inv.setChestplate(SpecialLeatherArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor));
        inv.setLeggings(SpecialLeatherArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor));
        inv.setBoots(SpecialLeatherArmor.createArmor(Material.LEATHER_BOOTS, teamColor));

        for (ItemStack item : kitItems) {
            inv.addItem(item);
        }
    }

    private void setUp() {
        // 石剑
        stoneSword = createSoulBoundItem(Material.STONE_SWORD, null, 1, 1, false);
        kitItems.add(stoneSword.clone());

        // 钩子
        hookItem = createSoulBoundItem(Material.NETHER_STAR, HOOK_ITEM_NAME, 1, 4, true);
        ItemMeta itemMeta = hookItem.getItemMeta();
        itemMeta.setDisplayName(HOOK_ITEM_NAME);
        hookItem.setItemMeta(itemMeta);
        kitItems.add(hookItem.clone());

        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());

        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());

        // 指南针
        compass = CompassItem.createCompass();
        kitItems.add(compass.clone());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 处理玩家交互事件
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item != null && isHookItem(item) && isThisKit(player)) {
                if (performSpecialAction(player, action)) {
                    event.setCancelled(true);
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
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + SKILL_COOLDOWN);
        startCooldownCheckTask(player);
        updateHookItem(player);
    }

    // 判断是否为钩子物品
    private boolean isHookItem(ItemStack stack) {
        return SoulBoundUtil.isSoulBoundItem(stack, Material.NETHER_STAR,4);
    }

    // 新增物品更新方法
    private void updateHookItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack heldItem = inv.getItemInMainHand();

        if (isHookItem(heldItem) && isThisKit(player)) {
            ItemMeta meta = heldItem.getItemMeta();
            long secondsLeft = getCooldownSecondsLeft(player);

            if (isOnCooldown(player)) {
                meta.setDisplayName(HOOK_COOLDOWN_PREFIX + secondsLeft + HOOK_COOLDOWN_SUFFIX);
            } else {
                meta.setDisplayName(HOOK_ITEM_NAME);
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
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.0f);
                        player.sendMessage(ChatColor.GREEN + "你的技能 " + ChatColor.YELLOW + "准备就绪！");
                        updateHookItemsInInventory(player);
                    }
                    cooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                } else {
                    updateHookItem(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        cooldownTasks.put(player.getUniqueId(), task);
    }

    private void updateHookItemsInInventory(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (isHookItem(item)) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(HOOK_ITEM_NAME);
                item.setItemMeta(meta);
            }
        }
        player.updateInventory();
    }

    // 执行特殊技能
    private boolean performSpecialAction(Player player, Action action) {
        if (isOnCooldown(player)) {
            long secondsLeft = getCooldownSecondsLeft(player);
            player.sendMessage(ChatColor.GREEN + "技能冷却中，剩余 " + ChatColor.YELLOW + secondsLeft + ChatColor.GREEN + " 秒");
            return false;
        }

        boolean isRightClick = action.toString().contains("RIGHT");

        if (isRightClick && !hasEnoughSpaceInFront(player)) {
            player.sendMessage(ChatColor.RED + "前方空间不足！");
            return false;
        }

        launchHook(player, !isRightClick); // 左键为队友模式
        startCooldown(player);
        return true;
    }

    // 投掷钩子
    private void launchHook(Player player, boolean isTeamHook) {
        // 创建投掷物
        Item hook = player.getWorld().dropItem(
                player.getEyeLocation().add(player.getLocation().getDirection()),
                new ItemStack(Material.NETHER_STAR)
        );
        hook.setPickupDelay(Integer.MAX_VALUE);
        hook.setVelocity(player.getEyeLocation().getDirection().multiply(1.8));

        // 启动追踪器（5秒自动清理）
        new HookTracer(hook, player, isTeamHook).runTaskTimer(kitManager.getPlugin(), 0, 1);
    }

    // 修改后的空间检测方法（检查前方1格处3格高度空间）
    private boolean hasEnoughSpaceInFront(Player player) {
        Location start = player.getLocation().add(0, 1, 0); // 从玩家腰部高度开始检测
        Vector direction = start.getDirection().normalize();
        Location checkLoc = start.clone().add(direction.multiply(1)); // 前方1格位置

        // 检查3格垂直空间
        for (int i = 0; i < 3; i++) {
            if (!checkLoc.clone().add(0, i, 0).getBlock().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (fallDamageImmunePlayers.containsKey(p.getUniqueId())) {
                    if (System.currentTimeMillis() < fallDamageImmunePlayers.get(p.getUniqueId())) {
                        event.setCancelled(true);
                    } else {
                        fallDamageImmunePlayers.remove(p.getUniqueId());
                    }
                }
            }
        }
    }

    // 检查玩家是否免疫坠落伤害
    private boolean isImmuneToFallDamage(Player player) {
        UUID playerId = player.getUniqueId();
        if (fallDamageImmunePlayers.containsKey(playerId)) {
            long immuneEndTime = fallDamageImmunePlayers.get(playerId);
            if (System.currentTimeMillis() < immuneEndTime) {
                return true; // 仍在免疫时间内
            } else {
                fallDamageImmunePlayers.remove(playerId); // 免疫时间结束，移除记录
            }
        }
        return false;
    }

    // 玩家退出时清理记录
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        cooldowns.remove(id);
        fallDamageImmunePlayers.remove(id);

        // 清理冷却任务
        if (cooldownTasks.containsKey(id)) {
            cooldownTasks.get(id).cancel();
            cooldownTasks.remove(id);
        }
    }

    // 防止钩子被拾取
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        if (item.getItemStack().getType() == Material.NETHER_STAR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)) {
            updateHookItem(player);
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Scorpio;
    }

    private class HookTracer extends BukkitRunnable {
        private final Item hookProjectile;
        private final Player shooter;
        private final boolean isTeamHook;
        private long startTime;
        private Location lastLocation;

        public HookTracer(Item hookProjectile, Player shooter, boolean isTeamHook) {
            this.hookProjectile = hookProjectile;
            this.shooter = shooter;
            this.isTeamHook = isTeamHook;
            this.startTime = System.currentTimeMillis();
            this.lastLocation = hookProjectile.getLocation();
        }

        @Override
        public void run() {
            if (System.currentTimeMillis() - startTime > 5000) {
                cleanup();
                return;
            }

            if (hookProjectile.getLocation().distanceSquared(lastLocation) > 0.01) {
                showTrailParticles();
                lastLocation = hookProjectile.getLocation();
            }

            checkCollision();
        }

        private void showTrailParticles() {
            Location loc = hookProjectile.getLocation();
            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        private void checkCollision() {
            for (Entity entity : hookProjectile.getNearbyEntities(1.5, 1.5, 1.5)) {
                if (entity instanceof Player) {
                    Player target = (Player) entity;
                    handleCollision(target);
                    return;
                }
            }
        }

        private void handleCollision(Player target) {
            // 新增：排除玩家自身
            if (target.equals(shooter)) {
                return;
            }

            if (isTeamHook && !teamManager.isSameTeam(shooter, target)) return;
            if (!isTeamHook && teamManager.isSameTeam(shooter, target)) return;

            if (isTeamHook) {
                pullShooterToTarget(target);
            } else {
                pullTargetToShooter(target);
            }

            cleanup();
        }

        private void pullShooterToTarget(Player target) {
            Location targetLoc = target.getLocation();
            Vector velocity = targetLoc.toVector()
                    .subtract(shooter.getLocation().toVector())
                    .setY(0.25)  // 保持适当高度
                    .multiply(0.8);
            shooter.setVelocity(velocity);

            playHookSound(shooter.getLocation());
            playHookSound(targetLoc);
        }

        private void pullTargetToShooter(Player target) {
            // 计算传送位置（玩家面前2格）
            Location pullLocation = calculatePullLocation();

            // 设置视角（面向射手）
            pullLocation.setYaw(shooter.getLocation().getYaw());
            pullLocation.setPitch(shooter.getLocation().getPitch());

            target.teleport(pullLocation);
            fallDamageImmunePlayers.put(target.getUniqueId(), System.currentTimeMillis() + 10000);

            playHookSound(shooter.getLocation());
            playHookSound(target.getLocation());
        }

        private Location calculatePullLocation() {
            Location loc = shooter.getLocation();
            Vector dir = loc.getDirection().normalize();
            return loc.add(dir.multiply(2)).add(0, 0.5, 0); // 调整到玩家站立高度
        }

        private void playHookSound(Location loc) {
            loc.getWorld().playSound(loc, Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 0.1f);
        }

        private void cleanup() {
            hookProjectile.remove();
            cancel();
        }
    }
}