package cn.zhuobing.testPlugin.anni;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCManager implements Listener {

    private final Map<UUID, NPCData> npcMap = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToNpcMap = new ConcurrentHashMap<>();
    private final TeamManager teamManager;
    private final GameManager gameManager;
    private final Plugin plugin;

    /** 标记因 NPC 被击杀需在玩家重返时适用死亡惩罚的玩家 */
    private final Set<UUID> npcKilledPlayers = ConcurrentHashMap.newKeySet();
    /** 标记因服务器自动清除 NPC 而不应适用死亡惩罚的玩家 */
    private final Set<UUID> autoClearedPlayers = ConcurrentHashMap.newKeySet();
    /** 标记在玩家死亡时不掉落物品的玩家（由 npcKilledPlayers 驱动） */
    private final Set<UUID> noDropPlayers = ConcurrentHashMap.newKeySet();

    public NPCManager(TeamManager teamManager, GameManager gameManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 常规离线NPC流程
        if (gameManager.getCurrentPhase() >= 1 && teamManager.isInTeam(player)) {
            // 最后一个玩家不生成NPC
            if (Bukkit.getOnlinePlayers().size() > 1) {
                if (playerToNpcMap.containsKey(playerId)) {
                    removeNPC(playerId);
                }
                npcKilledPlayers.remove(playerId);
                createNPC(player);
            }
        }

        // 检查是否所有“合法玩家”都退出了
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean anyTeamOnline = Bukkit.getOnlinePlayers().stream()
                        .anyMatch(p -> teamManager.isInTeam(p) && gameManager.getCurrentPhase() >= 1);
                if (!anyTeamOnline) {
                    plugin.getLogger().warning("[NPCManager] 所有合法玩家已退出，开始自动清除所有存留NPC");
                    clearAllNPCs();
                }
            }
        }.runTaskLater(plugin, 1L); // 延迟一个 tick 保证在线列表更新
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore() || gameManager.getCurrentPhase() < 1 || !teamManager.isInTeam(player)) {
            return;
        }
        // 将 NPC 清理与状态恢复延后 1 tick，避免遍历全世界实体阻塞 Join 导致进服卡顿
        Bukkit.getScheduler().runTaskLater(plugin, () -> runDeferredNPCJoinLogic(player), 1L);
    }

    private void runDeferredNPCJoinLogic(Player player) {
        if (!player.isOnline()) return;
        UUID playerId = player.getUniqueId();

        boolean hadNpc = playerToNpcMap.containsKey(playerId);
        if (hadNpc) {
            removeNPC(playerId);
        }

        cleanupPlayerNPCs(player);

        if (npcKilledPlayers.remove(playerId)) {
            noDropPlayers.add(playerId);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setHealth(0);
                    player.sendMessage(ChatColor.RED + "你的离线NPC已被击杀，死亡惩罚已生效！");
                }
                noDropPlayers.remove(playerId);
            }, 1L);
            return;
        }

        if (autoClearedPlayers.remove(playerId)) {
            if (player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "你的离线NPC已被服务器清除，状态已安全恢复！");
            }
            return;
        }

        if (hadNpc && player.isOnline()) {
            player.sendMessage(ChatColor.GREEN + "你的离线NPC安全的活了下来，状态已安全恢复！");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        if (noDropPlayers.contains(id)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        UUID entId = event.getEntity().getUniqueId();
        if (!npcMap.containsKey(entId)) {
            // 干净清理未注册的 [离线] 骷髅
            if (event.getEntity() instanceof Skeleton && event.getEntity().getCustomName() != null
                    && event.getEntity().getCustomName().contains("[离线]")) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }
            return;
        }
        // 正常玩家击杀NPC
        NPCData data = npcMap.remove(entId);
        playerToNpcMap.remove(data.getPlayerId());
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.getDrops().addAll(data.getSavedItems());
        npcKilledPlayers.add(data.getPlayerId());
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!npcMap.containsKey(event.getEntity().getUniqueId())) return;
        NPCData data = npcMap.get(event.getEntity().getUniqueId());
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (teamManager.isSameTeam(data.getPlayerId(), attacker.getUniqueId())) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "你不能攻击队友的离线NPC！");
                return;
            }
        }
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) event.getEntity();
            if (living.getHealth() > 0.5) living.setHealth(0.5);
        }
    }

    /** 清除并记录所有未被玩家击杀的NPC */
    private void clearAllNPCs() {
        for (NPCData data : new ArrayList<>(npcMap.values())) {
            UUID playerId = data.getPlayerId();
            autoClearedPlayers.add(playerId);
            UUID npcId = data.getNpc().getUniqueId();
            // 从映射中移除
            playerToNpcMap.remove(playerId);
            npcMap.remove(npcId);
            // 强制移除实体
            forceRemoveEntity(data.getNpc());
            plugin.getLogger().info("[NPCManager] 自动清除NPC - 玩家: " + playerId + " 实体ID: " + npcId);
        }
    }

    private void createNPC(Player player) {
        World world = Optional.ofNullable(player.getLocation().getWorld()).orElse(null);
        if (world == null) {
            plugin.getLogger().warning("[NPCManager] 玩家 " + player.getName() + " 的世界为空，无法创建离线NPC！");
            return;
        }
        Skeleton npc = (Skeleton) world.spawnEntity(player.getLocation(), EntityType.SKELETON);
        customizeNPC(npc, player);

        List<ItemStack> saved = new ArrayList<>();
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR && !SoulBoundUtil.isSoulBoundItem(item, item.getType())) {
                saved.add(item.clone());
            }
        }
        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() != Material.AIR && !SoulBoundUtil.isSoulBoundItem(off, off.getType())) {
            saved.add(off.clone());
        }

        EntityEquipment eq = npc.getEquipment();
        if (eq != null) {
            eq.setHelmet(inv.getHelmet());
            eq.setChestplate(inv.getChestplate());
            eq.setLeggings(inv.getLeggings());
            eq.setBoots(inv.getBoots());
            eq.setItemInMainHand(inv.getItemInMainHand());
            eq.setItemInOffHand(inv.getItemInOffHand());
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
            eq.setItemInMainHandDropChance(0f);
            eq.setItemInOffHandDropChance(0f);
        }

        NPCData data = new NPCData(player.getUniqueId(), npc, saved);
        npcMap.put(npc.getUniqueId(), data);
        playerToNpcMap.put(player.getUniqueId(), npc.getUniqueId());
        plugin.getLogger().info("[NPCManager] 已为玩家 " + player.getName() + " 创建离线NPC，实体ID: " + npc.getUniqueId());
    }

    private void customizeNPC(Skeleton npc, Player player) {
        npc.setAI(false);
        npc.setGravity(false);
        npc.setSilent(true);
        npc.setInvulnerable(false);
        npc.setCollidable(false);
        npc.setCanPickupItems(false);
        npc.setRemoveWhenFarAway(false);
        npc.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(0.5);
        npc.setHealth(0.5);
        npc.setInvisible(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!npc.isDead()) npc.setInvisible(false);
            }
        }.runTaskLater(plugin, 1L);
        String team = teamManager.getPlayerTeamName(player);
        ChatColor color = teamManager.getTeamColor(team);
        npc.setCustomName(ChatColor.GRAY + "[离线]" + color + player.getName());
        npc.setCustomNameVisible(true);
    }

    private void removeNPC(UUID playerId) {
        if (!playerToNpcMap.containsKey(playerId)) {
            plugin.getLogger().warning("[NPCManager] 玩家 " + playerId + " 的离线NPC不存在于映射中");
            return;
        }
        UUID npcId = playerToNpcMap.remove(playerId);
        NPCData data = npcMap.remove(npcId);
        if (data == null || data.getNpc() == null) {
            plugin.getLogger().warning("[NPCManager] NPC 数据为空，直接清理映射关系");
            return;
        }
        plugin.getLogger().info("[NPCManager] 正在移除NPC - 实体ID: " + npcId);
        forceRemoveEntity(data.getNpc());
    }

    private void cleanupPlayerNPCs(Player player) {
        String playerName = player.getName();
        plugin.getLogger().info("[NPCManager] 开始清理玩家 " + playerName + " 的所有NPC");
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Skeleton)) continue;
                String customName = entity.getCustomName();
                if (customName == null) continue;
                if (customName.contains(ChatColor.GRAY + "[离线]") && customName.contains(playerName)) {
                    count++;
                    UUID entityId = entity.getUniqueId();
                    if (npcMap.containsKey(entityId)) {
                        NPCData data = npcMap.remove(entityId);
                        if (data != null) {
                            playerToNpcMap.remove(data.getPlayerId());
                        }
                    }
                    playerToNpcMap.remove(player.getUniqueId());
                    forceRemoveEntity(entity);
                }
            }
        }
        plugin.getLogger().info("[NPCManager] 清理完成 - 共处理 " + count + " 个NPC");
    }

    private void forceRemoveEntity(Entity entity) {
        try {
            if (entity.isValid()) {
                entity.remove();
            }
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                living.setHealth(0.0);
                living.damage(1000.0);
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isValid()) {
                        plugin.getLogger().warning("[NPCManager] 实体 " + entity.getUniqueId() + " 未被移除，尝试强制移除");
                        entity.remove();
                    }
                }
            }.runTaskLater(plugin, 5L);
        } catch (Exception e) {
            plugin.getLogger().warning("强制移除实体时出错: " + e.getMessage());
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        // 阻止离线NPC在阳光下燃烧
        Entity e = event.getEntity();
        if (e instanceof Skeleton && npcMap.containsKey(e.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private static class NPCData {
        private final UUID playerId;
        private final Skeleton npc;
        private final List<ItemStack> savedItems;
        public NPCData(UUID playerId, Skeleton npc, List<ItemStack> savedItems) {
            this.playerId = playerId;
            this.npc = npc;
            this.savedItems = savedItems;
        }
        public UUID getPlayerId() { return playerId; }
        public Skeleton getNpc() { return npc; }
        public List<ItemStack> getSavedItems() { return savedItems; }
    }
}
