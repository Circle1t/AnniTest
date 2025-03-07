package cn.zhuobing.testPlugin.team;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.ChatPaginator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamManager {
    private final Scoreboard scoreboard;
    private final Map<String, ChatColor> teamColors = new HashMap<>();
    private final Map<String, String> englishToChineseMap = new HashMap<>();
    private final Map<String, Integer> teamPlayerCounts = new HashMap<>();
    // 使用 UUID 作为键来存储玩家和其所在队伍英文名的关系
    private final Map<UUID, String> playerTeamMap = new HashMap<>();

    public TeamManager() {
        // 初始化计分板
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        this.scoreboard = scoreboardManager.getNewScoreboard();
        initTeams();
        setupTabList();
    }

    private void initTeams() {
        createTeam("red", ChatColor.RED, "红");
        createTeam("yellow", ChatColor.YELLOW, "黄");
        createTeam("blue", ChatColor.BLUE, "蓝");
        createTeam("green", ChatColor.GREEN, "绿");
    }

    private void createTeam(String englishName, ChatColor color, String chineseName) {
        // 创建队伍并设置颜色
        Team team = scoreboard.registerNewTeam(englishName);
        team.setPrefix(ChatColor.WHITE + "[" + color.toString() + chineseName + ChatColor.WHITE + "]" + color.toString());
        team.setColor(color);
        teamColors.put(englishName, color);
        englishToChineseMap.put(englishName, chineseName);
        // 初始化队伍的玩家数量为 0
        teamPlayerCounts.put(englishName, 0);
    }

    // 更新队伍的玩家人数
    public void updateTeamPlayerCount(String teamName, int change) {
        if (teamPlayerCounts.containsKey(teamName)) {
            int currentCount = teamPlayerCounts.get(teamName);
            teamPlayerCounts.put(teamName, currentCount + change);
        }
    }

    // 获取队伍的玩家人数
    public int getTeamPlayerCount(String teamName) {
        return teamPlayerCounts.getOrDefault(teamName, 0);
    }

    // 获取队伍玩家 Map
    public Map<String, Integer> getTeamPlayerCounts() {
        return teamPlayerCounts;
    }

    // Getter 方法
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public Map<String, ChatColor> getTeamColors() {
        return teamColors;
    }

    public ChatColor getTeamColor(String englishName) {
        return teamColors.get(englishName);
    }

    public Map<String, String> getEnglishToChineseMap() {
        return englishToChineseMap;
    }

    // 将玩家添加到队伍中，并更新映射
    public void addPlayerToTeam(Player player, String teamName) {
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.addEntry(player.getName());
            playerTeamMap.put(player.getUniqueId(), teamName);
            updateTeamPlayerCount(teamName, 1);
        }
    }

    // 将玩家从队伍中移除，并更新映射
    public void removePlayerFromTeam(Player player) {
        UUID uuid = player.getUniqueId();
        String teamName = playerTeamMap.get(uuid);
        if (teamName != null) {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.removeEntry(player.getName());
                playerTeamMap.remove(uuid);
                updateTeamPlayerCount(teamName, -1);
            }
        }
    }

    // 获取玩家所在队伍的英文名
    public String getPlayerTeamName(Player player) {
        return playerTeamMap.get(player.getUniqueId());
    }

    // 获取玩家所在的队伍（Team 类型）
    public Team getPlayerTeam(Player player) {
        String teamName = getPlayerTeamName(player);
        return teamName != null ? scoreboard.getTeam(teamName) : null;
    }

    // 判断玩家是否在任意队伍中
    public boolean isInTeam(Player player) {
        return playerTeamMap.containsKey(player.getUniqueId());
    }

    // 获取队伍中文名称
    public String getTeamChineseName(String teamName) {
        return englishToChineseMap.getOrDefault(teamName, "未知");
    }

    // 获取某个队伍的所有玩家
    public List<Player> getPlayersInTeam(String teamName) {
        List<Player> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playerTeamMap.entrySet()) {
            if (teamName.equals(entry.getValue())) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    // 判断是否是有效队伍
    public boolean isValidTeamName(String teamName) {
        return teamColors.containsKey(teamName);
    }

    private void setupTabList() {
        // 构建新的 header，在原有的 header 下方添加一行提示
        String header = ChatColor.GOLD + "核心战争\n" + ChatColor.YELLOW + "欢迎体验一个全新的核心战争！";
        // 构建新的 footer，在原有的 footer 下方添加一行提示
        String footer = ChatColor.AQUA + "📺 Bilibili 烧烤蒸馏水\n" + ChatColor.GREEN + "🐱  GitHub Circle1t/AnniTest  插件已开源";

        for (Player player : Bukkit.getOnlinePlayers()) {
            // 设置玩家的 tab 列表的头部和底部信息
            player.setPlayerListHeaderFooter(header, footer);
            // 为玩家设置计分板
            player.setScoreboard(scoreboard);
        }
    }

    public void applyScoreboardToPlayer(Player player) {
        // 构建新的 header，在原有的 header 下方添加一行提示
        String header = ChatColor.GOLD + "核心战争\n" + ChatColor.YELLOW + "欢迎体验一个全新的核心战争！";
        // 构建新的 footer，在原有的 footer 下方添加一行提示
        String footer = ChatColor.AQUA + "📺 Bilibili 烧烤蒸馏水\n" + ChatColor.GREEN + "🐱  GitHub Circle1t/AnniTest  插件已开源";
        // 设置玩家的 tab 列表的头部和底部信息
        player.setPlayerListHeaderFooter(header, footer);
        // 为玩家设置计分板
        player.setScoreboard(scoreboard);
    }

    public boolean isSameTeam(Player p1, Player p2) {
        return getPlayerTeamName(p1).equals(getPlayerTeamName(p2));
    }
}