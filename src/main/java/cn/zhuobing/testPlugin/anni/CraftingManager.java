package cn.zhuobing.testPlugin.anni;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 管理物品合成规则：禁止合成指定类型（如盾牌）、添加自定义配方（如附魔金苹果）。
 */
public class CraftingManager implements Listener {

    private final Set<Material> forbiddenResultTypes = new HashSet<>();

    public CraftingManager(Plugin plugin) {
        forbiddenResultTypes.add(Material.SHIELD);
        forbiddenResultTypes.add(Material.PISTON);
        forbiddenResultTypes.add(Material.STICKY_PISTON);
        registerEnchantedGoldenAppleRecipe(plugin);
    }

    /** 添加旧版附魔金苹果配方：8 金块 + 1 苹果（工作台 3x3，苹果居中） */
    private void registerEnchantedGoldenAppleRecipe(Plugin plugin) {
        ItemStack result = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);
        NamespacedKey key = new NamespacedKey(plugin, "enchanted_golden_apple");
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("GGG", "GAG", "GGG");
        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('A', Material.APPLE);
        Bukkit.addRecipe(recipe);
    }

    /** 禁止合成结果为指定类型的物品，可多次调用以添加多种禁止类型 */
    public void forbidCraftResult(Material resultType) {
        if (resultType != null) {
            forbiddenResultTypes.add(resultType);
        }
    }

    /** 获取当前禁止合成的结果类型（只读） */
    public Set<Material> getForbiddenResultTypes() {
        return Collections.unmodifiableSet(forbiddenResultTypes);
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) return;

        if (forbiddenResultTypes.contains(result.getType())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "禁止合成该物品！");
            }
        }
    }
}
