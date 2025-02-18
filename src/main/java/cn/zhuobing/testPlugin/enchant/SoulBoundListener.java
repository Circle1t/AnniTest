package cn.zhuobing.testPlugin.enchant;

import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;

// 灵魂绑定管理器类，用于管理所有灵魂绑定物品及其相关事件
public class SoulBoundListener implements Listener {

    // 开发规范：灵魂绑定物品必须在物品lore中以如下格式命名 "灵魂绑定 I/II/III/IV/V"
    // 1-2级非职业物品 3-4级职业物品 5级大厅物品

    // 所有soulbound类型死亡后都会被销毁 禁止被附魔/铁砧操作 禁止移动出玩家背包
    // soulbound1：可丢弃且丢弃销毁的普通物品 适用于：盔甲 工具 等
    // soulbound2：不可丢弃的非职业绑定物品 适用于：boss 等
    // soulbound3：可丢弃的职业绑定物品 适用于：鸟人的弓 等
    // soulbound4：不可丢弃的职业绑定物品 适用于：斥候的抓钩 等
    // soulbound5：玩家不能移动该物品在背包和物品栏中的位置，也不能改变其数量 不可丢弃 适用于：大厅物品 等

    // 存储物品判断条件及其对应的灵魂绑定等级
    private static final List<Entry<Predicate<ItemStack>, Integer>> soulBoundEntries = new ArrayList<>();

    /**
     * 注册灵魂绑定物品的方法
     * @param level 灵魂绑定等级（1、2、3、4或5）
     * @param isItem 判断物品的条件
     */
    public static void registerSoulBoundItem(int level, Predicate<ItemStack> isItem) {
        if (level >= 1 && level <= 5) {
            soulBoundEntries.add(new SimpleEntry<>(isItem, level));
        }
    }

    /**
     * 获取物品的最高灵魂绑定等级
     * @param item 要检查的物品
     * @return 最高灵魂绑定等级（若无则返回0）
     */
    public static int getSoulBoundLevel(ItemStack item) {
        int maxLevel = 0;
        for (Entry<Predicate<ItemStack>, Integer> entry : soulBoundEntries) {
            if (entry.getKey().test(item)) {
                maxLevel = Math.max(maxLevel, entry.getValue());
            }
        }
        return maxLevel;
    }

    /**
     * 处理玩家在库存中点击物品的事件
     * @param event 库存点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && getSoulBoundLevel(currentItem) >= 5) {
            event.setCancelled(true);
        }
        if (currentItem != null && getSoulBoundLevel(currentItem) >= 1 &&
                event.getInventory().getHolder() != null && !(event.getInventory().getHolder() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理玩家在库存中拖动物品的事件
     * @param event 库存拖动事件
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        for (ItemStack item : event.getNewItems().values()) {
            if (item != null && getSoulBoundLevel(item) >= 5) {
                event.setCancelled(true);
                return;
            }
            if (item != null && getSoulBoundLevel(item) >= 1 &&
                    event.getInventory().getHolder() != null && !(event.getInventory().getHolder() instanceof Player)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 处理玩家丢弃物品的事件
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        int level = getSoulBoundLevel(droppedItem);

        switch (level) {
            case 2:
            case 4:
            case 5:
                // 阻止2级、4级、5级物品丢弃
                event.setCancelled(true);
                event.getItemDrop().remove();
                break;
            case 1:
            case 3:
                // 允许1级、3级正常丢弃并销毁
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
     * @param event 玩家死亡事件
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        java.util.Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (getSoulBoundLevel(item) >= 1) {
                iterator.remove();
            }
        }
    }

    /**
     * 处理准备附魔事件，禁止灵魂绑定物品附魔
     * @param event 准备附魔事件
     */
    @EventHandler
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();
        if (getSoulBoundLevel(item) >= 1) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理准备铁砧操作事件，禁止灵魂绑定物品在铁砧上操作（包括附魔、重命名等）
     * @param event 准备铁砧操作事件
     */
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack firstItem = event.getInventory().getItem(0);
        ItemStack secondItem = event.getInventory().getItem(1);
        if ((firstItem != null && getSoulBoundLevel(firstItem) >= 1) || (secondItem != null && getSoulBoundLevel(secondItem) >= 1)) {
            event.setResult(null);
        }
    }

    /**
     * 处理物品在库存间移动的事件，阻止灵魂绑定物品移出玩家背包
     * @param event 物品移动事件
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