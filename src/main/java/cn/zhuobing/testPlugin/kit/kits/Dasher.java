package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Dasher extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack woodSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack blinkItem;

    // 冷却相关字段
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final String BLINK_ITEM_NAME = ChatColor.YELLOW + "闪现 " + ChatColor.GREEN + "准备就绪";
    private final String BLINK_COOLDOWN_PREFIX = ChatColor.RED + "冷却中 ";
    private final String BLINK_COOLDOWN_SUFFIX = " 秒";
    private final HashMap<UUID, BukkitTask> cooldownTasks = new HashMap<>();

    // 无法瞬移到的方块
    private static final List<Material> BLOCKED_MATERIALS = Arrays.asList(
            Material.GLASS,
            Material.BRICKS,
            Material.BRICK_STAIRS,
            Material.STONE_BRICK_SLAB,
            Material.IRON_BARS,
            Material.OAK_FENCE,
            Material.NETHER_BRICK_FENCE
    );

    // 记录每个玩家当前显示的 Fake Block 位置
    private final Map<UUID, Location> fakeBlockLocations = new HashMap<>();

    public Dasher(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "末影人";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.LIGHT_PURPLE + "末影人";
    }

    @Override
    public String getDescription() {
        return "自带名为“闪现”的紫色染料，在潜行状态下，右键紫色染料可瞬移。瞬移距离为 5 - 20 格，瞬移位置取决于鼠标瞄准显示的方块。冷却时间取决于瞬移格数（1格 = 冷却时间增加1s）。瞬移后会根据距离获得对应的饥饿效果，绿宝石块距离饥饿1秒，金块距离3秒，钻石块距离5秒。瞬移后还会获得与饥饿效果同秒数的虚弱效果。瞬移位置上方必须有至少 2 格的空间才能成功瞬移。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Dasher",
                "",
                ChatColor.YELLOW + "你是速度与瞬移的掌控者。",
                "",
                ChatColor.AQUA + "你拥有“闪现”的超凡能力，潜行时右键瞬移。",
                ChatColor.AQUA + "瞬移距离 5 - 20 格，冷却时间随距离增加。",
                ChatColor.AQUA + "不过瞬移后会使你根据距离获得饥饿和虚弱效果。",
                ChatColor.AQUA + "瞬移位置上方必须有至少 2 格的空间才能成功瞬移。",
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

        // Blink 物品
        blinkItem = createSoulBoundItem(Material.PURPLE_DYE, null, 1, 4, true);
        ItemMeta itemMeta = blinkItem.getItemMeta();
        itemMeta.setDisplayName(BLINK_ITEM_NAME);
        blinkItem.setItemMeta(itemMeta);
        kitItems.add(blinkItem.clone());

        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 执行瞬移操作
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item != null && isBlinkItem(item) && isThisKit(player) && player.isSneaking()) {
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

    private void startCooldown(Player player, int distance) {
        int cooldownTime = distance * 1000; // 1格 = 冷却时间增加1s
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldownTime);
        startCooldownCheckTask(player);
        updateBlinkItem(player);
    }

    // 统一 isBlinkItem 方法逻辑
    private boolean isBlinkItem(ItemStack stack) {
        return stack != null && SoulBoundUtil.isSoulBoundItem(stack, Material.PURPLE_DYE);
    }

    // 新增物品更新方法
    private void updateBlinkItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack heldItem = inv.getItemInMainHand();

        if (isBlinkItem(heldItem) && isThisKit(player)) {
            ItemMeta meta = heldItem.getItemMeta();
            long secondsLeft = getCooldownSecondsLeft(player);

            if (isOnCooldown(player)) {
                meta.setDisplayName(BLINK_COOLDOWN_PREFIX + secondsLeft + BLINK_COOLDOWN_SUFFIX);
            } else {
                meta.setDisplayName(BLINK_ITEM_NAME);
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
                        updateBlinkItemsInInventory(player);
                    }
                    cooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                } else {
                    updateBlinkItem(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        cooldownTasks.put(player.getUniqueId(), task);
    }

    private void updateBlinkItemsInInventory(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (isBlinkItem(item)) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(BLINK_ITEM_NAME);
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

        RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 20);
        if (rayTrace == null || rayTrace.getHitBlock() == null) {
            player.sendMessage(ChatColor.RED + "未找到有效目标！");
            return false;
        }

        Location targetLocation = rayTrace.getHitBlock().getLocation().add(0.5, 1, 0.5);
        double distance = player.getLocation().distance(targetLocation);

        // 新增：必须满足 Fake Block 的显示条件才能瞬移
        if (!isValidBlinkTarget(player, rayTrace.getHitBlock().getLocation(), distance)) {
            player.sendMessage(ChatColor.RED + "无效的瞬移目标！");
            return false;
        }

        if (distance < 5 || distance > 20) {
            player.sendMessage(ChatColor.RED + "瞬移距离必须在 5 - 20 格之间！");
            return false;
        }

        if (BLOCKED_MATERIALS.contains(rayTrace.getHitBlock().getType())) {
            player.sendMessage(ChatColor.RED + "无法瞬移到该方块！");
            return false;
        }

        if (!hasEnoughSpaceAbove(targetLocation)) {
            player.sendMessage(ChatColor.RED + "瞬移位置上方空间不足！");
            return false;
        }

        // 瞬移轨迹粒子效果
        displayTeleportTrail(player.getLocation(), targetLocation);

        // 瞬移
        player.teleport(targetLocation);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // 添加饥饿和虚弱效果
        int duration;
        if (distance >= 5 && distance < 10) {
            duration = 20;
        } else if (distance >= 10 && distance < 15) {
            duration = 3 * 20;
        } else if (distance >= 15 && distance <= 20) {
            duration = 5 * 20;
        } else {
            duration = 0;
        }

        if (duration > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0));
        }

        // 开始冷却
        startCooldown(player, (int) distance);
        return true;
    }

    // 验证目标位置是否符合 Fake Block 显示条件
    private boolean isValidBlinkTarget(Player player, Location blockLocation, double distance) {
        // 检查距离范围
        if (distance < 5 || distance > 20) return false;

        // 检查方块类型
        if (BLOCKED_MATERIALS.contains(blockLocation.getBlock().getType())) return false;

        // 检查上方空间
        if (!hasEnoughSpaceAbove(blockLocation.clone().add(0.5, 0, 0.5))) return false;

        // 与 showTargetBlock 完全相同的条件判断
        return true;
    }

    // 显示瞬移轨迹粒子效果
    private void displayTeleportTrail(Location start, Location end) {
        World world = start.getWorld();
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        for (double i = 0; i < distance; i += 0.2) {
            Location particleLoc = start.clone().add(direction.clone().multiply(i));
            world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    // 显示准星方块
    private void showTargetBlock(Player player) {
        RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 20);
        if (rayTrace != null && rayTrace.getHitBlock() != null) {
            Location targetLocation = rayTrace.getHitBlock().getLocation();
            double distance = player.getLocation().distance(targetLocation);

            // 使用与瞬移完全相同的验证逻辑
            if (!isValidBlinkTarget(player, targetLocation, distance)) {
                restorePreviousFakeBlock(player);
                return;
            }

            Material displayMaterial;

            // 根据距离选择显示的方块
            if (distance >= 5 && distance < 10) {
                displayMaterial = Material.EMERALD_BLOCK; // 绿宝石块
            } else if (distance >= 10 && distance < 15) {
                displayMaterial = Material.GOLD_BLOCK; // 金块
            } else if (distance >= 15 && distance <= 20) {
                displayMaterial = Material.DIAMOND_BLOCK; // 钻石块
            } else {
                // 距离不在范围内，恢复之前的 Fake Block
                restorePreviousFakeBlock(player);
                return;
            }

            if (!hasEnoughSpaceAbove(targetLocation)) {
                // 空间不足，恢复之前的 Fake Block
                restorePreviousFakeBlock(player);
                return;
            }

            // 恢复之前的 Fake Block
            restorePreviousFakeBlock(player);

            // 发送 Fake Block 数据包给玩家
            sendFakeBlock(player, targetLocation, displayMaterial);

            // 记录当前显示的 Fake Block 位置
            fakeBlockLocations.put(player.getUniqueId(), targetLocation);
        } else {
            // 未找到目标方块，恢复之前的 Fake Block
            restorePreviousFakeBlock(player);
        }
    }

    private void sendFakeBlock(Player player, Location location, Material material) {
        // 获取目标方块的 BlockData
        BlockData blockData = material.createBlockData();

        // 发送 Fake Block 数据包
        player.sendBlockChange(location, blockData);
    }

    private void restorePreviousFakeBlock(Player player) {
        UUID playerId = player.getUniqueId();
        if (fakeBlockLocations.containsKey(playerId)) {
            Location previousLocation = fakeBlockLocations.get(playerId);
            BlockData originalBlockData = previousLocation.getBlock().getBlockData();
            player.sendBlockChange(previousLocation, originalBlockData);
            fakeBlockLocations.remove(playerId);
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)) {
            // 恢复之前的 Fake Block
            restorePreviousFakeBlock(player);

            // 如果手持的是闪现物品，显示目标方块
            if (isBlinkItem(player.getInventory().getItem(event.getNewSlot()))) {
                showTargetBlock(player);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)) {
            // 如果手持的是闪现物品，显示目标方块
            if (isBlinkItem(player.getInventory().getItemInMainHand())) {
                showTargetBlock(player);
            } else {
                // 手持非闪现物品，恢复之前的 Fake Block
                restorePreviousFakeBlock(player);
            }
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Dasher;
    }

    // 检查瞬移位置上方是否有至少 2 格的空间
    private boolean hasEnoughSpaceAbove(Location location) {
        World world = location.getWorld();
        for (int i = 1; i <= 2; i++) {
            Location above = location.clone().add(0, i, 0);
            if (!above.getBlock().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}