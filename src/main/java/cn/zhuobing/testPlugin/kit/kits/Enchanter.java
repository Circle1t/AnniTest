package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Enchanter extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private final List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack goldSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack expBottle;

    public Enchanter(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "附魔师";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.LIGHT_PURPLE + "附魔师";
    }

    @Override
    public String getDescription() {
        return "掌控经验之力，强化装备的大师";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "附魔师");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Enchanter",
                "",
                ChatColor.YELLOW + "你是经验能量的掌控者。",
                "",
                ChatColor.AQUA + "采集资源时获得额外经验。",
                ChatColor.AQUA + "可用经验快速强化装备属性。",
                ChatColor.AQUA + "有机率从矿石中获取经验瓶。",
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
        goldSword = SoulBoundUtil.createSoulBoundItem(Material.WOODEN_SWORD, null, 1, 1, false);
        kitItems.add(goldSword.clone());

        // 木镐
        woodPickaxe = SoulBoundUtil.createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());

        // 木斧
        woodAxe = SoulBoundUtil.createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());

        // 初始经验瓶
        expBottle = SoulBoundUtil.createSoulBoundItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.LIGHT_PURPLE + "经验之瓶", 3, 3, false);
        kitItems.add(expBottle.clone());

        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    //请到OreManager接口查看功能实现
    //2倍经验
    //随机经验瓶
}