package cn.zhuobing.testPlugin.specialitem.items;

import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

// 地图选择器物品类，用于创建和判断地图选择器物品
public class MapSelectorItem {
    // 地图选择器物品的显示名称
    private static final String ITEM_IDENTIFIER = ChatColor.GREEN + "地图选择";
    // 灵魂绑定等级
    private static final int SOUL_BOUND_LEVEL = 5;

    /**
     * 创建地图选择器物品的方法
     * @return 地图选择器物品
     */
    public static ItemStack createMapSelector() {
        // 使用 SoulBoundUtil 创建灵魂绑定物品
        ItemStack item = SoulBoundUtil.createSoulBoundItem(
                Material.GRASS_BLOCK,
                ITEM_IDENTIFIER,
                1,
                SOUL_BOUND_LEVEL,
                true
        );

        ItemMeta meta = item.getItemMeta();
        // 设置物品的描述信息
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "右键打开地图选择界面",
                ChatColor.DARK_GRAY + "特殊地图物品",
                "", // 隔一行
                ChatColor.GOLD + "灵魂绑定 V"
        ));

        // 隐藏不可破坏的标签
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        // 将元数据应用到物品上
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 判断物品是否为地图选择器物品
     * @param item 要判断的物品
     * @return 如果物品是地图选择器则返回 true，否则返回 false
     */
    public static boolean isMapSelector(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.getType() != Material.GRASS_BLOCK) {
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
        String identifierWithoutColor = ChatColor.stripColor(ITEM_IDENTIFIER);
        return displayName.equals(identifierWithoutColor);
    }
}