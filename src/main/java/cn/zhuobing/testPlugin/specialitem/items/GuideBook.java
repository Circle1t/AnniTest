package cn.zhuobing.testPlugin.specialitem.items;

import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 核心战争游戏介绍书本类
public class GuideBook {
    // 书本显示名称："核心战争"（红、黄、蓝、绿加粗）+"游戏介绍"（金色不加粗）
    private static final String ITEM_IDENTIFIER =
            ChatColor.RED + "§l核" +
                    ChatColor.YELLOW + "§l心" +
                    ChatColor.BLUE + "§l战" +
                    ChatColor.GREEN + "§l争" +
                    ChatColor.GOLD + " 游戏介绍";

    // 灵魂绑定等级
    private static final int SOUL_BOUND_LEVEL = 5;

    /**
     * 创建核心战争游戏介绍书本
     * @return 玩法介绍书本物品
     */
    public static ItemStack createGameGuideBook() {
        // 使用 SoulBoundUtil 创建灵魂绑定物品
        ItemStack item = SoulBoundUtil.createSoulBoundItem(
                Material.WRITTEN_BOOK,
                ITEM_IDENTIFIER,
                1,
                SOUL_BOUND_LEVEL,
                true
        );

        // 获取物品的元数据并转换为BookMeta
        BookMeta meta = (BookMeta) item.getItemMeta();

        // 设置书本基础信息
        meta.setTitle(ChatColor.stripColor(ITEM_IDENTIFIER)); // 标题去除颜色代码（书本标题不支持颜色）
        meta.setAuthor("Bilibili 烧烤蒸馏水");

        // 构建书页内容
        List<String> pages = new ArrayList<>();

        // 第一页：游戏介绍
        pages.add(
                ChatColor.DARK_BLUE + "§l【游戏介绍】\n\n" +
                        ChatColor.DARK_PURPLE + "§l核心战争" + ChatColor.BLACK + "分为5个阶段\n" +
                        ChatColor.BLACK + "每个阶段会有不同的事件\n\n" +
                        ChatColor.DARK_GREEN + "§l获胜条件：\n" +
                        ChatColor.BLACK + "摧毁敌对" + ChatColor.GOLD + "§l核心（末地石）\n" +
                        ChatColor.DARK_RED + "§l最后存活的队伍，" + ChatColor.BLACK + "获得胜利\n"

        );

        // 第二页：注意事项
        pages.add(
                ChatColor.GOLD + "§l【注意事项】\n\n" +
                        ChatColor.DARK_RED + "§l必须选择队伍" + ChatColor.BLACK + "才能加入游戏\n" +
                        ChatColor.DARK_BLUE + "§l三阶段" + ChatColor.BLACK + "后无法加入队伍\n\n" +
                        ChatColor.DARK_RED + "§l死亡后装备会掉落\n\n" +
                        ChatColor.DARK_RED + "§l核心同时只能被一人破坏"
        );

        // 第三页：工具与方块破坏规则
        pages.add(
                ChatColor.GOLD + "§l【工具破坏规则】\n\n" +
                        ChatColor.DARK_PURPLE + "§l你必须使用对应的工具才能破坏相应方块\n\n" +
                        ChatColor.DARK_RED + "§l剑：" + ChatColor.BLACK + "可破坏\n" +
                        ChatColor.DARK_GREEN + "§l蜘蛛网、西瓜块、树叶\n\n" +
                        ChatColor.GRAY + "§l剪刀：" + ChatColor.BLACK + "可破坏\n" +
                        ChatColor.DARK_GREEN + "§l树叶、蜘蛛网和" + ChatColor.DARK_BLUE + "羊毛\n\n" +
                        ChatColor.GOLD + "§l镐子：" + ChatColor.BLACK + "可破坏所有\n能用镐挖的方块\n\n"

        );

        // 第四页：工具与方块破坏规则（续）
        pages.add(
                ChatColor.GOLD + "§l【工具破坏规则】\n\n" +
                        ChatColor.DARK_RED + "§l斧头：" + ChatColor.BLACK + "可破坏\n" +
                        ChatColor.GOLD + "§l木质物品、" + ChatColor.DARK_GREEN + "§l原木\n\n" +
                        ChatColor.DARK_BLUE + "§l铲子：" + ChatColor.BLACK + "可破坏所有\n能用铲子挖的方块\n\n"
        );

        // 第五页：特殊机制
        pages.add(
                ChatColor.GOLD + "§l【特殊机制】\n\n" +
                        ChatColor.DARK_PURPLE + "§l地狱门" + ChatColor.BLACK + "可" +
                        ChatColor.DARK_AQUA + "§l切换职业\n\n" +
                        ChatColor.GOLD + "§l附魔金苹果：\n" +
                        ChatColor.BLACK + "食用后获得30秒\n" +
                        ChatColor.DARK_RED + "§l生命恢复III" + ChatColor.BLACK + "效果\n" +
                        ChatColor.BLACK + "食用3个后" + ChatColor.DARK_BLUE + "§l冷却2分钟"
        );

        // 第六页：特殊机制（续）
        pages.add(
                ChatColor.GOLD + "§l【特殊机制】\n\n" +
                        ChatColor.BLACK + "• 可使用材料制作\n" +
                        ChatColor.GRAY + "§l铁块、" + ChatColor.GOLD + "§l金块\n" +
                        ChatColor.RED + "§l红石灯、" + ChatColor.GRAY + "§l石质压力板\n" +
                        ChatColor.BLACK + "制作" + ChatColor.DARK_AQUA + "§l发射台\n" +
                        ChatColor.BLACK + "将自己弹射一定距离\n\n" +
                        ChatColor.BLACK + "• 所有的" + ChatColor.DARK_GREEN + "§l树叶\n" +
                        ChatColor.BLACK + "都会掉落" + ChatColor.GOLD + "§l苹果"
        );

        // 第七页：特殊机制（续）
        pages.add(
                ChatColor.GOLD + "§l【特殊机制】\n\n" +
                        ChatColor.DARK_PURPLE + "§l末影熔炉\n" + ChatColor.BLACK + "能够加快你的物品烧制速度，你只能使用你自己队伍的末影熔炉。" +
                        "放在此熔炉里的东西" + ChatColor.DARK_RED + "仅你可见。"
        );

        // 第七页：快捷指令
        pages.add(
                ChatColor.GOLD + "§l【快捷指令】\n\n" +
                        ChatColor.BLACK + "/kl 或 /suicide\n" +
                        ChatColor.DARK_GREEN + "§l→ 快速重生\n\n" +
                        ChatColor.BLACK + "/team\n" + ChatColor.DARK_GRAY + "red/yellow\n/blue/green\n" +
                        ChatColor.DARK_GREEN + "§l→ 快速加入队伍\n\n" +
                        ChatColor.BLACK + "/compass\n" +
                        ChatColor.DARK_GREEN + "§l→ 获得指南针"
        );

        // 第八页：快捷指令（续）
        pages.add(
                ChatColor.GOLD + "§l【快捷指令】\n\n" +
                        ChatColor.BLACK + "全体说话：\n" +
                        ChatColor.BLACK + "在消息前加符号\n" +
                        ChatColor.GOLD + "§l！、@、！\n\n" +
                        ChatColor.DARK_GRAY + "§l提示：" + ChatColor.BLACK + "使用快捷指令\n" +
                        ChatColor.BLACK + "可以更方便地\n进行游戏操作"
        );

        // 第九页：阶段一介绍
        pages.add(
                ChatColor.DARK_BLUE + "§l【阶段一】\n\n" +
                        ChatColor.DARK_GREEN + "§l和平发展阶段\n\n" +
                        ChatColor.BLACK + "• 在有限时间内发展装备\n" +
                        ChatColor.BLACK + "• 可以骚扰敌人\n" +
                        ChatColor.BLACK + "• 此阶段无法破坏核心\n\n" +
                        ChatColor.DARK_GRAY + "§l策略：" + ChatColor.BLACK + "快速收集资源\n" +
                        ChatColor.BLACK + "建立防御工事"
        );

        // 第十页：阶段二介绍
        pages.add(
                ChatColor.DARK_BLUE + "§l【阶段二】\n\n" +
                        ChatColor.GOLD + "§l核心不再无敌\n\n" +
                        ChatColor.BLACK + "• 可开始破坏核心\n" +
                        ChatColor.BLACK + "• 游戏进入攻防阶段\n" +
                        ChatColor.BLACK + "• 需要保护己方核心\n\n" +
                        ChatColor.DARK_GRAY + "§l策略：" + ChatColor.BLACK + "可以继续发育，也可以破坏敌方核心\n" +
                        ChatColor.BLACK + "在骚扰的同时，注意防守"
        );

        // 第十一页：阶段三介绍
        pages.add(
                ChatColor.DARK_BLUE + "§l【阶段三】\n\n" +
                        ChatColor.DARK_AQUA + "§l游戏白热化\n\n" +
                        ChatColor.BLACK + "• " + ChatColor.DARK_AQUA + "§l钻石" + ChatColor.BLACK + "生成在地图中心\n" +
                        ChatColor.BLACK + "• 挖掘可获得更强力装备\n" +
                        ChatColor.BLACK + "• " + ChatColor.DARK_PURPLE + "§l女巫" + ChatColor.BLACK + "开始生成\n\n" +
                        ChatColor.DARK_GRAY + "§l策略：" + ChatColor.DARK_BLUE + "争夺中心资源\n" +
                        ChatColor.DARK_PURPLE + "击杀女巫获取资源制作药水"
        );

        // 第十二页：阶段四介绍
        pages.add(
                ChatColor.DARK_BLUE + "§l【阶段四】\n\n" +
                        ChatColor.BLACK + "• 可在商店购买" +
                        ChatColor.GOLD + "§l烈焰粉\n" +
                        ChatColor.BLACK + "• " + ChatColor.DARK_RED + "§lBOSS" + ChatColor.BLACK + "生成\n" +
                        ChatColor.BLACK + "• " + ChatColor.DARK_PURPLE + "§l末地传送门" + ChatColor.BLACK + "出现\n\n" +
                        ChatColor.DARK_GRAY + "§l策略：" + ChatColor.BLACK + "准备最终决战\n" +
                        ChatColor.BLACK + "击败BOSS获取优势"
        );

        // 第十三页：阶段五介绍
        pages.add(
                ChatColor.DARK_BLUE + "§l【阶段五】\n\n" +
                        ChatColor.DARK_RED + "§l最后攻势\n\n" +
                        ChatColor.BLACK + "• 挖掘核心具有" +
                        ChatColor.DARK_RED + "§l双倍伤害\n" +
                        ChatColor.BLACK + "• 游戏进入最终阶段\n" +
                        ChatColor.BLACK + "• 胜负即将揭晓\n\n" +
                        ChatColor.DARK_GRAY + "§l策略：" + ChatColor.BLACK + "全力进攻敌方核心\n" +
                        ChatColor.BLACK + "争取最终胜利"
        );

        // 第十四页：作者介绍
        pages.add(
                ChatColor.DARK_BLUE + "§l【作者介绍】\n\n" +
                        ChatColor.DARK_PURPLE + "§l插件开发：\n" + ChatColor.DARK_AQUA + "BiliBili 烧烤蒸馏水\n\n" +
                        ChatColor.DARK_GREEN + "§l非常感谢您对开源插件的支持，祝体验愉快！\n\n" +
                        ChatColor.DARK_BLUE + "插件使用者请保留此条作者声明。"
        );

        // 设置书页
        meta.setPages(pages);

        // 设置物品的描述信息（在灵魂绑定基础上添加）
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "右键打开核心战争游戏介绍",
                ChatColor.DARK_GRAY + "介绍物品",
                "", // 隔一行
                ChatColor.GOLD + "灵魂绑定 V"
        ));

        // 隐藏额外标签
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        // 将元数据应用到物品上
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 判断物品是否为核心战争游戏介绍书本
     * @param item 要判断的物品
     * @return 是则返回true，否则返回false
     */
    public static boolean isGameGuideBook(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }

        // 比较去除颜色后的显示名称
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        String identifierWithoutColor = ChatColor.stripColor(ITEM_IDENTIFIER);
        return displayName.equals(identifierWithoutColor);
    }
}