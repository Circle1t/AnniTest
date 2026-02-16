package cn.zhuobing.testPlugin.kit.kits;

import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.kit.Kit;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.specialitem.items.CompassItem;
import cn.zhuobing.testPlugin.specialitem.items.SpecialArmor;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.CooldownUtil;
import cn.zhuobing.testPlugin.utils.SoulBoundUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static cn.zhuobing.testPlugin.utils.SoulBoundUtil.createSoulBoundItem;

public class Transporter extends Kit implements Listener {
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private final NexusManager nexusManager;
    private List<ItemStack> kitItems = new ArrayList<>();

    private ItemStack stoneSword;
    private ItemStack woodPickaxe;
    private ItemStack woodAxe;

    // 传送门管理
    private final Map<UUID, Teleporter> teleporters = new ConcurrentHashMap<>();

    private static final int PORTAL_COOLDOWN_MS = 5000; // 5 秒冷却
    private final CooldownUtil portalCooldown;

    // 玩家已提示传送门的标记
    private final Set<UUID> portalTipPlayers = ConcurrentHashMap.newKeySet();

    // 粒子效果任务
    private final Map<Teleporter, BukkitTask> particleTasks = new ConcurrentHashMap<>();

    public Transporter(TeamManager teamManager, KitManager kitManager, NexusManager nexusManager) {
        this.teamManager = teamManager;
        this.kitManager = kitManager;
        this.nexusManager = nexusManager;
        this.portalCooldown = new CooldownUtil(kitManager.getPlugin(), PORTAL_COOLDOWN_MS);
        setUp();
    }

    @Override
    public String getName() {
        return "传送师";
    }

    @Override
    public String getNameWithColor() {
        return ChatColor.AQUA + "传送师";
    }

    @Override
    public String getDescription() {
        return "传送师可以创建传送门供队友使用。右键方块将其变为传送门（下界石英矿石），再次右键另一方块设立第二个传送门并连接它们。队友蹲下使用传送门进行瞬移，传送后有5秒冷却。传送门只能被敌人或放置者破坏，破坏后恢复原方块。";
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.NETHER_QUARTZ_ORE);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(getNameWithColor());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Transporter",
                "",
                ChatColor.YELLOW + "你是团队的重要角色，能迅速将队友传送到战场。",
                "",
                ChatColor.AQUA + "右键方块将其变为传送门（下界石英矿石）",
                ChatColor.AQUA + "放置两个传送门后自动连接",
                ChatColor.AQUA + "队友蹲下使用传送门瞬移",
                ChatColor.AQUA + "传送后有5秒冷却时间",
                ChatColor.AQUA + "敌人轻击可摧毁",
                ChatColor.AQUA + "破坏后恢复为原来的方块",
                " "
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    public List<ItemStack> getKitArmors(Player player) {
        String teamColor = teamManager.getPlayerTeamName(player);

        return Arrays.asList(
                SpecialArmor.createArmor(Material.LEATHER_HELMET, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_CHESTPLATE, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_LEGGINGS, teamColor),
                SpecialArmor.createArmor(Material.LEATHER_BOOTS, teamColor)
        );
    }

    @Override
    public void applyKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // 皮革护甲
        List<ItemStack> armors = getKitArmors(player);
        for (ItemStack armor : armors) {
            if (armor != null) {
                switch (armor.getType()) {
                    case LEATHER_HELMET:
                        inv.setHelmet(armor);
                        break;
                    case LEATHER_CHESTPLATE:
                        inv.setChestplate(armor);
                        break;
                    case LEATHER_LEGGINGS:
                        inv.setLeggings(armor);
                        break;
                    case LEATHER_BOOTS:
                        inv.setBoots(armor);
                        break;
                    default:
                        inv.addItem(armor);
                }
            }
        }

        for (ItemStack item : kitItems) {
            inv.addItem(item);
        }
    }

    private void setUp() {
        // 石剑
        stoneSword = createSoulBoundItem(Material.STONE_SWORD, null, 1, 1, false);
        kitItems.add(stoneSword);

        // 木镐
        woodPickaxe = createSoulBoundItem(Material.WOODEN_PICKAXE, null, 1, 1, false);
        kitItems.add(woodPickaxe.clone());
        // 木斧
        woodAxe = createSoulBoundItem(Material.WOODEN_AXE, null, 1, 1, false);
        kitItems.add(woodAxe.clone());

        // 传送标记（下界石英）
        ItemStack portalMarker = createSoulBoundItem(Material.QUARTZ, null, 1, 4, true);
        ItemMeta meta = portalMarker.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "传送标记");
        portalMarker.setItemMeta(meta);
        kitItems.add(portalMarker);

        // 告示牌
        ItemStack sign = createSoulBoundItem(Material.OAK_SIGN, null, 1, 1, true);
        kitItems.add(sign);

        // 指南针
        kitItems.add(CompassItem.createCompass());
    }

    @Override
    public List<ItemStack> getKitItems() {
        return kitItems;
    }

    @Override
    public void onKitUnset(Player player) {
        // 玩家切换职业时移除所有传送门
        removePlayerPortals(player);
    }

    // 设置方块所有者
    private void setBlockOwner(Block block, UUID playerId) {
        block.setMetadata("Owner", new FixedMetadataValue(kitManager.getPlugin(), playerId.toString()));
    }

    // 获取方块所有者
    private UUID getBlockOwner(Block block) {
        if (block.hasMetadata("Owner")) {
            for (MetadataValue meta : block.getMetadata("Owner")) {
                if (meta.getOwningPlugin().equals(kitManager.getPlugin())) {
                    return UUID.fromString(meta.asString());
                }
            }
        }
        return null;
    }

    // 放置传送门
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !isThisKit(player)) return;

        // 检查是否是传送标记
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.getDisplayName().contains("传送标记")) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // 左键单击破坏传送门
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (clickedBlock.getType() == Material.NETHER_QUARTZ_ORE) {
                event.setCancelled(true);
                breakPortal(clickedBlock, player);
            }
            return;
        }

        // 右键放置传送门
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // 传送师右键自己的传送门方块可摧毁传送门
            if (clickedBlock.getType() == Material.NETHER_QUARTZ_ORE) {
                UUID ownerId = getBlockOwner(clickedBlock);
                if (ownerId != null && ownerId.equals(player.getUniqueId())) {
                    event.setCancelled(true);
                    breakPortal(clickedBlock, player);
                    return;
                }
            }

            Material type = clickedBlock.getType();
            if (type == Material.AIR || type == Material.NETHER_QUARTZ_ORE
                    || type.name().endsWith("_ORE")
                    || type == Material.MELON
                    || type == Material.GRAVEL) {
                player.sendMessage(ChatColor.RED + "不能在这里放置传送门！");
                return;
            }

            // 检查放置位置是否有效
            if (!isValidPortalLocation(clickedBlock.getLocation())) {
                player.sendMessage(ChatColor.RED + "不能在这里放置传送门！");
                return;
            }

            event.setCancelled(true);

            // 获取或创建传送器
            Teleporter teleporter = teleporters.get(player.getUniqueId());
            if (teleporter == null) {
                teleporter = new Teleporter(player.getUniqueId());
                teleporters.put(player.getUniqueId(), teleporter);
            }

            // 如果已经有完整的传送门对，先清除旧的
            if (teleporter.isLinked()) {
                teleporter.clear();
                player.sendMessage(ChatColor.AQUA + "已清除旧的传送门！");
            }

            // 设置传送门位置并改变方块类型
            Block block = clickedBlock;
            BlockState state = block.getState();
            block.setType(Material.NETHER_QUARTZ_ORE);
            setBlockOwner(block, player.getUniqueId());

            // 播放放置声音（草被破坏的声音）
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 1.0f);

            if (!teleporter.hasLoc1()) {
                teleporter.setLoc1(block.getLocation(), state);
                player.sendMessage(ChatColor.AQUA + "已放置第一个传送门！" + ChatColor.WHITE + "右键另一个方块放置第二个传送门。");
            } else if (!teleporter.hasLoc2()) {
                teleporter.setLoc2(block.getLocation(), state);
                player.sendMessage(ChatColor.AQUA + "已连接两个传送门！" + ChatColor.WHITE + "传送已建立。");

                // 连接后启动粒子效果
                startParticleEffect(teleporter);
            }
        }
    }

    // 传送门粒子：白色、竖直向上发射
    private static final double PORTAL_PARTICLE_OX = 0.12;
    private static final double PORTAL_PARTICLE_OY = 0.9;  // 竖直方向扩散大，形成向上喷涌
    private static final double PORTAL_PARTICLE_OZ = 0.12;
    private static final double PORTAL_PARTICLE_EXTRA = 0.06;

    private void startParticleEffect(Teleporter teleporter) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (teleporter.isLinked()) {
                    Location c1 = centerAbove(teleporter.getLoc1().toLocation());
                    Location c2 = centerAbove(teleporter.getLoc2().toLocation());
                    // 白色粒子：END_ROD 偏白且自带向上飘动
                    if (c1 != null) {
                        showParticles(c1, Particle.END_ROD, 18, PORTAL_PARTICLE_OX, PORTAL_PARTICLE_OY, PORTAL_PARTICLE_OZ, PORTAL_PARTICLE_EXTRA);
                        showParticles(c1, Particle.CLOUD, 8, PORTAL_PARTICLE_OX, PORTAL_PARTICLE_OY, PORTAL_PARTICLE_OZ, 0.02);
                    }
                    if (c2 != null) {
                        showParticles(c2, Particle.END_ROD, 18, PORTAL_PARTICLE_OX, PORTAL_PARTICLE_OY, PORTAL_PARTICLE_OZ, PORTAL_PARTICLE_EXTRA);
                        showParticles(c2, Particle.CLOUD, 8, PORTAL_PARTICLE_OX, PORTAL_PARTICLE_OY, PORTAL_PARTICLE_OZ, 0.02);
                    }
                } else {
                    this.cancel();
                    particleTasks.remove(teleporter);
                }
            }
        }.runTaskTimer(kitManager.getPlugin(), 0, 10); // 每0.5秒执行一次

        particleTasks.put(teleporter, task);
    }

    private Location centerAbove(Location blockLoc) {
        if (blockLoc == null || blockLoc.getWorld() == null) return null;
        return blockLoc.clone().add(0.5, 1.0, 0.5);
    }

    /** 在位置上方中心生成粒子，使用传入的粒子类型。 */
    private void showParticles(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (location == null || location.getWorld() == null) return;
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    /** 传送瞬间在两端播放白色向上粒子。 */
    private void showTeleportEffect(Location from, Location to) {
        if (from != null && from.getWorld() != null) {
            Location cFrom = from.clone().add(0.5, 1.0, 0.5);
            showParticles(cFrom, Particle.END_ROD, 35, 0.15, 0.7, 0.15, 0.12);
            showParticles(cFrom, Particle.CLOUD, 20, 0.15, 0.7, 0.15, 0.04);
        }
        if (to != null && to.getWorld() != null) {
            Location cTo = to.clone().add(0.5, 1.0, 0.5);
            showParticles(cTo, Particle.END_ROD, 35, 0.15, 0.7, 0.15, 0.12);
            showParticles(cTo, Particle.CLOUD, 20, 0.15, 0.7, 0.15, 0.04);
        }
    }

    // 检查位置是否有效
    private boolean isValidPortalLocation(Location location) {
        Block block = location.getBlock();

        // 检查是否在核心保护区域内
        if(nexusManager.isInProtectedArea(location)){
            return false;
        }

        // 检查是否在水下
        if (block.isLiquid() || block.getRelative(0, 1, 0).isLiquid()) {
            return false;
        }

        // 检查空间是否足够（至少2格高）
        return block.getRelative(0, 1, 0).getType() == Material.AIR;
    }

    // 使用传送门
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        Block block = player.getLocation().subtract(0, 0.1, 0).getBlock();

        if (block.getType() != Material.NETHER_QUARTZ_ORE) return;

        UUID ownerId = getBlockOwner(block);
        if (ownerId == null) return;

        Teleporter teleporter = teleporters.get(ownerId);
        if (teleporter == null || !teleporter.isLinked()) return;

        // 检查两端方块是否都为下界石英矿石
        Location loc1 = teleporter.getLoc1().toLocation();
        Location loc2 = teleporter.getLoc2().toLocation();
        if (loc1.getBlock().getType() != Material.NETHER_QUARTZ_ORE ||
                loc2.getBlock().getType() != Material.NETHER_QUARTZ_ORE) {
            // 清除传送门
            teleporter.clear();
            teleporters.remove(ownerId);
            BukkitTask task = particleTasks.remove(teleporter);
            if (task != null) task.cancel();
            // 播放放置声音（草被破坏的声音）
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "传送门已损坏，无法传送！");
            return;
        }

        if (portalCooldown.isOnCooldown(player)) {
            player.sendMessage(ChatColor.RED + "传送冷却中，剩余 " + portalCooldown.getSecondsLeft(player) + " 秒");
            return;
        }

        // 用Loc的equals判断
        Location blockLoc = block.getLocation();
        Location targetLoc = null;
        if (teleporter.getLoc1().toLocation().equals(blockLoc)) {
            targetLoc = teleporter.getLoc2().toLocation();
        } else if (teleporter.getLoc2().toLocation().equals(blockLoc)) {
            targetLoc = teleporter.getLoc1().toLocation();
        } else {
            return;
        }

        // 检查玩家队伍
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner == null || !teamManager.isSameTeam(player, owner)) {
            player.sendMessage(ChatColor.RED + "你不能使用其他队伍的传送门！");
            return;
        }

        // 执行传送
        Location teleportLoc = targetLoc.clone().add(0.5, 1, 0.5);
        teleportLoc.setDirection(player.getLocation().getDirection());
        player.teleport(teleportLoc);

        portalCooldown.startCooldown(player);

        // 给传送师经验
        if (owner.isOnline()) {
            owner.giveExp(1);
            owner.sendMessage(ChatColor.GREEN + player.getName() + " 使用了你的传送门！");
        }

        // 传送效果：两端播放末影人传送音效 + 传送门/火焰粒子（参考原版 Effect.MOBSPAWNER_FLAMES + ENDERMAN_TELEPORT）
        loc1.getWorld().playSound(loc1, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, (float) (0.8 + Math.random() * 0.4));
        loc2.getWorld().playSound(loc2, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, (float) (0.8 + Math.random() * 0.4));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        showTeleportEffect(block.getLocation(), targetLoc);
    }

    // 玩家移动时提示传送门
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block toBlock = event.getTo().getBlock().getRelative(BlockFace.DOWN);
        Block fromBlock = event.getFrom().getBlock().getRelative(BlockFace.DOWN);

        // 只有从非传送门方块走到传送门方块时才提示
        if (toBlock.getType() == Material.NETHER_QUARTZ_ORE) {
            if (fromBlock.getType() != Material.NETHER_QUARTZ_ORE && !portalTipPlayers.contains(player.getUniqueId())) {
                UUID ownerId = getBlockOwner(toBlock);
                if (ownerId != null) {
                    Teleporter teleporter = teleporters.get(ownerId);
                    if (teleporter != null && teleporter.isLinked()) {
                        Player owner = Bukkit.getPlayer(ownerId);
                        if (owner != null && teamManager.isSameTeam(player, owner)) {
                            player.sendMessage(ChatColor.WHITE + owner.getName() + ChatColor.AQUA + " 的传送门，蹲下使用可传送到另一端！");
                            portalTipPlayers.add(player.getUniqueId());
                        }
                    }
                }
            }
        } else {
            // 玩家离开传送门方块时移除提示标记
            portalTipPlayers.remove(player.getUniqueId());
        }
    }

    /** 敌人（或放置者）轻轻敲击一下传送门方块即摧毁传送门，无需挖掉 */
    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.NETHER_QUARTZ_ORE) return;

        UUID ownerId = getBlockOwner(block);
        if (ownerId == null) return;

        Player player = event.getPlayer();
        Player owner = Bukkit.getPlayer(ownerId);

        if (player.getUniqueId().equals(ownerId)) {
            event.setCancelled(true);
            breakPortal(block, player);
        } else if (owner != null && !teamManager.isSameTeam(player, owner)) {
            event.setCancelled(true);
            breakPortal(block, player);
        }
    }

    // 保护传送门不被破坏（挖掉时也走 breakPortal，此处保留兼容）
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.NETHER_QUARTZ_ORE) return;

        UUID ownerId = getBlockOwner(block);
        if (ownerId == null) return;

        Player breaker = event.getPlayer();
        Player owner = Bukkit.getPlayer(ownerId);

        // 所有者可以破坏
        if (breaker.getUniqueId().equals(ownerId)) {
            breakPortal(block, breaker);
            event.setCancelled(true);
        }
        // 敌人可以破坏
        else if (owner != null && !teamManager.isSameTeam(breaker, owner)) {
            breakPortal(block, breaker);
            event.setCancelled(true);
        }
        // 队友不能破坏
        else {
            event.setCancelled(true);
            breaker.sendMessage(ChatColor.RED + "这是你队友的传送门，无法破坏！");
        }
    }

    // 破坏传送门
    private void breakPortal(Block block, Player breaker) {
        UUID ownerId = getBlockOwner(block);
        if (ownerId == null) return;

        Teleporter teleporter = teleporters.get(ownerId);
        if (teleporter == null) return;

        // 播放破坏声音 - 使用草被破坏的声音
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 1.0f);

        // 清除前记录两端位置，用于播放破坏粒子（参考原版 Effect.STEP_SOUND）
        Location loc1 = teleporter.getLoc1() != null ? teleporter.getLoc1().toLocation() : null;
        Location loc2 = teleporter.getLoc2() != null ? teleporter.getLoc2().toLocation() : null;

        // 清除整个传送门对
        teleporter.clear();
        teleporters.remove(ownerId);

        if (loc1 != null) showParticles(loc1.clone().add(0.5, 0.5, 0.5), Particle.CLOUD, 18, 0.25, 0.25, 0.25, 0.03);
        if (loc2 != null) showParticles(loc2.clone().add(0.5, 0.5, 0.5), Particle.CLOUD, 18, 0.25, 0.25, 0.25, 0.03);

        // 停止粒子效果
        BukkitTask task = particleTasks.remove(teleporter);
        if (task != null) {
            task.cancel();
        }

        if (breaker.getUniqueId().equals(ownerId)) {
            breaker.sendMessage(ChatColor.AQUA + "你破坏了传送门！");
        } else {
            breaker.sendMessage(ChatColor.GREEN + "你破坏了敌人的传送门！");
        }
    }

    // 移除玩家所有传送门
    private void removePlayerPortals(Player player) {
        Teleporter teleporter = teleporters.remove(player.getUniqueId());
        if (teleporter != null) {
            teleporter.clear();

            // 停止粒子效果
            BukkitTask task = particleTasks.remove(teleporter);
            if (task != null) {
                task.cancel();
            }

            player.sendMessage(ChatColor.AQUA + "你的传送门已被移除！");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 将退出消息设置为 null，这样就不会有退出信息提示
        event.setQuitMessage(null);
        removePlayerPortals(event.getPlayer());
    }

    private boolean isThisKit(Player player) {
        return kitManager.getPlayerKit(player.getUniqueId()) instanceof Transporter;
    }

    // 传送器内部类
    private static class Teleporter {
        private final UUID owner;
        private Loc loc1;
        private BlockState state1;
        private Loc loc2;
        private BlockState state2;
        private long nextUse;

        public Teleporter(UUID owner) {
            this.owner = owner;
        }

        public void setLoc1(Location location, BlockState state) {
            this.loc1 = new Loc(location);
            this.state1 = state;
        }

        public void setLoc2(Location location, BlockState state) {
            this.loc2 = new Loc(location);
            this.state2 = state;
        }

        public Loc getLoc1() {
            return loc1;
        }

        public Loc getLoc2() {
            return loc2;
        }

        public boolean hasLoc1() {
            return loc1 != null;
        }

        public boolean hasLoc2() {
            return loc2 != null;
        }

        public boolean isLinked() {
            return loc1 != null && loc2 != null;
        }

        public UUID getOwner() {
            return owner;
        }

        public void clear() {
            if (state1 != null) {
                // 只在当前方块还是下界石英矿石时才恢复
                Location loc = state1.getLocation();
                if (loc.getBlock().getType() == Material.NETHER_QUARTZ_ORE) {
                    state1.update(true, false);
                }
                state1 = null;
            }
            if (state2 != null) {
                Location loc = state2.getLocation();
                if (loc.getBlock().getType() == Material.NETHER_QUARTZ_ORE) {
                    state2.update(true, false);
                }
                state2 = null;
            }
            loc1 = null;
            loc2 = null;
        }

        public void delay() {
            this.nextUse = System.currentTimeMillis() + 5000;
        }

        public boolean canUse() {
            return System.currentTimeMillis() >= nextUse;
        }
    }

    // 位置辅助类
    private static class Loc {
        private final int x;
        private final int y;
        private final int z;
        private final World world;

        public Loc(Location location) {
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
            this.world = location.getWorld();
        }

        public Location toLocation() {
            return new Location(world, x, y, z);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Loc loc = (Loc) obj;
            return x == loc.x && y == loc.y && z == loc.z && world.equals(loc.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, world);
        }
    }
}