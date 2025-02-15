package cn.zhuobing.testPlugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SoulBoundUtil {
    public static boolean isSoulBoundItem(ItemStack stack, String displayName, Material material) {
        if (stack == null || !stack.hasItemMeta() || !stack.getItemMeta().hasDisplayName()) {
            return false;
        }
        String stackName = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
        String targetName = ChatColor.stripColor(displayName);
        return stack.getType() == material
                && stackName.equals(targetName)
                && stack.getItemMeta().isUnbreakable();
    }
}
