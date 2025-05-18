package cn.zhuobing.testPlugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageRenderer {
    private final Plugin plugin;
    private final Map<String, List<String>> phaseMessages = new HashMap<>();
    private final Map<String, List<String>> teamMessages = new HashMap<>();

    public MessageRenderer(Plugin plugin) {
        this.plugin = plugin;
        loadAllImages();
    }

    private void loadAllImages() {
        // 加载阶段图片
        for (int i = 0; i <= 5; i++) {
            loadImage("phase/Phase" + i + ".png", phaseMessages, "Phase" + i);
        }

        // 加载队伍图片
        for (String team : new String[]{"Red", "Blue", "Green", "Yellow"}) {
            loadImage("team/" + team + "Team.png", teamMessages, team + "Team");
        }
    }

    private void loadImage(String path, Map<String, List<String>> target, String key) {
        try (InputStream is = plugin.getResource(path)) {
            if (is == null) {
                plugin.getLogger().warning("无法加载资源图片: " + path);
                target.put(key, new ArrayList<>());
                return;
            }

            BufferedImage image = ImageIO.read(is);
            List<String> lines = new ArrayList<>();

            for (int y = 0; y < 10; y++) {
                StringBuilder line = new StringBuilder();
                for (int x = 0; x < 10; x++) {
                    int rgb = image.getRGB(x, y);
                    String color = getHexColor(new int[]{
                            (rgb >> 16) & 0xFF,  // Red
                            (rgb >> 8) & 0xFF,   // Green
                            rgb & 0xFF           // Blue
                    });
                    line.append(color).append("▒");
                }
                lines.add(line.toString());
            }
            target.put(key, lines);
        } catch (IOException e) {
            plugin.getLogger().warning("加载图片失败: " + e.getMessage());
            target.put(key, new ArrayList<>());
        }
    }

    private String getHexColor(int[] rgb) {
        // 将RGB值转换为两位十六进制并格式化为Minecraft颜色代码
        String hexR = String.format("%02X", rgb[0]);
        String hexG = String.format("%02X", rgb[1]);
        String hexB = String.format("%02X", rgb[2]);

        return ChatColor.COLOR_CHAR + "x" +
                ChatColor.COLOR_CHAR + hexR.charAt(0) +
                ChatColor.COLOR_CHAR + hexR.charAt(1) +
                ChatColor.COLOR_CHAR + hexG.charAt(0) +
                ChatColor.COLOR_CHAR + hexG.charAt(1) +
                ChatColor.COLOR_CHAR + hexB.charAt(0) +
                ChatColor.COLOR_CHAR + hexB.charAt(1);
    }

    // 以下方法保持不变...
    public List<String> formatMessage(List<String> imageLines, String... texts) {
        List<String> message = new ArrayList<>();
        int textStartLine = (10 - texts.length) / 2;

        for (int i = 0; i < 10; i++) {
            String line = imageLines.get(i);
            if (i >= textStartLine && i < textStartLine + texts.length) {
                line += " " + texts[i - textStartLine];
            }
            message.add(line);
        }
        return message;
    }

    public List<String> getPhaseMessage(int phase) {
        return phaseMessages.get("Phase" + phase);
    }

    public List<String> getTeamMessage(String teamName) {
        String formattedName = teamName.substring(0, 1).toUpperCase()
                + teamName.substring(1).toLowerCase()
                + "Team";
        return teamMessages.getOrDefault(formattedName, new ArrayList<>());
    }
}