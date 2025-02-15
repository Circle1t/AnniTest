package cn.zhuobing.testPlugin.utils;

import cn.zhuobing.testPlugin.enchant.SoulBoundLevel;
import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class SoulBoundUtil {

    /**
     * 创建一个灵魂绑定物品
     *
     * @param material        物品材质
     * @param displayName     自定义显示名称（如果为 null，则使用默认名称）
     * @param soulBoundLevel  灵魂绑定等级
     * @param isUnbreakable   是否不可破坏
     * @return 灵魂绑定物品
     */
    public static ItemStack createSoulBoundItem(Material material, String displayName, int soulBoundLevel, boolean isUnbreakable) {
        SoulBoundLevel level = SoulBoundLevel.fromInt(soulBoundLevel);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // 设置物品名称和 Lore
        if(displayName != null) {
            meta.setDisplayName(displayName);
        }

        meta.setLore(Arrays.asList(
                ChatColor.GOLD + "灵魂绑定 " + level.getDisplay()
        ));
        meta.setUnbreakable(isUnbreakable);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);

        // 注册灵魂绑定
        Predicate<ItemStack> isItem = stack -> SoulBoundUtil.isSoulBoundItem(stack, material);
        SoulBoundListener.registerSoulBoundItem(soulBoundLevel, isItem);
        return item;
    }


    /**
     * 判断物品是否为灵魂绑定物品
     *
     * @param stack    物品堆栈
     * @param material 物品材质
     * @return 是否为灵魂绑定物品
     */
    public static boolean isSoulBoundItem(ItemStack stack, Material material) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (!meta.hasLore()) {
            return false;
        }
        List<String> lore = meta.getLore();
        for (String line : lore) {
            if (ChatColor.stripColor(line).contains("灵魂绑定")) {
                return stack.getType() == material;
            }
        }
        return false;
    }
}