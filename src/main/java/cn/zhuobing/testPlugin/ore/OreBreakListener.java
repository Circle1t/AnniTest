package cn.zhuobing.testPlugin.ore;

import cn.zhuobing.testPlugin.game.GameManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class OreBreakListener implements Listener {
    private final OreManager oreManager;
    private final GameManager gameManager;

    public OreBreakListener(OreManager oreManager, GameManager gameManager) {
        this.oreManager = oreManager;
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        OreType oreType = OreType.fromMaterial(block.getType());

        if (oreType == null) {
            return;
        }

        // 阶段验证
        if (gameManager.getCurrentPhase() < oreType.availablePhase) {
            block.setType(Material.COBBLESTONE);
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

        // 取消原事件，使用自定义逻辑处理挖掘
        event.setCancelled(true);
        oreManager.processOreBreak(player, block);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlock();
        if (placedBlock.getType() == Material.DIAMOND_ORE && gameManager.getCurrentPhase() < 3) {
            placedBlock.setType(Material.COBBLESTONE);
        }
    }
}