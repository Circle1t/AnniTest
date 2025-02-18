package cn.zhuobing.testPlugin.utils;

import cn.zhuobing.testPlugin.enchant.SoulBoundLevel;
import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.function.Predicate;

public class SoulBoundUtil {

    /**
     * 创建一个灵魂绑定物品
     *
     * @param material        物品材质
     * @param displayName     自定义显示名称（如果为 null，则使用默认名称）
     * @param amount          物品堆叠数量
     * @param soulBoundLevel  灵魂绑定等级
     * @param isUnbreakable   是否不可破坏
     * @return 灵魂绑定物品
     */
    public static ItemStack createSoulBoundItem(Material material, String displayName, int amount, int soulBoundLevel, boolean isUnbreakable) {
        SoulBoundLevel level = SoulBoundLevel.fromInt(soulBoundLevel);
        ItemStack item = new ItemStack(material, amount); // 设置物品堆叠数量
        ItemMeta meta = item.getItemMeta();

        // 设置物品名称
        if (displayName != null) {
            meta.setDisplayName(displayName);
        }

        // 获取或创建物品的 lore
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        // 在 lore 最后一行下方添加灵魂绑定信息
        lore.add(ChatColor.GOLD + "灵魂绑定 " + level.getDisplay());

        meta.setLore(lore);
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

    /**
     * 清除玩家所有灵魂绑定等级为 3或4 的物品
     *
     * @param player 目标玩家
     */
    public static void clearSoulBoundLevel2Items(Player player) {
        if (player == null) {
            return;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null) {
                int level = SoulBoundListener.getSoulBoundLevel(item);
                if (level == 3 || level == 4) {
                    contents[i] = null; // 移除灵魂绑定等级为 2 的物品
                }
            }
        }
        player.getInventory().setContents(contents);
    }
}