package cn.zhuobing.testPlugin.boss;

import cn.zhuobing.testPlugin.command.CommandHandler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WitchCommandHandler implements CommandHandler {
    private final WitchDataManager witchDataManager;

    public WitchCommandHandler(WitchDataManager witchDataManager) {
        this.witchDataManager = witchDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("witch")) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "用法：/witch <set/remove> <队伍英文名>");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String teamName = args[1].toLowerCase();

        if (subCommand.equals("set")) {
            Location playerLocation = player.getLocation();
            witchDataManager.setWitchLocation(teamName, playerLocation);
            player.sendMessage(ChatColor.GREEN + "已为 " + teamName + " 队设置女巫重生点！");
        } else if (subCommand.equals("remove")) {
            if (witchDataManager.hasWitchLocation(teamName)) {
                witchDataManager.removeWitchLocation(teamName);
                player.sendMessage(ChatColor.GREEN + "已移除 " + teamName + " 队的女巫重生点！");
            } else {
                player.sendMessage(ChatColor.RED + teamName + " 队没有设置女巫重生点！");
            }
        } else {
            player.sendMessage(ChatColor.RED + "无效的子命令！用法：/witch <set/remove> <队伍英文名>");
        }

        return true;
    }
}