package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Civilian extends Kit {
    private final TeamManager teamManager;
    private ItemStack stoneSword;
    private ItemStack stonePickaxe;
    private ItemStack stoneAxe;
    private ItemStack stoneShovel;
    private ItemStack stoneHoe;
    private ItemStack craftingTable;

    public Civilian(TeamManager teamManager) {
        this.teamManager = teamManager;
        setUp();
    }

    @Override
    public String getName() {
        return "平民";
    }

    @Override
    public String getDescription() {
        return "基础职业，配备全套皮革装备和石质工具";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "平民");
        meta.setLore(Arrays.asList(
                ChatColor.YELLOW + "你是后勤的保障。",
                "",
                ChatColor.AQUA + "穿梭于战场周边，为团队提供坚实后盾。",
                ChatColor.AQUA + "使用坚不可摧的石质工具，搭建起防御的壁垒，",
                ChatColor.AQUA + "并借助自带的工作台与箱子，保障物资的供应。",
                " " // 预留一行用于显示选择状态
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();
        String teamColor = teamManager.getPlayerTeamName(player);

        // 皮革护甲
        inv.setHelmet(SpecialLeatherArmor.createArmor(Material.LEATHER_HELMET, teamColor));
        inv.setChestplate(SpecialLeatherArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor));
        inv.setLeggings(SpecialLeatherArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor));
        inv.setBoots(SpecialLeatherArmor.createArmor(Material.LEATHER_BOOTS, teamColor));

        // 基础物品
        inv.addItem(stoneSword.clone());
        inv.addItem(stonePickaxe.clone());
        inv.addItem(stoneAxe.clone());
        inv.addItem(stoneShovel.clone());
        inv.addItem(stoneHoe.clone());
        inv.addItem(new ItemStack(Material.CRAFTING_TABLE));
        inv.addItem(new ItemStack(Material.CHEST));
        inv.addItem(CompassItem.createCompass());
    }

    private void setUp() {
        // 石剑
        stoneSword = createSoulBoundItem(Material.STONE_SWORD, null, 1,3,true);
        // 石镐
        stonePickaxe = createSoulBoundItem(Material.STONE_PICKAXE, null, 1,3,true);
        // 石斧
        stoneAxe = createSoulBoundItem(Material.STONE_AXE, null, 1,3,true);
        // 石铲
        stoneShovel = createSoulBoundItem(Material.STONE_SHOVEL, null, 1,1,true);
        // 石锄
        stoneHoe = createSoulBoundItem(Material.STONE_HOE, null, 1,1,true);
    }



}