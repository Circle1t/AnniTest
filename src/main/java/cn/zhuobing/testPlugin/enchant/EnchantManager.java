package cn.zhuobing.testPlugin.enchant;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;

public class EnchantManager {
    private final Set<Player> inEnchanting = new HashSet<>();

    /**
     * 处理附魔台打开事件，在青金石槽位填充 64 个青金石，并设置名称
     * @param inventory 附魔台库存
     */
    public void handleEnchantTableOpen(Inventory inventory) {
        int lapisSlot = 1;
        ItemStack lapis = new ItemStack(Material.LAPIS_LAZULI, 64);
        ItemMeta meta = lapis.getItemMeta();
        if (meta != null) {
            // 设置青金石名称为红字提示
            meta.setDisplayName(ChatColor.RED + "附魔台界面丢弃的附魔书会被直接销毁！");
            lapis.setItemMeta(meta);
        }
        inventory.setItem(lapisSlot, lapis);
    }

    /**
     * 检查玩家是否正在进行附魔流程
     * @param player 玩家对象
     * @return 如果玩家正在附魔流程中返回 true，否则返回 false
     */
    public boolean isInEnchanting(Player player) {
        return inEnchanting.contains(player);
    }

    /**
     * 标记玩家开始附魔流程
     * @param player 玩家对象
     */
    public void startEnchanting(Player player) {
        inEnchanting.add(player);
    }

    /**
     * 标记玩家结束附魔流程
     * @param player 玩家对象
     */
    public void endEnchanting(Player player) {
        inEnchanting.remove(player);
    }

    /**
     * 检查物品是否为附魔后的书
     * @param item 物品栈
     * @return 如果是附魔后的书返回 true，否则返回 false
     */
    public boolean isEnchantedBook(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.ENCHANTED_BOOK) return false;
        return !item.getEnchantments().isEmpty();
    }
}