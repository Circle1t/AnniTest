package cn.zhuobing.testPlugin.ore;

import cn.zhuobing.testPlugin.command.CommandHandler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiamondCommand implements CommandHandler {
    private final OreManager oreManager;

    public DiamondCommand(OreManager oreManager) {
        this.oreManager = oreManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("diamond")) {
            return false;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用这个指令！");
            return true;
        }
        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "只有管理员可以使用这个指令！");
            return true;
        }

        boolean isCancelCommand = args.length > 0 && args[0].equalsIgnoreCase("cancel");
        handleDiamondCommand(player, isCancelCommand);
        return true;
    }

    private void handleDiamondCommand(Player player, boolean isCancel) {
        org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage(ChatColor.RED + "未找到可操作的方块，请靠近方块再试！");
            return;
        }
        Location targetLocation = targetBlock.getLocation();
        if (isCancel) {
            if (oreManager.getDiamondDataManager().getDiamondSpawnLocations().contains(targetLocation)) {
                oreManager.removeDiamondSpawnLocation(targetLocation);
                targetBlock.setType(Material.COBBLESTONE);
                player.sendMessage(ChatColor.GREEN + "成功取消该位置的钻石生成点！");
            } else {
                player.sendMessage(ChatColor.RED + "该位置不是钻石生成点！");
            }
        } else {
            oreManager.addDiamondSpawnLocation(targetLocation);
            targetBlock.setType(Material.DIAMOND_ORE);
            player.sendMessage(ChatColor.GREEN + "成功设置钻石生成点！");
        }
        oreManager.updateDiamondBlocks();
    }
}