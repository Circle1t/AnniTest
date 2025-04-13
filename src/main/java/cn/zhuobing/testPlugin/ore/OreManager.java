package cn.zhuobing.testPlugin.ore;

import cn.zhuobing.testPlugin.AnniTest;
import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.kit.kits.Enchanter;
import cn.zhuobing.testPlugin.kit.kits.Miner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OreManager {
    private final GameManager gameManager;
    private final KitManager kitManager;
    private final Map<Location, CoolingOre> coolingOres = new ConcurrentHashMap<>();
    private final DiamondDataManager diamondDataManager;

    // 工具等级映射
    private static final Map<Material, Integer> TOOL_LEVELS = new HashMap<>();

    static {
        TOOL_LEVELS.put(Material.WOODEN_PICKAXE, 1);
        TOOL_LEVELS.put(Material.GOLDEN_PICKAXE, 2);
        TOOL_LEVELS.put(Material.STONE_PICKAXE, 3);
        TOOL_LEVELS.put(Material.IRON_PICKAXE, 4);
        TOOL_LEVELS.put(Material.DIAMOND_PICKAXE, 5);
        TOOL_LEVELS.put(Material.NETHERITE_PICKAXE, 6);
    }

    public OreManager(GameManager gameManager, DiamondDataManager diamondDataManager,KitManager kitManager) {
        this.gameManager = gameManager;
        this.kitManager = kitManager;
        this.diamondDataManager = diamondDataManager;
        startCoolDownCheckTask();
        setGameManager();


        updateDiamondBlocks(); // 加载完配置后更新钻石块状态
    }

    private void startCoolDownCheckTask() {
        Bukkit.getScheduler().runTaskTimer(AnniTest.getInstance(), () -> {
            Iterator<Map.Entry<Location, CoolingOre>> iterator = coolingOres.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Location, CoolingOre> entry = iterator.next();
                CoolingOre coolingOre = entry.getValue();
                if (coolingOre.getRestoreTask().isCancelled()) {
                    iterator.remove();
                }
            }
        }, 0L, 20L);
    }

    public void processOreBreak(Player player, Block block) {
        OreType oreType = OreType.fromMaterial(block.getType());
        if (oreType == null) return;

        // 阶段验证
        if (gameManager.getCurrentPhase() < oreType.availablePhase) {
            block.setType(Material.COBBLESTONE);
            return;
        }

        // 工具验证
        if (!OreUtils.isValidTool(player.getInventory().getItemInMainHand(), oreType)) {
            player.sendMessage("§c需要使用正确的工具！");
            return;
        }

        // 给予奖励
        giveRewards(player, oreType, block);
        startCoolDown(block, oreType);
    }

    private void giveRewards(Player player, OreType oreType, Block block) {
        Random random = new Random();

        // 判断是否为附魔师
        boolean isEnchanter = kitManager.getPlayerKit(player.getUniqueId()) != null &&
                kitManager.getPlayerKit(player.getUniqueId()) instanceof Enchanter;
        // 给予经验
        int xp = oreType.xp;
        if (xp <= 0) {
            // 触发方块的自然掉落机制
            block.breakNaturally(player.getInventory().getItemInMainHand());
            return;
        }

        //如果是附魔师就经验翻倍
        if(isEnchanter){
            player.giveExp(xp*2);
        }else{
            player.giveExp(xp);
        }

        // 固定音量
        float volume = 0.6f;
        // 基础音调
        float basePitch = 0.5f;
        // 音调浮动范围，这里设置为 ±0.1
        float pitchVariation = 0.1f;

        // 生成随机的音调偏移值
        float randomOffset = (float) ((Math.random() * 2 - 1) * pitchVariation);
        // 计算最终的音调
        float pitch = basePitch + randomOffset;

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume, pitch);

        // 计算时运
        int fortuneLevel = OreUtils.getFortuneLevel(player.getInventory().getItemInMainHand());

        // 处理沙砾类型的掉落
        List<ItemStack> actualDrops;
        if (oreType == OreType.GRAVEL) {
            actualDrops = oreType.getGravelDrops();
        } else { //非沙砾类型
            // 动态生成掉落物
            actualDrops = new ArrayList<>();
            for (ItemStack drop : oreType.drops) {
                ItemStack toGive = drop.clone();
                actualDrops.add(toGive);
            }
        }

        // 判断玩家职业是否为矿工
        boolean isMiner = kitManager.getPlayerKit(player.getUniqueId()) != null &&
                kitManager.getPlayerKit(player.getUniqueId()) instanceof Miner;
        // 给予物品
        for (ItemStack toGive : actualDrops) {
            int baseAmount = oreType.getRandomDropAmount();
            int finalAmount = OreUtils.calculateDropAmount(baseAmount, fortuneLevel);

            // 如果是矿工，有50%概率翻倍
            if (isMiner && random.nextBoolean()) {
                finalAmount *= 2;
            }

            toGive.setAmount(finalAmount);
            player.getInventory().addItem(toGive).values().forEach(left -> {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            });
        }

        // 当玩家是附魔师时，有2%概率掉落经验瓶
        if (isEnchanter && random.nextInt(100) < 2) {
            ItemStack experienceBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
            player.getInventory().addItem(experienceBottle).values().forEach(left -> {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            });
        }

        // 损耗工具
        OreUtils.damageTool(player.getInventory().getItemInMainHand());
    }


    private void startCoolDown(Block block, OreType oreType) {
        // 判断是否为羊毛或泥土类型
        if (oreType == OreType.WOOL || oreType == OreType.DIRT) {
            block.setType(oreType.cooledForm); // 设置为冷却形态（这里是 AIR）
            return; // 不设置恢复任务，即不会重生
        }

        block.setType(oreType.cooledForm);
        CoolingOre coolingOre = new CoolingOre(block.getLocation(), oreType);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(AnniTest.getInstance(), () -> {
            if (gameManager.getCurrentPhase() >= oreType.availablePhase) {
                block.setType(oreType.getOriginalMaterial());
            }
            coolingOres.remove(block.getLocation());
        }, oreType.coolDown * 20L);

        coolingOre.setRestoreTask(task);
        coolingOres.put(block.getLocation(), coolingOre);
    }

    public void refreshAllOres() {
        coolingOres.values().forEach(coolingOre -> {
            Block block = coolingOre.getLocation().getBlock();
            if (gameManager.getCurrentPhase() >= coolingOre.getOreType().availablePhase) {
                block.setType(coolingOre.getOreType().getOriginalMaterial());
            }
        });
        coolingOres.clear();
    }

    public boolean isOreInCoolDown(Block block) {
        return coolingOres.containsKey(block.getLocation());
    }

    // 添加钻石生成点
    public void addDiamondSpawnLocation(Location location) {
        diamondDataManager.addDiamondSpawnLocation(location);
        updateDiamondBlocks();
    }

    // 移除钻石生成点
    public void removeDiamondSpawnLocation(Location location) {
        diamondDataManager.removeDiamondSpawnLocation(location);
        updateDiamondBlocks();
    }

    // 根据游戏阶段更新钻石块状态
    public void updateDiamondBlocks() {
        for (Location location : diamondDataManager.getDiamondSpawnLocations()) {
            Block block = location.getBlock();
            if (gameManager.getCurrentPhase() < 3) {
                block.setType(Material.COBBLESTONE);
            } else {
                block.setType(Material.DIAMOND_ORE);
            }
        }
    }

    public DiamondDataManager getDiamondDataManager() {
        return diamondDataManager;
    }

    private void setGameManager() {
        gameManager.setOreManager(this);
    }
}