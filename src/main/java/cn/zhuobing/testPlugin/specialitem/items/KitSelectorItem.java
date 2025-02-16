package cn.zhuobing.testPlugin.specialitem.items;

import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.function.Predicate;

// 职业选择器物品类，用于创建和判断职业选择器物品
public class KitSelectorItem {
    // 职业选择器物品的显示名称
    private static final String ITEM_IDENTIFIER = ChatColor.RESET + ChatColor.AQUA.toString() + "职业选择";

    /**
     * 创建职业选择器物品的方法
     * @return 职业选择器物品
     */
    public static ItemStack createKitSelector() {
        // 创建一个书物品
        ItemStack item = new ItemStack(Material.BOOK);
        // 获取物品的元数据
        ItemMeta meta = item.getItemMeta();

        // 设置物品的显示名称
        meta.setDisplayName(ITEM_IDENTIFIER);
        // 设置物品的描述信息
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "右键打开职业选择界面",
                ChatColor.DARK_GRAY + "特殊职业物品",
                "", // 隔一行
                ChatColor.GOLD + "灵魂绑定 V"
        ));

        // 设置物品不可破坏
        meta.setUnbreakable(true);
        // 隐藏不可破坏的标签
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        // 将元数据应用到物品上
        item.setItemMeta(meta);

        // 注册灵魂绑定IV级
        Predicate<ItemStack> isKitSelector = KitSelectorItem::isKitSelector;
        SoulBoundListener.registerSoulBoundItem(5, isKitSelector);

        return item;
    }

    /**
     * 判断物品是否为职业选择器物品
     * @param item 要判断的物品
     * @return 如果物品是职业选择器则返回 true，否则返回 false
     */
    public static boolean isKitSelector(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.getType() != Material.BOOK) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        // 获取物品的元数据
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }
        // 去除显示名称中的颜色代码后进行比较
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        String identifierWithoutColor = ChatColor.stripColor(ITEM_IDENTIFIER);
        return displayName.equals(identifierWithoutColor);
    }
}