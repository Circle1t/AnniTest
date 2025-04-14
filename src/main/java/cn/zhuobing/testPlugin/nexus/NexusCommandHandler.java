package cn.zhuobing.testPlugin.nexus;

import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class NexusCommandHandler implements CommandHandler, TabCompleter {
    private final NexusManager dataManager;
    private final NexusInfoBoard nexusInfoBoard;
    private final TeamManager teamManager;

    public NexusCommandHandler(NexusManager dataManager, NexusInfoBoard nexusInfoBoard, TeamManager teamManager) {
        this.dataManager = dataManager;
        this.nexusInfoBoard = nexusInfoBoard;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("nexus") || !(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;
        if(!player.isOp()){
            sender.sendMessage(ChatColor.RED + "你没有权限执行这个命令！");
            return true;
        }
        if (args.length == 0) {
            sendUsageMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "set":
                handleSetCommand(player, args);
                return true;
            case "sethealth":
                handleSetHealthCommand(player, args);
                return true;
            case "remove":
                handleRemoveCommand(player, args);
                return true;
            case "save":
                handleSaveCommand(player,args);
                return true;
            case "setborder":
                handleSetBorderCommand(player, args);
                return true;
            default:
                player.sendMessage(ChatColor.RED + "未知子命令！用法: /nexus set <队伍> " +
                        "或 /nexus sethealth <队伍> <血量> " +
                        "或 /nexus setborder <队伍> <first/second>" +
                        "或 /nexus remove <队伍> 或 /nexus save");
                return true;
        }
    }

    private void handleSaveCommand(Player player, String[] args) {
        dataManager.saveConfig();
        player.sendMessage(ChatColor.GREEN + "核心设置保存成功！");
    }

    private void handleSetBorderCommand(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(ChatColor.RED + "用法: /nexus setborder <队伍> <first/second>");
            return;
        }

        String teamName = args[1];
        String position = args[2].toLowerCase();

        if (!teamManager.getTeamColors().containsKey(teamName)) {
            player.sendMessage(ChatColor.RED + "该队伍不存在！");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage(ChatColor.RED + "请对准要设置的边界方块！");
            return;
        }

        dataManager.setBorder(teamName, position, targetBlock.getLocation());
        player.sendMessage(ChatColor.GREEN + "成功设置 " + teamName + " 队的" + position + "边界点");
    }

    private void handleRemoveCommand(CommandSender sender, String[] args) {
        String teamName = args[1];
        Map<String, Location> nexusLocations = dataManager.getNexusLocations();
        Player player = (Player) sender;
        if(nexusLocations.containsKey(teamName)) {
            dataManager.removeNexus(teamName);
            player.sendMessage(ChatColor.GREEN + teamName + " 核心已被移除！");
        }else{
            player.sendMessage(ChatColor.RED + "用法: /nexus remove <队伍>");
        }
    }

    private void sendUsageMessage(Player player) {
        player.sendMessage(ChatColor.RED + "用法: /nexus set <队伍> 或 /nexus sethealth <队伍> <血量>");
    }

    private void handleSetCommand(Player player, String[] args) {
        String teamName = args[1];

        if (args.length != 2) {
            sendUsageMessage(player);
            return;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "你没有权限设置核心！");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage(ChatColor.RED + "未找到目标方块，请靠近目标并将准星对准方块再次尝试！");
            return;
        }

        if(!teamManager.getTeamColors().containsKey(teamName)) {
            player.sendMessage(ChatColor.RED + "此队伍名称不存在！");
            return;
        }

        if (dataManager.hasNexus(teamName)) {
            dataManager.removeNexus(teamName);
            player.sendMessage(ChatColor.GREEN + teamName + " 之前的核心已被移除！");
        }

        for(Map.Entry<String, Location> entry : dataManager.getNexusLocations().entrySet()) {
            if(entry.getValue().equals(targetBlock.getLocation())) {
                player.sendMessage(ChatColor.RED + "该位置已经存在核心了，请移除后重试！");
                return;
            }
        }

        dataManager.setNexusLocation(teamName, targetBlock.getLocation());
        nexusInfoBoard.updateInfoBoard();
        player.sendMessage(ChatColor.GREEN + teamName + " 核心设置成功！");
    }

    private void handleSetHealthCommand(Player player, String[] args) {
        if (args.length != 3) {
            sendUsageMessage(player);
            return;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "你没有权限修改核心血量！");
            return;
        }

        String teamName = args[1];
        try {
            int newHealth = Integer.parseInt(args[2]);
            if (dataManager.hasNexus(teamName)) {
                dataManager.setNexusHealth(teamName, newHealth);
                nexusInfoBoard.updateInfoBoard();
                player.sendMessage(ChatColor.GREEN + teamName + " 核心血量已设置为 " + newHealth + "！");
            } else {
                player.sendMessage(ChatColor.RED + "该队伍核心未设置，无法修改血量！");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "请输入有效的血量数值！");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("set", "sethealth", "remove", "save", "setborder");
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("sethealth") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("setborder")) {
                List<String> teamNames = new ArrayList<>(teamManager.getTeamColors().keySet());
                for (String teamName : teamNames) {
                    if (teamName.startsWith(args[1].toLowerCase())) {
                        completions.add(teamName);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setborder")) {
            List<String> positions = Arrays.asList("first", "second");
            for (String position : positions) {
                if (position.startsWith(args[2].toLowerCase())) {
                    completions.add(position);
                }
            }
        }
        return completions;
    }
}