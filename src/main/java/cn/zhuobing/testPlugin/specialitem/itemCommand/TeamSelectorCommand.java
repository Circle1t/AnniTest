// 新建SpecialItemCommand.java
package cn.zhuobing.testPlugin.specialitem.itemCommand;

import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.specialitem.items.TeamSelectorItem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamSelectorCommand implements CommandHandler {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("getteamstar")) return false;

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if(!player.isOp()) return true;

            player.getInventory().addItem(TeamSelectorItem.createTeamStar());
            player.sendMessage(ChatColor.GREEN + "已获得队伍选择之星！");
        }
        return true;
    }
}