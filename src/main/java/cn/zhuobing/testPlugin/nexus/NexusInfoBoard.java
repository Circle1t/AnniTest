package cn.zhuobing.testPlugin.nexus;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.map.MapSelectManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.text.SimpleDateFormat;
import java.util.*;

// 定义一个内部类来存储队伍信息，方便排序
class TeamInfo implements Comparable<TeamInfo> {
    String teamName;
    int health;
    String displayName;

    public TeamInfo(String teamName, int health, String displayName) {
        this.teamName = teamName;
        this.health = health;
        this.displayName = displayName;
    }

    @Override
    public int compareTo(TeamInfo other) {
        // 按照血量从高到低排序
        return Integer.compare(other.health, this.health);
    }
}

public class NexusInfoBoard {
    private final ScoreboardManager scoreboardManager;
    private final NexusManager dataManager;
    private final TeamManager teamManager;
    private final Map<String, String> teamNames;
    private final GameManager gameManager;
    private MapSelectManager mapSelectManager;
    private boolean voteFlag = true;

    public NexusInfoBoard(NexusManager dataManager, TeamManager teamManager, GameManager gameManager, MapSelectManager mapSelectManager) {
        this.dataManager = dataManager;
        this.scoreboardManager = Bukkit.getScoreboardManager();
        this.teamNames = teamManager.getEnglishToChineseMap();
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.mapSelectManager = mapSelectManager;
        gameManager.setNexusInfoBoard(this);
    }

    public void updateInfoBoard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard playerScoreboard = player.getScoreboard();

            // 获取或创建目标
            Objective objective = playerScoreboard.getObjective("nexusInfo");
            if (objective == null) {
                objective = playerScoreboard.registerNewObjective("nexusInfo", "dummy", ChatColor.GOLD + ChatColor.BOLD.toString() + "核 心 战 争");
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }

            // 清空计分板上的所有条目
            for (String entry : new ArrayList<>(playerScoreboard.getEntries())) {
                playerScoreboard.resetScores(entry);
            }

            // 计分板分数
            int score = 9;

            // 显示日期
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            String date = dateFormat.format(calendar.getTime());
            Score dateScore = objective.getScore(ChatColor.GRAY + date);
            dateScore.setScore(score);
            score--;

            // 日期下方空一行
            Score emptyLine1 = objective.getScore("    ");
            emptyLine1.setScore(score);
            score--;

            if (voteFlag) {
                // 在投票时间段内，展示地图投票数量情况
                Score voteMsg = objective.getScore(ChatColor.AQUA + "请为地图投票");
                voteMsg.setScore(score);
                score--;

                Score emptyLine = objective.getScore(" ");
                emptyLine.setScore(score);
                score--;

                List<String> candidateMaps = mapSelectManager.getCandidateMaps();
                for (String mapName : candidateMaps) {
                    int voteCount = mapSelectManager.getVoteCount(mapName);
                    String info = ChatColor.GREEN + mapSelectManager.getMapMappingName(mapName) + ChatColor.GRAY + " : " + ChatColor.YELLOW + "[ " + ChatColor.YELLOW + voteCount + " ]";
                    Score mapScore = objective.getScore(info);
                    mapScore.setScore(score);
                    score--;
                }
            } else {
                // 游戏开始，展示每个队伍的核心血量信息
                // 存储未被摧毁的队伍信息
                List<TeamInfo> aliveTeams = new ArrayList<>();
                // 存储被摧毁的队伍信息
                List<TeamInfo> destroyedTeams = new ArrayList<>();

                // 收集队伍信息
                for (Map.Entry<String, Location> entry : dataManager.getNexusLocations().entrySet()) {
                    String teamName = entry.getKey();
                    int health = dataManager.getNexusHealth(teamName);
                    String displayName = teamNames.getOrDefault(teamName, teamName);
                    if (health > 0) {
                        aliveTeams.add(new TeamInfo(teamName, health, displayName));
                    } else {
                        destroyedTeams.add(new TeamInfo(teamName, health, displayName));
                    }
                }

                // 对未被摧毁的队伍按血量从高到低排序
                Collections.sort(aliveTeams);

                // 显示未被摧毁的队伍
                for (TeamInfo teamInfo : aliveTeams) {
                    String info = teamManager.getTeamColor(teamInfo.teamName) + teamInfo.displayName + "队" + ChatColor.GREEN + " ✔  " + ChatColor.GRAY + "[ " + ChatColor.RESET + teamInfo.health + ChatColor.GRAY + " ]";
                    Score teamScore = objective.getScore(info);
                    teamScore.setScore(score);
                    score--;
                }

                // 显示被摧毁的队伍
                for (TeamInfo teamInfo : destroyedTeams) {
                    String info = teamManager.getTeamColor(teamInfo.teamName) + teamInfo.displayName + "队" + ChatColor.GRAY + " ❌  已被摧毁";
                    Score teamScore = objective.getScore(info);
                    teamScore.setScore(score);
                    score--;
                }

                Score emptyLine = objective.getScore("     ");
                emptyLine.setScore(score);
                score--;
                // 在 AnniTest logo 上一行展示“地图：” + 地图名称
                String gameMap = mapSelectManager.getGameMapMappingName();
                if (gameMap != null) {
                    Score mapNameScore = objective.getScore(ChatColor.WHITE + "地图：" + ChatColor.GREEN + gameMap);
                    mapNameScore.setScore(score);
                    score--;
                }
            }

            // 最后一个队伍下方空一行
            Score emptyLine2 = objective.getScore("  ");
            emptyLine2.setScore(score);
            score--;

            // 在下下行写上 “AnniTest”
            Score footerScore = objective.getScore(ChatColor.GOLD + "    AnniTest");
            footerScore.setScore(score);
        }
    }

    public void setMapSelectManager(MapSelectManager mapSelectManager){
        this.mapSelectManager = mapSelectManager;
    }

    public void setVoteFlag(boolean voteFlag) {
        this.voteFlag = voteFlag;
    }

}