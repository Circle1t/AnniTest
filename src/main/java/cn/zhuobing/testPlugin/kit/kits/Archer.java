package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Archer extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack woodSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack woodShovel;
    private ItemStack bow;
    private ItemStack arrows;

    public Archer(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "弓箭手";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.DARK_PURPLE + "弓箭手";
    }

    @Override
    public String getDescription() {
        return "使用弓远程攻击敌人，可额外造成 +1 点伤害，能用燧石和木棍制作箭矢的职业";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.BOW);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Archer",
                "",
                ChatColor.YELLOW + "你是箭雨的主宰。",
                "",
                ChatColor.AQUA + "从远处消灭你的敌人，",
                ChatColor.AQUA + "使用弓可额外造成 +1 点伤害，",
                ChatColor.AQUA + "还能用燧石和木棍制作箭矢。",
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
        // 创建合成配方
        ShapelessRecipe recipe = new ShapelessRecipe(new ItemStack(Material.ARROW, 3)).addIngredient(Material.FLINT).addIngredient(Material.STICK);
        Bukkit.addRecipe(recipe);

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
        // 弓，带有力量 1 附魔
        bow = createSoulBoundItem(Material.BOW, null, 1, 3, false);
        bow.addUnsafeEnchantment(Enchantment.POWER, 1);
        kitItems.add(bow.clone());
        // 箭矢，数量设置为 16
        arrows = createSoulBoundItem(Material.ARROW, null, 16, 3, true);
        kitItems.add(arrows.clone());

        // 治疗药水
        ItemStack soulBoundHealthPotion = createSoulBoundItem(Material.POTION, null, 1, 3, true);
        PotionMeta potionMeta = (PotionMeta) soulBoundHealthPotion.getItemMeta();
        // 设置药水效果为立即治疗 I
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0), true); // 1 tick, 等级 0（I 级）
        // 设置药水名称和描述
        potionMeta.setDisplayName(ChatColor.RESET + ChatColor.LIGHT_PURPLE.toString() + "治疗药水");
        soulBoundHealthPotion.setItemMeta(potionMeta);
        // 复制药水元数据
        kitItems.add(soulBoundHealthPotion);

        // 指南针
        kitItems.add(CompassItem.createCompass());
    }


    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 阻止非弓箭手使用弓箭手的配方制作箭矢
    @EventHandler(priority = EventPriority.HIGHEST)
    public void arrowCraftingStopper(CraftItemEvent event) {
        if (event.getRecipe().getResult().getType() == Material.ARROW && event.getRecipe().getResult().getAmount() == 3) {
            Player player = (Player) event.getWhoClicked();
            if (!isThisKit(player)) {
                event.setCancelled(true);
            }
        }
    }

    // 增加 +1 点箭矢伤害
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void damageListener(final EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.ARROW) {
            ProjectileSource s = ((Projectile) event.getDamager()).getShooter();
            if (s instanceof Player) {
                Player shooter = (Player) s;
                if (isThisKit(shooter)) {
                    event.setDamage(event.getDamage() + 1);
                }
            }
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Archer;
    }
}