package cn.zhuobing.testPlugin.enchant;

public enum SoulBoundLevel {
    I("I", 1),
    II("II", 2),
    III("III", 3),
    IV("IV",4);


    private final String display;
    private final int level;

    SoulBoundLevel(String display, int level) {
        this.display = display;
        this.level = level;
    }

    public String getDisplay() {
        return display;
    }

    public int getLevel() {
        return level;
    }

    public static SoulBoundLevel fromInt(int level) {
        for (SoulBoundLevel soulBoundLevel : values()) {
            if (soulBoundLevel.getLevel() == level) {
                return soulBoundLevel;
            }
        }
        throw new IllegalArgumentException("无效的灵魂绑定等级：" + level);
    }
}