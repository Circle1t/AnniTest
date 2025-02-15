package cn.zhuobing.testPlugin.kit.civilian;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.civilian.items.CivilianItems;
import cn.zhuobing.testPlugin.specialitem.items.SpecialLeatherArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class CivilianKit extends Kit {
    private TeamManager teamManager;

    public CivilianKit(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public String getName() {
        return "平民";
    }

    @Override
    public String getDescription() {
        return "基础职业，配备全套皮革装备和石质工具";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "平民");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "职业特性：",
                ChatColor.WHITE + "• 全套灵魂绑定皮革护甲",
                ChatColor.WHITE + "• 全套坚不可摧的石质工具",
                ChatColor.WHITE + "• 自带工作台",
                " " // 预留一行用于显示选择状态
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public void applyKit(Player player) {
        // 生成装备（需要实现 CivilianItems 类）
        CivilianItems.giveCivilianKit(player,teamManager.getPlayerTeamName(player));
    }
}