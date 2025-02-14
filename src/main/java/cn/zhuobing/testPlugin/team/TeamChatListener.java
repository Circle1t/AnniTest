package cn.zhuobing.testPlugin.team;

import cn.zhuobing.testPlugin.game.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TeamChatListener implements Listener {
    private final TeamManager teamManager;
    private final GameManager gameManager;

    public TeamChatListener(TeamManager teamManager, GameManager gameManager) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        int currentPhase = gameManager.getCurrentPhase();
        Team team = teamManager.getPlayerTeam(player);
        String message = event.getMessage();

        if (team != null) {
            ChatColor teamColor = teamManager.getTeamColor(team.getName());
            String chineseName = teamManager.getTeamChineseName(team.getName());

            if (currentPhase == 0) {
                String format = ChatColor.GRAY + "[" + teamColor + chineseName + ChatColor.GRAY + "] "
                        + "%s" + ChatColor.RESET + ": " + message;
                event.setFormat(format);
                return;
            }
            if (message.startsWith("!") || message.startsWith("！") || message.startsWith("@")) {
                // 消息开头是！或! 或@ ，让全局玩家看到消息
                message = message.substring(1);
                String format = ChatColor.GOLD + "(全局) " + ChatColor.GRAY + "[" + teamColor + chineseName + ChatColor.GRAY + "] "
                        + "%s" + ChatColor.RESET + ": " + message;
                event.setFormat(format);
            } else {
                // 消息开头不是！或! 或@，只让同队伍的玩家看到消息
                String format = ChatColor.GRAY + "[" + teamColor + chineseName + ChatColor.GRAY + "] "
                        + ChatColor.GRAY + player.getName() + ChatColor.RESET + ": " + message;
                event.setCancelled(true); // 取消原聊天事件
                for(Player player1 : teamManager.getPlayersInTeam(team.getName())){
                    player1.sendMessage(format);
                }
            }
            return;
        }

        // 玩家在大厅，让所有玩家看到消息
        String format = ChatColor.GRAY + "[" + ChatColor.DARK_PURPLE + "大厅" + ChatColor.GRAY + "] "
                + "%s" + ChatColor.RESET + ": %s";
        event.setFormat(format);

    }
}