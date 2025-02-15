package cn.zhuobing.testPlugin.specialitem.items;

import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

// 特殊皮革装甲类，用于创建和判断特殊皮革装甲
public class SpecialLeatherArmor {

    /**
     * 创建特殊皮革装甲的方法
     * @param armorType 装甲类型（头盔、胸甲、护腿、靴子）
     * @param teamColor 队伍颜色（用于设置皮革颜色）
     * @return 特殊皮革装甲物品
     */
    public static ItemStack createArmor(Material armorType, String teamColor) {
        // 检查传入的 armorType 是否为皮革装甲
        if (!isLeatherArmor(armorType)) {
            throw new IllegalArgumentException("提供的物品类型不是皮革装甲！");
        }

        // 创建一个皮革装甲物品
        ItemStack item = new ItemStack(armorType);
        // 获取物品的元数据
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();

        // 设置物品的显示名称，使用 ChatColor.RESET 重置样式
        meta.setDisplayName(ChatColor.RESET + getArmorTypeName(armorType));
        // 设置物品的描述信息
        List<String> lore = Arrays.asList(
                ChatColor.DARK_GRAY + "特殊皮革装甲",
                "", // 隔一行
                ChatColor.GOLD + "灵魂绑定 I"
        );
        meta.setLore(lore);

        // 设置皮革颜色
        meta.setColor(getColorFromTeam(teamColor));

        // 将元数据应用到物品上
        item.setItemMeta(meta);

        // 注册灵魂绑定I级
        Predicate<ItemStack> isSpecialArmor = SpecialLeatherArmor::isSpecialArmor;
        SoulBoundListener.registerSoulBoundItem(1, isSpecialArmor);

        return item;
    }

    /**
     * 判断物品是否为特殊皮革装甲
     * @param item 要判断的物品
     * @return 如果物品是特殊皮革装甲则返回 true，否则返回 false
     */
    public static boolean isSpecialArmor(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (!isLeatherArmor(item.getType())) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName() ||!meta.hasLore()) {
            return false;
        }

        // 检查显示名称
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        String expectedName = ChatColor.stripColor(getArmorTypeName(item.getType()));
        if (!displayName.equals(expectedName)) {
            return false;
        }

        // 检查描述信息中是否包含灵魂绑定标识
        List<String> lore = meta.getLore();
        for (String line : lore) {
            if (ChatColor.stripColor(line).contains("灵魂绑定 I")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断物品类型是否为皮革装甲
     * @param material 物品类型
     * @return 如果是皮革装甲则返回 true，否则返回 false
     */
    private static boolean isLeatherArmor(Material material) {
        return material == Material.LEATHER_HELMET ||
                material == Material.LEATHER_CHESTPLATE ||
                material == Material.LEATHER_LEGGINGS ||
                material == Material.LEATHER_BOOTS;
    }

    /**
     * 获取皮革装甲的类型名称
     * @param armorType 装甲类型
     * @return 装甲类型名称
     */
    private static String getArmorTypeName(Material armorType) {
        switch (armorType) {
            case LEATHER_HELMET:
                return "头盔";
            case LEATHER_CHESTPLATE:
                return "胸甲";
            case LEATHER_LEGGINGS:
                return "护腿";
            case LEATHER_BOOTS:
                return "靴子";
            default:
                throw new IllegalArgumentException("提供的物品类型不是皮革装甲！");
        }
    }

    /**
     * 根据队伍颜色字符串获取对应的 Color 对象
     * @param teamColor 队伍颜色字符串
     * @return 对应的 Color 对象
     */
    private static Color getColorFromTeam(String teamColor) {
        switch (teamColor.toLowerCase()) {
            case "red":
                return Color.RED;
            case "blue":
                return Color.BLUE;
            case "green":
                return Color.GREEN;
            case "yellow":
                return Color.YELLOW;
            case "purple":
                return Color.PURPLE;
            case "orange":
                return Color.ORANGE;
            case "black":
                return Color.BLACK;
            case "white":
                return Color.WHITE;
            default:
                return Color.GRAY; // 默认颜色
        }
    }
}