package cn.zhuobing.testPlugin.game;

import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.ore.OreManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class GameCommandHandler implements CommandHandler {
    private final GameManager gameManager;
    private final OreManager oreManager;

    public GameCommandHandler(GameManager gameManager,OreManager oreManager) {
        this.gameManager = gameManager;
        this.oreManager = oreManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("annistart")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行这个命令！");
                return true;
            }
            gameManager.startGame();
            sender.sendMessage(ChatColor.AQUA + "游戏已启动！");
            return true;
        }
        if (command.getName().equalsIgnoreCase("phase")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行这个命令！");
                return true;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                try {
                    int phaseIndex = Integer.parseInt(args[1]);
                    GamePhaseManager phaseManager = gameManager.getPhaseManager();
                    if (phaseIndex >= 0 && phaseIndex < phaseManager.getPhaseCount()) {
                        gameManager.setCurrentPhase(phaseIndex);
                        oreManager.updateDiamondBlocks();
                        sender.sendMessage(ChatColor.GREEN + "游戏阶段已设置为 " + phaseManager.getPhase(phaseIndex).getName());
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "无效的阶段编号，请输入 0 到 " + (phaseManager.getPhaseCount() - 1) + " 之间的数字。");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "阶段编号必须是一个有效的整数。");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "用法：/phase set <阶段编号>");
            }
            return true;
        }
        return false;
    }
}