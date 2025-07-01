package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.nexus.NexusInfoBoard;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.game.GameManager;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Handyman extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private final NexusManager nexusManager;
    private final GameManager gameManager;
    private final NexusInfoBoard nexusInfoBoard;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack woodSword;
    private ItemStack stonePickaxe;
    private ItemStack woodAxe;
    private ItemStack compass;

    public Handyman(TeamManager teamManager, KitManager kitManager, NexusManager nexusManager, GameManager gameManager, NexusInfoBoard nexusInfoBoard) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        this.nexusManager = nexusManager;
        this.gameManager = gameManager;
        this.nexusInfoBoard = nexusInfoBoard;
        setUp();
    }

    @Override
    public String getName() {
        return "修复者";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.GREEN + "修复者";
    }

    @Override
    public String getDescription() {
        return "拥有木剑，效率1石镐，木斧，指南针。当使用此职业去挖掘敌人核心时，有几率增加自己队伍核心的血量，每阶段概率如下：阶段2: 25%；阶段3: 20%；阶段4: 15%；阶段5: 10%。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.ANVIL);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Handyman",
                "",
                ChatColor.YELLOW + "你是核心的守护者与维修者。",
                "",
                ChatColor.AQUA + "挖掘敌人核心时，有概率提升己方核心血量。",
                ChatColor.GOLD + "阶段2: 25%",
                ChatColor.GOLD + "阶段3: 20%",
                ChatColor.GOLD + "阶段4: 15%",
                ChatColor.GOLD + "阶段5: 10%",
                " "
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public List<ItemStack> getKitArmors(Player player) {
        String teamColor = teamManager.getPlayerTeamName(player);

        return Arrays.asList(
                SpecialArmor.createArmor(Material.LEATHER_HELMET, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor),
                SpecialArmor.createArmor(Material.CHAINMAIL_BOOTS, teamColor)
        );
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // 皮革护甲
        List<ItemStack> armors = getKitArmors(player);
        for (ItemStack armor : armors) {
            if (armor != null) {
                switch (armor.getType()) {
                    case LEATHER_HELMET:
                        inv.setHelmet(armor);
                        break;
                    case LEATHER_CHESTPLATE:
                        inv.setChestplate(armor);
                        break;
                    case LEATHER_LEGGINGS:
                        inv.setLeggings(armor);
                        break;
                    case CHAINMAIL_BOOTS:
                        inv.setBoots(armor);
                        break;
                    default:
                        inv.addItem(armor);
                }
            }
        }

        for (ItemStack item : kitItems) {
            inv.addItem(item);
        }
    }

    private void setUp() {
        // 木剑
        woodSword = createSoulBoundItem(Material.WOODEN_SWORD, null, 1, 1, false);
        kitItems.add(woodSword.clone());

        // 效率1石镐
        stonePickaxe = createSoulBoundItem(Material.STONE_PICKAXE, null, 1, 1, false);
        ItemMeta pickaxeMeta = stonePickaxe.getItemMeta();
        pickaxeMeta.addEnchant(Enchantment.EFFICIENCY, 1, true);
        stonePickaxe.setItemMeta(pickaxeMeta);
        kitItems.add(stonePickaxe.clone());

        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());

        // 指南针
        compass = CompassItem.createCompass();
        kitItems.add(compass.clone());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isThisKit(player)) {
            String playerTeam = teamManager.getPlayerTeamName(player);
            Location blockLocation = event.getBlock().getLocation();

            // 检查破坏的是否是敌人核心
            for (String team : nexusManager.getNexusLocations().keySet()) {
                if (!team.equals(playerTeam)) {
                    Location nexusLocation = nexusManager.getTeamNexusLocation(team);
                    if (nexusLocation != null && nexusLocation.equals(blockLocation)) {
                        tryIncreaseOwnNexusHealth(player, playerTeam);
                        break;
                    }
                }
            }
        }
    }

    private void tryIncreaseOwnNexusHealth(Player player, String playerTeam) {
        Random random = new Random();
        double chance = random.nextDouble();

        int currentStage = gameManager.getCurrentPhase();

        boolean shouldIncrease = false;
        switch (currentStage) {
            case 2:
                shouldIncrease = chance < 0.25;
                break;
            case 3:
                shouldIncrease = chance < 0.2;
                break;
            case 4:
                shouldIncrease = chance < 0.15;
                break;
            case 5:
                shouldIncrease = chance < 0.1;
                break;
        }

        if (shouldIncrease) {
            int currentHealth = nexusManager.getNexusHealth(playerTeam);
            int newHealth = currentHealth + 1; // 每次增加1点血量，可根据需求调整
            nexusManager.setNexusHealth(playerTeam, newHealth);
            nexusInfoBoard.updateInfoBoard();

            String teamChineseName = teamManager.getTeamChineseName(playerTeam);
            ChatColor teamColor = teamManager.getTeamColor(playerTeam);
            String message = teamColor + teamChineseName + "队 " + ChatColor.GREEN + "的核心血量被修复者提升！当前血量: " + ChatColor.YELLOW + newHealth;
            Bukkit.broadcastMessage(message);
        }
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Handyman;
    }
}