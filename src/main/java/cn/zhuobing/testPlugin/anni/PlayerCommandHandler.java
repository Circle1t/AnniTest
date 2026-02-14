package cn.zhuobing.testPlugin.anni;

import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.specialitem.items.GuideBook;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerCommandHandler implements CommandHandler {

    private static final String HELP_HEADER = ChatColor.GOLD + "§l——— 核心战争 玩家指令 ———";
    private static final String CMD = ChatColor.YELLOW.toString();
    private static final String DESC = ChatColor.WHITE.toString();
    private static final String AUTHOR_LINE = ChatColor.GRAY + "插件开发：BiliBili " + ChatColor.AQUA + "烧烤蒸馏水" + ChatColor.GRAY + "（灼冰/Circle1t）";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().equalsIgnoreCase("annihelp") ? "annihelp" : null;
        if (name == null && !command.getName().equalsIgnoreCase("kl") && !command.getName().equalsIgnoreCase("suicide")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
            return true;
        }

        Player player = (Player) sender;

        if ("annihelp".equals(name)) {
            if (args.length > 0 && "book".equalsIgnoreCase(args[0])) {
                ItemStack book = GuideBook.createGameGuideBook();
                player.getInventory().addItem(book).values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
                player.sendMessage(ChatColor.GREEN + "已获得游戏教程书！");
                return true;
            }
            sendHelp(player);
            return true;
        }

        // /kl 或 /suicide
        player.setHealth(0.0);
        return true;
    }

    /** 向玩家发送面向玩家的游戏指令帮助（紧凑排版、亮色）。 */
    private void sendHelp(Player player) {
        player.sendMessage(HELP_HEADER);
        player.sendMessage(CMD + "/team <红/黄/蓝/绿/random/leave> " + DESC + "→ 加入或离开队伍");
        player.sendMessage(CMD + "/compass " + DESC + "→ 获得指向敌方核心的指南针");
        player.sendMessage(CMD + "/kl 或 /suicide " + DESC + "→ 快速重生");
        player.sendMessage(CMD + "/annihelp book " + DESC + "→ 获取游戏教程书");
        player.sendMessage(DESC + "聊天前加 " + ChatColor.AQUA + "!" + DESC + " 或 " + ChatColor.AQUA + "@ " + DESC + "→ 全体说话");
        player.sendMessage(AUTHOR_LINE);
    }
}