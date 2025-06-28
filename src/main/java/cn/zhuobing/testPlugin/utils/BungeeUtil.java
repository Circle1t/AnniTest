package cn.zhuobing.testPlugin.utils;

import cn.zhuobing.testPlugin.AnniTest;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;

public class BungeeUtil {
    public static void connectToServer(Player player, String serverName) {
        if (!AnniConfigManager.BUNGEE_ENABLED) {
            return;
        }

        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);

            player.sendPluginMessage(AnniTest.getInstance(), "BungeeCord", out.toByteArray());
            player.sendMessage("§a正在将你传送到 §e" + serverName + " §a服务器...");
        } catch (Exception e) {
            AnniTest.getInstance().getLogger().warning("BungeeCord 传送失败: " + e.getMessage());
            player.sendMessage("§c传送失败！请联系管理员。");
        }
    }

    public static void sendToLobby(Player player) {
        connectToServer(player, AnniConfigManager.BUNGEE_LOBBY_SERVER);
    }
}