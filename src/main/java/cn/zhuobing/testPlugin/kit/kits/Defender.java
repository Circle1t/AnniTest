package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.MessageUtil;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Defender extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private final NexusManager nexusManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    // 效果相关字段
    private final Map<UUID, BukkitTask> effectTasks = new HashMap<>();
    private final PotionEffect REGENERATION_EFFECT =
            new PotionEffect(PotionEffectType.REGENERATION, 6 * 20, 1, false, false, true);
    private final int CHECK_INTERVAL = 3 * 20; // 3秒检测间隔

    // 职业物品
    private ItemStack woodSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;

    public Defender(TeamManager teamManager, KitManager kitManager, NexusManager nexusManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        this.nexusManager = nexusManager;
        setUp();
    }

    @Override
    public String getName() {
        return "保卫者";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.GREEN + "保卫者";
    }

    @Override
    public String getDescription() {
        return "在己方核心保护区域内获得生命恢复效果，离开区域效果消失。适合守护核心的职业，能有效抵御敌人的进攻。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.SHIELD);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Defender",
                "",
                ChatColor.YELLOW + "你是核心的守护者。",
                "",
                ChatColor.AQUA + "在己方核心保护区域内时，",
                ChatColor.AQUA + "你将获得生命恢复 II 效果。",
                ChatColor.AQUA + "离开区域后效果消失。",
                " "
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public List<ItemStack> getKitArmors(Player player) {
        String teamColor = teamManager.getPlayerTeamName(player);

        return Arrays.asList(
                SpecialArmor.createArmor(Material.LEATHER_HELMET, teamColor), // 皮革头盔
                SpecialArmor.createArmor(Material.CHAINMAIL_CHESTPLATE, teamColor,3,false), // 锁链胸甲
                SpecialArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor), // 皮革裤子
                SpecialArmor.createArmor(Material.LEATHER_BOOTS, teamColor) // 皮革靴子
        );
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // 装备护甲
        List<ItemStack> armors = getKitArmors(player);
        inv.setHelmet(armors.get(0));
        inv.setChestplate(armors.get(1));
        inv.setLeggings(armors.get(2));
        inv.setBoots(armors.get(3));

        // 添加职业物品
        for (ItemStack item : kitItems) {
            inv.addItem(item);
        }

        // 启动效果检测
        startEffectCheck(player);
    }

    @Override
    public void onKitUnset(Player player) {
        // 移除职业时清除效果和任务
        player.removePotionEffect(PotionEffectType.REGENERATION);
        BukkitTask task = effectTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void setUp() {
        // 木剑
        woodSword = createSoulBoundItem(Material.WOODEN_SWORD, null, 1, 1, false);
        kitItems.add(woodSword.clone());

        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());

        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());

        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    // 启动效果检测任务
    private void startEffectCheck(Player player) {
        Plugin plugin = kitManager.getPlugin();
        if (plugin == null) return;

        // 取消现有任务（如果存在）
        BukkitTask existingTask = effectTasks.get(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        // 创建新任务
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isThisKit(player)) {
                    cancel();
                    effectTasks.remove(player.getUniqueId());
                    return;
                }

                String teamName = teamManager.getPlayerTeamName(player);
                if (teamName == null) {
                    player.removePotionEffect(PotionEffectType.REGENERATION);
                    return;
                }

                // 检查是否在核心保护区域
                boolean inArea = nexusManager.isPlayerInTeamProtectionArea(player, teamName);

                if (inArea) {
                    // 在区域内 - 应用或刷新效果
                    player.addPotionEffect(REGENERATION_EFFECT);
                    //MessageUtil.sendActionBarMessage(player,ChatColor.GREEN + "核心守护 - 生命恢复激活");
                } else {
                    // 不在区域内 - 移除效果
                    player.removePotionEffect(PotionEffectType.REGENERATION);
                }
            }
        }.runTaskTimer(plugin, 0, CHECK_INTERVAL);

        effectTasks.put(player.getUniqueId(), task);
    }

    // 玩家移动时快速响应
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isThisKit(player)) return;

        // 位置变化时才检测
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            String teamName = teamManager.getPlayerTeamName(player);
            if (teamName == null) return;

            boolean inArea = nexusManager.isPlayerInTeamProtectionArea(player, teamName);
            boolean hadEffect = player.hasPotionEffect(PotionEffectType.REGENERATION);

            if (inArea && !hadEffect) {
                // 进入区域 - 立即应用效果
                player.addPotionEffect(REGENERATION_EFFECT);
                //MessageUtil.sendActionBarMessage(player,ChatColor.GREEN + "核心守护 - 生命恢复激活");
            } else if (!inArea && hadEffect) {
                // 离开区域 - 立即移除效果
                player.removePotionEffect(PotionEffectType.REGENERATION);
            }
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Defender;
    }
}