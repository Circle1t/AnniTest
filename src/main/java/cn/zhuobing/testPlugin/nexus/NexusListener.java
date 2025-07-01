package cn.zhuobing.testPlugin.nexus;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.MessageRenderer;
import cn.zhuobing.testPlugin.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Map;

public class NexusListener implements Listener {

    private final NexusManager nexusManager;
    private final NexusInfoBoard nexusInfoBoard;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final MessageRenderer messageRenderer;

    private String winningTeam = null;

    private final Plugin plugin;

    public NexusListener(NexusManager nexusManager, NexusInfoBoard nexusInfoBoard, GameManager gameManager, TeamManager teamManager,Plugin plugin,MessageRenderer messageRenderer) {
        this.nexusManager = nexusManager;
        this.nexusInfoBoard = nexusInfoBoard;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.messageRenderer = messageRenderer;
        this.plugin = plugin;
}
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (nexusManager.isInProtectedArea(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "此区域禁止放置方块！");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String playerTeam = getTeamName(player);
        boolean isNexus = false;

        for (Map.Entry<String, Location> entry : nexusManager.getNexusLocations().entrySet()) {
            String teamName = entry.getKey();
            String chineseTeamName = teamManager.getTeamChineseName(teamName);
            Location nexusLocation = entry.getValue();
            if (block.getLocation().equals(nexusLocation)) {
                if (playerTeam == null) {
                    player.sendMessage(ChatColor.RED + "你必须在一个队伍中才能挖掘核心！");
                    event.setCancelled(true);
                    return;
                }
                if (playerTeam.equals(teamName)) {
                    // 同一队伍玩家不能破坏核心
                    player.sendMessage(ChatColor.RED + "你不能破坏自己队伍的核心！");
                    event.setCancelled(true);
                    return;
                }
                if (gameManager.getCurrentPhase() < 2) {
                    // 二阶段前不能破坏核心
                    player.sendMessage(ChatColor.RED + "二阶段前不能破坏核心");
                    event.setCancelled(true);
                    return;
                }
                event.setCancelled(true); // 阻止方块掉落

                isNexus = true;
                // 挖掘时播放火花粒子效果
                playFlameParticles(nexusLocation);

                int currentHealth = nexusManager.getNexusHealth(teamName);
                if (gameManager.getCurrentPhase() == 5) {
                    currentHealth -= 2;
                } else {
                    currentHealth--;
                }
                nexusManager.setNexusHealth(teamName, currentHealth);
                // 更新计分板
                nexusInfoBoard.updateInfoBoard();

                if (currentHealth <= 0) {
                    // 核心血量为 0，变为基岩
                    block.setType(Material.BEDROCK);
                    // 发送队伍消息
                    player.sendMessage(" ");
                    List<String> welcomeMessage = messageRenderer.formatMessage(
                            messageRenderer.getTeamMessage(teamName),
                            teamManager.getTeamColor(teamName) + chineseTeamName + "队" + ChatColor.GOLD + " 核心已被摧毁！" ,
                            ChatColor.GOLD + "摧毁者 " + teamManager.getTeamColor(playerTeam) + player.getName()
                    );
                    for (String line : welcomeMessage) {
                        player.sendMessage(line);
                    }
                    //Bukkit.broadcastMessage(teamManager.getTeamColor(teamName) + chineseTeamName + "队" + ChatColor.GOLD + " 核心已被摧毁 | 破坏者 " + teamManager.getTeamColor(playerTeam) + player.getName());
                    // 检查是否只有一个队伍的核心未被摧毁
                    // 延迟检查获胜者，确保所有核心状态更新完成
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        String winner = checkForWinner();
                        if (winner != null) {
                            gameManager.endGameWithWinner(winner);
                        }
                    }, 2L); // 延迟2 tick执行

                    // 播放 TNT 爆炸声音给全局玩家
                    playTntExplosionSound(nexusLocation);
                } else {
                    //游戏结束后将不能挖掘核心
                    if(gameManager.isGameOver()){
                        player.sendMessage(ChatColor.RED + "游戏已结束！");
                        return;
                    }
                    String message = teamManager.getTeamColor(playerTeam) + player.getName() + ChatColor.GOLD + " 正在破坏 " +
                            teamManager.getTeamColor(teamName) + translate(teamName) + "队" + ChatColor.GOLD + " 核心 " + ChatColor.RED + currentHealth;
                    // 向所有在线玩家发送行动栏消息
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        MessageUtil.sendActionBarMessage(onlinePlayer, message);
                    }
                    // 重置核心方块
                    block.setType(block.getType());
                    // 减少耐久
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item != null && item.getType().getMaxDurability() > 0) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta instanceof Damageable) {
                            Damageable damageable = (Damageable) meta;
                            int newDamage = damageable.getDamage() + 1;
                            damageable.setDamage(newDamage);

                            // 检查武器是否损坏
                            if (newDamage >= item.getType().getMaxDurability()) {
                                player.getInventory().setItemInMainHand(null);
                                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                            } else {
                                item.setItemMeta(meta);
                            }
                        }
                    }
                }

                // 播放声音逻辑
                playSounds(nexusLocation, teamName);

                if (winningTeam != null) {
                    gameManager.endGameWithWinner(winningTeam);
                }

                break;
            }
        }
        if (nexusManager.isInProtectedArea(event.getBlock().getLocation()) && !isNexus) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "此区域受到核心保护！");
        }
    }

    private void playSounds(Location nexusLocation, String teamName) {
        final double MAX_DISTANCE = 15;
        final float ALERT_VOLUME = 0.65f; // 警报音效的音量
        final float ALERT_PITCH = 2.0f;  // 警报音效的音调
        final Sound ALERT_SOUND = Sound.BLOCK_NOTE_BLOCK_PLING; // 警报音效

        // 检查 teamName 是否为 null 或空字符串
        if (teamName == null || teamName.isEmpty()) {
            return; // 如果 teamName 无效，直接返回
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if(!teamManager.isInTeam(player)){
                continue;
            }
            double distance = player.getLocation().distance(nexusLocation);
            if (distance <= MAX_DISTANCE) {
                // 距离核心 15 格范围内，播放音调不同的铁砧放置声，音量随距离衰减
                float volume = (float) (1 - (distance / MAX_DISTANCE));
                if (volume < 0.1f) {
                    volume = 0.1f; // 设置最小音量
                }
                // 缩小音调的随机范围，让音调在 0.9 到 1.1 之间浮动
                float pitch = (float) (1 + (Math.random() * 0.3) - 0.2);
                player.playSound(nexusLocation, Sound.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, volume, pitch);
            } else {
                // 超出 30 格范围，只有被挖掘核心的队伍玩家能听见警报音效
                String playerTeamName = teamManager.getPlayerTeamName(player);
                if (playerTeamName != null && playerTeamName.equalsIgnoreCase(teamName)) {
                    player.playSound(player.getLocation(), ALERT_SOUND, SoundCategory.BLOCKS, ALERT_VOLUME, ALERT_PITCH);
                }
            }
        }
    }
    private void playTntExplosionSound(Location location) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.5f, 1f);
        }
    }

    private String getTeamName(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                return team.getName();
            }
        }
        return null;
    }

    private String checkForWinner() {
        int destroyed = 0;
        String result = null;
        Map<String, Integer> nexusHealthOfAllTeam = nexusManager.getNexusHealthOfAllTeam();
        for (Map.Entry<String, Integer> entry : nexusHealthOfAllTeam.entrySet()) {
            if (entry.getValue() <= 0) {
                destroyed++;
            } else {
                result = entry.getKey();
            }
        }
        if (destroyed == nexusHealthOfAllTeam.size() - 1) {
            return result;
        }
        return null;
    }

    private String translate(String engName) {
        Map<String, String> englishToChineseMap = teamManager.getEnglishToChineseMap();
        return englishToChineseMap.get(engName);
    }


    /**
     * 在指定位置播放火花粒子效果，让火花粒子更集中
     * @param location 粒子效果播放的位置
     */
    private void playFlameParticles(Location location) {
        Location center = location.clone().add(0.5, 0.5, 0.5);
        // 减小偏移量，让粒子更集中
        double offset = 0.02;
        location.getWorld().spawnParticle(Particle.FLAME, center, 10, offset, offset, offset, 0.1);
    }
}