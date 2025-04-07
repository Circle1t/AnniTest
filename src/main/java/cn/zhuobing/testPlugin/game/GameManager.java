package cn.zhuobing.testPlugin.game;

import cn.zhuobing.testPlugin.AnniTest;
import cn.zhuobing.testPlugin.boss.BossDataManager;
import cn.zhuobing.testPlugin.boss.WitchDataManager;
import cn.zhuobing.testPlugin.map.MapSelectManager;
import cn.zhuobing.testPlugin.nexus.NexusInfoBoard;
import cn.zhuobing.testPlugin.ore.OreManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;

public class GameManager {
    private final GamePhaseManager phaseManager;
    private MapSelectManager mapSelectManager;
    private int remainingTime;
    private BossBar bossBar;
    private boolean gameStarted = false;
    private GameCountdownTask countdownTask;
    private TeamManager teamManager;
    private BossDataManager bossDataManager;
    private OreManager oreManager;
    private WitchDataManager witchDataManager;
    private NexusInfoBoard nexusInfoBoard;
    private int currentPhase = 0;
    private boolean gameOver = false;
    private boolean teamSelectionOpen = false;
    private int halfTime;

    public GameManager(TeamManager teamManager, MapSelectManager mapSelectManager,
                       BossDataManager bossDataManager, OreManager oreManager, WitchDataManager witchDataManager, NexusInfoBoard nexusInfoBoard) {
        this.teamManager = teamManager;
        this.mapSelectManager = mapSelectManager;
        this.bossDataManager = bossDataManager;
        this.oreManager = oreManager;
        this.witchDataManager = witchDataManager;
        this.nexusInfoBoard = nexusInfoBoard;
        phaseManager = new GamePhaseManager();
        bossBar = Bukkit.createBossBar(ChatColor.YELLOW + "您正在游玩 核心战争" + ChatColor.RESET + "  |  " + ChatColor.AQUA + "请等待游戏启动...", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        remainingTime = phaseManager.getPhase(currentPhase).getDuration();
        halfTime = remainingTime / 2; // 计算倒计时一半的时间
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

        // 检查是否到达一半时间，开放队伍选择
        if (phaseIndex == 0 && remainingTime < halfTime) {
            teamSelectionOpen = true;
            bossBar.setTitle(ChatColor.GOLD + "核心战争" + ChatColor.RESET + "  |  " + ChatColor.GREEN + "请选择你的队伍" + ChatColor.RESET + "  |  " + ChatColor.WHITE + timeDisplay);
        }else if(phaseIndex == 0 && remainingTime <= halfTime + 5) {
            if(remainingTime == halfTime) {
                // 地图确定，禁止投票
                bossBar.setTitle(ChatColor.GOLD + "核心战争" + ChatColor.RESET + "  |  " + ChatColor.LIGHT_PURPLE + "地图已锁定" + ChatColor.RESET + "  |  " + ChatColor.WHITE + timeDisplay);
                mapSelectManager.lockVoting();
            }else {
                bossBar.setTitle(ChatColor.GOLD + "核心战争" + ChatColor.RESET + "  |  " + ChatColor.RED + "地图即将锁定..." + ChatColor.RESET + "  |  " + ChatColor.WHITE + timeDisplay);
            }
        } else{
            bossBar.setTitle(ChatColor.GOLD + "核心战争" + ChatColor.RESET + "  |  " + ChatColor.AQUA + phase.getName() + ChatColor.RESET + "  |  " + ChatColor.WHITE + timeDisplay);
        }
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

        for (Player player : Bukkit.getOnlinePlayers()) {
            // 播放获得经验的声效，音量设置为 2.0F（较大），音高设置为 2.0F（较高）
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2.0F, 2.0F);
        }

        // 当游戏进入阶段 1 时，杀死所有选择了队伍的玩家，并将他们传送到票数最高的地图
        if (currentPhase == 1) {
            String highestVotedMap = mapSelectManager.getHighestVotedMap();
            if (highestVotedMap != null) {
                World gameWorld = Bukkit.getWorld(highestVotedMap);
                if (gameWorld != null) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (teamManager.isInTeam(player)) {
                            player.teleport(gameWorld.getSpawnLocation());
                            player.setHealth(0);
                        }
                    }
                }
            }
            nexusInfoBoard.updateInfoBoard();
        }
        // 当游戏进入阶段 4 时, 出现boss
        if (currentPhase == 4) {
            bossDataManager.spawnBoss();
        }
        // 阶段 3 时生成女巫，更新钻石
        if(currentPhase == 3) {
            oreManager.updateDiamondBlocks();
            witchDataManager.startWitchesSpawn();
        }

    }

    public boolean isTeamSelectionOpen() {
        return teamSelectionOpen;
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

    public void setOreManager(OreManager oreManager){
        this.oreManager = oreManager;
    }

    public void setMapSelectManager(MapSelectManager mapSelectManager) {
        this.mapSelectManager = mapSelectManager;
    }

    public void setNexusInfoBoard(NexusInfoBoard nexusInfoBoard) {
        this.nexusInfoBoard = nexusInfoBoard;
    }
}