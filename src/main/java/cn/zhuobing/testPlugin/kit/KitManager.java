package cn.zhuobing.testPlugin.kit;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class KitManager {
    private static KitManager instance;
    private final Map<UUID, String> playerKits = new HashMap<>();
    private final Map<String, Kit> registeredKits = new HashMap<>();
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
        Inventory gui = Bukkit.createInventory(null, 9, "选择你的职业");

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
            SoulBoundUtil.clearSoulBoundLevel2Items(player);
        }
        playerKits.put(playerId, kitName.toLowerCase());

        if(gameManager.getCurrentPhase() >= 1 && teamManager.isInTeam(player)){
            // 死亡惩罚
            player.setHealth(0.0);
        }
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
}