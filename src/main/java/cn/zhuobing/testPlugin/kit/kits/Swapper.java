package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
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

public class Swapper extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack woodSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack woodShovel;
    private ItemStack swapperItem;

    // 冷却相关字段
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final int SWAP_COOLDOWN = 20 * 1000; // 20秒冷却
    private final String SWAPPER_ITEM_NAME = ChatColor.YELLOW + "交换之音 " + ChatColor.GREEN + "准备就绪";
    private final String SWAPPER_COOLDOWN_PREFIX = ChatColor.RED + "冷却中 ";
    private final String SWAPPER_COOLDOWN_SUFFIX = " 秒";
    private final HashMap<UUID, BukkitTask> cooldownTasks = new HashMap<>();


    public Swapper(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "交换者";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.GREEN + "交换者";
    }

    @Override
    public String getDescription() {
        return "能够与附近的敌人交换位置，每 20 秒可使用一次。交换后敌人会获得 3 秒缓慢 II 效果。交换者是一个适合团队配合的职业，可将敌人拉到有利位置或使自己占据敌人的优势位置。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.MUSIC_DISC_CAT);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Swapper",
                "",
                ChatColor.YELLOW + "你是位置的掌控者。",
                "",
                ChatColor.AQUA + "每 20 秒可与附近的敌人交换位置，",
                ChatColor.AQUA + "交换后敌人会获得 3 秒缓慢 II 效果。",
                ChatColor.AQUA + "适合团队配合，可改变战局。",
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
        // 木剑
        woodSword = createSoulBoundItem(Material.WOODEN_SWORD, null, 1, 1, false);
        kitItems.add(woodSword.clone());
        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());
        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());
        // 木铲
        woodShovel = createSoulBoundItem(Material.WOODEN_SHOVEL, null, 1, 1, false);
        kitItems.add(woodShovel.clone());

        // 交换者物品
        swapperItem = createSoulBoundItem(Material.MUSIC_DISC_CAT, SWAPPER_ITEM_NAME, 1, 4, true);
        kitItems.add(swapperItem.clone());

        // 指南针
        kitItems.add(CompassItem.createCompass());
    }


    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 执行交换操作
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item != null && isSwapperItem(item) && isThisKit(player)) {
                if (performSpecialAction(player)) {
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
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + SWAP_COOLDOWN);
        startCooldownCheckTask(player);
        updateSwapperItem(player);
    }

    // 统一 isSwapperItem 方法逻辑
    private boolean isSwapperItem(ItemStack stack) {
        return stack != null && SoulBoundUtil.isSoulBoundItem(stack, Material.MUSIC_DISC_CAT);
    }

    // 新增物品更新方法
    private void updateSwapperItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack heldItem = inv.getItemInMainHand();

        if (isSwapperItem(heldItem) && isThisKit(player)) {
            ItemMeta meta = heldItem.getItemMeta();
            long secondsLeft = getCooldownSecondsLeft(player);

            if (secondsLeft > 0) {
                meta.setDisplayName(SWAPPER_COOLDOWN_PREFIX + secondsLeft + SWAPPER_COOLDOWN_SUFFIX);
            } else {
                meta.setDisplayName(SWAPPER_ITEM_NAME);
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
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.0f);
                    player.sendMessage(ChatColor.GREEN + "你的技能 " + ChatColor.YELLOW + "准备就绪！");
                    updateSwapperItemsInInventory(player);
                    cooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                } else {
                    updateSwapperItem(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        cooldownTasks.put(player.getUniqueId(), task);
    }

    private void updateSwapperItemsInInventory(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (isSwapperItem(item)) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(SWAPPER_ITEM_NAME);
                item.setItemMeta(meta);
            }
        }
        player.updateInventory();
    }


    private boolean performSpecialAction(Player player) {
        if (isOnCooldown(player)) {
            long secondsLeft = getCooldownSecondsLeft(player);
            player.sendMessage(ChatColor.GREEN + "技能冷却中，剩余 " + ChatColor.YELLOW + secondsLeft + ChatColor.GREEN + " 秒");
            return false;
        }

        Player target = getPlayerInSight(player, 15);
        if (target == null || isSameTeam(player, target)) {
            //player.sendMessage(ChatColor.RED + "未找到有效目标！");
            return false;
        }

        // 获取当前位置
        Location playerLoc = player.getLocation().clone();
        Location targetLoc = target.getLocation().clone();

        // 确保交换后视线平视原位置的XZ坐标
        Location playerNewLoc = targetLoc.clone();
        Location targetNewLoc = playerLoc.clone();

        // 设置玩家交换后看向原位置XZ坐标的朝向
        Vector playerToOriginalXZ = new Vector(playerLoc.getX() - targetLoc.getX(), 0, playerLoc.getZ() - targetLoc.getZ());
        playerNewLoc.setDirection(playerToOriginalXZ);

        // 设置目标玩家交换后看向原位置XZ坐标的朝向
        Vector targetToOriginalXZ = new Vector(targetLoc.getX() - playerLoc.getX(), 0, targetLoc.getZ() - playerLoc.getZ());
        targetNewLoc.setDirection(targetToOriginalXZ);

        // 交换位置并设置朝向
        player.teleport(playerNewLoc);
        target.teleport(targetNewLoc);

        // 添加缓慢效果
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 1));

        // 播放交换音效
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // 显示粒子效果
        displayParticleEffect(playerLoc, targetLoc);

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
            updateSwapperItem(player);
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
            world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Swapper;
    }
}