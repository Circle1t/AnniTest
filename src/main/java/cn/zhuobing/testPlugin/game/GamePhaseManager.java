package cn.zhuobing.testPlugin.game;

import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import org.bukkit.boss.BarColor;

import java.util.ArrayList;
import java.util.List;

/**
 *  在配置文件中读取 AnniConfigManager.java
 *  private static void createDefaultPhases() {
 *         GAME_PHASES.add(new GamePhase("游戏即将开始 请为地图投票", 30, BarColor.BLUE));
 *         GAME_PHASES.add(new GamePhase("阶段一", 600, BarColor.BLUE));
 *         GAME_PHASES.add(new GamePhase("阶段二", 600, BarColor.BLUE));
 *         GAME_PHASES.add(new GamePhase("阶段三", 600, BarColor.BLUE));
 *         GAME_PHASES.add(new GamePhase("阶段四", 600, BarColor.PURPLE));
 *         GAME_PHASES.add(new GamePhase("阶段五", 0, BarColor.WHITE));
 *     }
 */

public class GamePhaseManager {
    private final List<GamePhase> phases;

    public GamePhaseManager() {
        phases = new ArrayList<>(AnniConfigManager.GAME_PHASES);
    }

    public GamePhase getPhase(int index) {
        if (index < 0) {
            return phases.get(0);
        }
        if (index >= phases.size()) {
            return phases.get(phases.size() - 1);
        }
        return phases.get(index);
    }

    public int getPhaseCount() {
        return phases.size();
    }

    public BarColor getPhaseColor(int phaseIndex) {
        if (phaseIndex < 0 || phaseIndex >= phases.size()) {
            return null;
        }
        return phases.get(phaseIndex).getColor();
    }
}