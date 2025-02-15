package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
import cn.zhuobing.testPlugin.enchant.SoulBoundLevel;
import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.function.Predicate;

public class Civilian extends Kit {
    private TeamManager teamManager;
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
                ChatColor.GRAY + "职业特性：",
                ChatColor.WHITE + "• 全套灵魂绑定皮革护甲",
                ChatColor.WHITE + "• 全套坚不可摧的石质工具",
                ChatColor.WHITE + "• 自带工作台",
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
        inv.addItem(craftingTable.clone());
        inv.addItem(CompassItem.createCompass());
    }

    private void setUp() {
        // 石剑
        stoneSword = createSoulBoundItem(Material.STONE_SWORD, ChatColor.RESET + "石剑", 1);
        // 石镐
        stonePickaxe = createSoulBoundItem(Material.STONE_PICKAXE, ChatColor.RESET + "石镐", 1);
        // 石斧
        stoneAxe = createSoulBoundItem(Material.STONE_AXE, ChatColor.RESET + "石斧", 1);
        // 石铲
        stoneShovel = createSoulBoundItem(Material.STONE_SHOVEL, ChatColor.RESET + "石铲", 1);
        // 石锄
        stoneHoe = createSoulBoundItem(Material.STONE_HOE, ChatColor.RESET + "石锄", 1);
        // 工作台
        craftingTable = createSoulBoundItem(Material.CRAFTING_TABLE, ChatColor.RESET + "工作台", 1);
    }

    @Override
    public ItemStack createSoulBoundItem(Material material, String displayName, int soulBoundLevel) {
        SoulBoundLevel level = SoulBoundLevel.fromInt(soulBoundLevel);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "平民基础物品",
                "", // 隔一行
                ChatColor.GOLD + "灵魂绑定 " + level.getDisplay()
        ));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);

        // 注册灵魂绑定
        Predicate<ItemStack> isItem = stack -> SoulBoundUtil.isSoulBoundItem(stack, displayName,material);
        SoulBoundListener.registerSoulBoundItem(soulBoundLevel, isItem);
        return item;
    }


}