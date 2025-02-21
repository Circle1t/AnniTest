package cn.zhuobing.testPlugin.kit;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.kit.kits.General;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class KitManager {
    private static KitManager instance;
    private final Map<UUID, String> playerKits = new HashMap<>();
    private final Map<String, Kit> registeredKits = new LinkedHashMap<>();
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final Plugin plugin;

    public KitManager(GameManager gameManager, TeamManager teamManager, Plugin plugin) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.plugin = plugin;
    }

    // 注册职业
    public void registerKit(Kit kit) {
        registeredKits.put(kit.getName().toLowerCase(), kit);

        // 检查 Kit 是否实现了 Listener 接口，如果实现了则注册为事件监听器
        if (kit instanceof Listener) {
            Bukkit.getPluginManager().registerEvents((Listener) kit, plugin);
        }
    }

    public void openKitSelection(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "选择你的职业");

        // 获取玩家当前选择的职业
        Kit selectedKit = getPlayerKit(player.getUniqueId());

        registeredKits.values().forEach(kit -> {
            ItemStack icon = kit.getIcon().clone(); // 克隆图标以避免修改原始数据
            ItemMeta meta = icon.getItemMeta();

            // 动态添加 Lore
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            } else {
                lore = new ArrayList<>(lore); // 避免直接修改原始 Lore
            }

            // 添加选择状态
            if (selectedKit != null && selectedKit.getName().equalsIgnoreCase(kit.getName())) {
                lore.add(ChatColor.GREEN + "已选择");
            } else {
                lore.add(ChatColor.RED + "未选择");
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);

            gui.addItem(icon);
        });

        player.openInventory(gui);
    }

    // 设置玩家职业
    public void setPlayerKit(UUID playerId, String kitName) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {

            SoulBoundUtil.clearSoulBoundLevelItems(player);
            // 获取玩家当前职业
            Kit currentKit = getPlayerKit(playerId);

            // 获取玩家将要变为的职业
            if (currentKit instanceof General) {
                // 如果当前职业是将军，移除缓慢效果
                player.removePotionEffect(PotionEffectType.SLOWNESS);
            }
            if(kitName.equalsIgnoreCase("将军")) {
                // 如果选择的职业是将军，添加缓慢效果
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
                    }
                }.runTaskLater(plugin, 1L);
            }

            playerKits.put(playerId, kitName.toLowerCase());

            if (gameManager.getCurrentPhase() >= 1 && teamManager.isInTeam(player)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openKitItemSelection(player, kitName);
                }, 2L);
            }
        }
    }

    // 打开职业物品选择界面
    private void openKitItemSelection(Player player, String kitName) {
        Kit kit = registeredKits.get(kitName.toLowerCase());
        if (kit == null) {
            return;
        }

        String teamColor = teamManager.getPlayerTeamName(player);
        List<ItemStack> items = new ArrayList<>();

        // 添加皮革护甲
        items.add(SpecialLeatherArmor.createArmor(Material.LEATHER_HELMET, teamColor));
        items.add(SpecialLeatherArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor));
        items.add(SpecialLeatherArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor));
        items.add(SpecialLeatherArmor.createArmor(Material.LEATHER_BOOTS, teamColor));

        Kit playerKit = getPlayerKit(player.getUniqueId());
        if (playerKit != null) {
            List<ItemStack> kitItems = playerKit.getKitItems();
            items.addAll(kitItems);
        }

        int inventorySize = 27;
        Inventory gui = Bukkit.createInventory(null, inventorySize, kit.getName() + " 物品选择");

        for (ItemStack item : items) {
            gui.addItem(item);
        }

        // 打开箱子音效
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
        player.openInventory(gui);
    }

    // 获取玩家职业
    public Kit getPlayerKit(UUID playerId) {
        String kitName = playerKits.get(playerId);
        return kitName != null ? registeredKits.get(kitName) : null;
    }

    //获取玩家职业名称
    public String getPlayerKitName(UUID playerId) {
        return playerKits.get(playerId);
    }

    public Map<String, Kit> getRegisteredKits() {
        return registeredKits;
    }

    public Plugin getPlugin(){
        return plugin;
    }
}