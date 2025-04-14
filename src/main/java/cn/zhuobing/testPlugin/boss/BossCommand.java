package cn.zhuobing.testPlugin.boss;

import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BossCommand implements CommandHandler, TabCompleter {
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

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家能执行这个命令！");
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

        // 不用进入boss点就能执行的命令
        if(args.length == 1 && args[0].equalsIgnoreCase("enter")) {
            bossDataManager.enterBossMap(player);
            return true;
        }
        if(args.length == 1 && args[0].equalsIgnoreCase("spawn")){
            bossDataManager.spawnBossManually(player);
            return true;
        }
        if(args.length == 1 && args[0].equalsIgnoreCase("clear")){
            bossDataManager.clearBoss();
            player.sendMessage(ChatColor.GREEN + "boss已清除");
            return true;
        }

        if(!player.getWorld().getName().equals("AnniBoss")){
            player.sendMessage(ChatColor.RED + "请进入boss地图后再尝试此命令！");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            String teamName = args[1].toLowerCase();
            setBossLocationForTeam(player, teamName);
            return true;
        }
        else if (args.length == 1 && args[0].equalsIgnoreCase("set")) {
            setBossLocation(player);
            return true;
        }
        else if(args.length == 1 && args[0].equalsIgnoreCase("leave")) {
            bossDataManager.leaveBossMap(player);
            return true;
        }

        return true;
    }

    private void setBossLocationForTeam(Player player, String teamName) {
        if (!teamManager.isValidTeamName(teamName)) {
            player.sendMessage(ChatColor.RED + "无效的队伍名称！");
            return;
        }

        Location currentLocation = player.getLocation();
        bossDataManager.setBossTeamTpLocation(teamName, currentLocation);
        player.sendMessage(ChatColor.GREEN + "成功设置 " + teamName + " 队的 Boss 传送点！");
    }

    private void setBossLocation(Player player) {
        Location loc = player.getLocation();
        bossDataManager.setBossSpawnLocation(loc);
        player.sendMessage(ChatColor.GREEN + "成功设置Boss生成位置！");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> mainCommands = Arrays.asList("tp", "set", "spawn", "clear", "enter", "leave");
            for (String cmd : mainCommands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            // 假设 teamManager 有获取所有有效队伍名称的方法
            List<String> teamNames = teamManager.getAllTeamNames();
            for (String teamName : teamNames) {
                if (teamName.startsWith(args[1].toLowerCase())) {
                    completions.add(teamName);
                }
            }
        }
        return completions;
    }
}