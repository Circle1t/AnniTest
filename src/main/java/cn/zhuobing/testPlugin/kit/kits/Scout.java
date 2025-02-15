package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
import cn.zhuobing.testPlugin.enchant.SoulBoundLevel;
import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.function.Predicate;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Scout extends Kit implements Listener {
    private TeamManager teamManager;
    private ItemStack grapple;
    private ItemStack goldSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private String grappleName = ChatColor.AQUA + "抓钩";

    public Scout(TeamManager teamManager) {
        this.teamManager = teamManager;
        setUp();
    }

    @Override
    public String getName() {
        return "斥候";
    }

    @Override
    public String getDescription() {
        return "使用抓钩快速移动，摔落伤害减半的职业";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "斥候");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "职业特性：",
                ChatColor.WHITE + "• 拥有抓钩可快速移动",
                ChatColor.WHITE + "• 摔落伤害减半",
                " " // 预留一行用于显示选择状态
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // 皮革护甲
        String teamColor = teamManager.getPlayerTeamName(player);
        inv.setHelmet(SpecialLeatherArmor.createArmor(Material.LEATHER_HELMET, teamColor));
        inv.setChestplate(SpecialLeatherArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor));
        inv.setLeggings(SpecialLeatherArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor));
        inv.setBoots(SpecialLeatherArmor.createArmor(Material.LEATHER_BOOTS, teamColor));

        // 基础物品
        inv.addItem(goldSword.clone());
        // 将抓钩放在第二个槽位
        inv.setItem(1, grapple.clone());
        inv.addItem(woodPickaxe.clone());
        inv.addItem(woodAxe.clone());
        inv.addItem(CompassItem.createCompass());
    }

    private void setUp() {
        // 金剑
        goldSword = createSoulBoundItem(Material.GOLDEN_SWORD, null, 1, false);
        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, false);
        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, false);
        // 抓钩
        grapple = createSoulBoundItem(Material.FISHING_ROD, grappleName, 2, true);
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void Grappler(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (isGrappleItem(player.getInventory().getItemInMainHand())) {
            if (event.getState() == State.FISHING) {
                // 设置钩子的初始速度
                Entity hook = event.getHook();
                setHookVelocity(hook, player);
            } else if (event.getState() == State.IN_GROUND || event.getState() == State.CAUGHT_ENTITY) {
                // 钩子命中目标后的逻辑
                Location playerLoc = player.getLocation();
                Location hookLoc = event.getHook().getLocation();

                if (playerLoc.distance(hookLoc) < 3.0D) {
                    pullPlayerSlightly(player, hookLoc);
                } else {
                    pullEntityToLocation(player, hookLoc);
                }

                // 重置鱼竿耐久度
                player.getInventory().getItemInMainHand().setDurability((short) 0);
            }
        }
    }

    private void setHookVelocity(Entity hook, Player player) {
        Vector direction = player.getLocation().getDirection().normalize();
        hook.setVelocity(direction.multiply(1.5)); // 降低速度，1.5 是一个适中的值
        hook.setGravity(true); // 启用重力，使钩子有抛物线效果
    }

    private void pullPlayerSlightly(Player p, Location loc) {
        Location playerLoc = p.getLocation();

        // 调整玩家位置，避免卡墙
        playerLoc.setY(playerLoc.getY() + 0.5D);
        p.teleport(playerLoc);

        // 抛物线参数
        double g = -0.08D; // 重力加速度
        double d = loc.distance(playerLoc); // 钩子与玩家的距离
        double t = d; // 时间因子

        // 计算速度分量
        double v_x = (1.0D + 0.07D * t) * (loc.getX() - playerLoc.getX()) / t;
        double v_y = (1.0D + 0.03D * t) * (loc.getY() - playerLoc.getY()) / t - 0.5D * g * t;
        double v_z = (1.0D + 0.07D * t) * (loc.getZ() - playerLoc.getZ()) / t;

        // 设置速度
        Vector v = p.getVelocity();
        v.setX(v_x);
        v.setY(v_y);
        v.setZ(v_z);
        p.setVelocity(v);
    }

    private void pullEntityToLocation(Entity e, Location loc) {
        Location entityLoc = e.getLocation();

        // 调整玩家位置，避免卡墙
        entityLoc.setY(entityLoc.getY() + 0.5D);
        e.teleport(entityLoc);

        // 抛物线参数
        double g = -0.08D; // 重力加速度
        double d = loc.distance(entityLoc); // 钩子与玩家的距离
        double t = d; // 时间因子

        // 计算速度分量
        double v_x = (1.0D + 0.07D * t) * (loc.getX() - entityLoc.getX()) / t;
        double v_y = (1.0D + 0.03D * t) * (loc.getY() - entityLoc.getY()) / t - 0.5D * g * t;
        double v_z = (1.0D + 0.07D * t) * (loc.getZ() - entityLoc.getZ()) / t;

        // 设置速度
        Vector v = e.getVelocity();
        v.setX(v_x);
        v.setY(v_y);
        v.setZ(v_z);
        e.setVelocity(v);
    }

    private boolean isValidHookLocation(Location hookLocation) {
        // 检查钩子是否在有效方块上（包括底部）
        return hookLocation.getBlock().getType().isSolid()
                || hookLocation.clone().add(0, -1, 0).getBlock().getType().isSolid();
    }
}