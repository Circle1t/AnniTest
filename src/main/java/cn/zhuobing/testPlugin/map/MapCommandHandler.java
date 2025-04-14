package cn.zhuobing.testPlugin.map;

import cn.zhuobing.testPlugin.command.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapCommandHandler implements CommandHandler, TabCompleter {
    private final BorderManager borderManager;
    private final LobbyManager lobbyManager;
    private final MapSelectManager mapSelectManager;

    public MapCommandHandler(BorderManager borderManager, LobbyManager lobbyManager, MapSelectManager mapSelectManager) {
        this.borderManager = borderManager;
        this.lobbyManager = lobbyManager;
        this.mapSelectManager = mapSelectManager;
    }

    /**
     * 处理命令
     * @param sender 命令发送者
     * @param command 命令对象
     * @param label 命令标签
     * @param args 命令参数
     * @return 命令处理结果
     */
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
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        if (player.getWorld().getName().equalsIgnoreCase(lobbyManager.getLobbyWorld().getName())) {
            player.sendMessage(ChatColor.RED + "你不能在大厅中使用此命令！");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("leave")) {
            World world = player.getWorld();
            List<Player> players = world.getPlayers();
            for (Player player1 : players) {
                if(lobbyManager.teleportToLobby(player)){
                    mapSelectManager.removeEditingPlayer(player);
                    player1.sendMessage(ChatColor.GREEN + "你已被传送回大厅！");
                }else{
                    player1.sendMessage(ChatColor.RED + "大厅传送失败！");
                }
            }
            if(world.getPlayers().isEmpty()){
                mapSelectManager.unloadMap(world.getName());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("setborder")) {
            if(args.length != 2){
                player.sendMessage(ChatColor.RED + "用法：/annimap setborder 1/2/3/4");
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
        }

        // 添加设置地图名称映射的指令处理逻辑
        if (args[0].equalsIgnoreCase("setmapname")) {
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "用法: /annimap setmapname <地图名>");
                return true;
            }
            String mapName = player.getWorld().getName();
            String mappingName = args[1];
            mapSelectManager.setMappingName(mapName, mappingName);
            player.sendMessage(ChatColor.GREEN + "已将地图 " + mapName + " 的映射名设置为 " + mappingName);
            return true;
        }

        // 添加设置地图标志的指令处理逻辑
        if (args[0].equalsIgnoreCase("setmapicon")) {
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "用法: /annimap setmapicon <材质名>");
                return true;
            }
            String mapName = player.getWorld().getName();
            String materialName = args[1];
            try {
                Material material = Material.valueOf(materialName);
                mapSelectManager.setMapIcon(mapName, material);
                player.sendMessage(ChatColor.GREEN + "已将地图 " + mapName + " 的标志设置为 " + materialName);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "无效的材质名！请确保材质名在 Material 中存在。");
            }
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("leave", "setborder", "setmapname", "setmapicon");
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setborder")) {
                List<String> borderNumbers = Arrays.asList("1", "2", "3", "4");
                for (String borderNumber : borderNumbers) {
                    if (borderNumber.startsWith(args[1].toLowerCase())) {
                        completions.add(borderNumber);
                    }
                }
            }
        }
        return completions;
    }
}