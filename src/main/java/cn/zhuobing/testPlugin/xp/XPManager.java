package cn.zhuobing.testPlugin.xp;

import cn.zhuobing.testPlugin.utils.AnniConfigManager; // 新增导入
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一XP管理类
 */
public class XPManager {
    // 单例实例（全局唯一，方便业务类直接调用）
    private static XPManager instance;
    // 总XP：长期存储，跨回合保留
    private final Map<UUID, Integer> totalXpMap = new ConcurrentHashMap<>();
    // 本局XP：单局内有效，回合结束后重置
    private final Map<UUID, Integer> currentGameXpMap = new ConcurrentHashMap<>();

    // 私有构造器（禁止外部实例化）
    private XPManager() {}

    // 获取单例实例（业务类通过此方法调用）
    public static synchronized XPManager getInstance() {
        if (instance == null) {
            instance = new XPManager();
        }
        return instance;
    }

    public void setXpSystemEnabled(boolean enabled){
        AnniConfigManager.XP_SYSTEM_ENABLED = enabled;
    }

    /**
     * 获取当前XP系统开关状态（优先读取配置类）
     * @return true=开启，false=关闭
     */
    public boolean isXpSystemEnabled() {
        // 从配置类读取，保证配置实时性
        return AnniConfigManager.XP_SYSTEM_ENABLED;
    }

    // ==================== 总XP操作（跨回合保留）====================
    /**
     * 给单个玩家添加总XP（长期累计）
     * @param player 目标玩家
     * @param amount 新增总XP数量
     */
    private void addTotalXP(Player player, int amount) {
        if (player == null || amount <= 0) return;
        UUID uuid = player.getUniqueId();
        totalXpMap.put(uuid, totalXpMap.getOrDefault(uuid, 0) + amount);
    }

    /**
     * 获取玩家总XP（跨回合累计值）
     * @param player 目标玩家
     * @return 总XP数量
     */
    public int getTotalXP(Player player) {
        if (!isXpSystemEnabled()) return 0;
        return player != null ? totalXpMap.getOrDefault(player.getUniqueId(), 0) : 0;
    }

    /**
     * 重置玩家总XP（仅用于特殊场景，如管理员操作）
     * @param player 目标玩家
     */
    public void resetTotalXP(Player player) {
        if (!isXpSystemEnabled()) return;
        if (player != null) {
            totalXpMap.remove(player.getUniqueId());
        }
    }

    // ==================== 本局XP操作（单局有效）====================
    /**
     * 给单个玩家添加本局XP（同时累加总XP）
     * @param player 目标玩家
     * @param rewardType 奖励类型（枚举）
     */
    public void addXP(Player player, XPRewardType rewardType) {
        if (!isXpSystemEnabled()) return;
        if (player == null || rewardType == null) return;

        int addXp = rewardType.getBaseXP();
        UUID uuid = player.getUniqueId();

        // 1. 更新本局XP
        currentGameXpMap.put(uuid, currentGameXpMap.getOrDefault(uuid, 0) + addXp);
        // 2. 同步更新总XP
        addTotalXP(player, addXp);
        // 3. 玩家提示（同时显示本局新增和当前总XP）
        player.sendMessage(ChatColor.GREEN + "+" + addXp + " XP (" + rewardType.getDescription() + ")");
    }

    /**
     * 给团队所有玩家添加本局XP（同时累加总XP）
     * @param teamPlayers 团队玩家列表
     * @param rewardType 奖励类型（枚举）
     */
    public void addXPToTeam(List<Player> teamPlayers, XPRewardType rewardType) {
        if (!isXpSystemEnabled()) return;
        if (teamPlayers == null || teamPlayers.isEmpty() || rewardType == null) return;

        int addXpPerPlayer = rewardType.getBaseXP();
        for (Player player : teamPlayers) {
            if (!player.isOnline()) continue;

            UUID uuid = player.getUniqueId();
            // 1. 更新本局XP
            currentGameXpMap.put(uuid, currentGameXpMap.getOrDefault(uuid, 0) + addXpPerPlayer);
            // 2. 同步更新总XP
            addTotalXP(player, addXpPerPlayer);
            // 3. 玩家提示
            player.sendMessage(ChatColor.GREEN + "+" + addXpPerPlayer + " XP (" + rewardType.getDescription() + ")");
        }
    }

    /**
     * 给团队添加获胜奖励（本局XP+总XP同步累加）
     * @param teamPlayers 获胜团队玩家列表
     * @param remainingNexusHealth 核心剩余血量
     */
    public void addWinRewardToTeam(List<Player> teamPlayers, int remainingNexusHealth) {
        if (!isXpSystemEnabled()) return;
        if (teamPlayers == null || teamPlayers.isEmpty()) return;

        int baseWinXp = XPRewardType.WON_ROUND_BASE.getBaseXP();
        int healthBonusXp = remainingNexusHealth * XPRewardType.NEXUS_HEALTH_BONUS.getBaseXP();
        int totalXpPerPlayer = baseWinXp + healthBonusXp;

        for (Player player : teamPlayers) {
            if (!player.isOnline()) continue;

            UUID uuid = player.getUniqueId();
            // 1. 更新本局XP
            currentGameXpMap.put(uuid, currentGameXpMap.getOrDefault(uuid, 0) + totalXpPerPlayer);
            // 2. 同步更新总XP
            addTotalXP(player, totalXpPerPlayer);
            // 3. 玩家提示
            player.sendMessage(ChatColor.GREEN + "+" + totalXpPerPlayer + " XP (" + "获胜奖励" + ")");
        }
    }

    /**
     * 获取玩家本局XP（单局内累计值）
     * @param player 目标玩家
     * @return 本局XP数量
     */
    public int getCurrentGameXP(Player player) {
        if (!isXpSystemEnabled()) return 0;
        return player != null ? currentGameXpMap.getOrDefault(player.getUniqueId(), 0) : 0;
    }

    /**
     * 重置单个玩家本局XP（如玩家因作弊被踢出）
     * @param player 目标玩家
     */
    public void resetCurrentGameXP(Player player) {
        if (!isXpSystemEnabled()) return;
        if (player != null) {
            currentGameXpMap.remove(player.getUniqueId());
        }
    }

    /**
     * 重置所有玩家本局XP（回合结束、游戏重启时调用）
     */
    public void resetAllCurrentGameXP() {
        if (!isXpSystemEnabled()) return;
        currentGameXpMap.clear();
    }
}