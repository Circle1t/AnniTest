package cn.zhuobing.testPlugin.specialitem.manager;

import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;

import static org.bukkit.Bukkit.getLogger;

public class TeamSelectorManager {
    private final TeamManager teamManager;

    public TeamSelectorManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public Inventory createTeamSelectorGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "§8队伍选择");

        // 动态获取队伍信息
        Map<String, String> englishToChineseMap = teamManager.getEnglishToChineseMap();
        int slot = 0;
        for (Map.Entry<String, String> entry : englishToChineseMap.entrySet()) {
            String teamName = entry.getKey();
            inv.setItem(slot++, createTeamWool(teamName, slot));
        }

        // 随机加入
        inv.setItem(7, createSpecialWool(Material.BLACK_WOOL, "§f随机加入", "§7点击随机加入一个队伍"));

        // 退出队伍
        inv.setItem(8, createSpecialWool(Material.WHITE_WOOL, "§f退出队伍", "§7点击离开当前队伍"));

        return inv;
    }

    private ItemStack createTeamWool(String teamName, int slot) {
        try {
            ItemStack wool = new ItemStack(Material.valueOf(teamName.toUpperCase() + "_WOOL"));
            ItemMeta meta = wool.getItemMeta();

            int count = teamManager.getTeamPlayerCount(teamName);
            wool.setAmount(Math.max(1, Math.min(count, 64)));

            String chineseName = teamManager.getEnglishToChineseMap().get(teamName);
            meta.setDisplayName(teamManager.getTeamColor(teamName) + chineseName + "队");
            meta.setLore(Arrays.asList(
                    "§7当前人数: " + count,
                    "§a点击加入该队伍"
            ));

            wool.setItemMeta(meta);
            return wool;
        } catch (IllegalArgumentException e) {
            getLogger().severe("无效的材质名称: " + teamName.toUpperCase() + "_WOOL");
            return new ItemStack(Material.AIR);
        }
    }

    private ItemStack createSpecialWool(Material material, String name, String lore) {
        ItemStack wool = new ItemStack(material);
        ItemMeta meta = wool.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        wool.setItemMeta(meta);
        return wool;
    }
}