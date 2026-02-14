package cn.zhuobing.testPlugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 技能冷却通用工具，供各职业复用。
 * 支持：是否在冷却、剩余秒数、开始冷却（含可选周期回调与结束回调）。
 *
 * <p>性能：每个 CooldownUtil 仅使用一个全局定时任务轮询在冷却中的玩家，
 * 避免 120 人场景下每人一个任务带来的调度与回调开销。
 *
 * <p>离线暂存：玩家离线时保存剩余冷却时长（毫秒），重进后从该剩余时间继续计时，离线期间不扣时。
 *
 * <p>计时精度：技能可用时间以 endTime 与当前时间实时计算，无延迟；
 * onReady 回调与物品显示更新按轮询间隔执行，间隔越短反馈越及时（默认 10 tick ≈ 0.5s）。
 */
public final class CooldownUtil implements Listener {

    /** 轮询间隔（tick）。4 tick ≈ 0.2s，兼顾反馈延迟与性能 */
    private static final long TICK_INTERVAL = 4L;

    private final Plugin plugin;
    private final long defaultDurationMs;

    /** 当前在线且在冷却的玩家：UUID -> 冷却结束时间戳 */
    private final Map<UUID, Long> endTimes = new ConcurrentHashMap<>();
    /** 离线玩家退出时的剩余冷却毫秒数，重进后从该剩余时间继续（离线期间不扣时） */
    private final Map<UUID, Long> pendingRemainingMs = new ConcurrentHashMap<>();

    private volatile Consumer<Player> cachedOnReady;
    private volatile BiConsumer<Player, Long> cachedOnTick;
    private volatile BukkitTask tickTask;

    public CooldownUtil(Plugin plugin, long defaultDurationMs) {
        this.plugin = plugin;
        this.defaultDurationMs = defaultDurationMs;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** 是否仍在冷却中（仅查当前在线表 endTimes） */
    public boolean isOnCooldown(Player player) {
        Long end = endTimes.get(player.getUniqueId());
        return end != null && end > System.currentTimeMillis();
    }

    /**
     * 剩余冷却秒数，未在冷却或已过期返回 0。
     * 以 endTime 与当前时间实时计算，无轮询延迟。
     */
    public long getSecondsLeft(Player player) {
        Long end = endTimes.get(player.getUniqueId());
        if (end == null) return 0;
        return Math.max(0, (end - System.currentTimeMillis()) / 1000);
    }

    /** 使用默认时长开始冷却，无回调 */
    public void startCooldown(Player player) {
        startCooldown(player, defaultDurationMs, null, null);
    }

    /** 使用指定时长（毫秒）开始冷却，无回调 */
    public void startCooldown(Player player, long durationMs) {
        startCooldown(player, durationMs, null, null);
    }

    /**
     * 使用指定时长开始冷却，并注册回调。
     *
     * @param player     玩家
     * @param durationMs 冷却时长（毫秒）
     * @param onReady    冷却结束且玩家在线时调用（可为 null）
     * @param onTick     按轮询间隔调用，参数为 (玩家, 剩余秒数)，用于更新物品显示等（可为 null）
     */
    public void startCooldown(Player player, long durationMs,
                              Consumer<Player> onReady,
                              BiConsumer<Player, Long> onTick) {
        UUID id = player.getUniqueId();
        long end = System.currentTimeMillis() + durationMs;
        endTimes.put(id, end);

        if (onReady != null) cachedOnReady = onReady;
        if (onTick != null) cachedOnTick = onTick;

        ensureTickTaskRunning();
    }

    private void ensureTickTaskRunning() {
        if (tickTask != null && !tickTask.isCancelled()) return;
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Consumer<Player> onReady = cachedOnReady;
                BiConsumer<Player, Long> onTick = cachedOnTick;

                Iterator<Map.Entry<UUID, Long>> it = endTimes.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Long> e = it.next();
                    UUID id = e.getKey();
                    long end = e.getValue();
                    Player p = Bukkit.getPlayer(id);

                    if (p == null || !p.isOnline()) {
                        long remainingMs = end - now;
                        if (remainingMs > 0) pendingRemainingMs.put(id, remainingMs);
                        it.remove();
                        continue;
                    }

                    long leftSec = (end - now) / 1000;
                    if (leftSec <= 0) {
                        it.remove();
                        if (onReady != null) onReady.accept(p);
                        continue;
                    }
                    if (onTick != null) onTick.accept(p, leftSec);
                }

                if (endTimes.isEmpty()) {
                    cancel();
                    tickTask = null;
                }
            }
        }.runTaskTimer(plugin, 0L, TICK_INTERVAL);
    }

    /** 玩家重进时：若该玩家有未过期的暂存冷却则恢复并继续计时 */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        restoreIfPending(event.getPlayer());
    }

    /** 玩家退出时：若正在冷却则保存剩余时长到暂存，重进后从该剩余时间继续（离线不扣时） */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Long end = endTimes.remove(id);
        if (end != null) {
            long remainingMs = end - System.currentTimeMillis();
            if (remainingMs > 0) pendingRemainingMs.put(id, remainingMs);
        }
    }

    private void restoreIfPending(Player player) {
        UUID id = player.getUniqueId();
        Long remainingMs = pendingRemainingMs.remove(id);
        if (remainingMs == null || remainingMs <= 0) return;
        endTimes.put(id, System.currentTimeMillis() + remainingMs);
        ensureTickTaskRunning();
    }

    /** 清除该玩家的冷却与暂存（如主动重置时调用）；若无人在冷却则停止定时任务 */
    public void clear(UUID playerId) {
        endTimes.remove(playerId);
        pendingRemainingMs.remove(playerId);
        if (endTimes.isEmpty() && tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }
}
