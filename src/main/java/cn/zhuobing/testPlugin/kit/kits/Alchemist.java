package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Alchemist extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private final GameManager gameManager;
    private List<ItemStack> kitItems = new ArrayList<>();
    private final Map<UUID, Location> personalBrewingStands = new ConcurrentHashMap<>();


    private ItemStack woodSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack brewingStand;

    // 材料仓库相关
    private ItemStack materialBook;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final int OPEN_BOOK_COOLDOWN = 90 * 1000; // 90秒冷却
    private final String MATERIAL_BOOK_NAME = ChatColor.YELLOW + "炼药材料库 " + ChatColor.GREEN + "准备就绪";
    private final String MATERIAL_BOOK_COOLDOWN_PREFIX = ChatColor.RED + "冷却中 ";
    private final String MATERIAL_BOOK_COOLDOWN_SUFFIX = " 秒";
    private final HashMap<UUID, BukkitTask> cooldownTasks = new HashMap<>();

    private BukkitTask brewingAcceleratorTask = null;

    // 材料概率分布
    private static final Map<Material, Double> MATERIAL_PROBABILITIES = new LinkedHashMap<>();

    static {
        // 配置材料概率
        MATERIAL_PROBABILITIES.put(Material.FERMENTED_SPIDER_EYE, 0.32);  // 很常见
        MATERIAL_PROBABILITIES.put(Material.GLISTERING_MELON_SLICE, 0.28); // 常见
        MATERIAL_PROBABILITIES.put(Material.GOLDEN_CARROT, 0.28);         // 常见
        MATERIAL_PROBABILITIES.put(Material.SUGAR, 0.28);                 // 常见
        MATERIAL_PROBABILITIES.put(Material.SPIDER_EYE, 0.28);            // 常见
        MATERIAL_PROBABILITIES.put(Material.MAGMA_CREAM, 0.28);           // 常见
        MATERIAL_PROBABILITIES.put(Material.GUNPOWDER, 0.28);             // 常见
        MATERIAL_PROBABILITIES.put(Material.GLOWSTONE_DUST, 0.15);        // 不常见
        MATERIAL_PROBABILITIES.put(Material.GHAST_TEAR, 0.07);            // 稀有
        MATERIAL_PROBABILITIES.put(Material.NETHER_WART, 0.07);           // 稀有
        MATERIAL_PROBABILITIES.put(Material.BLAZE_POWDER, 0.03);          // 极其罕见
        MATERIAL_PROBABILITIES.put(Material.ROTTEN_FLESH, 0.15);          // 垃圾
        MATERIAL_PROBABILITIES.put(Material.POISONOUS_POTATO, 0.15);      // 垃圾
        MATERIAL_PROBABILITIES.put(Material.SNOWBALL, 0.15);              // 垃圾
        MATERIAL_PROBABILITIES.put(Material.STICK, 0.15);                 // 垃圾
    }

    private static final String WAREHOUSE_GUI_TITLE = ChatColor.LIGHT_PURPLE + "炼药材料库";

    public Alchemist(TeamManager teamManager, KitManager kitManager, GameManager gameManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        this.gameManager = gameManager;
        setUp();
    }

    @Override
    public String getName() {
        return "炼药师";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.LIGHT_PURPLE + "炼药师";
    }

    @Override
    public String getDescription() {
        return "炼药师是精通药水调配的大师，手持魔法炼药台与神秘材料库。魔法炼药台仅限本人使用且无法破坏，炼药速度是普通的两倍。材料库每90秒提供一次随机炼药材料，概率分布：32%常见材料，28%普通材料，15%不常见材料，7%稀有材料，3%极其罕见材料（第4阶段后解锁），15%垃圾材料。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.BREWING_STAND);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Alchemist",
                "",
                ChatColor.LIGHT_PURPLE + "你是精通药水调配的大师，手持魔法炼药台与神秘材料库。",
                "",
                ChatColor.AQUA + "魔法炼药台仅限本人使用且别人无法破坏",
                ChatColor.AQUA + "炼药速度是普通炼药台的两倍",
                ChatColor.AQUA + "材料库90秒冷却，提供随机炼药材料",
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

        // 魔法炼药台
        brewingStand = createSoulBoundItem(Material.BREWING_STAND, null, 1, 4, true);
        ItemMeta meta = brewingStand.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "魔法炼药台");
        brewingStand.setItemMeta(meta);
        kitItems.add(brewingStand);

        // 材料仓库（附魔书）
        materialBook = createSoulBoundItem(Material.ENCHANTED_BOOK, null, 1, 4, true);
        meta = materialBook.getItemMeta();
        meta.setDisplayName(MATERIAL_BOOK_NAME);
        materialBook.setItemMeta(meta);
        kitItems.add(materialBook.clone());

        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    private String getChineseName(Material material) {
        switch (material) {
            case FERMENTED_SPIDER_EYE: return "发酵蛛眼";
            case GLISTERING_MELON_SLICE: return "闪烁的西瓜片";
            case GOLDEN_CARROT: return "金胡萝卜";
            case SUGAR: return "糖";
            case SPIDER_EYE: return "蜘蛛眼";
            case MAGMA_CREAM: return "岩浆膏";
            case GUNPOWDER: return "火药";
            case GLOWSTONE_DUST: return "萤石粉";
            case GHAST_TEAR: return "恶魂之泪";
            case NETHER_WART: return "下界疣";
            case BLAZE_POWDER: return "烈焰粉";
            case ROTTEN_FLESH: return "腐肉";
            case POISONOUS_POTATO: return "毒马铃薯";
            case SNOWBALL: return "雪球";
            case STICK: return "木棍";
            default: return formatMaterialName(material);
        }
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    @Override
    public void onKitUnset(Player player) {
        // 玩家切换职业时移除魔法炼药台
        removePersonalBrewingStand(player);
    }

    // 放置魔法炼药台
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        //player.sendMessage("DEBUG: 记录炼药台位置 " + block.getLocation());
        if (block.getType() == Material.BREWING_STAND &&
                isThisKit(player) &&
                SoulBoundUtil.isSoulBoundItem(event.getItemInHand(), Material.BREWING_STAND)) {

            if(!personalBrewingStands.isEmpty()){
                removePersonalBrewingStand(player);
            }

            // 记录魔法炼药台位置
            personalBrewingStands.put(player.getUniqueId(), block.getLocation());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "已放置魔法炼药台！仅你能使用且队友无法破坏。");

            // 设置炼药台标题
            BrewingStand brewingStand = (BrewingStand) block.getState();
            brewingStand.setCustomName(ChatColor.LIGHT_PURPLE + player.getName() + "的魔法炼药台");
            brewingStand.update();

            // 显示紫色粒子效果
            showParticles(block.getLocation(), 5);

            startBrewingStandAccelerator();
        }
    }

    // 显示粒子效果
    private void showParticles(Location location, int duration) {
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count++ >= duration) {
                    cancel();
                    return;
                }

                // 在炼药台周围生成紫色粒子
                Location particleLoc = location.clone().add(0.5, 0.5, 0.5);
                particleLoc.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        particleLoc,
                        10,  // 粒子数量
                        0.3, // x偏移
                        0.3, // y偏移
                        0.3, // z偏移
                        0.1  // 速度
                );
            }
        }.runTaskTimer(kitManager.getPlugin(), 0, 10); // 每0.5秒执行一次
    }

    // 保护魔法炼药台不被破坏
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Location blockLocation = block.getLocation();

        if (block.getType() == Material.BREWING_STAND) {
            for (Location location : personalBrewingStands.values()) {
                if (isSameBlock(location, blockLocation)) { // 使用isSameBlock比较
                    UUID ownerId = getOwner(blockLocation);
                    if (ownerId != null) {
                        Player owner = Bukkit.getPlayer(ownerId);

                        event.setCancelled(true);
                        // 队友不能破坏
                        if (!player.getUniqueId().equals(ownerId) && teamManager.isSameTeam(player, owner)) {
                            player.sendMessage(ChatColor.RED + "这是你队友的魔法炼药台，无法破坏！");
                            return;
                        }

                        // 所有者或不同队伍可以破坏
                        // 如果是敌人破坏
                        if(!teamManager.isSameTeam(player, owner)){
                            personalBrewingStands.remove(ownerId);
                            player.sendMessage(ChatColor.RED + "你的魔法炼药台已被敌人摧毁！");
                            return;
                        }

                        // 如果是所有者
                        removePersonalBrewingStand(owner);

                        if(player.getUniqueId().equals(ownerId)) {
                            player.getInventory().addItem(brewingStand);
                        }
                    }
                    return;
                }
            }
        }
    }

    // 魔法炼药台使用权限
    @EventHandler(priority = EventPriority.HIGH)
    public void onBrewingStandInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.BREWING_STAND) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        Location blockLocation = block.getLocation();

        //player.sendMessage("DEBUG: 你点击了 " + block.getLocation());

        for (Location location : personalBrewingStands.values()) {
            if (isSameBlock(location, blockLocation)) { // 使用isSameBlock比较
                UUID ownerId = getOwner(blockLocation);
                if (ownerId != null) {
                    Player owner = Bukkit.getPlayer(ownerId);

                    // 调试信息
                    if (owner == null) {
                        player.sendMessage(ChatColor.RED + "错误：所有者不在线");
                        return;
                    }

                    // 所有者可以使用
                    if (player.getUniqueId().equals(ownerId)) {
                        return;
                    }

                    // 队友不能使用
                    if (teamManager.isSameTeam(player, owner)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "这是 " + owner.getName() + " 的魔法炼药台，无法使用！");
                        return;
                    }

                    // 其他队伍可以使用
                    player.sendMessage(ChatColor.YELLOW + "你正在使用敌对炼药师 " + owner.getName() + " 的炼药台");
                    return;
                }
            }
        }
    }

    private void startBrewingStandAccelerator() {
        if (brewingAcceleratorTask != null && !brewingAcceleratorTask.isCancelled()) return; // 已经有任务在跑

        brewingAcceleratorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (personalBrewingStands.isEmpty()) {
                    this.cancel();
                    brewingAcceleratorTask = null;
                    return;
                }
                for (Location loc : personalBrewingStands.values()) {
                    if (loc == null || loc.getBlock().getType() != Material.BREWING_STAND) continue;
                    BrewingStand stand = (BrewingStand) loc.getBlock().getState();
                    int brewingTime = stand.getBrewingTime();
                    if (brewingTime > 1) {
                        stand.setBrewingTime(Math.max(1, brewingTime - 1));
                        stand.update();
                        loc.getWorld().spawnParticle(
                                Particle.PORTAL,
                                loc.clone().add(0.5, 1, 0.5),
                                10,
                                0.2, 0.2, 0.2,
                                0.1
                        );
                    }
                }
            }
        }.runTaskTimer(kitManager.getPlugin(), 1L, 1L);
    }

    // 材料仓库使用
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item != null && isMaterialBook(item) && isThisKit(player)) {
                event.setCancelled(true);
                if (performSpecialAction(player)) {
                    updateMaterialBookItem(player);
                }
            }
        }
    }

    // 冷却检查
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

    private boolean isMaterialBook(ItemStack stack) {
        return stack != null && SoulBoundUtil.isSoulBoundItem(stack, Material.ENCHANTED_BOOK);
    }

    private void updateMaterialBookItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack heldItem = inv.getItemInMainHand();

        if (isMaterialBook(heldItem) && isThisKit(player)) {
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
                        player.sendMessage(ChatColor.GREEN + "你的材料库 " + ChatColor.YELLOW + "准备就绪！");
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
            if (isMaterialBook(item)) {
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
            player.sendMessage(ChatColor.GREEN + "材料库冷却中，剩余 " + ChatColor.YELLOW + secondsLeft + ChatColor.GREEN + " 秒");
            return false;
        }

        openMaterialWarehouse(player);
        startCooldown(player);
        return true;
    }

    // 打开材料仓库
    private void openMaterialWarehouse(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, WAREHOUSE_GUI_TITLE);
        Random random = new Random();
        double roll = random.nextDouble();

        // 根据概率分布选择材料
        Material selectedMaterial = null;
        double cumulative = 0.0;
        for (Map.Entry<Material, Double> entry : MATERIAL_PROBABILITIES.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                selectedMaterial = entry.getKey();
                break;
            }
        }

        // 确保选择了材料
        if (selectedMaterial == null) {
            selectedMaterial = Material.FERMENTED_SPIDER_EYE; // 默认材料
        }

        // 特殊规则：第4阶段前无法获得烈焰粉
        int currentPhase = gameManager.getCurrentPhase();;
        if (selectedMaterial == Material.BLAZE_POWDER && currentPhase < 4) {
            // 替换为稀有材料
            selectedMaterial = random.nextBoolean() ? Material.GHAST_TEAR : Material.NETHER_WART;
        }

        // 设置数量和显示名称
        ItemStack item = new ItemStack(selectedMaterial, getRandomQuantity());
        ItemMeta meta = item.getItemMeta();

        String rarity = getRarityName(selectedMaterial);
        String displayName = getChineseName(selectedMaterial) + " " + getRarityName(selectedMaterial);
        meta.setDisplayName(getRarityColor(rarity) + displayName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "来自炼药师的材料库");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "概率: " + ChatColor.GRAY +
                (int)(MATERIAL_PROBABILITIES.get(selectedMaterial) * 100) + "%");
        meta.setLore(lore);

        item.setItemMeta(meta);
        inv.setItem(13, item);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    private int getRandomQuantity() {
        Random random = new Random();
        if(random.nextDouble() < 0.25){
            return 2;
        }
        return 1;
    }

    private String getRarityName(Material material) {
        if (material == Material.FERMENTED_SPIDER_EYE) return "很常见";
        if (MATERIAL_PROBABILITIES.get(material) == 0.28) return "常见";
        if (material == Material.GLOWSTONE_DUST) return "不常见";
        if (material == Material.GHAST_TEAR || material == Material.NETHER_WART) return "稀有";
        if (material == Material.BLAZE_POWDER) return "极其罕见";
        return "垃圾";
    }

    private ChatColor getRarityColor(String rarity) {
        switch (rarity) {
            case "很常见": return ChatColor.BLUE;
            case "常见": return ChatColor.GREEN;
            case "不常见": return ChatColor.YELLOW;
            case "稀有": return ChatColor.LIGHT_PURPLE;
            case "极其罕见": return ChatColor.GOLD;
            default: return ChatColor.GRAY;
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.toString().toLowerCase();
        name = name.replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)) {
            updateMaterialBookItem(player);
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Alchemist;
    }

    private UUID getOwner(Location location) {
        for (Map.Entry<UUID, Location> entry : personalBrewingStands.entrySet()) {
            if (isSameBlock(entry.getValue(), location)) { // 使用isSameBlock比较
                return entry.getKey();
            }
        }
        return null;
    }


    private void removePersonalBrewingStand(Player player) {
        UUID playerId = player.getUniqueId();
        if (personalBrewingStands.containsKey(playerId)) {
            Location location = personalBrewingStands.get(playerId);
            if (isSameBlock(location, location.getBlock().getLocation())) {
                location.getBlock().setType(Material.AIR);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "你的魔法炼药台已被收回");
            }

            personalBrewingStands.remove(playerId);

            // 停止加速任务（如果没有炼药台了）
            if (personalBrewingStands.isEmpty() && brewingAcceleratorTask != null) {
                brewingAcceleratorTask.cancel();
                brewingAcceleratorTask = null;
            }
        }
    }

    // 在类内添加
    private boolean isSameBlock(Location a, Location b) {
        if (a == null || b == null) return false;
        boolean same = a.getWorld().getName().equals(b.getWorld().getName())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
//        if (!same) {
//            System.out.println("[Alchemist] 块不一致: " +
//                    "a=" + a.getWorld().getName() + "," + a.getBlockX() + "," + a.getBlockY() + "," + a.getBlockZ() +
//                    " b=" + b.getWorld().getName() + "," + b.getBlockX() + "," + b.getBlockY() + "," + b.getBlockZ());
//        }
        return same;
    }
}