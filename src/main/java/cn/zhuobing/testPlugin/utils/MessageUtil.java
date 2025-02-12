package cn.zhuobing.testPlugin.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class MessageUtil {
    /**
     * 向玩家发送行动栏消息
     * @param player 要发送消息的玩家
     * @param message 要显示的消息内容
     */
    public static void sendActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}
