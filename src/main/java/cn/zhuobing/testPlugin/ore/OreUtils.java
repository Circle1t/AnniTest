package cn.zhuobing.testPlugin.ore;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.concurrent.ThreadLocalRandom;

public class OreUtils {
    public static boolean isValidTool(ItemStack tool, OreType oreType) {
        if (oreType.toolType == null) return true;

        Material toolMaterial = tool.getType();
        String toolName = toolMaterial.name().toUpperCase();

        // 特殊处理剪刀
        if (oreType.toolType.equals("SHEARS")) {
            return toolMaterial == Material.SHEARS;
        }

        // 树叶可用剑或锄头
        if (oreType == OreType.LEAVES) {
            return toolName.endsWith("_SWORD") || toolName.endsWith("_HOE");
        }

        // 验证工具类型后缀
        if (!toolName.endsWith("_" + oreType.toolType)) {
            return false;
        }

        // 验证工具等级
        return getToolTier(toolMaterial) >= oreType.minToolLevel;
    }

    private static int getToolTier(Material tool) {
        String name = tool.name().toUpperCase();
        if (name.startsWith("WOODEN_")) return 1;
        if (name.startsWith("GOLDEN")) return 2;
        if (name.startsWith("STONE_")) return 3;
        if (name.startsWith("IRON_")) return 4;
        if (name.startsWith("DIAMOND_")) return 5;
        if (name.startsWith("NETHERITE_")) return 6;
        return 0;
    }
    public static int getFortuneLevel(ItemStack tool) {
        return tool.getEnchantmentLevel(Enchantment.FORTUNE);
    }

    public static int calculateDropAmount(int base, int fortuneLevel) {
        if (fortuneLevel <= 0) return base;
        return base + ThreadLocalRandom.current().nextInt(fortuneLevel + 1);
    }

    public static void damageTool(ItemStack tool) {
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable && tool.getType().getMaxDurability() > 0) {
            Damageable damageable = (Damageable) meta;
            damageable.setDamage(damageable.getDamage() + 1);
            tool.setItemMeta(meta);
            if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
                tool.setAmount(0);
            }
        }
    }
}