package cn.zhuobing.testPlugin.specialitem.items;

import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.List;

/**
 * 特殊装甲工厂类，支持创建各种材质的特殊装甲
 * 对于皮革装甲保持原有颜色设置逻辑，其他材质装甲使用通用处理
 */
public class SpecialArmor {
    // 灵魂绑定等级
    private static final int DEFAULT_SOUL_BOUND_LEVEL = 1;

    /**
     * 创建特殊装甲
     * @param armorType 装甲类型（头盔、胸甲、护腿、靴子）
     * @param teamColor 队伍颜色（仅对皮革装甲有效）
     * @return 特殊装甲物品
     */
    public static ItemStack createArmor(Material armorType, String teamColor) {
        return createArmor(armorType, teamColor, DEFAULT_SOUL_BOUND_LEVEL, false);
    }

    /**
     * 创建特殊装甲（带灵魂绑定等级和不可破坏属性）
     * @param armorType 装甲类型
     * @param teamColor 队伍颜色
     * @param soulBoundLevel 灵魂绑定等级
     * @param isUnbreakable 是否不可破坏
     * @return 特殊装甲物品
     */
    public static ItemStack createArmor(
            Material armorType,
            String teamColor,
            int soulBoundLevel,
            boolean isUnbreakable
    ) {
        return createArmor(armorType, teamColor, soulBoundLevel, isUnbreakable, null);
    }

    /**
     * 创建特殊装甲（完整参数版本）
     * @param armorType 装甲类型
     * @param teamColor 队伍颜色
     * @param soulBoundLevel 灵魂绑定等级
     * @param isUnbreakable 是否不可破坏
     * @param enchantments 附魔列表（可为null）
     * @return 特殊装甲物品
     */
    public static ItemStack createArmor(
            Material armorType,
            String teamColor,
            int soulBoundLevel,
            boolean isUnbreakable,
            Enchantment... enchantments
    ) {
        // 检查是否为有效装甲类型
        if (!isValidArmor(armorType)) {
            throw new IllegalArgumentException("提供的物品类型不是有效装甲！");
        }

        // 创建基础物品（带灵魂绑定）
        ItemStack item = SoulBoundUtil.createSoulBoundItem(
                armorType,
                ChatColor.WHITE + getArmorTypeName(armorType),
                1,
                soulBoundLevel,
                isUnbreakable
        );

        ItemMeta meta = item.getItemMeta();

        // 设置物品描述信息
        List<String> lore = Arrays.asList(
                ChatColor.DARK_GRAY + "特殊" + getArmorTypeName(armorType),
                "",
                ChatColor.GOLD + "灵魂绑定 " + getRomanNumeral(soulBoundLevel)
        );

        // 添加附魔信息到Lore
        if (enchantments != null && enchantments.length > 0) {
            lore.add("");
            lore.add(ChatColor.AQUA + "附魔属性:");
            for (Enchantment enchantment : enchantments) {
                lore.add(ChatColor.GRAY + "- " + getEnchantmentName(enchantment) + " I");
            }
        }

        meta.setLore(lore);

        // 对皮革装甲设置颜色
        if (isLeatherArmor(armorType) && meta instanceof LeatherArmorMeta) {
            LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
            leatherMeta.setColor(getColorFromTeam(teamColor));
            meta = leatherMeta;
        }

        // 应用附魔
        if (enchantments != null) {
            for (Enchantment enchantment : enchantments) {
                item.addUnsafeEnchantment(enchantment, 1);
            }
        }

        // 应用元数据
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 判断是否为有效装甲类型
     */
    private static boolean isValidArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") ||
                name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") ||
                name.endsWith("_BOOTS");
    }

    /**
     * 判断是否为皮革装甲
     */
    private static boolean isLeatherArmor(Material material) {
        return material == Material.LEATHER_HELMET ||
                material == Material.LEATHER_CHESTPLATE ||
                material == Material.LEATHER_LEGGINGS ||
                material == Material.LEATHER_BOOTS;
    }

    /**
     * 获取装甲类型名称
     */
    private static String getArmorTypeName(Material armorType) {
        switch (armorType) {
            case LEATHER_HELMET:
            case IRON_HELMET:
            case GOLDEN_HELMET:
            case DIAMOND_HELMET:
            case CHAINMAIL_HELMET:
                return "头盔";
            case LEATHER_CHESTPLATE:
            case IRON_CHESTPLATE:
            case GOLDEN_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
            case CHAINMAIL_CHESTPLATE:
                return "胸甲";
            case LEATHER_LEGGINGS:
            case IRON_LEGGINGS:
            case GOLDEN_LEGGINGS:
            case DIAMOND_LEGGINGS:
            case CHAINMAIL_LEGGINGS:
                return "护腿";
            case LEATHER_BOOTS:
            case IRON_BOOTS:
            case GOLDEN_BOOTS:
            case DIAMOND_BOOTS:
            case CHAINMAIL_BOOTS:
                return "靴子";
            default:
                throw new IllegalArgumentException("提供的物品类型不是有效装甲！");
        }
    }

    /**
     * 根据队伍颜色获取对应的 Color 对象
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
                return Color.GRAY;
        }
    }

    /**
     * 获取附魔显示名称
     */
    private static String getEnchantmentName(Enchantment enchantment) {
        String name = enchantment.getKey().getKey();
        return name.replace("_", " ").substring(0, 1).toUpperCase() + name.replace("_", " ").substring(1);
    }

    /**
     * 获取罗马数字表示
     */
    private static String getRomanNumeral(int number) {
        if (number < 1 || number > 10) {
            return String.valueOf(number);
        }
        String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return roman[number - 1];
    }
}