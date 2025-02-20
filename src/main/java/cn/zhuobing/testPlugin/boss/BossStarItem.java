package cn.zhuobing.testPlugin.boss;

import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class BossStarItem implements Listener {
    // 字符常量提取到头部
    private static final String ITEM_IDENTIFIER = ChatColor.AQUA + "Boss之星";
    private static final String GUI_TITLE = "Boss物品选择";
    private static final String RIGHT_CLICK_OPEN_MSG = ChatColor.GRAY + "右键打开Boss物品选择界面";
    private static final String SPECIAL_ITEM_MSG = ChatColor.DARK_GRAY + "特殊物品";
    private static final String CHAINED_HELMET_NAME = ChatColor.BLUE + "烈焰守护头盔";
    private static final String CHAINED_HELMET_LORE = ChatColor.GRAY + "穿戴后会获得无限防火效果";
    private static final String CHAINED_CHESTPLATE_NAME = ChatColor.BLUE + "生命守护胸甲";
    private static final String CHAINED_CHESTPLATE_LORE = ChatColor.GRAY + "攻击敌人后获得生命恢复效果";
    private static final String CHAINED_LEGGINGS_NAME = ChatColor.BLUE + "迅捷守护护腿";
    private static final String CHAINED_LEGGINGS_LORE = ChatColor.GRAY + "攻击敌人后获得速度效果";
    private static final String CHAINED_BOOTS_NAME = ChatColor.BLUE + "轻盈守护靴子";
    private static final String CHAINED_BOOTS_LORE = ChatColor.GRAY + "免疫摔落伤害";
    private static final String EFFICIENCY_V_PICKAXE_NAME = ChatColor.YELLOW + "高效金镐";
    private static final String FORTUNE_III_PICKAXE_NAME = ChatColor.YELLOW + "幸运金镐";
    private static final String FLAME_BOW_NAME = ChatColor.RED + "逐日之弓";
    private static final String LEVITATION_BOW_NAME = ChatColor.AQUA + "漂浮之弓";
    private static final String KNOCKBACK_BOW_NAME = ChatColor.GREEN + "冲击之弓";
    private static final String BURNING_SWORD_NAME = ChatColor.RED + "灼热之刃";
    private static final String POISONOUS_SWORD_NAME = ChatColor.GREEN + "剧毒之刃";
    private static final String SPLASH_STRENGTH_POTION_NAME = ChatColor.RED + "力量药水";
    private static final String SPLASH_SPEED_POTION_NAME = ChatColor.YELLOW + "速度药水";
    private static final String SPLASH_REGENERATION_POTION_NAME = ChatColor.GREEN + "再生药水";
    private static final String SPLASH_INVISIBILITY_POTION_NAME = ChatColor.BLUE + "隐身药水";
    private static final String DRINKABLE_HASTE_POTION_NAME = ChatColor.GOLD + "急迫药水";

    private final Plugin plugin;
    private final Random random = new Random();

    public BossStarItem(Plugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createBossStar() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ITEM_IDENTIFIER);
        meta.setLore(Arrays.asList(RIGHT_CLICK_OPEN_MSG, SPECIAL_ITEM_MSG));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBossStar(ItemStack item) {
        return item != null && item.getType() == Material.NETHER_STAR &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals(ChatColor.stripColor(ITEM_IDENTIFIER));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if ((event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
                event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) &&
                isBossStar(event.getItem())) {
            openBossItemSelectorGUI(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                ItemStack selectedItem = event.getCurrentItem().clone();

                // 检查玩家是否有足够的Boss之星
                ItemStack bossStar = player.getInventory().getItemInMainHand();
                if (bossStar == null || !isBossStar(bossStar) || bossStar.getAmount() < 1) {
                    player.sendMessage(ChatColor.RED + "你需要至少一个Boss之星来兑换物品！");
                    return;
                }

                // 尝试将物品放入背包
                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(selectedItem);
                if (!remaining.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "背包已满，无法获取物品！");
                    return;
                }

                // 减少Boss之星数量
                if (bossStar.getAmount() > 1) {
                    bossStar.setAmount(bossStar.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }

                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "成功兑换物品: " + selectedItem.getItemMeta().getDisplayName());
            }
        }
    }


    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            ItemStack chestplate = player.getInventory().getChestplate();
            ItemStack leggings = player.getInventory().getLeggings();

            if (chestplate != null && SoulBoundUtil.isSoulBoundItem(chestplate, Material.CHAINMAIL_CHESTPLATE)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
            }

            if (leggings != null && SoulBoundUtil.isSoulBoundItem(leggings, Material.CHAINMAIL_LEGGINGS)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0));
            }

            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon != null && SoulBoundUtil.isSoulBoundItem(weapon, Material.DIAMOND_SWORD)
                    && weapon.getItemMeta().getDisplayName().equals(POISONOUS_SWORD_NAME)
                    && random.nextDouble() < 0.3) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            ItemStack bow = player.getInventory().getItemInMainHand();

            if (bow != null && SoulBoundUtil.isSoulBoundItem(bow, Material.BOW)
                    && bow.getItemMeta().getDisplayName().equals(LEVITATION_BOW_NAME)
                    && random.nextDouble() < 0.3) {
                if (event.getHitEntity() != null) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 0));
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            ItemStack boots = player.getInventory().getBoots();

            if (boots != null && SoulBoundUtil.isSoulBoundItem(boots, Material.CHAINMAIL_BOOTS)
                    && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
            }
        }
    }


    private void openBossItemSelectorGUI(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 54, GUI_TITLE);

        // 盔甲类
        inv.setItem(0, createChainedHelmet());
        inv.setItem(1, createChainedChestplate());
        inv.setItem(2, createChainedLeggings());
        inv.setItem(3, createChainedBoots());

        // 工具类
        inv.setItem(9, createEfficiencyVGoldPickaxe());
        inv.setItem(10, createFortuneIIIGoldPickaxe());

        // 弓箭类
        inv.setItem(18, createFlameBow());
        inv.setItem(19, createLevitationBow());
        inv.setItem(20, createKnockbackBow());

        // 剑类
        inv.setItem(27, createBurningSword());
        inv.setItem(28, createPoisonousSword());

        // 药水类
        inv.setItem(36, createSplashStrengthPotion());
        inv.setItem(37, createSplashSpeedPotion());
        inv.setItem(38, createSplashRegenerationPotion());
        inv.setItem(39, createSplashInvisibilityPotion());
        inv.setItem(40, createDrinkableHastePotion());

        player.openInventory(inv);
    }

    private ItemStack createChainedHelmet() {
        ItemStack helmet = SoulBoundUtil.createSoulBoundItem(Material.CHAINMAIL_HELMET,
                CHAINED_HELMET_NAME, 1, 2, true);

        ItemMeta meta = helmet.getItemMeta();
        meta.setLore(Arrays.asList(
                CHAINED_HELMET_LORE,
                ChatColor.GOLD + "灵魂绑定 II"
        ));
        meta.addEnchant(Enchantment.PROTECTION, 3, true);

        helmet.setItemMeta(meta);

        return helmet;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            checkHelmetUpdate((Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            checkHelmetUpdate((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClickHelmet(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();

            // 立即更新效果
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkHelmetUpdate(player);
                }
            }.runTaskLater(plugin, 1L); // 延迟1tick确保物品已更新
        }
    }

    @EventHandler
    public void onRightClickHelmet(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // 立即更新效果
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkHelmetUpdate(event.getPlayer());
                }
            }.runTaskLater(plugin, 1L); // 延迟1tick确保物品已更新
        }
    }

    private void checkHelmetUpdate(Player player) {
        ItemStack newHelmet = player.getInventory().getHelmet();
        boolean hasHelmet = isChainedHelmet(newHelmet);

        // 获取当前防火效果状态
        boolean hasEffect = player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE);

        // 需要更新效果的三种情况：
        // 1. 当前戴着头盔但没有效果
        // 2. 当前没戴头盔但有效果
        // 3. 更换了不同头盔
        if (hasHelmet && !hasEffect) {
            addFireResistanceEffect(player);
        } else if (!hasHelmet && hasEffect) {
            removeFireResistanceEffect(player);
        }
    }
    // 判断是否为烈焰守护头盔
    private boolean isChainedHelmet(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(CHAINED_HELMET_NAME);
    }


    // 添加防火效果
    private void addFireResistanceEffect(Player player) {
        PotionEffect fireRes = new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE,
                99999, // 永久持续时间
                0,
                false, // 无环境粒子
                true   // 显示图标
        );
        player.addPotionEffect(fireRes, true); // 强制覆盖旧效果
    }

    // 移除防火效果
    private void removeFireResistanceEffect(Player player) {
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }



    private ItemStack createChainedChestplate() {
        ItemStack chestplate = SoulBoundUtil.createSoulBoundItem(Material.CHAINMAIL_CHESTPLATE, CHAINED_CHESTPLATE_NAME, 1, 2, true);
        ItemMeta meta = chestplate.getItemMeta();
        meta.setLore(Arrays.asList(CHAINED_CHESTPLATE_LORE, ChatColor.GOLD + "灵魂绑定 II"));
        meta.addEnchant(Enchantment.PROTECTION, 3, true);
        chestplate.setItemMeta(meta);
        return chestplate;
    }

    private ItemStack createChainedLeggings() {
        ItemStack leggings = SoulBoundUtil.createSoulBoundItem(Material.CHAINMAIL_LEGGINGS, CHAINED_LEGGINGS_NAME, 1, 2, true);
        ItemMeta meta = leggings.getItemMeta();
        meta.setLore(Arrays.asList(CHAINED_LEGGINGS_LORE, ChatColor.GOLD + "灵魂绑定 II"));
        meta.addEnchant(Enchantment.PROTECTION, 3, true);
        leggings.setItemMeta(meta);
        return leggings;
    }

    private ItemStack createChainedBoots() {
        ItemStack boots = SoulBoundUtil.createSoulBoundItem(Material.CHAINMAIL_BOOTS, CHAINED_BOOTS_NAME, 1, 2, true);
        ItemMeta meta = boots.getItemMeta();
        meta.setLore(Arrays.asList(CHAINED_BOOTS_LORE, ChatColor.GOLD + "灵魂绑定 II"));
        meta.addEnchant(Enchantment.PROTECTION, 3, true);
        meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
        boots.setItemMeta(meta);
        return boots;
    }

    private ItemStack createEfficiencyVGoldPickaxe() {
        ItemStack pickaxe = SoulBoundUtil.createSoulBoundItem(Material.GOLDEN_PICKAXE, EFFICIENCY_V_PICKAXE_NAME, 1, 1, true);
        pickaxe.addUnsafeEnchantment(Enchantment.EFFICIENCY, 5);
        return pickaxe;
    }

    private ItemStack createFortuneIIIGoldPickaxe() {
        ItemStack pickaxe = SoulBoundUtil.createSoulBoundItem(Material.GOLDEN_PICKAXE, FORTUNE_III_PICKAXE_NAME, 1, 1, true);
        pickaxe.addUnsafeEnchantment(Enchantment.FORTUNE, 3);
        return pickaxe;
    }

    private ItemStack createFlameBow() {
        ItemStack bow = SoulBoundUtil.createSoulBoundItem(Material.BOW, FLAME_BOW_NAME, 1, 1, false);
        bow.addUnsafeEnchantment(Enchantment.FLAME, 2);
        bow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
        return bow;
    }

    private ItemStack createLevitationBow() {
        ItemStack bow = SoulBoundUtil.createSoulBoundItem(Material.BOW, LEVITATION_BOW_NAME, 1, 1, false);
        bow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
        return bow;
    }

    private ItemStack createKnockbackBow() {
        ItemStack bow = SoulBoundUtil.createSoulBoundItem(Material.BOW, KNOCKBACK_BOW_NAME, 1, 1, false);
        bow.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
        bow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
        return bow;
    }

    private ItemStack createBurningSword() {
        ItemStack sword = SoulBoundUtil.createSoulBoundItem(Material.DIAMOND_SWORD, BURNING_SWORD_NAME, 1, 1, true);
        sword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 3);
        return sword;
    }

    private ItemStack createPoisonousSword() {
        ItemStack sword = SoulBoundUtil.createSoulBoundItem(Material.DIAMOND_SWORD, POISONOUS_SWORD_NAME, 1, 1, true);
        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
        return sword;
    }

    private ItemStack createSplashStrengthPotion() {
        ItemStack potion = SoulBoundUtil.createSoulBoundItem(Material.SPLASH_POTION, SPLASH_STRENGTH_POTION_NAME, 1, 1, false);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setDisplayName(SPLASH_STRENGTH_POTION_NAME);

        // 1.20推荐方式：直接添加效果并设置颜色
        meta.addCustomEffect(new PotionEffect(
                PotionEffectType.STRENGTH, // 对应原版力量效果
                180 * 20,  // 持续时间（180秒）
                0,         // 效果等级（0对应I级）
                true,      // 环境粒子效果
                true,      // 显示图标
                true       // 显示粒子
        ), true);

        // 设置药水颜色（红色）
        meta.setColor(Color.RED);
        potion.setItemMeta(meta);
        return potion;
    }

    private ItemStack createSplashSpeedPotion() {
        ItemStack potion = SoulBoundUtil.createSoulBoundItem(Material.SPLASH_POTION, SPLASH_SPEED_POTION_NAME, 1, 1, false);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setDisplayName(SPLASH_SPEED_POTION_NAME);

        meta.addCustomEffect(new PotionEffect(
                PotionEffectType.SPEED,
                180 * 20,
                0,  // 速度I级
                true, true, true
        ), true);

        meta.setColor(Color.AQUA);
        potion.setItemMeta(meta);
        return potion;
    }

    private ItemStack createSplashRegenerationPotion() {
        ItemStack potion = SoulBoundUtil.createSoulBoundItem(Material.SPLASH_POTION, SPLASH_SPEED_POTION_NAME, 1, 1, false);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setDisplayName(SPLASH_REGENERATION_POTION_NAME);

        meta.addCustomEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                90 * 20,
                0,
                true, true, true
        ), true);

        meta.setColor(Color.FUCHSIA);
        potion.setItemMeta(meta);
        return potion;
    }

    private ItemStack createSplashInvisibilityPotion() {
        ItemStack potion = SoulBoundUtil.createSoulBoundItem(Material.SPLASH_POTION, SPLASH_SPEED_POTION_NAME, 1, 1, false);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setDisplayName(SPLASH_INVISIBILITY_POTION_NAME);

        meta.addCustomEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                480 * 20,
                0,
                true, true, true
        ), true);

        meta.setColor(Color.GRAY);
        potion.setItemMeta(meta);
        return potion;
    }

    private ItemStack createDrinkableHastePotion() {
        ItemStack potion = SoulBoundUtil.createSoulBoundItem(Material.POTION, DRINKABLE_HASTE_POTION_NAME, 1, 1, false);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setDisplayName(DRINKABLE_HASTE_POTION_NAME);

        // 设置黄色药水外观
        meta.setColor(Color.YELLOW);
        potion.setItemMeta(meta);
        return potion;
    }

    @EventHandler
    public void onConsumeLoyaltyItem(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().equals(DRINKABLE_HASTE_POTION_NAME)) {
            // 添加急迫I效果，持续10秒
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 10 * 20, 0));
        }
    }
}