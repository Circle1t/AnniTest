package cn.zhuobing.testPlugin.map;

import cn.zhuobing.testPlugin.command.CommandHandler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MapCommandHandler implements CommandHandler {
    private final BorderManager borderManager;

    public MapCommandHandler(BorderManager borderManager) {
        this.borderManager = borderManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("annimap")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "用法：/annimap setborder 1/2/3/4");
            return true;
        }

        if (!args[0].equalsIgnoreCase("setborder")) {
            player.sendMessage(ChatColor.RED + "无效的子命令！用法：/annimap setborder 1/2/3/4");
            return true;
        }

        try {
            int borderNumber = Integer.parseInt(args[1]);
            if (borderNumber < 1 || borderNumber > 4) {
                player.sendMessage(ChatColor.RED + "边界编号必须是 1 到 4 之间的整数！");
                return true;
            }

            Location targetBlockLocation = player.getTargetBlockExact(5).getLocation();
            borderManager.setBorder(borderNumber, targetBlockLocation);
            player.sendMessage(ChatColor.GREEN + "已设置边界 " + borderNumber + "！");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "边界编号必须是整数！");
        } catch (NullPointerException e) {
            player.sendMessage(ChatColor.RED + "未找到目标方块！");
        }

        return true;
    }
}