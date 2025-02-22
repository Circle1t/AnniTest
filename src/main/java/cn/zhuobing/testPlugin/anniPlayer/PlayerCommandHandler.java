package cn.zhuobing.testPlugin.anniPlayer;

import cn.zhuobing.testPlugin.command.CommandHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerCommandHandler implements CommandHandler {

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查命令名称是否为 /kl
        if (!command.getName().equalsIgnoreCase("kl") && !command.getName().equalsIgnoreCase("suicide")) {
            return false;
        }

        // 检查命令发送者是否为玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
            return true;
        }

        Player player = (Player) sender;
        // 将玩家的血量设置为 0
        player.setHealth(0.0);
        //player.sendMessage(ChatColor.GREEN + "你的血量已被设置为 0！");
        return true;
    }
}