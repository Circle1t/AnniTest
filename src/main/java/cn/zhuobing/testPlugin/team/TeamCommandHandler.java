package cn.zhuobing.testPlugin.team;

import cn.zhuobing.testPlugin.anni.RespawnDataManager;
import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.nexus.NexusInfoBoard;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.utils.MessageRenderer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TeamCommandHandler implements CommandHandler, TabCompleter {
    private final TeamManager teamManager;
    private final NexusInfoBoard nexusInfoBoard;
    private final GameManager gameManager;
    private final RespawnDataManager respawnDataManager;
    private final NexusManager nexusManager;
    private final KitManager kitManager;
    private final MessageRenderer messageRenderer;

    public TeamCommandHandler(TeamManager teamManager, NexusManager nexusManager, NexusInfoBoard nexusInfoBoard,
                              GameManager gameManager, RespawnDataManager respawnDataManager, KitManager kitManager,MessageRenderer messageRenderer) {
        this.teamManager = teamManager;
        this.nexusInfoBoard = nexusInfoBoard;
        this.gameManager = gameManager;
        this.respawnDataManager = respawnDataManager;
        this.nexusManager = nexusManager;
        this.kitManager = kitManager;
        this.messageRenderer = messageRenderer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("team")) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
            return true;
        }

        Player player = (Player) sender;
        return useTeamCommand(player, args);
    }

    public boolean useTeamCommand(Player player, String[] args) {
        int currentPhase = gameManager.getCurrentPhase();

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "用法：/team <red/yellow/blue/green/leave/random/respawn/respawnremove> [队伍名]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "respawn":
                return handleRespawnCommand(player, args);
            case "respawnremove":
                return handleRespawnCancelCommand(player, args);
            default:
                return handleOriginalCommands(player, args, currentPhase);
        }
    }

    private boolean handleRespawnCommand(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "你没有此命令的操作权限！");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "用法：/team respawn <队伍英文名>");
            return true;
        }

        String teamName = args[1].toLowerCase();
        if (!teamManager.getTeamColors().containsKey(teamName)) {
            player.sendMessage(ChatColor.RED + "无效的队伍名称！");
            return true;
        }

        Location playerLocation = player.getLocation();
        playerLocation.setWorld(player.getWorld());
        respawnDataManager.addRespawnLocation(teamName, playerLocation);
        player.sendMessage(ChatColor.GREEN + "已为 " + teamName + " 添加重生点！");
        return true;
    }

    private boolean handleRespawnCancelCommand(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "你没有此命令的操作权限！");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "用法：/team respawnremove <队伍英文名>");
            return true;
        }

        String teamName = args[1].toLowerCase();
        if (!teamManager.getTeamColors().containsKey(teamName)) {
            player.sendMessage(ChatColor.RED + "无效的队伍名称！");
            return true;
        }

        respawnDataManager.removeRespawnLocations(teamName);
        player.sendMessage(ChatColor.GREEN + "已取消 " + teamName + " 队的重生点设置！");
        return true;
    }

    private boolean handleOriginalCommands(Player player, String[] args, int currentPhase) {
        if (args.length > 1) {
            player.sendMessage(ChatColor.RED + "用法：/team <red/yellow/blue/green/leave/random>");
            return true;
        }

        // 检查队伍选择是否开放
        if (!gameManager.isTeamSelectionOpen() && currentPhase == 0) {
            player.sendMessage(ChatColor.RED + "请等待地图确定");
            return true;
        }

        String teamName = args[0].toLowerCase();
        Map<String, ChatColor> teamColors = teamManager.getTeamColors();
        Scoreboard scoreboard = teamManager.getScoreboard();

        if (teamName.equalsIgnoreCase("leave")) {
            if (teamManager.isInTeam(player) && currentPhase > 0) {
                player.sendMessage(ChatColor.RED + "游戏已经开始，你不能再更改队伍！");
                return true;
            }
            removePlayerFromTeams(player, scoreboard);
            player.sendMessage(ChatColor.GREEN + "你现在已不在任何队伍中！");
            return true;
        }

        if (teamName.equalsIgnoreCase("random")) {
            if (currentPhase >= 3) {
                player.sendMessage(ChatColor.RED + "你只能在阶段三前加入队伍！");
                return true;
            }
            if (teamManager.isInTeam(player) && currentPhase > 0) {
                player.sendMessage(ChatColor.RED + "游戏已经开始，你不能再更改队伍！");
                return true;
            }
            removePlayerFromTeams(player, scoreboard);

            String smallestTeamName = findSmallestTeam();
            if (smallestTeamName == null) {
                player.sendMessage(ChatColor.RED + "无法找到合适的队伍！");
                return true;
            }

            addPlayerToTeam(player, smallestTeamName, scoreboard, teamColors);
            return true;
        }

        if (!teamColors.containsKey(teamName)) {
            player.sendMessage(ChatColor.RED + "无效的队伍名称！");
            return true;
        }

        if (currentPhase >= 3) {
            player.sendMessage(ChatColor.RED + "你只能在阶段三前加入队伍！");
            return true;
        }

        if (teamManager.isInTeam(player) && currentPhase > 0) {
            player.sendMessage(ChatColor.RED + "游戏已经开始，你不能再更改队伍！");
            return true;
        }

        removePlayerFromTeams(player, scoreboard);
        addPlayerToTeam(player, teamName, scoreboard, teamColors);
        return true;
    }

    private void removePlayerFromTeams(Player player, Scoreboard scoreboard) {
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
                teamManager.removePlayerFromTeam(player);
                return;
            }
        }
    }

    private String findSmallestTeam() {
        Map<String, Integer> teamPlayerCounts = teamManager.getTeamPlayerCounts();
        if (teamPlayerCounts.isEmpty()) {
            return null;
        }
        String smallestTeamName = teamPlayerCounts.keySet().iterator().next();
        int minPlayers = teamPlayerCounts.get(smallestTeamName);

        for (Map.Entry<String, Integer> entry : teamPlayerCounts.entrySet()) {
            if (entry.getValue() < minPlayers) {
                minPlayers = entry.getValue();
                smallestTeamName = entry.getKey();
            }
        }
        return smallestTeamName;
    }

    private void addPlayerToTeam(Player player, String teamName, Scoreboard scoreboard, Map<String, ChatColor> teamColors) {
        Team selectedTeam = scoreboard.getTeam(teamName);
        if (selectedTeam == null) {
            player.sendMessage(ChatColor.RED + "队伍不存在！");
            return;
        }
        if(nexusManager.getNexusHealth(teamName) <= 0) {
            player.sendMessage(ChatColor.RED + "该队伍核心已被摧毁！");
            return;
        }

        //设置默认职业
        if(kitManager.getPlayerKit(player.getUniqueId()) == null){
            kitManager.setPlayerKit(player.getUniqueId(),"合成师");
        }

        selectedTeam.addEntry(player.getName());
        teamManager.addPlayerToTeam(player,teamName);

        Map<String, String> englishToChineseMap = teamManager.getEnglishToChineseMap();
        teamManager.applyScoreboardToPlayer(player);
        nexusInfoBoard.updateInfoBoard();
        //player.sendMessage(ChatColor.GREEN + "你已加入 " + teamColors.get(teamName) + englishToChineseMap.get(teamName) + "队" + ChatColor.GREEN + " ！");

        if(gameManager.getCurrentPhase() > 0){
            player.setHealth(0.0);
        }

        // 发送队伍加入消息
        player.sendMessage(" ");
        List<String> welcomeMessage = messageRenderer.formatMessage(
                messageRenderer.getTeamMessage(teamName),
                ChatColor.GREEN + "你已加入 " + teamColors.get(teamName) + englishToChineseMap.get(teamName) + "队" + ChatColor.GREEN + " ！"
        );
        for (String line : welcomeMessage) {
            player.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> mainCommands = Arrays.asList("red", "yellow", "blue", "green", "leave", "random", "respawn", "respawnremove");
            for (String cmd : mainCommands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("respawn") || args[0].equalsIgnoreCase("respawnremove"))) {
            List<String> teamNames = new ArrayList<>(teamManager.getTeamColors().keySet());
            for (String teamName : teamNames) {
                if (teamName.startsWith(args[1].toLowerCase())) {
                    completions.add(teamName);
                }
            }
        }
        return completions;
    }
}