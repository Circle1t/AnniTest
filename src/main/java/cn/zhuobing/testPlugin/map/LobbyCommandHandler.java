package cn.zhuobing.testPlugin.map;

import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.map.LobbyManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommandHandler implements CommandHandler {
    private final LobbyManager lobbyManager;

    public LobbyCommandHandler(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("lobby")) {
            return false;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家能执行这个命令！");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 1 && args[0].equalsIgnoreCase("respawn")) {
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "你没有权限执行这个命令！");
                return true;
            }
            if(!player.getWorld().getName().equals(lobbyManager.getLobbyWorld().getName())){
                player.sendMessage(ChatColor.RED + "你只能在大厅中执行这个命令！");
                return true;
            }
            lobbyManager.addRespawnPoint(player.getLocation());
            player.sendMessage(ChatColor.GREEN + "成功设置大厅重生点！");
            return true;
        }
        return false;
    }
}