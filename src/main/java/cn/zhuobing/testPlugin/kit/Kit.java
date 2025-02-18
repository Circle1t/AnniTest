package cn.zhuobing.testPlugin.kit;

import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class Kit {
    public abstract String getName();
    public abstract String getNameWithColor();
    public abstract String getDescription();
    public abstract ItemStack getIcon();
    public abstract void applyKit(Player player);
    public abstract List<ItemStack> getKitItems();
}