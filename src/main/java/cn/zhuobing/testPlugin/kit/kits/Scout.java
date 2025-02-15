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
        goldSword = createSoulBoundItem(Material.GOLDEN_SWORD, ChatColor.RESET + "金剑", 1);
        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, ChatColor.RESET + "木镐", 1);
        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, ChatColor.RESET + "木斧", 1);
        // 抓钩
        grapple = createSoulBoundItem(Material.FISHING_ROD, grappleName, 2);
    }

    @Override
    public ItemStack createSoulBoundItem(Material material, String displayName, int soulBoundLevel) {
        SoulBoundLevel level = SoulBoundLevel.fromInt(soulBoundLevel);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "斥候基础物品",
                "", // 隔一行
                ChatColor.GOLD + "灵魂绑定 " + level.getDisplay()
        ));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);

        // 注册灵魂绑定
        Predicate<ItemStack> isItem = stack -> SoulBoundUtil.isSoulBoundItem(stack, displayName, material);
        SoulBoundListener.registerSoulBoundItem(soulBoundLevel, isItem);
        return item;
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
        if (event.getState() == State.IN_GROUND) {
            if (isGrappleItem(player.getInventory().getItemInMainHand())) {
                Location playerloc = player.getLocation();
                Location loc = event.getHook().getLocation();
                if (playerloc.distance(loc) < 3.0D)
                    pullPlayerSlightly(player, loc);
                else
                    pullEntityToLocation(player, loc);
                player.getInventory().getItemInMainHand().setDurability((short) 0);
            }
        }
    }

    private void pullPlayerSlightly(Player p, Location loc) {
        if (loc.getY() > p.getLocation().getY()) {
            p.setVelocity(new Vector(0.0D, 0.25D, 0.0D));
            return;
        }

        Location playerLoc = p.getLocation();

        Vector vector = loc.toVector().subtract(playerLoc.toVector());
        p.setVelocity(vector);
    }

    private void pullEntityToLocation(Entity e, Location loc) {
        Location entityLoc = e.getLocation();

        entityLoc.setY(entityLoc.getY() + 0.5D);
        e.teleport(entityLoc);

        double g = -0.08D;
        double d = loc.distance(entityLoc);
        double t = d;
        double v_x = (1.0D + 0.07000000000000001D * t)
                * (loc.getX() - entityLoc.getX()) / t;
        double v_y = (1.0D + 0.03D * t) * (loc.getY() - entityLoc.getY()) / t
                - 0.5D * g * t;
        double v_z = (1.0D + 0.07000000000000001D * t)
                * (loc.getZ() - entityLoc.getZ()) / t;

        Vector v = e.getVelocity();
        v.setX(v_x);
        v.setY(v_y);
        v.setZ(v_z);
        e.setVelocity(v);
    }
}