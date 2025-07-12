package cn.zhuobing.testPlugin.anni;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LaunchPadListener implements Listener {

    // 使用ConcurrentHashMap确保线程安全
    private final Map<UUID, Long> cooldownMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> launchTimeMap = new ConcurrentHashMap<>(); // 记录弹射时间
    private final Map<UUID, Double> launchPowerMap = new ConcurrentHashMap<>(); // 记录弹射力度

    // 冷却时间（5秒）
    private static final long COOLDOWN = 5000; // 毫秒
    // 摔落伤害免疫时间（3秒）
    private static final long FALL_DAMAGE_IMMUNITY = 3000; // 毫秒

    // 弹射倍数
    private static final double IRON_POWER = 2.0;
    private static final double LAMP_POWER = 3.0;
    private static final double DIAMOND_POWER = 4.0;
    private static final double EMERALD_POWER = 1.2;
    private static final double GOLD_POWER = 1.4;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 检查是否在冷却时间内
        if (isOnCooldown(playerId)) {
            return;
        }

        // 获取玩家脚下的方块
        Location loc = player.getLocation();
        Block standingBlock = loc.subtract(0, 0.1, 0).getBlock();

        // 检查是否是任意类型的压力板
        if (!isAnyPressurePlate(standingBlock.getType())) {
            return;
        }

        // 获取压力板下方的方块
        Block launchBlock = standingBlock.getLocation().subtract(0, 1, 0).getBlock();
        Material blockType = launchBlock.getType();

        // 处理不同类型的弹射板
        if (isLaunchPad(blockType)) {
            handleLaunchPad(player, blockType);
        } else if (isJumpPad(blockType)) {
            handleJumpPad(player, blockType);
        }
    }

    // 监听实体伤害事件，防止摔落伤害
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();
            UUID playerId = player.getUniqueId();

            // 检查是否在弹射后的摔落伤害免疫时间内
            if (launchTimeMap.containsKey(playerId)) {
                long currentTime = System.currentTimeMillis();
                long launchTime = launchTimeMap.get(playerId);

                if (currentTime - launchTime < FALL_DAMAGE_IMMUNITY) {
                    event.setCancelled(true); // 取消摔落伤害
                }
            }
        }
    }

    private boolean isAnyPressurePlate(Material material) {
        // 支持所有类型的压力板
        return material == Material.STONE_PRESSURE_PLATE ||
                material == Material.OAK_PRESSURE_PLATE ||
                material == Material.HEAVY_WEIGHTED_PRESSURE_PLATE ||
                material == Material.LIGHT_WEIGHTED_PRESSURE_PLATE;
    }

    private boolean isOnCooldown(UUID playerId) {
        if (cooldownMap.containsKey(playerId)) {
            long lastUse = cooldownMap.get(playerId);
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastUse < COOLDOWN) {
                return true;
            }
        }
        return false;
    }

    private boolean isLaunchPad(Material material) {
        return material == Material.IRON_BLOCK ||
                material == Material.REDSTONE_LAMP ||
                material == Material.DIAMOND_BLOCK;
    }

    private boolean isJumpPad(Material material) {
        return material == Material.EMERALD_BLOCK ||
                material == Material.GOLD_BLOCK;
    }

    private void handleLaunchPad(Player player, Material blockType) {
        // 获取玩家朝向向量
        Vector direction = player.getLocation().getDirection();

        // 根据方块类型设置弹射力度
        double power = 0.0;
        switch (blockType) {
            case IRON_BLOCK:
                power = IRON_POWER;
                break;
            case REDSTONE_LAMP:
                power = LAMP_POWER;
                break;
            case DIAMOND_BLOCK:
                power = DIAMOND_POWER;
                break;
            default:
                return;
        }

        // 应用弹射效果
        applyLaunchEffect(player, direction, power);
    }

    private void handleJumpPad(Player player, Material blockType) {
        // 根据方块类型设置跳跃力度
        double power = 0.0;
        switch (blockType) {
            case EMERALD_BLOCK:
                power = EMERALD_POWER;
                break;
            case GOLD_BLOCK:
                power = GOLD_POWER;
                break;
            default:
                return;
        }

        // 创建向上弹射的向量
        Vector direction = new Vector(0, power, 0);

        // 应用弹射效果
        applyLaunchEffect(player, direction, power);
    }

    private void applyLaunchEffect(Player player, Vector direction, double power) {
        // 设置弹射向量
        Vector velocity = direction.multiply(power);
        player.setVelocity(velocity);

        // 播放音效
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.5f);

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // 设置冷却时间
        cooldownMap.put(playerId, currentTime);

        // 记录弹射时间和力度，用于摔落伤害免疫
        launchTimeMap.put(playerId, currentTime);
        launchPowerMap.put(playerId, power);

        // 定时清理数据，避免内存泄漏
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        cooldownMap.remove(playerId);
                        launchTimeMap.remove(playerId);
                        launchPowerMap.remove(playerId);
                    }
                },
                Math.max(COOLDOWN, FALL_DAMAGE_IMMUNITY)
        );
    }
}