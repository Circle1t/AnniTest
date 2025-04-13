package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Acrobat extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final int JUMP_COOLDOWN = 10000; // 10秒冷却
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack woodSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack bow;
    private ItemStack arrows;

    public Acrobat(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "鸟人";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.GOLD + "鸟人";
    }

    @Override
    public String getDescription() {
        return "免疫摔落伤害，空中二段跳的职业";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.FEATHER);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Acrobat",
                "",
                ChatColor.YELLOW + "你是天空的舞者。",
                "",
                ChatColor.AQUA + "免疫所有摔落伤害，",
                ChatColor.AQUA + "在空中可进行二次跳跃，",
                ChatColor.AQUA + "灵活穿梭于天际。",
                " "
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

        for (ItemStack item : kitItems) {
            inv.addItem(item);
        }
    }

    private void setUp() {

        //createSoulBoundItem(Material material, String displayName, int amount, int soulBoundLevel, boolean isUnbreakable)

        // 木剑
        woodSword = createSoulBoundItem(Material.WOODEN_SWORD, null, 1, 1, false);
        kitItems.add(woodSword.clone());
        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());
        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());
        // 弓
        bow = createSoulBoundItem(Material.BOW, null, 1, 3, false);
        kitItems.add(bow.clone());
        // 箭矢，数量设置为 6
        arrows = createSoulBoundItem(Material.ARROW, null, 6, 3, true);
        kitItems.add(arrows.clone());
        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (isThisKit(player)) {
            event.setCancelled(true);
            player.setFlying(false);

            // 冷却检查
            if (cooldowns.containsKey(player.getUniqueId())) {
                long secondsLeft = (cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
                if (secondsLeft > 0) {
                    player.sendMessage(ChatColor.RED + "技能冷却中，剩余 " + secondsLeft + "秒");
                    return;
                }
            }

            // 执行二段跳
            Vector direction = player.getLocation().getDirection();
            player.setVelocity(new Vector(direction.getX(), 1.0D, direction.getZ()).multiply(1.2));
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);

            // 设置冷却
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + JUMP_COOLDOWN);
            player.setAllowFlight(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == DamageCause.FALL) {
            Player player = (Player) event.getEntity();
            if (isThisKit(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)
                &&!player.getAllowFlight()
                && player.getGameMode() != GameMode.CREATIVE
                && isOnGround(player)) {

            // 落地后重置冷却
            if (cooldowns.containsKey(player.getUniqueId())) {
                long remaining = cooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
                if (remaining <= 0) {
                    player.setAllowFlight(true);
                    cooldowns.remove(player.getUniqueId());
                    // 播放凋零攻击“哈”的音效
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.0f);
                }
            } else {
                player.setAllowFlight(true);
            }
        }
    }

    private boolean isOnGround(Player player) {
        return player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR;
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Acrobat;
    }
}