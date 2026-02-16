package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Scout extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private ItemStack grapple;
    private ItemStack goldSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private List<ItemStack> kitItems = new ArrayList<>();
    private String grappleName = ChatColor.AQUA + "抓钩";

    private static final long COMBAT_DURATION_MS = 3000L;
    private final Map<UUID, Long> combatUntil = new ConcurrentHashMap<>();

    public Scout(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "斥候";
    }

    @Override
    public String getNameWithColor(){
        return ChatColor.GOLD + "斥候";
    }

    @Override
    public String getDescription() {
        return "使用抓钩快速移动，摔落伤害减半。战斗状态下无法使用抓钩（最多3秒）。";
    }

    @Override
    public void onKitUnset(Player player) {
        combatUntil.remove(player.getUniqueId());
    }

    private void enterCombat(Player player) {
        if (!isThisKit(player)) return;
        combatUntil.put(player.getUniqueId(), System.currentTimeMillis() + COMBAT_DURATION_MS);
    }

    private boolean isInCombat(Player player) {
        Long until = combatUntil.get(player.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            combatUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Scout",
                "",
                ChatColor.YELLOW + "你是战场的先锋。",
                "",
                ChatColor.AQUA + "在战场上灵活穿梭，",
                ChatColor.AQUA + "并借助你的抓钩攀登至新的高度，",
                ChatColor.AQUA + "从而洞察战场局势。",
                " " // 预留一行用于显示选择状态
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public List<ItemStack> getKitArmors(Player player) {
        String teamColor = teamManager.getPlayerTeamName(player);

        return Arrays.asList(
                SpecialArmor.createArmor(Material.LEATHER_HELMET, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_BOOTS, teamColor)
        );
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // 皮革护甲
        List<ItemStack> armors = getKitArmors(player);
        for (ItemStack armor : armors) {
            if (armor != null) {
                switch (armor.getType()) {
                    case LEATHER_HELMET:
                        inv.setHelmet(armor);
                        break;
                    case LEATHER_CHESTPLATE:
                        inv.setChestplate(armor);
                        break;
                    case LEATHER_LEGGINGS:
                        inv.setLeggings(armor);
                        break;
                    case LEATHER_BOOTS:
                        inv.setBoots(armor);
                        break;
                    default:
                        inv.addItem(armor);
                }
            }
        }

        for(ItemStack item : kitItems) {
            inv.addItem(item);
        }

    }

    private void setUp() {
        // 金剑
        goldSword = createSoulBoundItem(Material.GOLDEN_SWORD, null, 1,1, false);
        kitItems.add(goldSword.clone());
        // 抓钩
        grapple = createSoulBoundItem(Material.FISHING_ROD, grappleName, 1,4, true);
        kitItems.add(grapple.clone());
        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1,false);
        kitItems.add(woodPickaxe.clone());
        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1,1, false);
        kitItems.add(woodAxe.clone());
        //指南针
        kitItems.add(CompassItem.createCompass());
    }

    public List<ItemStack> getKitItems(){
        return kitItems;
    }


    private boolean isGrappleItem(ItemStack stack) {
        if (stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            String name = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
            String grappleNameWithoutColor = ChatColor.stripColor(this.grappleName);
            if (name.contains(grappleNameWithoutColor) && stack.getItemMeta().isUnbreakable())
                return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void fallDamage(EntityDamageEvent event) {
        if (event.getEntity().getType() == EntityType.PLAYER && event.getCause() == DamageCause.FALL) {
            Player p = (Player) event.getEntity();
            if (p != null && p.getInventory().containsAtLeast(grapple, 1)) {
                event.setDamage(event.getDamage() / 2);
            }
        }
    }

    /** 仅当双方均为玩家时进入战斗状态（打动物不触发 CD） */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player attacker = getAttackerPlayer(event.getDamager());
        if (attacker == null) return;
        enterCombat(attacker);
        enterCombat((Player) event.getEntity());
    }

    private static Player getAttackerPlayer(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            ProjectileSource src = ((Projectile) damager).getShooter();
            if (src instanceof Player) return (Player) src;
        }
        return null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combatUntil.remove(event.getPlayer().getUniqueId());
    }

    /** 斥候死亡后重置战斗 CD，复活后即可使用抓钩 */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        combatUntil.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void Grappler(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (!isThisKit(player) || !isGrappleItem(player.getInventory().getItemInMainHand())) {
            return;
        }
        if (isInCombat(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "战斗状态下无法使用抓钩！");
            return;
        }

        switch (event.getState()) {
            case FISHING:
                setHookVelocity(event.getHook(), player);
                break;

            case IN_GROUND:
            case REEL_IN:
                // 修复1：添加钩子位置验证
                if (!isHookValid(event.getHook())) {
                    return;
                }

                if (event.getCaught() != null || isInLiquidAndNoSolidBelow(event.getHook().getLocation())) {
                    return;
                }
                pullEntityToLocation(player, event.getHook().getLocation());
                break;

            default:
                // 其他状态不处理
                break;
        }
    }

    private boolean isHookValid(Entity hook) {
        Location hookLoc = hook.getLocation();
        Location hookEyeLoc = hookLoc.clone().add(0, 0.5, 0); // 钩子的"眼睛"位置

        // 1. 检查钩子下方是否有固体方块（底部）
        Location belowLoc = hookLoc.clone().subtract(0, 0.2, 0);
        if (belowLoc.getBlock().getType().isSolid()) {
            return true;
        }

        // 2. 检查钩子上方是否有固体方块（顶部）
        Location aboveLoc = hookLoc.clone().add(0, 0.2, 0);
        if (aboveLoc.getBlock().getType().isSolid()) {
            return true;
        }

        // 3. 检查钩子"眼睛"位置是否有固体方块（更精确的顶部检测）
        if (hookEyeLoc.getBlock().getType().isSolid()) {
            return true;
        }

        return false;
    }


    private boolean isInLiquidAndNoSolidBelow(Location loc) {
        Material blockType = loc.getBlock().getType();
        if (blockType == Material.WATER || blockType == Material.LAVA) {
            Location belowLoc = loc.clone().subtract(0, 0.8, 0);
            Material belowBlockType = belowLoc.getBlock().getType();
            return belowBlockType == Material.WATER || belowBlockType == Material.LAVA || belowBlockType.isAir();
        }
        return false;
    }

    private void setHookVelocity(Entity hook, Player player) {
        Vector direction = player.getLocation().getDirection().normalize();
        hook.setVelocity(direction.multiply(1.8)); // 抓钩抛出速度
        hook.setGravity(true); // 启用重力，使钩子有抛物线效果
    }

    private void pullPlayerSlightly(Player p, Location loc) {
        pullEntityToLocation(p, loc);
    }

    private void pullEntityToLocation(Entity e, Location loc) {
        Location entityLoc = e.getLocation();

        // 添加防卡墙检测
        Location safeLoc = findSafeLocation(loc.clone());

        // 计算高度差（目标位置与玩家位置的高度差）
        double heightDifference = safeLoc.getY() - entityLoc.getY();

        // 计算水平距离（忽略Y轴）
        Location horizontalLoc = loc.clone();
        horizontalLoc.setY(entityLoc.getY());
        double horizontalDistance = entityLoc.distance(horizontalLoc);

        // 使用简化的向量计算
        Vector direction = safeLoc.toVector().subtract(entityLoc.toVector());
        double totalDistance = direction.length();

        // 基础速度系数
        double baseSpeed = 0.8;
        double maxSpeed = 1.6; // 最大速度

        // 根据水平距离动态调整速度系数
        double distanceFactor = 1.0;
        if (horizontalDistance > 5.0) {
            // 当水平距离较大时，增加推力
            distanceFactor = Math.min(1.5, 1.0 + (horizontalDistance * 0.04));
        }

        // 根据高度差动态调整速度系数
        double heightFactor = 1.0;
        if (heightDifference > 1.0) {
            heightFactor = Math.min(1.2, 1.0 + (heightDifference * 0.03));
        }

        // 设置速度 - 考虑距离因子和高度因子
        double speed = Math.min(maxSpeed, baseSpeed * totalDistance * distanceFactor * heightFactor);
        Vector velocity = direction.normalize().multiply(speed);

        // 根据高度差调整垂直分量
        if (heightDifference > 0) {
            // 向上拉取时，保持或增强垂直分量
            velocity.setY(velocity.getY() * (1.0 + (heightDifference * 0.02)));
        } else {
            // 向下或水平拉取时，减少垂直分量
            velocity.setY(velocity.getY() * 0.6);
        }

        // 确保最低垂直速度
        if (velocity.getY() < 0.15) {
            velocity.setY(0.15);
        }

        // 添加水平距离补偿 - 确保水平距离远时有足够推力
        if (horizontalDistance > 8.0) {
            double boost = Math.min(0.8, (horizontalDistance - 8.0) * 0.1);
            velocity.multiply(1.0 + boost);
        }

        e.setVelocity(velocity);
    }

    private Location findSafeLocation(Location loc) {
        // 尝试在当前位置上方寻找空气位置
        for (int i = 0; i < 3; i++) {
            Location testLoc = loc.clone().add(0, i, 0);
            if (isSafeLocation(testLoc)) {
                return testLoc;
            }
        }

        // 如果上方没有安全位置，尝试在当前位置寻找
        if (isSafeLocation(loc)) {
            return loc;
        }

        // 最后尝试在玩家当前位置上方
        return loc.clone().add(0, 2, 0);
    }

    private boolean isSafeLocation(Location loc) {
        Material blockType = loc.getBlock().getType();
        Material belowType = loc.clone().subtract(0, 1, 0).getBlock().getType();

        // 安全位置标准：非固体方块且下方有支撑
        return !blockType.isSolid() &&
                !blockType.toString().contains("WATER") &&
                !blockType.toString().contains("LAVA") &&
                (belowType.isSolid() || belowType.toString().contains("WATER"));
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Scout;
    }
}