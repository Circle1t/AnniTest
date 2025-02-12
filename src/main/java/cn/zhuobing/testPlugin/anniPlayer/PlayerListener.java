package cn.zhuobing.testPlugin.anniPlayer;

import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerListener implements Listener {
    private final TeamManager teamManager;

    public PlayerListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // 检查攻击者和被攻击者是否都是玩家
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            // 获取攻击者和被攻击者所在的队伍名称
            String attackerTeamName = teamManager.getPlayerTeamName(attacker);
            String victimTeamName = teamManager.getPlayerTeamName(victim);

            // 检查攻击者和被攻击者是否属于同一队伍
            if (attackerTeamName != null && attackerTeamName.equals(victimTeamName)) {
                // 阻止攻击行为
                event.setCancelled(true);
                attacker.sendMessage("你不能攻击自己的队友！");
            }
        }
    }
}