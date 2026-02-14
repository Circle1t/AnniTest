package cn.zhuobing.testPlugin.xp;

/**
 * XP奖励类型枚举
 */
public enum XPRewardType {
    // 单个玩家奖励
    KILLED_ENEMY(3, "击杀"),                // 基础战斗奖励：玩家成功击杀任意敌方玩家，获得基础战斗XP，鼓励主动对抗
    DEFENDED_NEXUS(6, "防守"),              // 核心守护奖励：在己方核心保护区域内击杀来犯敌人，奖励守护关键目标的行为
    RUSHED_NEXUS(6, "进攻"),                // 核心突袭奖励：在敌方核心保护区域内击杀防守敌人，奖励主动突袭关键目标的行为
    AVENGE_KILL(6, "复仇"),                    // 击杀上一个击杀自己的人会获得的奖励
    KNOCKED_OUT_1ST(10, "淘汰补偿"),              // 参与度补偿：游戏中第1个被淘汰的队伍，给予基础参与补偿XP，提升低存活时长玩家体验
    KNOCKED_OUT_2ND(20, "淘汰补偿"),              // 参与度补偿：游戏中第2个被淘汰的队伍，给予中等参与补偿XP，提升低存活时长玩家体验
    KNOCKED_OUT_3RD(30, "淘汰补偿"),              // 参与度补偿：游戏中第3个被淘汰的队伍，给予高额参与补偿XP，提升低存活时长玩家体验
    // 团队共享奖励（队伍全员获得）
    NEXUS_DAMAGED_PHASE_1_4(1, "破坏核心"),        // 团队协作奖励：1-4阶段攻击敌方核心并造成伤害，全队共享基础奖励，鼓励团队针对核心作战
    NEXUS_DAMAGED_PHASE_5(2, "破坏核心"),          // 团队协作奖励：5阶段（后期）攻击敌方核心造成伤害，奖励翻倍，鼓励后期集中突破核心
    NEXUS_RECOVERY(2,"修复核心"),                   // 修补者
    WON_ROUND_BASE(100, "获胜"),            // 胜利奖励：成为最后一个核心未被摧毁的队伍，全员获得基础获胜XP，激励团队夺冠
    NEXUS_HEALTH_BONUS(2, "核心血量加成");          // 防守成果奖励：获胜队伍的核心剩余血量×2倍转化为额外XP，奖励队伍全程防守成果

    private final int baseXP;          // 基础XP值
    private final String description; // 奖励触发场景简短描述（用于游戏内提示等场景）

    // 构造方法：初始化基础XP和简短描述
    XPRewardType(int baseXP, String description) {
        this.baseXP = baseXP;
        this.description = description;
    }

    // 获取基础XP值
    public int getBaseXP() {
        return baseXP;
    }

    // 获取奖励触发场景简短描述
    public String getDescription() {
        return description;
    }
}