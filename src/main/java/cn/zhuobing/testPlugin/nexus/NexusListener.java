package cn.zhuobing.testPlugin.nexus;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.ore.OreType;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.MessageRenderer;
import cn.zhuobing.testPlugin.utils.MessageUtil;
import cn.zhuobing.testPlugin.xp.XPManager;
import cn.zhuobing.testPlugin.xp.XPRewardType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NexusListener implements Listener {

    private final NexusManager nexusManager;
    private final NexusInfoBoard nexusInfoBoard;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final MessageRenderer messageRenderer;
    private final NexusAntiCheat nexusAntiCheat;
    private final XPManager xpManager = XPManager.getInstance();
    private final Plugin plugin;

    private String winningTeam = null;
    private final Map<String, Long> lastDigTime = new HashMap<>(); // 记录核心最后被挖掘的时间
    private final Map<String, String> lastDigPlayer = new HashMap<>(); // 记录核心最后被挖掘的玩家
    private static final int EXCLUSIVE_PERIOD = 500; // 500毫秒专属期，在这期间其他人不能挖核心
    private int destroyedTeamIndex = 1;


    public NexusListener(NexusManager nexusManager, NexusInfoBoard nexusInfoBoard, GameManager gameManager, TeamManager teamManager,Plugin plugin,MessageRenderer messageRenderer,NexusAntiCheat antiCheat) {
        this.nexusManager = nexusManager;
        this.nexusInfoBoard = nexusInfoBoard;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.messageRenderer = messageRenderer;
        this.nexusAntiCheat = antiCheat;
        this.plugin = plugin;
}
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Location loc = event.getBlock().getLocation();
        Material type = event.getBlock().getType();

        // 检测是否在保护区域
        if (nexusManager.isInProtectedArea(loc)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "此区域禁止放置方块！");
        }
    }

    // 检测船的放置
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Block clickedBlock = event.getClickedBlock();
        BlockFace face = event.getBlockFace();

        if (item == null || clickedBlock == null || face == null) return;

        // 检测是否为放置船
        if (item.getType().name().endsWith("_BOAT")) {
            Location placementLoc = clickedBlock.getRelative(face).getLocation();
            if (nexusManager.isInProtectedArea(placementLoc)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "核心保护区域内禁止放置船！");
            }
            return;
        }

        // 禁止放置水桶、岩浆桶、鱼桶等液体容器
        Material mat = item.getType();
        if ((mat == Material.WATER_BUCKET ||
                mat == Material.LAVA_BUCKET ||
                mat == Material.AXOLOTL_BUCKET ||
                mat == Material.COD_BUCKET ||
                mat == Material.SALMON_BUCKET ||
                mat == Material.TROPICAL_FISH_BUCKET ||
                mat == Material.PUFFERFISH_BUCKET) &&
                nexusManager.isInProtectedArea(clickedBlock.getRelative(face).getLocation())) {

            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "核心保护区域内禁止放置液体！");
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

                // ===== 专属期验证 =====
                long currentTime = System.currentTimeMillis();
                String lastPlayer = lastDigPlayer.get(teamName);
                Long lastTime = lastDigTime.get(teamName);

                // 检查是否在专属期内且非本人操作
                if (lastTime != null &&
                        currentTime - lastTime < EXCLUSIVE_PERIOD &&
                        !player.getName().equals(lastPlayer)) {

                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "该核心正在被攻击，请稍后再试！");
                    return;
                }

                // 更新专属期记录
                lastDigTime.put(teamName, currentTime);
                lastDigPlayer.put(teamName, player.getName());
                // ===== 专属期验证结束 =====

                event.setCancelled(true); // 阻止方块掉落

                isNexus = true;
                // 挖掘时播放火花粒子效果
                playFlameParticles(nexusLocation);

                int currentHealth = nexusManager.getNexusHealth(teamName);
                if (gameManager.getCurrentPhase() == 5) {
                    currentHealth -= 2;
                    xpManager.addXP(player, XPRewardType.NEXUS_DAMAGED_PHASE_5);
                } else {
                    currentHealth--;
                    xpManager.addXP(player,XPRewardType.NEXUS_DAMAGED_PHASE_1_4);
                }

                // 这里的反作弊没有开源，删除相关反作弊代码即可
                nexusAntiCheat.recordPlayerDig(player,teamName);

                nexusManager.setNexusHealth(teamName, currentHealth);
                // 计分板更新已节流并推迟到下一 tick，不阻塞破坏事件
                nexusInfoBoard.updateInfoBoard();

                if (currentHealth <= 0) {
                    // 核心被摧毁时清除专属期记录
                    lastDigTime.remove(teamName);
                    lastDigPlayer.remove(teamName);
                    // 核心血量为 0，变为基岩
                    block.setType(Material.BEDROCK);
                    // 全局广播：核心被摧毁的消息（所有在线玩家可见）
                    List<String> welcomeMessage = messageRenderer.formatMessage(
                            messageRenderer.getTeamMessage(teamName),
                            teamManager.getTeamColor(teamName) + chineseTeamName + "队" + ChatColor.GOLD + " 核心已被摧毁！",
                            ChatColor.GOLD + "摧毁者 " + teamManager.getTeamColor(playerTeam) + player.getName()
                    );
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage(" ");
                        for (String line : welcomeMessage) {
                            online.sendMessage(line);
                        }
                    }
                    // 对被摧毁队伍的所有玩家发放补贴奖励
                    if(destroyedTeamIndex == 1){
                        xpManager.addXPToTeam(teamManager.getPlayersInTeam(teamName), XPRewardType.KNOCKED_OUT_1ST);
                    } else if (destroyedTeamIndex == 2) {
                        xpManager.addXPToTeam(teamManager.getPlayersInTeam(teamName), XPRewardType.KNOCKED_OUT_2ND);
                    } else if (destroyedTeamIndex == 3) {
                        xpManager.addXPToTeam(teamManager.getPlayersInTeam(teamName), XPRewardType.KNOCKED_OUT_3RD);
                    }
                    destroyedTeamIndex++;

                    //Bukkit.broadcastMessage(teamManager.getTeamColor(teamName) + chineseTeamName + "队" + ChatColor.GOLD + " 核心已被摧毁 | 破坏者 " + teamManager.getTeamColor(playerTeam) + player.getName());
                    // 检查是否只有一个队伍的核心未被摧毁
                    // 延迟检查获胜者，确保所有核心状态更新完成
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        String winner = checkForWinner();
                        winningTeam = winner;
                        if (winner != null) {
                            gameManager.endGameWithWinner(winner);
                            // 为获胜队伍发放xp奖励
                            xpManager.addWinRewardToTeam(teamManager.getPlayersInTeam(winner), nexusManager.getNexusHealth(winningTeam));
                        }
                    }, 10L); // 延迟10 tick执行

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
            if(OreType.isOreInProtectedArea(OreType.fromMaterial(event.getBlock().getType()))){
                return;
            }
            event.setCancelled(true);
            //event.getPlayer().sendMessage(ChatColor.RED + "此区域受到核心保护！");
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
            double distance = player.getLocation().distance(nexusLocation);
            if (distance <= MAX_DISTANCE) {
                // 距离核心 15 格范围内，所有人（含未选队观战）都能听到挖掘声效
                float volume = (float) (1 - (distance / MAX_DISTANCE));
                if (volume < 0.1f) {
                    volume = 0.1f; // 设置最小音量
                }
                float pitch = (float) (1 + (Math.random() * 0.3) - 0.2);
                player.playSound(nexusLocation, Sound.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, volume, pitch);
            } else {
                // 超出 15 格，仅被挖掘核心的队伍玩家能听见警报音效
                String playerTeamName = teamManager.getPlayerTeamName(player);
                if (playerTeamName != null && playerTeamName.equalsIgnoreCase(teamName)) {
                    player.playSound(player.getLocation(), ALERT_SOUND, SoundCategory.BLOCKS, ALERT_VOLUME, ALERT_PITCH);
                }
            }
        }
    }
    /** 核心被摧毁时全服播放爆炸音效（在每位玩家位置播放，确保所有人都能听见） */
    private void playTntExplosionSound(Location location) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 1.5f, 1f);
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