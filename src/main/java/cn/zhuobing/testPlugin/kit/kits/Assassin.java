package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.CooldownUtil;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Assassin extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private static final int LEAP_COOLDOWN_MS = 40000; // 40 秒冷却
    private final List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack woodSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack feather;

    private static final String FEATHER_ITEM_NAME = ChatColor.YELLOW + "刺客羽毛" + ChatColor.GREEN + " 充能完成";
    private static final String FEATHER_COOLDOWN_PREFIX = ChatColor.RED + "充能中 ";
    private static final String FEATHER_COOLDOWN_SUFFIX = " 秒";

    private final CooldownUtil leapCooldown;

    public Assassin(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        this.leapCooldown = new CooldownUtil(kitManager.getPlugin(), LEAP_COOLDOWN_MS);
        setUp();
    }

    @Override
    public String getName() {
        return "刺客";
    }

    @Override
    public String getNameWithColor(){
        return ChatColor.GOLD + "刺客";
    }

    @Override
    public String getDescription() {
        return "隐身突袭，灵活跳跃的职业";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Assassin",
                "",
                ChatColor.YELLOW + "你是暗影中的猎手。",
                "",
                ChatColor.AQUA + "使用技能后隐身并跳跃，",
                ChatColor.AQUA + "获得速度与急迫效果，",
                ChatColor.AQUA + "攻击或受到伤害时解除隐身。",
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

        // 初始化羽毛物品名称
        updateFeatherItem(player);
    }

    private void setUp() {
        // 金剑
        woodSword = SoulBoundUtil.createSoulBoundItem(Material.GOLDEN_SWORD, null, 1, 1, false);
        kitItems.add(woodSword.clone());
        // 木镐
        woodPickaxe = SoulBoundUtil.createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());
        // 木斧
        woodAxe = SoulBoundUtil.createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());
        // 羽毛
        feather = SoulBoundUtil.createSoulBoundItem(Material.FEATHER, null, 1, 4, false);
        ItemMeta itemMeta = feather.getItemMeta();
        itemMeta.setDisplayName(FEATHER_ITEM_NAME);
        feather.setItemMeta(itemMeta);
        kitItems.add(feather.clone());
        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.PLAYER) {
            Player player = (Player) event.getDamager();
            if (isThisKit(player) && player.hasMetadata("AssassinLeap")) {
                endLeap(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isThisKit(player)) {
                if (event.getCause() == DamageCause.FALL) {
                    event.setCancelled(true);
                } else if (player.hasMetadata("AssassinLeap")) {
                    endLeap(player);
                }
            }
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (SoulBoundUtil.isSoulBoundItem(item,Material.FEATHER) && isThisKit(player)) {
                if (performSpecialAction(player)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    public boolean performSpecialAction(Player player) {
        if (leapCooldown.isOnCooldown(player)) {
            long secondsLeft = leapCooldown.getSecondsLeft(player);
            player.sendMessage(ChatColor.GREEN + "技能冷却中，剩余 " + ChatColor.YELLOW + secondsLeft + ChatColor.GREEN + " 秒");
            return false;
        }

        leapCooldown.startCooldown(player, LEAP_COOLDOWN_MS, this::onLeapCooldownReady, (p, sec) -> updateFeatherItem(p));

        // 保存当前护甲
        player.setMetadata("AssassinArmor", new org.bukkit.metadata.FixedMetadataValue(kitManager.getPlugin(), player.getInventory().getArmorContents()));

        // 移除护甲并给予效果
        player.getInventory().setArmorContents(null);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 160, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 160, 1));

        // 跳跃
        player.setVelocity(player.getLocation().getDirection().setY(1).multiply(1));
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);

        // 设置状态
        player.setMetadata("AssassinLeap", new org.bukkit.metadata.FixedMetadataValue(kitManager.getPlugin(), true));

        new BukkitRunnable() {
            @Override
            public void run() {
                endLeap(player);
            }
        }.runTaskLater(kitManager.getPlugin(), 160);

        updateFeatherItem(player);
        return true;
    }

    private void onLeapCooldownReady(Player player) {
        if (isThisKit(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "你的技能 " + ChatColor.YELLOW + "充能完毕");
            updateSoulBoundFeatherInInventory(player);
        }
    }

    private void endLeap(Player player) {
        if (player.hasMetadata("AssassinLeap")) {
            // 恢复护甲
            if (player.hasMetadata("AssassinArmor")) {
                ItemStack[] armor = (ItemStack[]) player.getMetadata("AssassinArmor").get(0).value();
                player.getInventory().setArmorContents(armor);
            }

            // 移除效果
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.HASTE);

            // 清除状态
            player.removeMetadata("AssassinLeap", kitManager.getPlugin());
            player.removeMetadata("AssassinArmor", kitManager.getPlugin());
        }
    }

    private void updateFeatherItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack heldItem = inv.getItemInMainHand();
        if (SoulBoundUtil.isSoulBoundItem(heldItem, Material.FEATHER) && isThisKit(player)) {
            ItemMeta meta = heldItem.getItemMeta();
            long secondsLeft = leapCooldown.getSecondsLeft(player);
            if (leapCooldown.isOnCooldown(player)) {
                meta.setDisplayName(FEATHER_COOLDOWN_PREFIX + secondsLeft + FEATHER_COOLDOWN_SUFFIX);
            } else {
                meta.setDisplayName(FEATHER_ITEM_NAME);
            }
            heldItem.setItemMeta(meta);
            player.updateInventory();
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Assassin;
    }

    private void updateSoulBoundFeatherInInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (SoulBoundUtil.isSoulBoundItem(item, Material.FEATHER)) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(FEATHER_ITEM_NAME);
                item.setItemMeta(meta);
            }
        }
        player.updateInventory(); // 强制更新客户端显示
    }
}