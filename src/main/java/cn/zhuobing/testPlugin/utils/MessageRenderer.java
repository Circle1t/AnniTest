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

    // Minecraft颜色与RGB近似值映射
    private static final Map<ChatColor, int[]> COLOR_MAP = new HashMap<>() {{
        put(ChatColor.BLACK, new int[]{0, 0, 0});
        put(ChatColor.DARK_BLUE, new int[]{0, 0, 170});
        put(ChatColor.DARK_GREEN, new int[]{0, 170, 0});
        put(ChatColor.DARK_AQUA, new int[]{0, 170, 170});
        put(ChatColor.DARK_RED, new int[]{170, 0, 0});
        put(ChatColor.DARK_PURPLE, new int[]{170, 0, 170});
        put(ChatColor.GOLD, new int[]{255, 170, 0});
        put(ChatColor.GRAY, new int[]{170, 170, 170});
        put(ChatColor.DARK_GRAY, new int[]{85, 85, 85});
        put(ChatColor.BLUE, new int[]{85, 85, 255});
        put(ChatColor.GREEN, new int[]{85, 255, 85});
        put(ChatColor.AQUA, new int[]{85, 255, 255});
        put(ChatColor.RED, new int[]{255, 85, 85});
        put(ChatColor.LIGHT_PURPLE, new int[]{255, 85, 255});
        put(ChatColor.YELLOW, new int[]{255, 255, 85});
        put(ChatColor.WHITE, new int[]{255, 255, 255});
    }};

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
                // 添加默认空数据防止NPE
                target.put(key, new ArrayList<>());
                return;
            }

            BufferedImage image = ImageIO.read(is);
            List<String> lines = new ArrayList<>();

            for (int y = 0; y < 10; y++) {
                StringBuilder line = new StringBuilder();
                for (int x = 0; x < 10; x++) {
                    int rgb = image.getRGB(x, y);
                    ChatColor color = getNearestColor(new int[]{
                            (rgb >> 16) & 0xFF,
                            (rgb >> 8) & 0xFF,
                            rgb & 0xFF
                    });
                    line.append(color).append("█");
                }
                lines.add(line.toString());
            }
            target.put(key, lines);
        } catch (IOException e) {
            plugin.getLogger().warning("加载图片失败: " + e.getMessage());
            target.put(key, new ArrayList<>()); // 确保有默认值
        }
    }

    private ChatColor getNearestColor(int[] rgb) {
        ChatColor closest = ChatColor.WHITE;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<ChatColor, int[]> entry : COLOR_MAP.entrySet()) {
            double distance = Math.pow(rgb[0]-entry.getValue()[0], 2)
                    + Math.pow(rgb[1]-entry.getValue()[1], 2)
                    + Math.pow(rgb[2]-entry.getValue()[2], 2);
            if (distance < minDistance) {
                minDistance = distance;
                closest = entry.getKey();
            }
        }
        return closest;
    }

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
        // 转换格式：red -> RedTeam
        String formattedName = teamName.substring(0, 1).toUpperCase()
                + teamName.substring(1).toLowerCase()
                + "Team";
        return teamMessages.getOrDefault(formattedName, new ArrayList<>());
    }
}