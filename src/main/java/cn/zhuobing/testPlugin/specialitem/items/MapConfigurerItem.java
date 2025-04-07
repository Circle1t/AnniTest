package cn.zhuobing.testPlugin.specialitem.items;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import java.util.Arrays;

public class MapConfigurerItem {
    private static final String ITEM_IDENTIFIER = ChatColor.AQUA + "地图配置";
    private static final int SOUL_BOUND_LEVEL = 5;

    public static ItemStack createMapConfigurer() {
        ItemStack item = SoulBoundUtil.createSoulBoundItem(
                Material.REPEATER,
                ITEM_IDENTIFIER,
                1,
                SOUL_BOUND_LEVEL,
                true
        );

        ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "右键打开地图配置界面",
                ChatColor.DARK_GRAY + "特殊地图物品",
                "",
                ChatColor.GOLD + "灵魂绑定 V"
        ));

        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);

        return item;
    }

    public static boolean isMapConfigurer(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.getType() != Material.REPEATER) {
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