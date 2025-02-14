package cn.zhuobing.testPlugin.boss;

import cn.zhuobing.testPlugin.boss.BossDataManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WitherSkullListener implements Listener {
    private final BossDataManager bossDataManager;

    public WitherSkullListener(BossDataManager bossDataManager) {
        this.bossDataManager = bossDataManager;
    }

    @EventHandler
    public void onSkullHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof WitherSkull &&
                event.getEntity().getShooter() instanceof Wither) {

            WitherSkull skull = (WitherSkull) event.getEntity();
            Wither wither = (Wither) skull.getShooter();

            if (wither.hasMetadata("customBoss")) {
                // 对命中玩家造成效果
                if (event.getHitEntity() instanceof Player) {
                    Player player = (Player) event.getHitEntity();
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.WITHER,
                            3 * 20, // 3秒
                            2       // 效果等级
                    ));
                    player.damage(6.0); // 3颗心的伤害
                }

                skull.remove(); // 移除头颅防止爆炸
            }
        }
    }
}