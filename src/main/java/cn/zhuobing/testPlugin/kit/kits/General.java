package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class General extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack ironSword;
    private ItemStack goldPickaxe;
    private ItemStack goldAxe;
    private ItemStack compass;
    private ItemStack loyaltyItem;
    private ItemStack purpleEgg;
    private ItemStack sunFlower;

    // 冷却相关字段
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final int PURPLE_EGG_COOLDOWN = 60 * 1000; // 60秒冷却
    private final String LOYALTY_ITEM_NAME = ChatColor.GOLD + "忠橙";
    private final String PURPLE_EGG_ITEM_NAME = ChatColor.DARK_PURPLE + "紫蛋 " + ChatColor.GREEN + "准备就绪";
    private final String PURPLE_EGG_COOLDOWN_PREFIX = ChatColor.RED + "冷却中 ";
    private final String PURPLE_EGG_COOLDOWN_SUFFIX = " 秒";
    private final HashMap<UUID, BukkitTask> cooldownTasks = new HashMap<>();


    public General(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "将军";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.RED + "将军";
    }

    @Override
    public String getDescription() {
        return "你从丹东来换我一片雪白，将军拥有着坚韧的意志和强大的力量。拥有灵魂绑定的铁剑、金镐、金斧和指南针，还有特殊的金头盔与皮革装甲。技能“忠橙”能带来 10 秒力量 1 效果，“紫蛋”可对敌人造成伤害并使其缓慢。将军职业始终拥有缓慢一的效果。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "General",
                "",
                ChatColor.RED + "你从丹东来，换我一城雪白！",
                "",
                ChatColor.AQUA + "拥有" + ChatColor.RED + "超强" + ChatColor.AQUA + "的初始装备，",
                ChatColor.AQUA + "吃下“忠橙”获得 10 秒力量 1 效果，",
                ChatColor.AQUA + "使用“紫蛋”对敌人造成伤害并使其缓慢，",
                ChatColor.AQUA + "不过肥胖将使你始终拥有缓慢一的效果，",
                ChatColor.GOLD + "并且将军喝下牛奶后牛奶会被立即蒸发。",
                " "
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // 护甲
        String teamColor = teamManager.getPlayerTeamName(player);
        inv.setHelmet(sunFlower);
        inv.setChestplate(SpecialLeatherArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor));
        inv.setLeggings(SpecialLeatherArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor));
        inv.setBoots(SpecialLeatherArmor.createArmor(Material.LEATHER_BOOTS, teamColor));

        for (ItemStack item : kitItems) {
            inv.addItem(item);
        }

        // 延迟 1 tick 后添加缓慢一的效果
        Plugin plugin = kitManager.getPlugin();
        if (plugin != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @Override
    public void onKitSet(Player player) {
        // 延迟 1 tick 后添加缓慢一的效果
        Plugin plugin = kitManager.getPlugin();
        if (plugin != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @Override
    public void onKitUnset(Player player) {
        // 移除缓慢效果
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    private void setUp() {
        sunFlower = createSoulBoundItem(Material.SUNFLOWER, ChatColor.YELLOW + "太阳", 1, 3, false);
        // 铁剑
        ironSword = createSoulBoundItem(Material.IRON_SWORD, null, 1, 3, false);
        kitItems.add(ironSword.clone());
        // 金镐
        goldPickaxe = createSoulBoundItem(Material.GOLDEN_PICKAXE, null, 1, 3, false);
        kitItems.add(goldPickaxe.clone());
        // 金斧
        goldAxe = createSoulBoundItem(Material.GOLDEN_AXE, null, 1, 3, false);
        kitItems.add(goldAxe.clone());
        // 忠橙
        loyaltyItem = createSoulBoundItem(Material.GLOW_BERRIES, LOYALTY_ITEM_NAME, 1, 3, true);
        kitItems.add(loyaltyItem.clone());
        // 紫蛋
        purpleEgg = createSoulBoundItem(Material.DRAGON_EGG, null, 1, 4, true);
        ItemMeta purpleMeta = purpleEgg.getItemMeta();
        purpleMeta.setDisplayName(PURPLE_EGG_ITEM_NAME); // 设置显示名称
        purpleEgg.setItemMeta(purpleMeta);
        kitItems.add(purpleEgg.clone());

        // 指南针
        compass = CompassItem.createCompass();
        kitItems.add(compass.clone());
    }


    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 忠橙技能处理
    @EventHandler
    public void onConsumeLoyaltyItem(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && isLoyaltyItem(item) && isThisKit(player)) {
            // 修改为添加力量 1 效果
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 10 * 20, 0));
        }
    }

    // 紫蛋技能处理
    @EventHandler
    public void onRightClickPurpleEgg(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item != null && isPurpleEgg(item) && isThisKit(player)) {
                event.setCancelled(true);
                if (performPurpleEggAction(player)) {
                    updatePurpleEggItem(player);
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
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + PURPLE_EGG_COOLDOWN);
        startCooldownCheckTask(player);
        updatePurpleEggItem(player);
    }

    // 统一 isLoyaltyItem 方法逻辑
    private boolean isLoyaltyItem(ItemStack stack) {
        return stack != null && SoulBoundUtil.isSoulBoundItem(stack, Material.GLOW_BERRIES);
    }

    // 统一 isPurpleEgg 方法逻辑
    private boolean isPurpleEgg(ItemStack stack) {
        return stack != null && SoulBoundUtil.isSoulBoundItem(stack, Material.DRAGON_EGG);
    }

    // 新增物品更新方法
    private void updatePurpleEggItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack heldItem = inv.getItemInMainHand();

        if (isPurpleEgg(heldItem) && isThisKit(player)) {
            ItemMeta meta = heldItem.getItemMeta();
            long secondsLeft = getCooldownSecondsLeft(player);

            if (isOnCooldown(player)) {
                meta.setDisplayName(PURPLE_EGG_COOLDOWN_PREFIX + secondsLeft + PURPLE_EGG_COOLDOWN_SUFFIX);
            } else {
                meta.setDisplayName(PURPLE_EGG_ITEM_NAME);
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
                    if (isThisKit(player)) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.0f);
                        player.sendMessage(ChatColor.GREEN + "你的技能 " + ChatColor.DARK_PURPLE + "紫蛋 " + ChatColor.GREEN + "准备就绪！");
                        updatePurpleEggItemsInInventory(player);
                    }
                    cooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                } else {
                    updatePurpleEggItem(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        cooldownTasks.put(player.getUniqueId(), task);
    }

    private void updatePurpleEggItemsInInventory(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (isPurpleEgg(item)) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(PURPLE_EGG_ITEM_NAME);
                item.setItemMeta(meta);
            }
        }
        player.updateInventory();
    }


    private boolean performPurpleEggAction(Player player) {
        if (isOnCooldown(player)) {
            long secondsLeft = getCooldownSecondsLeft(player);
            player.sendMessage(ChatColor.GREEN + "技能冷却中，剩余 " + ChatColor.YELLOW + secondsLeft + ChatColor.GREEN + " 秒");
            return false;
        }

        Player target = getPlayerInSight(player, 10);
        if (target == null || isSameTeam(player, target)) {
            //player.sendMessage(ChatColor.RED + "未找到有效目标！");
            return false;
        }

        // 修改紫蛋伤害为 8
        target.damage(8);
        // 添加缓慢效果
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2 * 20, 0));

        // 播放音效
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);

        // 显示粒子效果
        displayParticleEffect(player.getLocation(), target.getLocation());

        startCooldown(player);
        return true;
    }

    private Player getPlayerInSight(Player player, int range) {
        Location eyeLoc = player.getEyeLocation();
        // 先进行实体射线追踪，找到可能的目标玩家
        RayTraceResult entityTrace = player.getWorld().rayTraceEntities(eyeLoc, eyeLoc.getDirection(), range, entity -> entity instanceof Player && !isSameTeam(player, (Player) entity));
        if (entityTrace == null || !(entityTrace.getHitEntity() instanceof Player)) {
            return null;
        }
        Player target = (Player) entityTrace.getHitEntity();

        // 再进行方块射线追踪，检查是否有障碍物
        RayTraceResult blockTrace = player.getWorld().rayTraceBlocks(eyeLoc, eyeLoc.getDirection(), range);
        if (blockTrace != null) {
            // 获取方块和目标玩家到玩家的距离
            double blockDistance = blockTrace.getHitPosition().distance(eyeLoc.toVector());
            double entityDistance = entityTrace.getHitPosition().distance(eyeLoc.toVector());
            // 如果方块距离小于目标玩家距离，说明有障碍物，目标不在视线内
            if (blockDistance < entityDistance) {
                return null;
            }
        }

        return target;
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)) {
            updatePurpleEggItem(player);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (isPurpleEgg(item) && isThisKit(player)) {
            event.setCancelled(true);
            //player.sendMessage(ChatColor.RED + "将军的紫蛋禁止被放置！");
        }
    }

    private boolean isSameTeam(Player p1, Player p2) {
        return teamManager.getPlayerTeamName(p1).equals(teamManager.getPlayerTeamName(p2));
    }

    private void displayParticleEffect(Location loc1, Location loc2) {
        World world = loc1.getWorld();
        Vector direction = loc2.toVector().subtract(loc1.toVector()).normalize();
        double distance = loc1.distance(loc2);
        for (double i = 0; i < distance; i += 0.5) {
            Location particleLoc = loc1.clone().add(direction.clone().multiply(i));
            world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    // 处理玩家喝牛奶事件
    @EventHandler
    public void onPlayerDrinkMilk(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item.getType() == Material.MILK_BUCKET && isThisKit(player)) {
            // 取消事件默认行为，防止清除药水效果
            event.setCancelled(true);
            // 将牛奶桶替换为空桶
            PlayerInventory inventory = player.getInventory();
            int heldSlot = inventory.getHeldItemSlot();
            inventory.setItem(heldSlot, new ItemStack(Material.BUCKET));
            // 提示玩家
            player.sendMessage(ChatColor.GOLD + "☀ 将军的高温把牛奶蒸发了");
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof General;
    }
}