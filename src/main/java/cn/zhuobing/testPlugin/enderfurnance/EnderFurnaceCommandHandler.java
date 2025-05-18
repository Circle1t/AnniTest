package cn.zhuobing.testPlugin.enderfurnance;

import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class EnderFurnaceCommandHandler implements CommandHandler, TabCompleter {
    private final EnderFurnaceManager manager;
    private final TeamManager teamManager;

    public EnderFurnaceCommandHandler(EnderFurnaceManager manager, TeamManager teamManager) {
        this.manager = manager;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("enderfurnace")) return false;
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        if(!player.isOp()){
            player.sendMessage(ChatColor.RED + "你没有执行此命令的权限！");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法: /enderfurnace set <队伍> 或 /enderfurnace remove");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            handleSetCommand(player, args);
        } else if (args[0].equalsIgnoreCase("remove")) {
            handleRemoveCommand(player);
        }
        return true;
    }

    private void handleSetCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "请指定队伍名称");
            return;
        }

        Block target = player.getTargetBlockExact(10);
        if (target == null || target.getType() != org.bukkit.Material.FURNACE) {
            player.sendMessage(ChatColor.RED + "请对准一个高炉方块");
            return;
        }

        String teamName = args[1].toLowerCase();
        if (!teamManager.isValidTeamName(teamName)) {
            player.sendMessage(ChatColor.RED + "无效的队伍名称");
            return;
        }

        manager.setTeamFurnace(teamName, target.getLocation());
        player.sendMessage(ChatColor.GREEN + "成功设置" + teamName + "队伍的末影高炉");
    }

    private void handleRemoveCommand(Player player) {
        Block target = player.getTargetBlockExact(10);
        if (target == null || target.getType() != org.bukkit.Material.FURNACE) {
            player.sendMessage(ChatColor.RED + "请对准一个高炉方块");
            return;
        }

        if (manager.removeFurnace(target.getLocation())) {
            player.sendMessage(ChatColor.GREEN + "成功移除末影高炉");
        } else {
            player.sendMessage(ChatColor.RED + "这不是一个末影高炉");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "remove");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return teamManager.getAllTeamNames();
        }
        return null;
    }
}