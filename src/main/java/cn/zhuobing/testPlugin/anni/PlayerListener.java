package cn.zhuobing.testPlugin.anni;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.kit.kits.Acrobat;
import cn.zhuobing.testPlugin.kit.kits.Assassin;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.ore.OreType;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.xp.XPManager;
import cn.zhuobing.testPlugin.xp.XPRewardType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {
    private final TeamManager teamManager;
    private final GameManager gameManager;
    private final KitManager kitManager;
    private final NexusManager nexusManager;
    private final Set<Material> prohibitedMaterials;
    private final XPManager xpManager = XPManager.getInstance();
    // 存储复仇关系：键=受害者UUID，值=击杀者UUID（即该受害者的复仇目标）
    private final Map<UUID, UUID> revengeTargetMap = new ConcurrentHashMap<>();

    public PlayerListener(TeamManager teamManager, GameManager gameManager, KitManager kitManager, NexusManager nexusManager) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.kitManager = kitManager;
        this.nexusManager = nexusManager;
        // 初始化禁止放置的方块集合
        this.prohibitedMaterials = new HashSet<>();
        // 遍历 OreType 枚举
        for (OreType oreType : OreType.values()) {
            // 排除羊毛类和泥土类
            if (oreType != OreType.WOOL && oreType != OreType.DIRT) {
                // 将对应方块类型添加到禁止集合中
                prohibitedMaterials.addAll(Arrays.asList(oreType.getSourceBlocks()));
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // 检查攻击者和被攻击者是否都是玩家
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {

            // 游戏未开始，未开启pvp
            if(gameManager.getCurrentPhase() < 1){
                // 阻止攻击行为
                event.setCancelled(true);
                return;
            }
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            // 获取攻击者和被攻击者所在的队伍名称
            String attackerTeamName = teamManager.getPlayerTeamName(attacker);
            String victimTeamName = teamManager.getPlayerTeamName(victim);

            //不允许没有队伍的玩家攻击/被攻击
            if(attackerTeamName == null || victimTeamName == null) {
                event.setCancelled(true);
                return;
            }

            // 检查攻击者和被攻击者是否属于同一队伍
            if (attackerTeamName != null && attackerTeamName.equals(victimTeamName)) {
                // 阻止攻击行为
                event.setCancelled(true);
                return;
            }

            // 检查玩家是否处于隐身状态
            if (attacker.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                // 检查是否空手攻击
                if (attacker.getInventory().getItemInMainHand().getType() == Material.AIR) {
                    // 空手攻击敌方，不解除隐身
                    return;
                }
                // 移除隐身效果
                attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
                attacker.sendMessage(ChatColor.GOLD + "隐身已解除！");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material placedMaterial = event.getBlock().getType();

        // 检查放置的方块是否在禁止放置的方块集合中
        if (prohibitedMaterials.contains(placedMaterial)) {
            // 取消方块放置事件
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            // 玩家被其他玩家杀死
            String victimTeamName = teamManager.getPlayerTeamName(victim);
            String killerTeamName = teamManager.getPlayerTeamName(killer);

            ChatColor victimColor = teamManager.getTeamColor(victimTeamName);
            ChatColor killerColor = teamManager.getTeamColor(killerTeamName);

            double killerHealth = killer.getHealth();
            // 使用 String.format 方法格式化健康值为保留两位小数的字符串
            String formattedHealth = String.format("%.2f", killerHealth);

            // ===== 复仇击杀逻辑 =====
            UUID killerUUID = killer.getUniqueId();
            UUID victimUUID = victim.getUniqueId();
            boolean isRevengeKill = false;

            // 检查当前击杀是否为复仇（杀手的复仇目标就是当前受害者） 复仇击杀优先级最高
            if (revengeTargetMap.containsKey(killerUUID) && revengeTargetMap.get(killerUUID).equals(victimUUID)) {
                isRevengeKill = true;
                // 复仇成功，添加复仇XP奖励（与基础击杀奖励叠加）
                xpManager.addXP(killer, XPRewardType.AVENGE_KILL);
                // 清除复仇目标（避免重复触发复仇奖励）
                revengeTargetMap.remove(killerUUID);
            }

            // 记录当前受害者的复仇目标（即杀死他的人）
            revengeTargetMap.put(victimUUID, killerUUID);

            // ===== 击杀奖励逻辑 =====
            if(nexusManager.isPlayerInTeamProtectionArea(victim,victimTeamName)){
                if (!isRevengeKill) {  // 如果是复仇击杀，之前已经给过了奖励，这里就不给了
                    // 给进攻者加基础击杀XP
                    xpManager.addXP(killer, XPRewardType.RUSHED_NEXUS);
                }
                String deathMessage = victimColor + victim.getName() + ChatColor.GRAY + " 在防守核心时被击杀因为 " +
                        killerColor + killer.getName() + "[" + ChatColor.GOLD + formattedHealth + ChatColor.RED + "❤" + killerColor + "]("
                        + kitManager.getPlayerKit(killer.getUniqueId()).getNameWithColor() + killerColor + ")";
                event.getEntity().getServer().broadcastMessage(deathMessage);

            }
            else if(nexusManager.isPlayerInTeamProtectionArea(victim,killerTeamName)){
                if (!isRevengeKill) { // 如果是复仇击杀，之前已经给过了奖励，这里就不给了
                    // 给防守者加基础击杀XP
                    xpManager.addXP(killer, XPRewardType.DEFENDED_NEXUS);
                }
                String deathMessage = victimColor + victim.getName() + ChatColor.GRAY + " 在攻击敌方核心时被击杀因为 " +
                        killerColor + killer.getName() + "[" + ChatColor.GOLD + formattedHealth + ChatColor.RED + "❤" + killerColor + "]("
                        + kitManager.getPlayerKit(killer.getUniqueId()).getNameWithColor() + killerColor + ")";
                event.getEntity().getServer().broadcastMessage(deathMessage);
            }
            else {
                if (!isRevengeKill) { // 如果是复仇击杀，之前已经给过了奖励，这里就不给了
                    // 普通击杀加基础XP
                    xpManager.addXP(killer, XPRewardType.KILLED_ENEMY);
                }
                String deathMessage = victimColor + victim.getName() + ChatColor.GRAY + " 被击杀因为 " +
                        killerColor + killer.getName() + "[" + ChatColor.GOLD + formattedHealth + ChatColor.RED + "❤" + killerColor + "]("
                        + kitManager.getPlayerKit(killer.getUniqueId()).getNameWithColor() + killerColor + ")";
                event.getEntity().getServer().broadcastMessage(deathMessage);
            }
        }
        // 取消默认的死亡信息显示
        event.setDeathMessage(null);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        OreType oreType = OreType.fromMaterial(block.getType());

        // 检查方块是否为矿石，如果是则不解除隐身
        if (oreType != null) {
            return;
        }

        // 检查玩家是否处于隐身状态
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            // 移除隐身效果
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.sendMessage(ChatColor.GOLD + "隐身已解除！");
        }
    }

    @EventHandler
    public void onBlockPlaceBreakInvisibility(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block placedBlock = event.getBlock();
        OreType oreType = OreType.fromMaterial(placedBlock.getType());

        // 检查方块是否为矿石，如果是则不解除隐身
        if (oreType != null) {
            return;
        }

        // 检查玩家是否处于隐身状态
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            // 移除隐身效果
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.sendMessage(ChatColor.GOLD + "隐身已解除！");
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            // 检查玩家是否处于隐身状态
            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                // 检查是否是免疫摔落伤害的职业
                Kit playerKit = kitManager.getPlayerKit(player.getUniqueId());
                if(playerKit instanceof Acrobat || playerKit instanceof Assassin) {
                    return;
                }
                // 检查最终伤害是否为 0
                if (event.getFinalDamage() > 0) {
                    // 移除隐身效果
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    player.sendMessage(ChatColor.GOLD + "隐身已解除！");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof Boat) {
            // 进入任何队伍的核心保护区时破坏船只
            if (nexusManager.isInProtectedArea(player.getLocation())) {
                vehicle.remove();
                player.sendMessage(ChatColor.RED + "你不能乘船进入核心保护区域！");
            }
        }
    }
}