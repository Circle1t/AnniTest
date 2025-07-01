package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Builder extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack woodSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack woodShovel;
    private ItemStack materialBrick;

    // 冷却相关字段
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final int OPEN_BOOK_COOLDOWN = 90 * 1000; // 90秒冷却
    private final String MATERIAL_BOOK_NAME = ChatColor.YELLOW + "材料仓库 " + ChatColor.GREEN + "准备就绪";
    private final String MATERIAL_BOOK_COOLDOWN_PREFIX = ChatColor.RED + "冷却中 ";
    private final String MATERIAL_BOOK_COOLDOWN_SUFFIX = " 秒";
    private final HashMap<UUID, BukkitTask> cooldownTasks = new HashMap<>();

    // 放置方块计时
    private final HashMap<UUID, Long> blockPlaceTimers = new HashMap<>();

    // 材料及其最大数量
    private static final Map<Material, Integer> MATERIAL_MAX_QUANTITIES = new HashMap<>();

    static {
        MATERIAL_MAX_QUANTITIES.put(Material.GLASS, 20);
        MATERIAL_MAX_QUANTITIES.put(Material.OAK_SLAB, 20);
        MATERIAL_MAX_QUANTITIES.put(Material.OAK_PLANKS, 70);
        MATERIAL_MAX_QUANTITIES.put(Material.OAK_FENCE, 10);
        MATERIAL_MAX_QUANTITIES.put(Material.TORCH, 5);
        MATERIAL_MAX_QUANTITIES.put(Material.BRICKS, 40);
        MATERIAL_MAX_QUANTITIES.put(Material.STONE, 50);
        MATERIAL_MAX_QUANTITIES.put(Material.WHITE_WOOL, 30);
        MATERIAL_MAX_QUANTITIES.put(Material.DIRT, 60);
        MATERIAL_MAX_QUANTITIES.put(Material.IRON_BARS, 10);
    }

    // 新增 GUI 标题常量
    private static final String WAREHOUSE_GUI_TITLE = ChatColor.BLACK + "材料仓库";

    public Builder(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "建筑师";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.AQUA + "建筑师";
    }

    @Override
    public String getDescription() {
        return "在战火纷飞的世界中，建筑师是那双手能化腐朽为神奇的创造者。自带材料仓库，不断垒砌方块，每 1.5 秒便可收获 2 点经验，助力成长。右键打开材料仓库，虽有冷却，但其中丰富的材料将助你构建起坚不可摧的防线，适用于守家建筑与搭建天桥，是团队坚实的后盾。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.BRICK);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Builder",
                "",
                ChatColor.YELLOW + "你是世界的筑梦者，双手能化腐朽为神奇的创造者。",
                "",
                ChatColor.AQUA + "自带材料仓库，不断放置方块可获经验。",
                ChatColor.AQUA + "右键书打开仓库，90 秒冷却。",
                ChatColor.AQUA + "适用于守家建筑与搭建天桥。",
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

        // 材料仓库
        materialBrick = createSoulBoundItem(Material.BRICK, null, 1, 4, true);
        ItemMeta meta = materialBrick.getItemMeta();
        meta.setDisplayName(MATERIAL_BOOK_NAME);
        materialBrick.setItemMeta(meta);
        kitItems.add(materialBrick.clone());

        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 执行打开仓库操作
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item != null && isMaterialBrick(item) && isThisKit(player)) {
                event.setCancelled(true);
                if (performSpecialAction(player)) {
                    updateMaterialBookItem(player);
                }
            }
        }
    }

    // 放置方块获取经验
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)) {
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            if (!blockPlaceTimers.containsKey(playerId) || currentTime - blockPlaceTimers.get(playerId) >= 1500) {
                player.giveExp(2);
                blockPlaceTimers.put(playerId, currentTime);
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
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + OPEN_BOOK_COOLDOWN);
        startCooldownCheckTask(player);
        updateMaterialBookItem(player);
    }

    // 统一 isMaterialBook 方法逻辑
    private boolean isMaterialBrick(ItemStack stack) {
        return stack != null && SoulBoundUtil.isSoulBoundItem(stack, Material.BRICK);
    }

    // 新增物品更新方法
    private void updateMaterialBookItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack heldItem = inv.getItemInMainHand();

        if (isMaterialBrick(heldItem) && isThisKit(player)) {
            ItemMeta meta = heldItem.getItemMeta();
            long secondsLeft = getCooldownSecondsLeft(player);

            if (isOnCooldown(player)) {
                meta.setDisplayName(MATERIAL_BOOK_COOLDOWN_PREFIX + secondsLeft + MATERIAL_BOOK_COOLDOWN_SUFFIX);
            } else {
                meta.setDisplayName(MATERIAL_BOOK_NAME);
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
                        updateMaterialBookItemsInInventory(player);
                    }
                    cooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                } else {
                    updateMaterialBookItem(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        cooldownTasks.put(player.getUniqueId(), task);
    }

    private void updateMaterialBookItemsInInventory(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (isMaterialBrick(item)) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(MATERIAL_BOOK_NAME);
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

        // 打开 GUI
        openMaterialWarehouse(player);
        startCooldown(player);
        return true;
    }

    // 打开材料仓库界面
    private void openMaterialWarehouse(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, WAREHOUSE_GUI_TITLE);

        // 随机排列材料
        List<Material> materials = new ArrayList<>(MATERIAL_MAX_QUANTITIES.keySet());
        Collections.shuffle(materials);

        // 最多选取 6 种材料
        int materialCount = Math.min(6, materials.size());
        Random random = new Random();
        for (int i = 0; i < materialCount; i++) {
            Material material = materials.get(i);
            int maxQuantity = MATERIAL_MAX_QUANTITIES.get(material);
            int quantity = random.nextInt(maxQuantity) + 1;

            ItemStack item = new ItemStack(material, quantity);
            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)) {
            updateMaterialBookItem(player);
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Builder;
    }
}