package cn.zhuobing.testPlugin.specialitem.manager;

import cn.zhuobing.testPlugin.map.MapSelectManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MapConfigurerManager {
    private final MapSelectManager mapSelectManager;

    public MapConfigurerManager(MapSelectManager mapSelectManager) {
        this.mapSelectManager = mapSelectManager;
    }

    public Inventory createMapConfigurerGUI(Player player) {
        List<String> candidateMaps = mapSelectManager.getCandidateMaps();
        List<String> originalMaps = mapSelectManager.getOriginalMaps();
        int totalMaps = candidateMaps.size() + originalMaps.size();
        int size = Math.max(9, (int) Math.ceil(totalMaps / 9.0) * 9);
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.RESET + "地图配置");

        int slot = 0;
        for (String mapName : candidateMaps) {
            Material icon = mapSelectManager.getMapIcon(mapName);
            inv.setItem(slot++, createMapIconItem(mapName, icon, player));
        }
        for (String mapName : originalMaps) {
            Material icon = Material.SAND; // 可以自定义原始地图的图标
            inv.setItem(slot++, createMapIconItem(mapName, icon, player));
        }

        return inv;
    }

    private ItemStack createMapIconItem(String mapName, Material icon, Player player) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + mapName);

        UUID playerUUID = player.getUniqueId();
        String description = mapSelectManager.getMapDescription(mapName);
        String clickPrompt = ChatColor.YELLOW + "点击进入该地图";

        if(description != null) {
            meta.setLore(Arrays.asList(
                    ChatColor.GOLD + description,
                    clickPrompt
            ));
        }else {
            meta.setLore(Arrays.asList(
                    clickPrompt
            ));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }
}