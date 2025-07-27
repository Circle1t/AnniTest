package cn.zhuobing.testPlugin.ore;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public enum OreType {

    // 格式：矿石类型(冷却时间, 经验值, 所需工具类型, 掉落物列表, 冷却形态, 最低工具等级, 可用阶段，方块类型)
    // ItemStack，一个是物品类型，一个是数量

    // 西瓜
    WATERMELON(
            20, 1, null,
            Arrays.asList(new ItemStack(Material.MELON_SLICE)),
            Material.AIR, 0, 0,
            new Material[]{Material.MELON}
    ),
    // 原木
    LOG(
            20, 2, "AXE",
            Arrays.asList(new ItemStack(Material.OAK_LOG)),
            Material.AIR, 0, 0,
            new Material[]{
                    Material.OAK_LOG,
                    Material.SPRUCE_LOG,
                    Material.BIRCH_LOG,
                    Material.JUNGLE_LOG,
                    Material.ACACIA_LOG,
                    Material.DARK_OAK_LOG
            }
    ),
    // 沙砾
    GRAVEL(
            20, 2, "SHOVEL",
            Arrays.asList(
                    new ItemStack(Material.FLINT),
                    new ItemStack(Material.FEATHER),
                    new ItemStack(Material.STICK),
                    new ItemStack(Material.ARROW)
            ),
            Material.COBBLESTONE, 0, 0,
            new Material[]{Material.GRAVEL}
    ),
    // 煤矿
    COAL(
            10, 6, "PICKAXE",
            Arrays.asList(new ItemStack(Material.COAL)),
            Material.COBBLESTONE, 1, 0,
            new Material[]{Material.COAL_ORE}
    ),
    // 铁矿
    IRON(
            20, 9, "PICKAXE",
            Arrays.asList(new ItemStack(Material.IRON_ORE)),
            Material.COBBLESTONE, 3, 0,
            new Material[]{Material.IRON_ORE}
    ),
    // 青金石矿
    LAPIS(
            20, 9, "PICKAXE",
            Arrays.asList(new ItemStack(Material.LAPIS_LAZULI)),
            Material.COBBLESTONE, 3, 0,
            new Material[]{Material.LAPIS_ORE}
    ),
    // 红石矿
    REDSTONE(
            20, 10, "PICKAXE",
            Arrays.asList(new ItemStack(Material.REDSTONE)),
            Material.COBBLESTONE, 3, 0,
            new Material[]{Material.REDSTONE_ORE}
    ),
    // 金矿
    GOLD(
            20, 11, "PICKAXE",
            Arrays.asList(new ItemStack(Material.GOLD_ORE)),
            Material.COBBLESTONE, 4, 0,
            new Material[]{Material.GOLD_ORE}
    ),
    // 钻石矿
    DIAMOND(
            30, 15, "PICKAXE",
            Arrays.asList(new ItemStack(Material.DIAMOND)),
            Material.COBBLESTONE, 4, 3,
            new Material[]{Material.DIAMOND_ORE}
    ),
    // 绿宝石矿
    EMERALD(
            40, 18, "PICKAXE",
            Arrays.asList(new ItemStack(Material.EMERALD)),
            Material.COBBLESTONE, 4, 0,
            new Material[]{Material.EMERALD_ORE}
    ),
    // 羊毛
    WOOL(
            0, 0, "SHEARS",
            Arrays.asList(new ItemStack(Material.AIR)),
            Material.AIR, 0, 0,
            new Material[]{
                    Material.WHITE_WOOL,
                    Material.ORANGE_WOOL,
                    Material.MAGENTA_WOOL,
                    Material.LIGHT_BLUE_WOOL,
                    Material.YELLOW_WOOL,
                    Material.LIME_WOOL,
                    Material.PINK_WOOL,
                    Material.GRAY_WOOL,
                    Material.LIGHT_GRAY_WOOL,
                    Material.CYAN_WOOL,
                    Material.PURPLE_WOOL,
                    Material.BLUE_WOOL,
                    Material.BROWN_WOOL,
                    Material.GREEN_WOOL,
                    Material.RED_WOOL,
                    Material.BLACK_WOOL
            }
    ),
    // 泥土（包括草方块及其他土类物品）
    DIRT(
            0, 0, "SHOVEL",
            Arrays.asList(new ItemStack(Material.AIR)),
            Material.AIR, 0, 0,
            new Material[]{
                    Material.DIRT,
                    Material.GRASS_BLOCK,
                    Material.SAND,
                    Material.RED_SAND,
                    Material.CLAY,
                    Material.PODZOL,
                    Material.SANDSTONE,
                    Material.RED_SANDSTONE,
                    Material.COARSE_DIRT,
                    Material.MYCELIUM,
                    Material.DIRT_PATH
            }
    ),
    // 树叶
    LEAVES(
            20, 0, "SWORD",
            Arrays.asList(new ItemStack(Material.AIR)),
            Material.AIR, 0, 0,
            new Material[]{
                    Material.OAK_LEAVES,
                    Material.SPRUCE_LEAVES,
                    Material.BIRCH_LEAVES,
                    Material.JUNGLE_LEAVES,
                    Material.ACACIA_LEAVES,
                    Material.DARK_OAK_LEAVES
            }
    );

    public final int coolDown;
    public final int xp;
    public final String toolType;
    public final List<ItemStack> drops;
    public final Material cooledForm;
    public final int minToolLevel;
    public final int availablePhase;
    private final Material[] sourceBlocks;
    private static final Random random = new Random();

    OreType(int coolDown, int xp, String toolType, List<ItemStack> drops,
            Material cooledForm, int minToolLevel, int availablePhase,
            Material[] sourceBlocks) {
        this.coolDown = coolDown;
        this.xp = xp;
        this.toolType = toolType;
        this.drops = drops;
        this.cooledForm = cooledForm;
        this.minToolLevel = minToolLevel;
        this.availablePhase = availablePhase;
        this.sourceBlocks = sourceBlocks;
    }

    public static int getRandom(int min, int max){
        Random rand = new Random();
        return rand.nextInt(min,max);
    }
    public static OreType fromMaterial(Material material) {
        for (OreType type : values()) {
            for (Material m : type.sourceBlocks) {
                if (m == material) {
                    return type;
                }
            }
        }
        return null;
    }

    public Material[] getSourceBlocks() {
        return sourceBlocks;
    }

    public Material getOriginalMaterial() {
        return sourceBlocks[0];
    }

    public int getRandomDropAmount() {
        if (this == WATERMELON || this == LAPIS || this == REDSTONE) {
            return random.nextInt(8) + 1; // 生成 1 - 8 之间的随机数
        }
        return drops.get(0).getAmount();
    }

    //沙砾随机战利品
    //每次只能获得四种中的两种物品，数量1-3不等
    public List<ItemStack> getGravelDrops() {
        if (this != GRAVEL) {
            return drops;
        }
        List<ItemStack> allPossibleDrops = new ArrayList<>(drops);
        Collections.shuffle(allPossibleDrops);
        List<ItemStack> selectedDrops = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 2; i++) {
            ItemStack drop = allPossibleDrops.get(i).clone();
            int amount = random.nextInt(3) + 1; // 生成 1 - 3 之间的随机数
            drop.setAmount(amount);
            selectedDrops.add(drop);
        }
        return selectedDrops;
    }

    public static boolean isOreInProtectedArea(OreType oreType) {
        return oreType == OreType.GRAVEL ||
                oreType == OreType.COAL ||
                oreType == OreType.IRON ||
                oreType == OreType.LAPIS ||
                oreType == OreType.REDSTONE ||
                oreType == OreType.GOLD ||
                oreType == OreType.DIAMOND ||
                oreType == OreType.EMERALD;
    }
}