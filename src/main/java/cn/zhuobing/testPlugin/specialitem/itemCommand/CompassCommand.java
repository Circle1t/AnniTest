package cn.zhuobing.testPlugin.specialitem.itemCommand;

import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CompassCommand implements CommandHandler {
    private final TeamManager teamManager;
    public CompassCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("compass")) return false;

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (!teamManager.isInTeam(player)) {
                player.sendMessage(ChatColor.RED + "你需要加入队伍才能获得指南针！");
                return true;
            }

            player.getInventory().addItem(CompassItem.createCompass());
            player.sendMessage(ChatColor.GREEN + "已获得核心指南针！");
        }
        return true;
    }
}