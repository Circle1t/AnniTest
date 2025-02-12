package cn.zhuobing.testPlugin.enchant;

import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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

    // 所有soulbound类型死亡后都会被销毁
    // soulbound1：可丢弃且丢弃销毁
    // soulbound2：不可丢弃
    // soulbound3：玩家不能移动该物品在背包和物品栏中的位置，也不能改变其数量 可丢弃且丢弃销毁
    // soulbound4：玩家不能移动该物品在背包和物品栏中的位置，也不能改变其数量 不可丢弃

    // 存储物品判断条件及其对应的灵魂绑定等级
    private static final List<Entry<Predicate<ItemStack>, Integer>> soulBoundEntries = new ArrayList<>();

    /**
     * 注册灵魂绑定物品的方法
     * @param level 灵魂绑定等级（1、2、3或4）
     * @param isItem 判断物品的条件
     */
    public static void registerSoulBoundItem(int level, Predicate<ItemStack> isItem) {
        if (level >= 1 && level <= 4) {
            soulBoundEntries.add(new SimpleEntry<>(isItem, level));
        }
    }

    /**
     * 获取物品的最高灵魂绑定等级
     * @param item 要检查的物品
     * @return 最高灵魂绑定等级（若无则返回0）
     */
    private int getSoulBoundLevel(ItemStack item) {
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
        if (currentItem != null && getSoulBoundLevel(currentItem) >= 3) {
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
            if (item != null && getSoulBoundLevel(item) >= 3) {
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
                // 阻止2级和4级物品丢弃
                event.setCancelled(true);
                event.getItemDrop().remove();
                break;
            case 1:
            case 3:
                // 允许1级和3级正常丢弃并销毁
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
}