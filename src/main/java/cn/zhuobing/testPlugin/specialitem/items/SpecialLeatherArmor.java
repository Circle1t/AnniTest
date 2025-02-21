package cn.zhuobing.testPlugin.specialitem.items;

import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.List;

// 特殊皮革装甲类，用于创建和判断特殊皮革装甲
public class SpecialLeatherArmor {
    // 灵魂绑定等级
    private static final int SOUL_BOUND_LEVEL = 1;

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

        // 使用 SoulBoundUtil 创建灵魂绑定物品
        ItemStack item = SoulBoundUtil.createSoulBoundItem(
                armorType,
                ChatColor.WHITE + getArmorTypeName(armorType),
                1,
                SOUL_BOUND_LEVEL,
                false
        );

        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();

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

        return item;
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