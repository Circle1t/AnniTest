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
        if (sender instanceof Player) {
            if(args.length == 1 && args[0].equalsIgnoreCase("respawn")) {
                Player player = (Player) sender;
                lobbyManager.addRespawnPoint(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "成功设置大厅重生点！");
                return true;
            }
        }
        return false;
    }
}