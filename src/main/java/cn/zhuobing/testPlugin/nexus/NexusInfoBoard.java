package cn.zhuobing.testPlugin.nexus;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.map.MapSelectManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

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
    private static final long UPDATE_THROTTLE_MS = 600L;  // 挖核心时最多约 0.6 秒更新一次，减轻卡顿
    private static final int MAX_SIDEBAR_LINES = 20;      // 计分板最大行数，用 Team 占位避免 getEntries/resetScores

    /** 每行用不同长度空格作为唯一占位符（1～20 个空格），仅用于区分条目，真实内容在 Team prefix 中显示 */
    private static final String[] LINE_ENTRIES = new String[MAX_SIDEBAR_LINES];
    static {
        for (int i = 0; i < MAX_SIDEBAR_LINES; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j <= i; j++) sb.append(' ');
            LINE_ENTRIES[i] = sb.toString();
        }
    }

    private final NexusManager dataManager;
    private final TeamManager teamManager;
    private final Map<String, String> teamNames;
    private final GameManager gameManager;
    private final Plugin plugin;
    private MapSelectManager mapSelectManager;
    private boolean voteFlag = true;

    private volatile long lastUpdateTime = 0L;
    private volatile BukkitTask pendingUpdateTask = null;
    private volatile String cachedDateLine = "";
    private volatile long cachedDateMs = 0L;

    public NexusInfoBoard(NexusManager dataManager, TeamManager teamManager, GameManager gameManager, MapSelectManager mapSelectManager, Plugin plugin) {
        this.dataManager = dataManager;
        this.teamNames = teamManager.getEnglishToChineseMap();
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.mapSelectManager = mapSelectManager;
        this.plugin = plugin;
        gameManager.setNexusInfoBoard(this);
    }

    /**
     * 请求更新计分板。始终推迟到下一 tick 执行，不在事件线程做任何计分板逻辑，挖核心时明显减轻卡顿。
     * 若距上次更新不足 throttle 则节流，最多约 0.6 秒更新一次。
     */
    public void updateInfoBoard() {
        Bukkit.getScheduler().runTaskLater(plugin, this::doThrottledUpdate, 1L);
    }

    private void doThrottledUpdate() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime >= UPDATE_THROTTLE_MS) {
            lastUpdateTime = now;
            doUpdateInfoBoard();
            return;
        }
        if (pendingUpdateTask == null || pendingUpdateTask.isCancelled()) {
            long delayMs = UPDATE_THROTTLE_MS - (now - lastUpdateTime);
            int delayTicks = Math.max(1, (int) (delayMs / 50));
            pendingUpdateTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                pendingUpdateTask = null;
                lastUpdateTime = System.currentTimeMillis();
                doUpdateInfoBoard();
            }, delayTicks);
        }
    }

    /**
     * 使用共享计分板 + Team prefix 更新，只更新一行行文本，不调用 getEntries/resetScores，单 tick 完成。
     */
    private void doUpdateInfoBoard() {
        List<ScoreLine> content = buildContent();
        if (content.isEmpty()) return;

        Scoreboard sb = teamManager.getScoreboard();
        Objective objective = ensureObjective(sb);
        ensureLineTeams(sb);

        int n = Math.min(content.size(), MAX_SIDEBAR_LINES);
        for (int i = 0; i < n; i++) {
            ScoreLine line = content.get(i);
            org.bukkit.scoreboard.Team team = sb.getTeam("nexus_" + i);
            if (team != null) {
                team.setPrefix(line.text.length() > 64 ? line.text.substring(0, 64) : line.text);
                objective.getScore(LINE_ENTRIES[i]).setScore(line.score);
            }
        }
        for (int i = n; i < MAX_SIDEBAR_LINES; i++) {
            sb.resetScores(LINE_ENTRIES[i]);
            org.bukkit.scoreboard.Team team = sb.getTeam("nexus_" + i);
            if (team != null) team.setPrefix("");
        }
    }

    private Objective ensureObjective(Scoreboard sb) {
        Objective objective = sb.getObjective("nexusInfo");
        if (objective == null) {
            objective = sb.registerNewObjective(
                    "nexusInfo",
                    "dummy",
                    ChatColor.RED + ChatColor.BOLD.toString() + "核" + ChatColor.YELLOW + ChatColor.BOLD.toString() + "心"
                            + ChatColor.BLUE + ChatColor.BOLD.toString() + "战" + ChatColor.GREEN + ChatColor.BOLD.toString() + "争"
                            + ChatColor.GOLD + ChatColor.BOLD.toString() + " 重制版"
            );
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        return objective;
    }

    private void ensureLineTeams(Scoreboard sb) {
        for (int i = 0; i < MAX_SIDEBAR_LINES; i++) {
            String teamId = "nexus_" + i;
            org.bukkit.scoreboard.Team team = sb.getTeam(teamId);
            if (team == null) {
                team = sb.registerNewTeam(teamId);
                team.addEntry(LINE_ENTRIES[i]);
            }
        }
    }

    /** 每轮只构建一次内容，避免对每个玩家重复排序、拼串。日期行约 1 秒缓存一次。 */
    private List<ScoreLine> buildContent() {
        List<ScoreLine> out = new ArrayList<>();
        int score = 9;

        long now = System.currentTimeMillis();
        if (cachedDateLine.isEmpty() || now - cachedDateMs > 1000L) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            cachedDateLine = ChatColor.GRAY + dateFormat.format(new Date(now));
            cachedDateMs = now;
        }
        out.add(new ScoreLine(score--, cachedDateLine));
        out.add(new ScoreLine(score--, "    "));

        if (voteFlag) {
            out.add(new ScoreLine(score--, ChatColor.AQUA + "请为地图投票"));
            out.add(new ScoreLine(score--, " "));
            if (mapSelectManager != null) {
                for (String mapName : mapSelectManager.getCandidateMaps()) {
                    int voteCount = mapSelectManager.getVoteCount(mapName);
                    String info = ChatColor.WHITE + mapSelectManager.getMapMappingName(mapName) + ChatColor.GRAY + " : " + ChatColor.GRAY + "( " + ChatColor.WHITE + voteCount + ChatColor.GRAY + " )";
                    out.add(new ScoreLine(score--, info));
                }
            }
        } else {
            List<TeamInfo> aliveTeams = new ArrayList<>();
            List<TeamInfo> destroyedTeams = new ArrayList<>();
            for (String teamName : dataManager.getNexusLocations().keySet()) {
                int health = dataManager.getNexusHealth(teamName);
                String displayName = teamNames.getOrDefault(teamName, teamName);
                if (health > 0) aliveTeams.add(new TeamInfo(teamName, health, displayName));
                else destroyedTeams.add(new TeamInfo(teamName, health, displayName));
            }
            Collections.sort(aliveTeams);
            for (TeamInfo t : aliveTeams) {
                String info = teamManager.getTeamColor(t.teamName) + t.displayName + "队" + ChatColor.GREEN + " ✔  " + ChatColor.GRAY + "[ " + ChatColor.RESET + t.health + ChatColor.GRAY + " ]";
                out.add(new ScoreLine(score--, info));
            }
            for (TeamInfo t : destroyedTeams) {
                String info = teamManager.getTeamColor(t.teamName) + t.displayName + "队" + ChatColor.GRAY + " ❌  已被摧毁";
                out.add(new ScoreLine(score--, info));
            }
            out.add(new ScoreLine(score--, "     "));
            if (mapSelectManager != null) {
                String gameMap = mapSelectManager.getGameMapMappingName();
                if (gameMap != null) {
                    out.add(new ScoreLine(score--, ChatColor.WHITE + "地图：" + ChatColor.GREEN + gameMap));
                }
            }
        }

        out.add(new ScoreLine(score--, "  "));
        out.add(new ScoreLine(score, ChatColor.YELLOW + "内测版本 2.15.3"));
        return out;
    }

    private static class ScoreLine {
        final int score;
        final String text;
        ScoreLine(int score, String text) {
            this.score = score;
            this.text = text;
        }
    }

    /** 供需要立即刷新的场景使用（如阶段切换、玩家加入），不受 0.5 秒节流限制。 */
    public void updateInfoBoardImmediate() {
        if (pendingUpdateTask != null && !pendingUpdateTask.isCancelled()) {
            pendingUpdateTask.cancel();
            pendingUpdateTask = null;
        }
        lastUpdateTime = System.currentTimeMillis();
        doUpdateInfoBoard();
    }

    public void setMapSelectManager(MapSelectManager mapSelectManager){
        this.mapSelectManager = mapSelectManager;
    }

    public void setVoteFlag(boolean voteFlag) {
        this.voteFlag = voteFlag;
    }

}