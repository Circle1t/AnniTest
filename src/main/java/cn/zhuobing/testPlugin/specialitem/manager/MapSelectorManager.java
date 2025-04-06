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

public class MapSelectorManager {
    private final MapSelectManager mapSelectManager;

    public MapSelectorManager(MapSelectManager mapSelectManager) {
        this.mapSelectManager = mapSelectManager;
    }

    public Inventory createMapSelectorGUI(Player player) {
        List<String> candidateMaps = mapSelectManager.getCandidateMaps();
        // 确保Inventory至少有1行（9格）
        int size = Math.max(9, (int) Math.ceil(candidateMaps.size() / 9.0) * 9);
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.RESET + "地图投票");

        int slot = 0;
        for (String mapName : candidateMaps) {
            Material icon = mapSelectManager.getMapIcon(mapName);
            inv.setItem(slot++, createMapIconItem(mapName, icon, player));
        }

        return inv;
    }

    private ItemStack createMapIconItem(String mapName, Material icon, Player player) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(mapName);

        int voteCount = mapSelectManager.getVoteCount(mapName);
        String votePrompt;
        UUID playerUUID = player.getUniqueId();
        if (mapSelectManager.hasVoted(playerUUID, mapName)) {
            votePrompt = ChatColor.GREEN + "已投票";
        } else {
            votePrompt = ChatColor.YELLOW + "点击为该地图投票";
        }

        meta.setLore(Arrays.asList(
                votePrompt,
                ChatColor.GOLD + "当前票数: " + voteCount
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }
}