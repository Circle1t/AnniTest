package cn.zhuobing.testPlugin.game;

import cn.zhuobing.testPlugin.AnniTest;
import cn.zhuobing.testPlugin.boss.BossDataManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;

public class GameManager {
    private final GamePhaseManager phaseManager;
    private int remainingTime;
    private BossBar bossBar;
    private boolean gameStarted = false;
    private GameCountdownTask countdownTask;
    private TeamManager teamManager;
    private BossDataManager bossDataManager;
    private int currentPhase = 0;
    private boolean gameOver = false;

    public GameManager(TeamManager teamManager,BossDataManager bossDataManager) {
        this.teamManager = teamManager;
        this.bossDataManager = bossDataManager;
        phaseManager = new GamePhaseManager();
        bossBar = Bukkit.createBossBar(ChatColor.YELLOW + "您正在游玩 核心战争" + ChatColor.RESET + "  |  " + ChatColor.AQUA + "请等待游戏启动...", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        remainingTime = phaseManager.getPhase(currentPhase).getDuration();
    }

    public void startGame() {
        if (gameStarted) {
            return;
        }
        gameStarted = true;
        startCountdown();
    }

    private void startCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        countdownTask = new GameCountdownTask(this, bossBar, remainingTime, currentPhase);
        countdownTask.runTaskTimer(AnniTest.getInstance(), 0L, 20L);
    }

    public void updateBossBar(int phaseIndex, int remainingTime) {
        GamePhase phase = phaseManager.getPhase(phaseIndex);
        if (phase == null) return;

        int totalDuration = phase.getDuration();
        // 计算进度，处理总时间为0的情况
        double progress = (totalDuration > 0) ? (double) remainingTime / totalDuration : 0.0;
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress))); // 确保进度在0-1之间

        // 设置标题和时间显示
        String timeFormat = "%02d:%02d";
        String timeDisplay = String.format(timeFormat, remainingTime / 60, remainingTime % 60);
        bossBar.setTitle(ChatColor.GOLD + "核心战争" + ChatColor.RESET + "  |  " + ChatColor.AQUA + phase.getName() + ChatColor.RESET + "  |  " + ChatColor.WHITE + timeDisplay);
        bossBar.setColor(phase.getColor()); // 使用当前阶段的颜色
    }

    public GamePhaseManager getPhaseManager() {
        return phaseManager;
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(int currentPhase) {
        this.currentPhase = currentPhase;
        this.remainingTime = phaseManager.getPhase(currentPhase).getDuration();
        updateBossBar(currentPhase, remainingTime);

        startCountdown();

        // 当游戏进入阶段 1 时，杀死所有选择了队伍的玩家
        if (currentPhase == 1) {
            killPlayersInTeams();
        }
        // 当游戏进入阶段 4 时, 出现boss
        if (currentPhase == 4) {
            bossDataManager.spawnBoss();
        }

    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void endGameWithWinner(String winningTeam) {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        //设置获胜者判定值
        gameOver = true;
        Map<String, String> englishToChineseMap = teamManager.getEnglishToChineseMap();
        String cnWinnerTeam = englishToChineseMap.get(winningTeam);
        bossBar.setTitle(ChatColor.GOLD + ChatColor.BOLD.toString() + "游戏结束！" + teamManager.getTeamColor(winningTeam) + cnWinnerTeam + "队" + ChatColor.GOLD + " 获胜");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "游戏结束！" + teamManager.getTeamColor(winningTeam) + cnWinnerTeam + "队" + ChatColor.GOLD + " 获胜");
            // 显示 title
            player.sendTitle(ChatColor.GOLD + ChatColor.BOLD.toString() + "游 戏 结 束", teamManager.getTeamColor(winningTeam) + cnWinnerTeam + "队" + ChatColor.GOLD + " 获得胜利", 10, 70, 20);
            // 获胜者音效
            if (isPlayerInTeam(player, winningTeam)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
            }
        }
        bossBar.setColor(BarColor.WHITE);
    }

    // 判断玩家是否在指定队伍
    private boolean isPlayerInTeam(Player player, String teamName) {
        return teamManager.getScoreboard().getTeam(teamName).hasEntry(player.getName());
    }

    // 杀死所有选择了队伍的玩家
    private void killPlayersInTeams() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (teamManager.isInTeam(player)) {
                player.setHealth(0);
            }
        }
    }

    public void setBossDataManager(BossDataManager bossDataManager) {
        this.bossDataManager = bossDataManager;
    }

    public boolean isGameOver(){
        return gameOver;
    }
}