package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Berserker extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    // 最大生命值常量
    private static final double MAX_HEALTH = 26.0; // 13 hearts
    private static final double INITIAL_HEALTH = 18.0; // 9 hearts

    // 职业物品
    private ItemStack stoneSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;

    public Berserker(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "狂战士";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.RED + "狂战士";
    }

    @Override
    public String getDescription() {
        return "初始只有9颗心，每次击杀增加1颗心（最多13颗心）。生命值低于42%时攻击造成额外伤害。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Berserker",
                "",
                ChatColor.YELLOW + "你是力量的化身。",
                "",
                ChatColor.AQUA + "初始生命值: " + ChatColor.RED + "9❤",
                ChatColor.AQUA + "每次击杀增加" + ChatColor.RED + "1❤",
                ChatColor.AQUA + "最大生命值: " + ChatColor.RED + "13❤",
                ChatColor.AQUA + "低生命值时增加伤害",
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
                SpecialArmor.createArmor(Material.CHAINMAIL_CHESTPLATE, teamColor, 3, false),
                SpecialArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_BOOTS, teamColor)
        );
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // 装备护甲
        List<ItemStack> armors = getKitArmors(player);
        inv.setHelmet(armors.get(0));
        inv.setChestplate(armors.get(1));
        inv.setLeggings(armors.get(2));
        inv.setBoots(armors.get(3));

        // 添加职业物品
        for (ItemStack item : kitItems) {
            inv.addItem(item);
        }

        // 设置初始生命值
        player.setHealth(INITIAL_HEALTH);
        player.setMaxHealth(INITIAL_HEALTH);
    }

    @Override
    public void onKitUnset(Player player) {
        // 重置生命值
        player.setMaxHealth(20.0);
        if (player.getHealth() > 20.0) {
            player.setHealth(20.0);
        }
    }

    private void setUp() {
        // 石剑
        stoneSword = createSoulBoundItem(Material.STONE_SWORD, null, 1, 1, false);
        kitItems.add(stoneSword.clone());

        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());

        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());

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

    // 低生命值时增加伤害
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() != EntityType.PLAYER ||
                event.getDamager().getType() != EntityType.PLAYER) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        if (!isThisKit(attacker)) {
            return;
        }

        // 检查生命值是否低于42%
        if ((attacker.getHealth() / attacker.getMaxHealth()) <= 0.42) {
            event.setDamage(event.getDamage() + 1.0);
        }
    }

    // 击杀玩家增加生命值
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || !isThisKit(killer)) {
            return;
        }

        // 检查是否达到最大生命值
        if (killer.getMaxHealth() < MAX_HEALTH) {
            double newHealth = Math.min(killer.getMaxHealth() + 2.0, MAX_HEALTH);
            killer.setMaxHealth(newHealth);

            // 恢复玩家生命值
            if (killer.getHealth() < newHealth) {
                killer.setHealth(newHealth);
            }
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Berserker;
    }
}