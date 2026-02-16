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
            ChatColor.GRAY + "铁块、金块、红石灯、石质压力板可制作" + ChatColor.DARK_AQUA + "发射台" + ChatColor.GRAY + "弹射自己，使用后会有一段冷却时间。",
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
            ChatColor.DARK_RED + "阶段五" + ChatColor.GRAY + "：挖掘核心造成双倍伤害，全力进攻争取胜利。",
            ChatColor.GRAY + "锄头或剑可快速破坏" + ChatColor.DARK_GREEN + "树叶" + ChatColor.GRAY + "，沙砾有概率掉落燧石、羽毛、线、骨头等。",
            ChatColor.AQUA + "传送师" + ChatColor.GRAY + "的传送门为下界石英矿石，敌人轻击即可摧毁；传送师右键自己的传送门也可拆除。",
            ChatColor.GOLD + "斥候" + ChatColor.GRAY + "抓钩在" + ChatColor.RED + "与玩家交战" + ChatColor.GRAY + "后 3 秒内无法使用，打动物不会触发。",
            ChatColor.AQUA + "坦克" + ChatColor.GRAY + "盾牌灵魂绑定且无法破坏，主手或副手持盾即可抵伤。",
            ChatColor.RED + "狂战士" + ChatColor.GRAY + "初始 30 血，击杀玩家可涨至最多 50 血，低血量时造成额外伤害。",
            ChatColor.GRAY + "禁止合成" + ChatColor.DARK_GRAY + "盾牌、活塞、粘性活塞" + ChatColor.GRAY + "，工作台可做附魔金苹果（8 金块 + 1 苹果）。",
            ChatColor.GRAY + "床不能放在" + ChatColor.YELLOW + "边界线" + ChatColor.GRAY + "上；炼药台燃料槽为空时自动填满燃料条。",
            ChatColor.GRAY + "附魔金苹果" + ChatColor.GRAY + " 3 分钟内吃满 3 个会进入 2 分钟冷却，效果为原版（吸收、再生、抗性、防火）。",
            ChatColor.GRAY + "死亡后复活会恢复" + ChatColor.RED + "两排血（40）" + ChatColor.GRAY + "；狂战士复活为 30 血并可通过击杀提升。",
            ChatColor.GOLD + "鸟人" + ChatColor.GRAY + "免疫摔落伤害，空中可二段跳；切换职业后飞行状态会清除。",
            ChatColor.GREEN + "刺客" + ChatColor.GRAY + "使用羽毛技能可隐身跳跃并获得速度与急迫，攻击或受伤会解除隐身。",
            ChatColor.GREEN + "交换者" + ChatColor.GRAY + "可用唱片与附近敌人交换位置，交换后敌人获得缓慢 II，冷却 20 秒。",
            ChatColor.GRAY + "弓箭手" + ChatColor.GRAY + "拥有弓与箭矢，适合远程输出，注意与近战职业配合。",
            ChatColor.DARK_AQUA + "建造师" + ChatColor.GRAY + "可快速建造防御工事，善用方块保护核心与队友。",
            ChatColor.LIGHT_PURPLE + "炼药师" + ChatColor.GRAY + "拥有魔法炼药台加速酿造且仅本人使用，材料库定期提供炼药材料。",
            ChatColor.GREEN + "保卫者" + ChatColor.GRAY + "在己方核心保护区内获得生命恢复，适合守家。",
            ChatColor.RED + "火法师" + ChatColor.GRAY + "免疫火焰与岩浆，攻击可点燃敌人，箭矢自动点燃。",
            ChatColor.GRAY + "矿工" + ChatColor.GRAY + "专注采集资源，为队伍提供矿石与材料。",
            ChatColor.GRAY + "附魔师" + ChatColor.GRAY + "可强化装备，提升队伍整体战力。"
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
