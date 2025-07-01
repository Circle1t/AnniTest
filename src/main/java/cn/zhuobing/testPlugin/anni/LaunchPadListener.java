package cn.zhuobing.testPlugin.anni;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LaunchPadListener implements Listener {

    // 存储玩家最后使用弹射板的时间
    private final Map<UUID, Long> cooldownMap = new HashMap<>();
    // 存储玩家当前是否处于弹射状态
    private final Map<UUID, Boolean> launchStatusMap = new HashMap<>();

    // 冷却时间（5秒）
    private static final long COOLDOWN = 5000; // 毫秒

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

        // 检查是否是石质压力板
        if (standingBlock.getType() != Material.STONE_PRESSURE_PLATE) {
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

    private boolean isOnCooldown(UUID playerId) {
        // 检查是否处于弹射保护状态
        if (launchStatusMap.containsKey(playerId)) {
            return true;
        }

        // 检查冷却时间
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
        applyLaunchEffect(player, direction, 1.0);
    }

    private void applyLaunchEffect(Player player, Vector direction, double power) {
        // 设置弹射向量
        Vector velocity = direction.multiply(power);
        player.setVelocity(velocity);

        // 播放音效
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.5f);

        // 设置冷却时间
        UUID playerId = player.getUniqueId();
        cooldownMap.put(playerId, System.currentTimeMillis());

        // 设置弹射状态（5秒内免疫摔落伤害）
        launchStatusMap.put(playerId, true);

        // 5秒后移除弹射状态
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        launchStatusMap.remove(playerId);
                    }
                },
                COOLDOWN
        );
    }

    // 检查玩家是否处于弹射状态（用于防摔落伤害）
    public boolean isPlayerLaunched(UUID playerId) {
        return launchStatusMap.containsKey(playerId);
    }
}