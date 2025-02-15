package cn.zhuobing.testPlugin.kit.civilian.items;

import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.function.Predicate;

public class CivilianItems {

    /**
     * 给玩家发放平民职业套装
     * @param player 玩家对象
     */
    public static void giveCivilianKit(Player player, String teamColor) {
        PlayerInventory inv = player.getInventory();

        // 工具类物品
        ItemStack[] tools = {
                createSoulBoundTool(Material.STONE_SWORD),
                createSoulBoundTool(Material.STONE_PICKAXE),
                createSoulBoundTool(Material.STONE_AXE),
                createSoulBoundTool(Material.STONE_SHOVEL),
                createSoulBoundTool(Material.STONE_HOE),
                createCraftingTable()
        };

        inv.addItem(tools);

        // 皮革护甲
        inv.setHelmet(SpecialLeatherArmor.createArmor(Material.LEATHER_HELMET, teamColor));
        inv.setChestplate(SpecialLeatherArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor));
        inv.setLeggings(SpecialLeatherArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor));
        inv.setBoots(SpecialLeatherArmor.createArmor(Material.LEATHER_BOOTS, teamColor));
    }

    /**
     * 创建灵魂绑定的工具物品
     * @param material 物品材质
     * @return 工具物品
     */
    private static ItemStack createSoulBoundTool(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // 设置物品的显示名称，使用 ChatColor.RESET 重置样式
        meta.setDisplayName(ChatColor.RESET + getToolName(material));
        // 设置物品的描述信息
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "平民专用工具",
                ChatColor.DARK_GRAY + "灵魂绑定物品",
                "", // 隔一行
                ChatColor.GOLD + "灵魂绑定 I"
        ));

        // 设置物品不可破坏
        meta.setUnbreakable(true);
        // 隐藏不可破坏的标签
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        // 将元数据应用到物品上
        item.setItemMeta(meta);

        // 注册灵魂绑定I级
        Predicate<ItemStack> isTool = stack -> isTool(stack, material);
        SoulBoundListener.registerSoulBoundItem(1, isTool);

        return item;
    }

    /**
     * 创建工作台物品
     * @return 工作台物品
     */
    private static ItemStack createCraftingTable() {
        ItemStack item = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = item.getItemMeta();

        // 设置物品的显示名称，使用 ChatColor.RESET 重置样式
        meta.setDisplayName(ChatColor.RESET + "工作台");
        // 设置物品的描述信息
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "平民工作台"
        ));

        // 将元数据应用到物品上
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 根据材质获取工具名称
     * @param material 物品材质
     * @return 工具名称
     */
    private static String getToolName(Material material) {
        switch (material) {
            case STONE_SWORD:
                return "石剑";
            case STONE_PICKAXE:
                return "石镐";
            case STONE_AXE:
                return "石斧";
            case STONE_SHOVEL:
                return "石铲";
            case STONE_HOE:
                return "石锄";
            default:
                return "";
        }
    }

    /**
     * 判断物品是否为指定材质的工具物品
     * @param item 要判断的物品
     * @param material 物品材质
     * @return 如果是指定材质的工具物品则返回 true，否则返回 false
     */
    private static boolean isTool(ItemStack item, Material material) {
        if (item == null) {
            return false;
        }
        if (item.getType() != material) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        return displayName.equals(getToolName(material));
    }
}