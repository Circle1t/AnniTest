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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Handyman extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private final NexusManager nexusManager;
    private final GameManager gameManager;
    private final NexusInfoBoard nexusInfoBoard;

    // 记录每个玩家累计修复的血量
    private final Map<UUID, Integer> playerRepairMap = new ConcurrentHashMap<>();
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
    public String getName() { return "修复者"; }

    @Override
    public String getNameWithColor() { return ChatColor.GREEN + "修复者"; }

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
        for (ItemStack armor : getKitArmors(player)) {
            if (armor == null) continue;
            switch (armor.getType()) {
                case LEATHER_HELMET: inv.setHelmet(armor); break;
                case LEATHER_CHESTPLATE: inv.setChestplate(armor); break;
                case LEATHER_LEGGINGS: inv.setLeggings(armor); break;
                case CHAINMAIL_BOOTS: inv.setBoots(armor); break;
                default: inv.addItem(armor);
            }
        }
        kitItems.forEach(inv::addItem);
    }

    private void setUp() {
        woodSword = createSoulBoundItem(Material.WOODEN_SWORD, null, 1, 1, false);
        kitItems.add(woodSword.clone());

        stonePickaxe = createSoulBoundItem(Material.STONE_PICKAXE, null, 1, 1, false);
        ItemMeta pickaxeMeta = stonePickaxe.getItemMeta();
        pickaxeMeta.addEnchant(Enchantment.EFFICIENCY, 1, true);
        stonePickaxe.setItemMeta(pickaxeMeta);
        kitItems.add(stonePickaxe.clone());

        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());

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
        if (!isThisKit(player)) return;

        String playerTeam = teamManager.getPlayerTeamName(player);
        Location loc = event.getBlock().getLocation();
        for (String team : nexusManager.getNexusLocations().keySet()) {
            if (team.equals(playerTeam)) continue;
            Location nexusLoc = nexusManager.getTeamNexusLocation(team);
            if (nexusLoc != null && nexusLoc.equals(loc)) {
                if (tryIncreaseOwnNexusHealth(player, playerTeam)) break;
            }
        }
    }

    /**
     * 增加核心血量，并记录玩家修复量
     * @return 是否成功增加
     */
    private boolean tryIncreaseOwnNexusHealth(Player player, String playerTeam) {
        Random rand = new Random();
        double chance = rand.nextDouble();
        int stage = gameManager.getCurrentPhase();
        double threshold = switch (stage) {
            case 2 -> 0.25;
            case 3 -> 0.20;
            case 4 -> 0.15;
            case 5 -> 0.10;
            default -> 0;
        };
        if (chance >= threshold) return false;

        int currentHp = nexusManager.getNexusHealth(playerTeam);
        int newHp = currentHp + 1;
        nexusManager.setNexusHealth(playerTeam, newHp);
        nexusInfoBoard.updateInfoBoard();

        // 记录玩家累计修复量
        UUID id = player.getUniqueId();
        playerRepairMap.merge(id, 1, Integer::sum);

        String teamNameCn = teamManager.getTeamChineseName(playerTeam);
        ChatColor color = teamManager.getTeamColor(playerTeam);
        Bukkit.broadcastMessage(color + teamNameCn + "队 " + ChatColor.GREEN + "的核心血量被修复者提升！当前血量: " + ChatColor.YELLOW + newHp);
        return true;
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Handyman;
    }

    /**
     * 当检测到玩家作弊并恢复血量时，扣除该玩家曾修复的血量，保证队伍最低剩余5点
     */
    public void adjustAfterCheat(UUID playerId) {
        String team = teamManager.getPlayerTeam(playerId);
        int repaired = playerRepairMap.getOrDefault(playerId, 0);
        if (repaired <= 0) return;

        int currentHp = nexusManager.getNexusHealth(team);

        // 计算最多可扣除的血量（确保不低于5点）
        int maxDeduction = currentHp - 5;
        int deduction = Math.min(repaired, maxDeduction > 0 ? maxDeduction : 0);

        if (deduction > 0) {
            nexusManager.setNexusHealth(team, currentHp - deduction);
            nexusInfoBoard.updateInfoBoard();

            // 广播扣除信息
            String teamName = teamManager.getTeamChineseName(team);
            ChatColor color = teamManager.getTeamColor(team);
            Bukkit.broadcastMessage(ChatColor.RED + "[核心破坏检测] " + color + teamName + "队"
                    + ChatColor.RED + " 扣除异常修复血量: " + ChatColor.YELLOW + deduction);
        }

        // 清理玩家记录（无论是否扣除都清理）
        playerRepairMap.remove(playerId);
    }
}
