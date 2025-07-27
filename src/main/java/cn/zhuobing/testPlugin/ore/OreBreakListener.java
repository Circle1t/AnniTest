package cn.zhuobing.testPlugin.ore;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.map.BorderManager;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class OreBreakListener implements Listener {
    private final OreManager oreManager;
    private final GameManager gameManager;
    private final NexusManager nexusManager;

    public OreBreakListener(OreManager oreManager, GameManager gameManager,NexusManager nexusManager) {
        this.oreManager = oreManager;
        this.gameManager = gameManager;
        this.nexusManager = nexusManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        OreType oreType = OreType.fromMaterial(block.getType());

        if (oreType != null) {
            // 阶段验证
            if (gameManager.getCurrentPhase() < oreType.availablePhase) {
                block.setType(Material.COBBLESTONE);
                event.setCancelled(true);
                return;
            }

            // 核心保护区域验证
            if(nexusManager.isInProtectedArea(block.getLocation()) && !OreType.isOreInProtectedArea(OreType.fromMaterial(event.getBlock().getType()))) {
                event.setCancelled(true);
                return;
            }

            // 工具验证
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!OreUtils.isValidTool(tool, oreType)) {
                event.setCancelled(true);
                return;
            }

            // 冷却时间验证
            if (oreManager.isOreInCoolDown(block)) {
                player.sendMessage("§c该矿石正在冷却中，请稍后再试！");
                event.setCancelled(true);
                return;
            }

            // 如果是核心保护区域的原木不能被破坏
            if (nexusManager.isInProtectedArea(block.getLocation()) && oreType == OreType.LOG) {
                //player.sendMessage(ChatColor.RED + "你不能破坏核心保护区域中的原木！");
                event.setCancelled(true);
                return;
            }

            // 取消原事件，使用自定义逻辑处理挖掘
            event.setCancelled(true);
            oreManager.processOreBreak(player, block);

            // 处理树叶方块苹果掉落
            if (oreType == OreType.LEAVES) {
                Random random = new Random();
                double chance = AnniConfigManager.APPLE_DROP_RATE / 100.0;
                if (random.nextDouble() < chance) {
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlock();
        if (placedBlock.getType() == Material.DIAMOND_ORE && gameManager.getCurrentPhase() < 3) {
            placedBlock.setType(Material.COBBLESTONE);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block != null && isLog(block.getType()) && event.getAction().name().contains("RIGHT_CLICK")) {
            event.setCancelled(true);
            //event.getPlayer().sendMessage(ChatColor.RED + "禁止给树去皮！");
        }
    }

    private boolean isLog(Material material) {
        switch (material) {
            case OAK_LOG:
            case SPRUCE_LOG:
            case BIRCH_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
            case CRIMSON_STEM:
            case WARPED_STEM:
                return true;
            default:
                return false;
        }
    }
}