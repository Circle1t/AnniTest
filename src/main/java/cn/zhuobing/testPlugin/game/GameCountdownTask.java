package cn.zhuobing.testPlugin.game;

import cn.zhuobing.testPlugin.AnniTest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class GameCountdownTask extends BukkitRunnable {
    private final GameManager gameManager;
    private final BossBar bossBar;
    private int remainingTime;
    private int currentPhase;
    private static final Map<Integer, String[]> PHASE_TITLES = new HashMap<>();

    static {
        PHASE_TITLES.put(1, new String[]{ChatColor.AQUA + "阶段一" + ChatColor.WHITE + "【发育/骚扰】", ChatColor.GRAY + "核心正处于" + ChatColor.GOLD + "保护" + ChatColor.GRAY + "阶段"});
        PHASE_TITLES.put(2, new String[]{ChatColor.AQUA + "阶段二" + ChatColor.WHITE + "【保护/偷袭】", ChatColor.GRAY + "你现在可以"+ ChatColor.GOLD + "挖掘" + ChatColor.GRAY + "敌方核心"});
        PHASE_TITLES.put(3, new String[]{ChatColor.AQUA + "阶段三" + ChatColor.WHITE + "【抢夺/激战】", ChatColor.AQUA + "钻石" + ChatColor.GRAY + "已在中央岛屿生成"});
        PHASE_TITLES.put(4, new String[]{ChatColor.AQUA + "阶段四" + ChatColor.WHITE + "【合作/BOSS】", ChatColor.DARK_PURPLE + "BOSS" + ChatColor.GRAY +"已经生成"});
        PHASE_TITLES.put(5, new String[]{ChatColor.AQUA + "阶段五" + ChatColor.WHITE + "【决战/冲锋】", ChatColor.GRAY + "核心挖掘造成"+ChatColor.RED + "双倍" + ChatColor.GRAY + "伤害"});
    }

    public GameCountdownTask(GameManager gameManager, BossBar bossBar, int remainingTime, int currentPhase) {
        this.gameManager = gameManager;
        this.bossBar = bossBar;
        this.remainingTime = remainingTime;
        this.currentPhase = currentPhase;
    }

    @Override
    public void run() {
        if (remainingTime <= 0) {

            currentPhase++;

            if (currentPhase < gameManager.getPhaseManager().getPhaseCount() - 1) {
                remainingTime = gameManager.getPhaseManager().getPhase(currentPhase).getDuration();
                gameManager.updateBossBar(currentPhase, remainingTime);
                gameManager.setCurrentPhase(currentPhase);
                // 显示阶段切换的title
                showPhaseTitle(currentPhase);
            } else {
                cancel();
                bossBar.setTitle(ChatColor.GOLD + "核心战争" + ChatColor.RESET + " | " + ChatColor.AQUA + "阶段五" + ChatColor.RESET + " | " + ChatColor.GOLD + "挖掘核心造成双倍伤害");
                bossBar.setColor(gameManager.getPhaseManager().getPhase(currentPhase).getColor());
                gameManager.setCurrentPhase(currentPhase);
            }
        } else {
            remainingTime--;
            gameManager.updateBossBar(currentPhase, remainingTime);
        }
    }

    private void showPhaseTitle(int phase) {
        String[] titles = PHASE_TITLES.get(phase);
        if (titles != null) {
            String mainTitle = titles[0];
            String subTitle = titles[1];
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(mainTitle, subTitle, 10, 70, 20);
            }
        }
    }
}