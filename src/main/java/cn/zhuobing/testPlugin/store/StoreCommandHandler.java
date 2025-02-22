package cn.zhuobing.testPlugin.store;

import cn.zhuobing.testPlugin.command.CommandHandler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StoreCommandHandler implements CommandHandler {
    private final StoreManager storeManager;

    public StoreCommandHandler(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("store") || !(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;
        if (!player.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行这个命令！");
            return true;
        }

        if (args.length == 0) {
            sendUsageMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("brew")) {
            handleBrewCommand(player);
        } else if (subCommand.equals("weapon")) {
            handleWeaponCommand(player);
        } else if (subCommand.equals("remove")) {
            handleRemoveCommand(player);
        } else {
            player.sendMessage(ChatColor.RED + "未知子命令！用法: /store brew /store weapon /store remove");
        }
        return true;
    }

    private void handleBrewCommand(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
            player.sendMessage(ChatColor.RED + "请对准一个告示牌！");
            return;
        }

        Location signLocation = targetBlock.getLocation();
        if (storeManager.addBrewSignLocation(signLocation)) {
            player.sendMessage(ChatColor.GREEN + "酿造告示牌设置成功！");
        } else {
            player.sendMessage(ChatColor.RED + "该告示牌已经被设置为酿造商店告示牌！");
        }
    }

    private void handleWeaponCommand(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
            player.sendMessage(ChatColor.RED + "请对准一个告示牌！");
            return;
        }

        Location signLocation = targetBlock.getLocation();
        if (storeManager.addWeaponSignLocation(signLocation)) {
            player.sendMessage(ChatColor.GREEN + "武器告示牌设置成功！");
        } else {
            player.sendMessage(ChatColor.RED + "该告示牌已经被设置为武器商店告示牌！");
        }
    }

    private void handleRemoveCommand(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
            player.sendMessage(ChatColor.RED + "请对准一个告示牌！");
            return;
        }

        Location signLocation = targetBlock.getLocation();
        if (storeManager.removeSignLocation(signLocation)) {
            player.sendMessage(ChatColor.GREEN + "告示牌商店坐标移除成功！");
        } else {
            player.sendMessage(ChatColor.RED + "该告示牌不是商店告示牌，无法移除坐标！");
        }
    }

    private void sendUsageMessage(Player player) {
        player.sendMessage(ChatColor.RED + "用法: /store brew /store weapon /store remove");
    }
}