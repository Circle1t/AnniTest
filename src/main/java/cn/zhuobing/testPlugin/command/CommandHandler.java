package cn.zhuobing.testPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public interface CommandHandler {
    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
}
