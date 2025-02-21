package cn.zhuobing.testPlugin.enchant;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

// 灵魂绑定管理器类，用于管理所有灵魂绑定物品及其相关事件
public class SoulBoundListener implements Listener {

    // 开发规范：灵魂绑定物品必须在物品lore中以如下格式命名 "灵魂绑定 I/II/III/IV/V"
    // 灵魂绑定注册体系：根据物品材质与名称动态注册物品，一般采用工具类注册
    // 灵魂绑定等级获取体系：若存在物品材质与名称相同的注册物品，获取最高等级
    // 1-2级非职业物品 3-4级职业物品 5级大厅物品

    // 所有soulbound类型死亡后都会被销毁 禁止被附魔/铁砧操作 禁止移动出玩家背包
    // soulbound1：可丢弃且丢弃销毁的普通物品 适用于：盔甲 工具 等
    // soulbound2：不可丢弃的非职业绑定物品 适用于：boss 等
    // soulbound3：可丢弃的职业绑定物品 适用于：鸟人的弓 等
    // soulbound4：不可丢弃的职业绑定物品 适用于：斥候的抓钩 等
    // soulbound5：玩家不能移动该物品在背包和物品栏中的位置，也不能改变其数量 不可丢弃 适用于：大厅物品 等

    // 存储物品判断条件及其对应的灵魂绑定等级
    private static final List<SoulBoundEntry> soulBoundEntries = new ArrayList<>();

    /**
     * 注册灵魂绑定物品
     *
     * @param material    物品的材质，用于识别物品的类型
     * @param displayName 物品的显示名称，用于更精确地识别物品
     * @param level       灵魂绑定的等级，范围为 1 到 5
     */
    public static void registerSoulBoundItem(Material material, String displayName, int level) {
        if (level >= 1 && level <= 5) {
            soulBoundEntries.add(new SoulBoundEntry(material, displayName, level));
        }
    }

    /**
     * 获取物品的灵魂绑定等级
     *
     * @param item 要检查的物品
     * @return 物品的灵魂绑定等级，如果物品不是灵魂绑定物品则返回 0
     */
    public static int getSoulBoundLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        ItemMeta meta = item.getItemMeta();
        // 检查物品的lore中是否包含 "灵魂绑定" 字样
        boolean hasSoulBound = Optional.ofNullable(meta.getLore()).orElse(Collections.emptyList()).stream()
                .anyMatch(line -> ChatColor.stripColor(line).contains("灵魂绑定"));

        if (!hasSoulBound) return 0;

        String displayName = meta.hasDisplayName() ? meta.getDisplayName() : null;
        Material material = item.getType();

        // 查找匹配的灵魂绑定条目并返回最高等级
        return soulBoundEntries.stream()
                .filter(entry -> entry.matches(material, displayName))
                .mapToInt(SoulBoundEntry::level)
                .max()
                .orElse(0);
    }

    /**
     * 灵魂绑定条目的记录类，用于存储物品的材质、显示名称和灵魂绑定等级
     */
    private record SoulBoundEntry(Material material, String displayName, int level) {
        /**
         * 检查给定的材质和名称是否与当前条目匹配
         *
         * @param mat 要检查的物品材质
         * @param name 要检查的物品显示名称
         * @return 如果匹配则返回 true，否则返回 false
         */
        boolean matches(Material mat, String name) {
            return this.material == mat && Objects.equals(this.displayName, name);
        }
    }

    /**
     * 处理玩家在库存中点击物品的事件
     *
     * @param event 库存点击事件，包含了点击的物品和库存信息
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        // 如果点击的物品是 5 级灵魂绑定物品，取消事件，禁止操作
        if (currentItem != null && getSoulBoundLevel(currentItem) >= 5) {
            event.setCancelled(true);
        }
        // 如果点击的物品是 1 级及以上灵魂绑定物品，且操作的库存不是玩家背包，取消事件
        if (currentItem != null && getSoulBoundLevel(currentItem) >= 1 &&
                event.getInventory().getHolder() != null && !(event.getInventory().getHolder() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理玩家在库存中拖动物品的事件
     *
     * @param event 库存拖动事件，包含了拖动的物品和库存信息
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // 遍历拖动的所有物品
        for (ItemStack item : event.getNewItems().values()) {
            // 如果拖动的物品是 5 级灵魂绑定物品，取消事件，禁止操作
            if (item != null && getSoulBoundLevel(item) >= 5) {
                event.setCancelled(true);
                return;
            }
            // 如果拖动的物品是 1 级及以上灵魂绑定物品，且操作的库存不是玩家背包，取消事件
            if (item != null && getSoulBoundLevel(item) >= 1 &&
                    event.getInventory().getHolder() != null && !(event.getInventory().getHolder() instanceof Player)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 处理玩家丢弃物品的事件
     *
     * @param event 玩家丢弃物品事件，包含了丢弃的物品和玩家信息
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        int level = getSoulBoundLevel(droppedItem);

        switch (level) {
            case 2:
            case 4:
            case 5:
                // 阻止 2 级、4 级、5 级物品丢弃，取消事件并移除丢弃的物品
                event.setCancelled(true);
                event.getItemDrop().remove();
                break;
            case 1:
            case 3:
                // 允许 1 级、3 级正常丢弃并销毁，移除丢弃的物品并播放音效
                event.getItemDrop().remove();
                event.getPlayer().playSound(
                        event.getPlayer().getLocation(),
                        Sound.ENTITY_BLAZE_HURT,
                        1.0f,
                        0.8f
                );
                break;
        }
    }

    /**
     * 处理玩家死亡事件
     *
     * @param event 玩家死亡事件，包含了玩家死亡时掉落的物品信息
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        // 遍历玩家死亡时掉落的所有物品
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            // 如果物品是 1 级及以上灵魂绑定物品，从掉落列表中移除
            if (getSoulBoundLevel(item) >= 1) {
                iterator.remove();
            }
        }
    }

    /**
     * 处理准备附魔事件，禁止灵魂绑定物品附魔
     *
     * @param event 准备附魔事件，包含了要附魔的物品信息
     */
    @EventHandler
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();
        // 如果要附魔的物品是 1 级及以上灵魂绑定物品，取消事件
        if (getSoulBoundLevel(item) >= 1) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理准备铁砧操作事件，禁止灵魂绑定物品在铁砧上操作（包括附魔、重命名等）
     *
     * @param event 准备铁砧操作事件，包含了铁砧上的物品信息
     */
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack firstItem = event.getInventory().getItem(0);
        ItemStack secondItem = event.getInventory().getItem(1);
        // 如果铁砧上的任何一个物品是 1 级及以上灵魂绑定物品，取消操作结果
        if ((firstItem != null && getSoulBoundLevel(firstItem) >= 1) || (secondItem != null && getSoulBoundLevel(secondItem) >= 1)) {
            event.setResult(null);
        }
    }

    /**
     * 处理物品在库存间移动的事件，阻止灵魂绑定物品移出玩家背包
     *
     * @param event 物品移动事件，包含了物品移动的源库存和目标库存信息
     */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (getSoulBoundLevel(item) >= 1) {
            // 如果物品是从玩家背包移动到其他库存，取消操作
            if (event.getSource().getHolder() instanceof Player && !(event.getDestination().getHolder() instanceof Player)) {
                event.setCancelled(true);
            }
        }
    }
}