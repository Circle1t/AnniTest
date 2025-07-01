package cn.zhuobing.testPlugin.utils;

import cn.zhuobing.testPlugin.enchant.SoulBoundLevel;
import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (displayName != null) {
            meta.setDisplayName(displayName);
        }

        //在最物品lore最下方添加默认”灵魂绑定“字段，如果此字样被覆盖了，必须要手动设置！
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add(ChatColor.GOLD + "灵魂绑定 " + level.getDisplay());
        meta.setLore(lore);
        meta.setUnbreakable(isUnbreakable);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);

        // 注册材质、名称和等级
        SoulBoundListener.registerSoulBoundItem(material, displayName, soulBoundLevel);
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
     * 判断物品是否为灵魂绑定物品
     *
     * @param stack    物品堆栈
     * @param material 物品材质
     * @return 是否为灵魂绑定物品
     */
    public static boolean isSoulBoundItemWithDisplayName(ItemStack stack, Material material,String displayName) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (!meta.hasLore()) {
            return false;
        }
        if(!stack.getItemMeta().getDisplayName().equals(displayName)){
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
     * 判断物品是否为指定等级的灵魂绑定物品
     *
     * @param stack    物品堆栈
     * @param material 物品材质
     * @param level    灵魂绑定等级
     * @return 是否为指定等级的灵魂绑定物品
     */
    public static boolean isSoulBoundItem(ItemStack stack, Material material, int level) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (!meta.hasLore()) {
            return false;
        }
        List<String> lore = meta.getLore();
        String expectedLevelText = "灵魂绑定 " + SoulBoundLevel.fromInt(level).getDisplay();
        for (String line : lore) {
            if (ChatColor.stripColor(line).contains(expectedLevelText)) {
                return stack.getType() == material;
            }
        }
        return false;
    }

    /**
     * 清除玩家所有灵魂绑定等级为 3或 4 的物品
     *
     * @param player 目标玩家
     */
    public static void clearSoulBoundLevelItems(Player player) {
        if (player == null) return;

        Arrays.stream(player.getInventory().getContents())
                .forEach(item -> {
                    if (item != null) {
                        int level = SoulBoundListener.getSoulBoundLevel(item);
                        if (level == 3 || level == 4) item.setAmount(0);
                    }
                });
    }
}