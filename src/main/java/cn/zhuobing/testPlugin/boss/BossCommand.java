package cn.zhuobing.testPlugin.boss;

import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class BossCommand implements CommandHandler {
    private final BossDataManager bossDataManager;
    private final TeamManager teamManager;

    public BossCommand(BossDataManager bossDataManager, TeamManager teamManager) {
        this.bossDataManager = bossDataManager;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("boss")) {
            return false;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }

        if (args.length > 2) {
            player.sendMessage(ChatColor.RED + "用法: /boss tp <队伍名称> 或 /boss set/spawn/clear");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            String teamName = args[1].toLowerCase();
            setBossLocationForTeam(player, teamName);
        }
        else if (args.length == 1 && args[0].equalsIgnoreCase("set")) {
            setBossLocation(player);
        }
        else if(args.length == 1 && args[0].equalsIgnoreCase("spawn")){
            bossDataManager.spawnBossManually(player);
        }
        else if(args.length == 1 && args[0].equalsIgnoreCase("clear")){
            bossDataManager.clearBoss();
            player.sendMessage(ChatColor.GREEN + "boss已清除");
        }

        return true;
    }

    private void setBossLocationForTeam(Player player, String teamName) {
        if (!teamManager.isValidTeamName(teamName)) {
            player.sendMessage(ChatColor.RED + "无效的队伍名称！");
            return;
        }

        Location currentLocation = player.getLocation();
        bossDataManager.setBossLocation(teamName, currentLocation);
        player.sendMessage(ChatColor.GREEN + "成功设置 " + teamName + " 队的 Boss 传送点！");
    }

    private void setBossLocation(Player player) {
        Location currentLocation = player.getLocation();
        bossDataManager.setBossLocation(teamManager.getPlayerTeamName(player),currentLocation);
        player.sendMessage(ChatColor.GREEN + "成功设置 Boss 的生成位置！");
    }
}