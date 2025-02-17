package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Miner extends Kit{
    private final TeamManager teamManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack woodSword;
    private ItemStack stonePickaxe;
    private ItemStack woodAxe;

    public Miner(TeamManager teamManager) {
        this.teamManager = teamManager;
        setUp();
    }

    @Override
    public String getName() {
        return "矿工";
    }

    @Override
    public String getDescription() {
        return "挖掘珍贵资源，为团队和自己提供装备，起始拥有效率镐、4个煤炭和一个高炉，能更快获取矿物。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.STONE_PICKAXE);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "矿工");
        meta.setLore(Arrays.asList(
                ChatColor.YELLOW + "你是团队的基石。",
                "",
                ChatColor.AQUA + "挖掘珍贵资源，",
                ChatColor.AQUA + "为团队和自己武装起来，",
                ChatColor.AQUA + "在战场上迅速出击，势不可挡！",
                "",
                ChatColor.AQUA + "起始拥有一把效率石镐、4个煤炭和一个高炉，",
                ChatColor.AQUA + "让你更快获取矿物。",
                ""
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
        // 木剑（1.20.6 保持 WOODEN_SWORD）
        woodSword = createSoulBoundItem(Material.WOODEN_SWORD, null, 1, 1, false);
        kitItems.add(woodSword.clone());

        // 效率石镐（1.20.6 附魔系统变更）
        stonePickaxe = createSoulBoundItem(Material.STONE_PICKAXE, null, 1, 3, false);
        ItemMeta pickMeta = stonePickaxe.getItemMeta();
        pickMeta.addEnchant(Enchantment.EFFICIENCY, 1, true); // DIG_SPEED 已重命名为 EFFICIENCY
        stonePickaxe.setItemMeta(pickMeta);
        kitItems.add(stonePickaxe.clone());

        // 木斧（保持 WOODEN_AXE）
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());

        // 高炉
        kitItems.add(new ItemStack(Material.BLAST_FURNACE, 1));
        // 煤炭
        kitItems.add(new ItemStack(Material.COAL, 4));
        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 此类逻辑实现需要到OreManager giveRewards中查看
}