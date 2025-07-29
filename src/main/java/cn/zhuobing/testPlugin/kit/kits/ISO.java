package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.MessageUtil;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class ISO extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack goldSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack amethystItem;

    // 冷却相关字段
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final int FLOW_COOLDOWN = 60 * 1000; // 60秒冷却
    private final String FLOW_ITEM_NAME = ChatColor.LIGHT_PURPLE + "战斗心流 " + ChatColor.GREEN + "准备就绪";
    private final String FLOW_COOLDOWN_PREFIX = ChatColor.RED + "冷却中 ";
    private final String FLOW_COOLDOWN_SUFFIX = " 秒";
    private final HashMap<UUID, BukkitTask> cooldownTasks = new HashMap<>();

    // 心流状态相关
    private final Set<UUID> inFlowState = new HashSet<>();
    private final Map<Location, UUID> crystalClusters = new HashMap<>(); // 位置到所有者UUID的映射
    private final Map<Location, BukkitTask> crystalDespawnTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> flowStateTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> goldenHeartTasks = new HashMap<>();

    public ISO(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "壹决";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.LIGHT_PURPLE + "壹决";
    }

    @Override
    public String getDescription() {
        return "集中意念进入心流状态，击败敌人后在其位置生成紫水晶簇。拾取水晶簇可获得金心，大幅提升生存能力。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "ISO",
                "",
                ChatColor.YELLOW + "你是战斗节奏的掌控者。",
                "",
                ChatColor.AQUA + "每 60 秒可进入10秒心流状态，",
                ChatColor.AQUA + "击败敌人后在其位置生成" + ChatColor.LIGHT_PURPLE + "紫水晶簇。",
                ChatColor.AQUA + "点击水晶簇可获得持续5秒的" + ChatColor.GOLD + "心流护盾2❤",
                ChatColor.AQUA + "水晶簇5秒后自动消失。",
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

        // 皮革护甲
        List<ItemStack> armors = getKitArmors(player);
        for (ItemStack armor : armors) {
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
    }

    private void setUp() {
        // 金剑
        goldSword = createSoulBoundItem(Material.GOLDEN_SWORD, null, 1, 1, false);
        kitItems.add(goldSword.clone());
        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());
        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());

        // 紫水晶碎片（技能物品）- 绑定等级为4，只有玩家自己能使用
        amethystItem = createSoulBoundItem(Material.AMETHYST_SHARD, null, 1, 4, true);
        ItemMeta itemMeta = amethystItem.getItemMeta();
        itemMeta.setDisplayName(FLOW_ITEM_NAME);
        amethystItem.setItemMeta(itemMeta);
        kitItems.add(amethystItem.clone());

        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 执行技能操作
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 只有紫水晶碎片的拥有者可以使用
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && item != null && isAmethystItem(item) && isThisKit(player)) {
            if (performSpecialAction(player)) {
                event.setCancelled(true);
            }
        }

        // 检测玩家左右键点击紫水晶簇方块
        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getType() == Material.AMETHYST_CLUSTER) {
                Location loc = clickedBlock.getLocation();
                if (crystalClusters.containsKey(loc)) {
                    UUID ownerId = crystalClusters.get(loc);
                    if (!player.getUniqueId().equals(ownerId)) {
                        player.sendMessage(ChatColor.RED + "这不是你的水晶簇！");
                        event.setCancelled(true);
                        return;
                    }
                    // 移除水晶簇并给予金心
                    removeCrystalCluster(loc);
                    giveGoldenHeart(player);
                    event.setCancelled(true);
                }
            }
        }
    }

    // 防止水晶簇被破坏
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.AMETHYST_CLUSTER && crystalClusters.containsKey(block.getLocation())) {
            event.setCancelled(true);
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
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + FLOW_COOLDOWN);
        startCooldownCheckTask(player);
        updateAmethystItem(player);
    }

    private boolean isAmethystItem(ItemStack stack) {
        return stack != null && SoulBoundUtil.isSoulBoundItem(stack, Material.AMETHYST_SHARD);
    }

    private void updateAmethystItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack heldItem = inv.getItemInMainHand();

        if (isAmethystItem(heldItem) && isThisKit(player)) {
            ItemMeta meta = heldItem.getItemMeta();
            long secondsLeft = getCooldownSecondsLeft(player);

            if (isOnCooldown(player)) {
                meta.setDisplayName(FLOW_COOLDOWN_PREFIX + secondsLeft + FLOW_COOLDOWN_SUFFIX);
            } else {
                meta.setDisplayName(FLOW_ITEM_NAME);
            }

            heldItem.setItemMeta(meta);
            player.updateInventory();
        }
    }

    private void startCooldownCheckTask(Player player) {
        Plugin plugin = kitManager.getPlugin();
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
                        player.sendMessage(ChatColor.GREEN + "你的技能 " + ChatColor.LIGHT_PURPLE + "战斗心流 " + ChatColor.GREEN + "准备就绪！");
                        updateAmethystItemsInInventory(player);
                    }
                    cooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                } else {
                    updateAmethystItem(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        cooldownTasks.put(player.getUniqueId(), task);
    }

    private void updateAmethystItemsInInventory(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (isAmethystItem(item)) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(FLOW_ITEM_NAME);
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

        // 进入心流状态
        enterFlowState(player);
        startCooldown(player);
        return true;
    }

    private void enterFlowState(Player player) {
        UUID uuid = player.getUniqueId();
        inFlowState.add(uuid);

        // 添加视觉效果
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 10 * 20, 0));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "♫ 你进入了心流状态！击败敌人将生成紫水晶簇！");

        // 在行动栏显示剩余时间
        startFlowStateTimer(player);
    }

    private void startFlowStateTimer(Player player) {
        UUID uuid = player.getUniqueId();
        Plugin plugin = kitManager.getPlugin();

        // 取消现有的计时任务（如果有）
        if (flowStateTasks.containsKey(uuid)) {
            flowStateTasks.get(uuid).cancel();
        }

        giveGoldenHeart(player);

        flowStateTasks.put(uuid, new BukkitRunnable() {
            int secondsLeft = 10;

            @Override
            public void run() {
                if (!inFlowState.contains(uuid) || !player.isOnline()) {
                    this.cancel();
                    flowStateTasks.remove(uuid);
                    return;
                }

                if (secondsLeft > 0) {
                    // 在行动栏显示剩余时间
                    MessageUtil.sendActionBarMessage(player,
                            ChatColor.LIGHT_PURPLE + "心流状态: " +
                                    ChatColor.YELLOW + secondsLeft +
                                    ChatColor.LIGHT_PURPLE + "秒");
                    secondsLeft--;
                } else {
                    inFlowState.remove(uuid);
                    player.sendMessage(ChatColor.GRAY + "心流状态已结束。");
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 1.0f);
                    this.cancel();
                    flowStateTasks.remove(uuid);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    // 玩家击杀敌人时触发
    @EventHandler
    public void onPlayerKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || !inFlowState.contains(killer.getUniqueId()) || isSameTeam(killer, victim)) {
            return;
        }

        // 在死亡位置生成紫水晶簇，并记录所有者为killer
        spawnCrystalCluster(victim, killer);
    }

    private void spawnCrystalCluster(Player victim, Player owner) {
        Location loc = victim.getLocation().getBlock().getLocation();

        // 放置紫水晶簇方块
        loc.getBlock().setType(Material.AMETHYST_CLUSTER);

        // 记录所有者
        crystalClusters.put(loc, owner.getUniqueId());

        // 粒子和音效
        victim.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.1);
        victim.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.0f);

        // 5秒后自动消失
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (crystalClusters.containsKey(loc)) {
                    removeCrystalCluster(loc);
                }
                crystalDespawnTasks.remove(loc);
            }
        }.runTaskLater(kitManager.getPlugin(), 5 * 20);

        crystalDespawnTasks.put(loc, task);
    }

    private void removeCrystalCluster(Location loc) {
        // 移除紫水晶簇方块
        if (loc.getBlock().getType() == Material.AMETHYST_CLUSTER) {
            loc.getBlock().setType(Material.AIR);
        }

        // 添加消失粒子效果
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.1);

        // 从记录中移除
        crystalClusters.remove(loc);

        // 取消自动消失任务
        BukkitTask task = crystalDespawnTasks.remove(loc);
        if (task != null) {
            task.cancel();
        }
    }

    private void giveGoldenHeart(Player player) {
        UUID playerId = player.getUniqueId();

        // 取消现有的金心效果任务
        if (goldenHeartTasks.containsKey(playerId)) {
            goldenHeartTasks.get(playerId).cancel();
        }

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,  // 效果类型
                5 * 20,                      // 持续时间（5秒）
                0,                           // 等级设为0 = 2颗金心
                false,                        // 无粒子效果
                true,                         // 显示图标
                true                          // 环境音效
        ));

        // 可选：更新消息提示
        // player.sendMessage(ChatColor.GOLD + "♡ 你获得了金心！获得5秒" + ChatColor.GOLD + "2颗金心" + ChatColor.GOLD + "效果！");

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        // 5秒后效果结束
        goldenHeartTasks.put(playerId, new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    //player.sendMessage(ChatColor.GOLD + "心流护盾已消失！");

                    // 移除剩余吸收效果
                    if (player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
                        player.removePotionEffect(PotionEffectType.ABSORPTION);
                    }
                }
                goldenHeartTasks.remove(playerId);
            }
        }.runTaskLater(kitManager.getPlugin(), 5 * 20));
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)) {
            updateAmethystItem(player);
        }
    }

    // 让水晶簇没有碰撞体积（玩家可穿过）
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;
        Block block = to.getBlock();
        if (block.getType() == Material.AMETHYST_CLUSTER && crystalClusters.containsKey(block.getLocation())) {
            // 让玩家略微上浮，避免被卡住
            event.getPlayer().setVelocity(event.getPlayer().getVelocity().setY(0.1));
        }
    }

    private boolean isSameTeam(Player p1, Player p2) {
        return teamManager.getPlayerTeamName(p1).equals(teamManager.getPlayerTeamName(p2));
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof ISO;
    }
}