package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

/**
 * 坦克：石剑、木镐、木斧、灵魂绑定且无法破坏的盾牌、指南针。
 */
public class Tank extends Kit implements Listener {

    private static final String SHIELD_DISPLAY_NAME = ChatColor.GOLD + "盾牌";
    private static final int SOUL_BOUND_LEVEL = 4;

    private final TeamManager teamManager;
    private final KitManager kitManager;
    private ItemStack stoneSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;
    private ItemStack defensiveShield;
    private final List<ItemStack> kitItems = new ArrayList<>();

    public Tank(TeamManager teamManager, KitManager kitManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        setUp();
    }

    @Override
    public String getName() {
        return "坦克";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.AQUA + "坦克";
    }

    @Override
    public String getDescription() {
        return "持灵魂绑定、无法破坏的盾牌，石剑木镐木斧与指南针。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.SHIELD);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(getNameWithColor());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Tank",
                    "",
                    ChatColor.YELLOW + "你是坚不可摧的壁垒。",
                    "",
                    ChatColor.AQUA + "石剑、木镐、木斧与" + ChatColor.GOLD + "盾牌",
                    ChatColor.AQUA + "盾牌灵魂绑定且无法破坏。",
                    " "
            ));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public List<ItemStack> getKitArmors(Player player) {
        String teamColor = teamManager.getPlayerTeamName(player);
        return Arrays.asList(
                SpecialArmor.createArmor(Material.LEATHER_HELMET, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_BOOTS, teamColor)
        );
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> armors = getKitArmors(player);
        inv.setHelmet(armors.get(0));
        inv.setChestplate(armors.get(1));
        inv.setLeggings(armors.get(2));
        inv.setBoots(armors.get(3));
        for (ItemStack item : kitItems) {
            inv.addItem(item.clone());
        }
    }

    private void setUp() {
        stoneSword = createSoulBoundItem(Material.STONE_SWORD, null, 1, 1, false);
        kitItems.add(stoneSword.clone());
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());
        defensiveShield = createDefensiveShield();
        kitItems.add(defensiveShield.clone());
        kitItems.add(CompassItem.createCompass());
    }

    /** 灵魂绑定 4、无法破坏的坦克盾牌 */
    private static ItemStack createDefensiveShield() {
        return createSoulBoundItem(Material.SHIELD, SHIELD_DISPLAY_NAME, 1, SOUL_BOUND_LEVEL, true);
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }
}
