package cn.zhuobing.testPlugin.kit;

import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class Kit {
    public abstract String getName();
    public abstract String getDescription();
    public abstract ItemStack getIcon();
    public abstract void applyKit(Player player);
}