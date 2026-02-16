package cn.zhuobing.testPlugin.anni;

import cn.zhuobing.testPlugin.AnniTest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnchantedAppleListener implements Listener {

    private final Map<UUID, Integer> appleCountMap = new HashMap<>();
    private final Map<UUID, Long> lastEatTimeMap = new HashMap<>();
    private final Map<UUID, Long> cooldownStartMap = new HashMap<>();

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            // 检查冷却状态
            if (cooldownStartMap.containsKey(playerId)) {
                long cooldownStart = cooldownStartMap.get(playerId);
                long cooldownRemaining = (cooldownStart + 120000) - currentTime;

                if (cooldownRemaining > 0) {
                    event.setCancelled(true);
                    player.sendMessage("§c附魔金苹果冷却中！请等待 " + (cooldownRemaining / 1000) + " 秒");
                    return;
                } else {
                    // 冷却结束，重置计数器
                    appleCountMap.remove(playerId);
                    cooldownStartMap.remove(playerId);
                }
            }

            // 检查3分钟内的食用次数
            if (lastEatTimeMap.containsKey(playerId)) {
                long lastEatTime = lastEatTimeMap.get(playerId);

                if (currentTime - lastEatTime <= 180000) { // 3分钟内
                    int count = appleCountMap.getOrDefault(playerId, 0) + 1;
                    appleCountMap.put(playerId, count);

                    if (count >= 3) {
                        // 达到3次，触发冷却
                        cooldownStartMap.put(playerId, currentTime);
                        player.sendMessage("§6已达到3次食用限制！2分钟内无法食用附魔金苹果");

                        // 启动冷却计时器
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (cooldownStartMap.containsKey(playerId) &&
                                        System.currentTimeMillis() - cooldownStartMap.get(playerId) >= 120000) {
                                    appleCountMap.remove(playerId);
                                    cooldownStartMap.remove(playerId);
                                    player.sendMessage("§a附魔金苹果冷却已结束！");
                                    this.cancel();
                                }
                            }
                        }.runTaskTimer(AnniTest.getInstance(), 20L, 20L); // 每秒检查一次
                    }
                } else {
                    // 超过3分钟，重置计数器
                    appleCountMap.put(playerId, 1);
                }
            } else {
                // 第一次食用
                appleCountMap.put(playerId, 1);
            }

            lastEatTimeMap.put(playerId, currentTime);

            // 给予药水效果
            applyEnchantedAppleEffects(player);
        }
    }

    /** 原版附魔金苹果效果：吸收 IV 2分钟、再生 II 20秒、抗性 I 5分钟、防火 I 5分钟 */
    private void applyEnchantedAppleEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120 * 20, 3, true, true));   // 吸收 IV 2:00
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 20, 1, true, true));   // 再生 II 0:20
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300 * 20, 0, true, true));    // 抗性 I 5:00
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 300 * 20, 0, true, true)); // 防火 I 5:00
    }
}