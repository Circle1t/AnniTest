package cn.zhuobing.testPlugin.game;

import org.bukkit.boss.BarColor;

import java.util.ArrayList;
import java.util.List;

public class GamePhaseManager {
    private final List<GamePhase> phases;

    public GamePhaseManager() {
        phases = new ArrayList<>();
        // 初始化阶段信息
        phases.add(new GamePhase("游戏即将开始 请为地图投票", 30, BarColor.BLUE));
        phases.add(new GamePhase("阶段一", 600, BarColor.BLUE));
        phases.add(new GamePhase("阶段二", 600, BarColor.BLUE));
        phases.add(new GamePhase("阶段三", 600, BarColor.BLUE));
        phases.add(new GamePhase("阶段四", 600, BarColor.PURPLE));
        phases.add(new GamePhase("阶段五", 0, BarColor.WHITE));
    }

    public GamePhase getPhase(int index) {
        if (index < 0 || index >= phases.size()) {
            return null;
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