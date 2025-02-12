package cn.zhuobing.testPlugin.specialitem.items;

import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.function.Predicate;

// 团队选择之星物品类，用于创建和判断团队选择之星物品
public class TeamSelectorItem {
    // 团队选择之星物品的显示名称
    private static final String ITEM_IDENTIFIER = ChatColor.RESET + "§6团队选择";

    /**
     * 创建团队选择之星物品的方法
     * @return 团队选择之星物品
     */
    public static ItemStack createTeamStar() {
        // 创建一个 Nether Star 物品
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        // 获取物品的元数据
        ItemMeta meta = item.getItemMeta();

        // 设置物品的显示名称
        meta.setDisplayName(ITEM_IDENTIFIER);
        // 设置物品的描述信息
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "右键打开队伍选择界面",
                ChatColor.DARK_GRAY + "特殊团队物品",
                "", // 隔一行
                ChatColor.GOLD + "灵魂绑定 IV"
        ));

        // 设置物品不可破坏
        meta.setUnbreakable(true);
        // 隐藏不可破坏的标签
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        // 将元数据应用到物品上
        item.setItemMeta(meta);

        // 注册灵魂绑定IV级
        Predicate<ItemStack> isTeamStar = TeamSelectorItem::isTeamStar;
        SoulBoundListener.registerSoulBoundItem(4, isTeamStar);

        return item;
    }

    /**
     * 判断物品是否为团队选择之星物品
     * @param item 要判断的物品
     * @return 如果物品是团队选择之星则返回 true，否则返回 false
     */
    public static boolean isTeamStar(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.getType() != Material.NETHER_STAR) {
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