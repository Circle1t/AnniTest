package cn.zhuobing.testPlugin.game;

import org.bukkit.boss.BarColor;

public class GamePhase {
    private final String name;
    private final int duration;
    private final BarColor color;

    public GamePhase(String name, int duration, BarColor color) {
        this.name = name;
        this.duration = duration;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    public BarColor getColor() {
        return color;
    }
}