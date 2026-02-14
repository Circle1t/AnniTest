package cn.zhuobing.testPlugin.anni;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 定时向全体在线玩家广播游戏教程小提示（如每 2 分钟一条），文案参考教程书。
 */
public class GameTipBroadcaster {

    private static final long INTERVAL_TICKS = 2400L; // 2 分钟 = 120 秒

    private static final String PREFIX = ChatColor.AQUA + "[核心战争小提示] ";

    private static final List<String> TIPS = new CopyOnWriteArrayList<>(List.of(
            ChatColor.GRAY + "摧毁敌方" + ChatColor.GOLD + "核心（末地石）" + ChatColor.GRAY + "，最后存活的队伍获胜。",
            ChatColor.GRAY + "必须" + ChatColor.DARK_RED + "选择队伍" + ChatColor.GRAY + "才能加入游戏，三阶段后无法加入队伍。",
            ChatColor.GRAY + "核心同时" + ChatColor.DARK_RED + "只能被一人破坏" + ChatColor.GRAY + "，注意配合。",
            ChatColor.GRAY + "剑可破坏蜘蛛网、西瓜块、树叶；剪刀可破坏树叶、蜘蛛网和羊毛。",
            ChatColor.GRAY + "镐子可破坏所有能用镐挖的方块；斧头可破坏木质物品和原木。",
            ChatColor.GRAY + "铲子可破坏所有能用铲子挖的方块。",
            ChatColor.DARK_PURPLE + "地狱门" + ChatColor.GRAY + "可" + ChatColor.DARK_AQUA + "切换职业" + ChatColor.GRAY + "。",
            ChatColor.GOLD + "附魔金苹果" + ChatColor.GRAY + "食用后获得 30 秒生命恢复 III，食用 3 个后冷却 2 分钟。",
            ChatColor.GRAY + "铁块、金块、红石灯、石质压力板可制作" + ChatColor.DARK_AQUA + "发射台" + ChatColor.GRAY + "弹射自己。",
            ChatColor.GRAY + "所有树叶都会掉落" + ChatColor.GOLD + "苹果" + ChatColor.GRAY + "。",
            ChatColor.DARK_PURPLE + "末影熔炉" + ChatColor.GRAY + "加快烧制速度，仅能使用己方队伍的，放入物品仅你可见。",
            ChatColor.GRAY + "输入 " + ChatColor.WHITE + "/kl" + ChatColor.GRAY + " 或 " + ChatColor.WHITE + "/suicide" + ChatColor.GRAY + " 可快速重生。",
            ChatColor.GRAY + "输入 " + ChatColor.WHITE + "/team red/yellow/blue/green" + ChatColor.GRAY + " 快速加入队伍。",
            ChatColor.GRAY + "输入 " + ChatColor.WHITE + "/compass" + ChatColor.GRAY + " 获得指南针；" + ChatColor.WHITE + "/annihelp book" + ChatColor.GRAY + " 获取教程书。",
            ChatColor.GRAY + "在消息前加 " + ChatColor.GOLD + "!" + ChatColor.GRAY + " 或 " + ChatColor.GOLD + "@" + ChatColor.GRAY + " 可全体说话。",
            ChatColor.DARK_GREEN + "阶段一" + ChatColor.GRAY + "：和平发展，可骚扰敌人，此阶段无法破坏核心。",
            ChatColor.GOLD + "阶段二" + ChatColor.GRAY + "：核心不再无敌，可开始破坏核心，注意保护己方核心。",
            ChatColor.DARK_AQUA + "阶段三" + ChatColor.GRAY + "：钻石在地图中心生成，女巫开始刷新，争夺资源。",
            ChatColor.GRAY + "阶段四可购买" + ChatColor.GOLD + "烈焰粉" + ChatColor.GRAY + "，BOSS 生成，末地传送门出现。",
            ChatColor.DARK_RED + "阶段五" + ChatColor.GRAY + "：挖掘核心造成双倍伤害，全力进攻争取胜利。"
    ));

    private final Plugin plugin;
    private BukkitTask task;

    public GameTipBroadcaster(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 开始每 2 分钟广播一条小提示。
     */
    public void start() {
        if (task != null && !task.isCancelled()) return;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (TIPS.isEmpty()) return;
                int i = ThreadLocalRandom.current().nextInt(TIPS.size());
                String tip = TIPS.get(i);
                String message = PREFIX + tip;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOnline()) {
                        player.sendMessage(message);
                    }
                }
            }
        }.runTaskTimer(plugin, INTERVAL_TICKS, INTERVAL_TICKS);
    }

    /**
     * 停止广播任务。
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
