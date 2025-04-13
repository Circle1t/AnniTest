package cn.zhuobing.testPlugin.store;

import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StoreManager {
    private final Plugin plugin;
    private FileConfiguration config;
    private File configFile;
    private final Map<Material, Integer> itemPrices;
    private final Map<Material, Integer> itemAmounts;
    private final List<Location> brewSignLocations;
    private final List<Location> weaponSignLocations;
    private final LinkedHashMap<Material, String> itemDisplayNames;
    private boolean isPhase4 = false; // 是否进入阶段四
    private String mapFolderName;

    public StoreManager(Plugin plugin) {
        this.plugin = plugin;
        this.itemPrices = new LinkedHashMap<>();
        this.itemAmounts = new LinkedHashMap<>();
        this.brewSignLocations = new ArrayList<>();
        this.weaponSignLocations = new ArrayList<>();
        this.itemDisplayNames = new LinkedHashMap<>();
        initializeItems();
    }

    public void setPhase4(boolean isPhase4) {
        this.isPhase4 = isPhase4;
    }

    private void initializeItems() {
        // 酿造商店物品
        // 第一行物品
        addItem(Material.BREWING_STAND, 10, 1, "酿造台");
        addItem(Material.GLASS_BOTTLE, 1, 3, "玻璃瓶");
        addItem(Material.NETHER_WART, 5, 1, "下界疣");

        // 第二行物品
        addItem(Material.REDSTONE, 3, 1, "红石粉");
        addItem(Material.FERMENTED_SPIDER_EYE, 3, 1, "发酵蛛眼");
        addItem(Material.MAGMA_CREAM, 1, 1, "岩浆膏");
        addItem(Material.SUGAR, 2, 1, "糖");
        addItem(Material.GLISTERING_MELON_SLICE, 2, 1, "闪烁的西瓜片");
        addItem(Material.GHAST_TEAR, 15, 1, "恶魂之泪");
        addItem(Material.GOLDEN_CARROT, 2, 1, "金胡萝卜");

        // 武器商店物品
        // 第一行
        addItem(Material.IRON_HELMET, 3, 1, "铁头盔");
        addItem(Material.IRON_CHESTPLATE, 5, 1, "铁胸甲");
        addItem(Material.IRON_LEGGINGS, 5, 1, "铁裤子");
        addItem(Material.IRON_BOOTS, 3, 1, "铁鞋子");
        addItem(Material.IRON_SWORD, 1, 1, "铁剑");
        addItem(Material.BOW, 1, 1, "弓");
        addItem(Material.ARROW, 1, 16, "箭");

        // 第二行
        addItem(Material.COOKED_BEEF, 5, 10, "牛排");
        addItem(Material.CAKE, 5, 1, "蛋糕");
        addItem(Material.COBWEB, 1, 1, "蜘蛛网");
        addItem(Material.EXPERIENCE_BOTTLE, 2, 3, "附魔之瓶");
        addItem(Material.ENDER_PEARL, 35, 1, "末影珍珠");
        addItem(Material.MILK_BUCKET, 5, 1, "牛奶桶");
        addItem(Material.SPONGE, 5, 1, "海绵");
    }

    private void addItem(Material material, int price, int amount, String displayName) {
        itemPrices.put(material, price);
        itemAmounts.put(material, amount);
        itemDisplayNames.put(material, displayName);
    }

    public void loadConfig(String mapFolderName, World world) {
        this.mapFolderName = mapFolderName;
        File mapsFolder = new File(plugin.getDataFolder(), "maps");
        if (!mapsFolder.exists()) {
            mapsFolder.mkdirs();
        }
        File mapFolder = new File(mapsFolder, mapFolderName);
        if (!mapFolder.exists()) {
            mapFolder.mkdirs();
        }
        File configFolder = new File(mapFolder, AnniConfigManager.MAP_CONFIG_FOLDER);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        configFile = new File(configFolder, "store-config.yml");
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                config = YamlConfiguration.loadConfiguration(configFile);
                config.set("brew-signs.locations", new ArrayList<>());
                config.set("weapon-signs.locations", new ArrayList<>());
                saveConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadBrewSignLocations(world);
        loadWeaponSignLocations(world);
    }

    private void saveConfig() {
        File mapsFolder = new File(plugin.getDataFolder(), "maps");
        if (!mapsFolder.exists()) {
            mapsFolder.mkdirs();
        }
        File mapFolder = new File(mapsFolder, mapFolderName);
        if (!mapFolder.exists()) {
            mapFolder.mkdirs();
        }
        File configFolder = new File(mapFolder, AnniConfigManager.MAP_CONFIG_FOLDER);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        configFile = new File(configFolder, "store-config.yml");

        config.set("brew-signs.locations", null);
        int index = 0;
        for (Location location : brewSignLocations) {
            config.set("brew-signs.locations." + index, location);
            index++;
        }

        config.set("weapon-signs.locations", null);
        index = 0;
        for (Location location : weaponSignLocations) {
            config.set("weapon-signs.locations." + index, location);
            index++;
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBrewSignLocations(World world) {
        brewSignLocations.clear();
        if (config.contains("brew-signs.locations")) {
            for (String key : config.getConfigurationSection("brew-signs.locations").getKeys(false)) {
                Location location = config.getLocation("brew-signs.locations." + key);
                if (location != null) {
                    location.setWorld(world);
                    brewSignLocations.add(location);
                    Block block = location.getBlock();
                    if (block.getState() instanceof Sign) {
                        Sign sign = (Sign) block.getState();
                        updateBrewSignContent(sign);
                    }
                }
            }
        }
    }

    private void loadWeaponSignLocations(World world) {
        weaponSignLocations.clear();
        if (config.contains("weapon-signs.locations")) {
            for (String key : config.getConfigurationSection("weapon-signs.locations").getKeys(false)) {
                Location location = config.getLocation("weapon-signs.locations." + key);
                if (location != null) {
                    location.setWorld(world);
                    weaponSignLocations.add(location);
                    Block block = location.getBlock();
                    if (block.getState() instanceof Sign) {
                        Sign sign = (Sign) block.getState();
                        updateWeaponSignContent(sign);
                    }
                }
            }
        }
    }

    public void openBrewStoreInterface(Player player) {
        Inventory storeInventory = Bukkit.createInventory(null, 18, ChatColor.DARK_PURPLE + "酿造商店");
        List<Material> materials = new ArrayList<>();
        materials.add(Material.BREWING_STAND);
        materials.add(Material.GLASS_BOTTLE);
        materials.add(Material.NETHER_WART);
        materials.add(Material.REDSTONE);
        materials.add(Material.FERMENTED_SPIDER_EYE);
        materials.add(Material.MAGMA_CREAM);
        materials.add(Material.SUGAR);
        materials.add(Material.GLISTERING_MELON_SLICE);
        materials.add(Material.GHAST_TEAR);
        materials.add(Material.GOLDEN_CARROT);

        // 如果进入阶段四，添加烈焰粉
        if (isPhase4) {
            addItem(Material.BLAZE_POWDER, 15, 1, "烈焰粉"); // 添加烈焰粉
            materials.add(Material.BLAZE_POWDER);
        }

        // 第一行放置前3个物品（0 - 2槽位）
        for (int i = 0; i < 3 && i < materials.size(); i++) {
            addItemToSlot(storeInventory, i, materials.get(i));
        }

        // 第二行从9号槽位开始放置后续物品（9 - 17槽位）
        for (int i = 3; i < materials.size(); i++) {
            addItemToSlot(storeInventory, 6 + i, materials.get(i)); // 9号槽位开始
        }

        player.openInventory(storeInventory);
    }

    public void openWeaponStoreInterface(Player player) {
        // 将武器商店界面标题设置为黑色
        Inventory storeInventory = Bukkit.createInventory(null, 18, ChatColor.BLACK + "武器商店");
        List<Material> materials = new ArrayList<>();
        materials.add(Material.IRON_HELMET);
        materials.add(Material.IRON_CHESTPLATE);
        materials.add(Material.IRON_LEGGINGS);
        materials.add(Material.IRON_BOOTS);
        materials.add(Material.IRON_SWORD);
        materials.add(Material.BOW);
        materials.add(Material.ARROW);
        materials.add(Material.COOKED_BEEF);
        materials.add(Material.CAKE);
        materials.add(Material.COBWEB);
        materials.add(Material.EXPERIENCE_BOTTLE);
        materials.add(Material.ENDER_PEARL);
        materials.add(Material.MILK_BUCKET);
        materials.add(Material.SPONGE);

        // 第一行放置前7个物品（0 - 6槽位）
        for (int i = 0; i < 7 && i < materials.size(); i++) {
            addItemToSlot(storeInventory, i, materials.get(i));
        }

        // 第二行从9号槽位开始放置后续物品（9 - 17槽位）
        for (int i = 7; i < materials.size(); i++) {
            addItemToSlot(storeInventory, 2 + i, materials.get(i)); // 9号槽位开始
        }

        player.openInventory(storeInventory);
    }

    private void addItemToSlot(Inventory inv, int slot, Material material) {
        int price = itemPrices.get(material);
        int amount = itemAmounts.get(material);

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + itemDisplayNames.get(material));
        meta.setLore(Collections.singletonList(
                ChatColor.GOLD + "价格：" + price + "金锭 数量：" + amount
        ));
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    public boolean canPlayerAfford(Player player, Material itemMaterial) {
        int required = itemPrices.get(itemMaterial);
        int total = Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && item.getType() == Material.GOLD_INGOT)
                .mapToInt(ItemStack::getAmount)
                .sum();
        return total >= required;
    }

    public void purchaseItem(Player player, Material itemMaterial) {
        int price = itemPrices.get(itemMaterial);
        int amount = itemAmounts.get(itemMaterial);

        if (!canPlayerAfford(player, itemMaterial)) {
            player.sendMessage(ChatColor.RED + "你没有足够的金锭！");
            return;
        }

        // 扣除金锭
        int remaining = price;
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == Material.GOLD_INGOT) {
                int deduct = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - deduct);
                remaining -= deduct;
                if (remaining <= 0) break;
            }
        }

        // 给予物品
        ItemStack purchased = new ItemStack(itemMaterial, amount);
        player.getInventory().addItem(purchased).values().forEach(leftover ->
                player.getWorld().dropItem(player.getLocation(), leftover)
        );
        player.sendMessage(ChatColor.GREEN + "成功购买 " + itemDisplayNames.get(itemMaterial));
    }

    public boolean addBrewSignLocation(Location location) {
        if (brewSignLocations.contains(location)) return false;

        Block block = location.getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            updateBrewSignContent(sign);
        }

        brewSignLocations.add(location);
        saveBrewSignLocations();
        return true;
    }

    public boolean addWeaponSignLocation(Location location) {
        if (weaponSignLocations.contains(location)) return false;

        Block block = location.getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            updateWeaponSignContent(sign);
        }

        weaponSignLocations.add(location);
        saveWeaponSignLocations();
        return true;
    }

    public boolean removeSignLocation(Location location) {
        boolean removed = brewSignLocations.remove(location);
        if (!removed) {
            removed = weaponSignLocations.remove(location);
        }
        if (removed) {
            saveBrewSignLocations();
            saveWeaponSignLocations();
        }
        return removed;
    }

    private void updateBrewSignContent(Sign sign) {
        sign.setLine(0, " ");
        sign.setLine(1, ChatColor.LIGHT_PURPLE + "[酿造商店]");
        sign.setLine(2, ChatColor.GREEN + "右键打开");
        sign.update(true);
    }

    private void updateWeaponSignContent(Sign sign) {
        sign.setLine(0, " ");
        sign.setLine(1, ChatColor.WHITE + "[武器商店]");
        sign.setLine(2, ChatColor.GREEN + "右键打开");
        sign.update(true);
    }

    private void saveBrewSignLocations() {
        List<String> locations = brewSignLocations.stream()
                .map(loc -> loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ())
                .toList();
        config.set("brew-signs", locations);
        saveConfig();
    }

    private void saveWeaponSignLocations() {
        List<String> locations = weaponSignLocations.stream()
                .map(loc -> loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ())
                .toList();
        config.set("weapon-signs", locations);
        saveConfig();
    }

    public boolean isBrewSignLocation(Location location) {
        return brewSignLocations.stream()
                .anyMatch(loc -> loc.equals(location));
    }

    public boolean isWeaponSignLocation(Location location) {
        return weaponSignLocations.stream()
                .anyMatch(loc -> loc.equals(location));
    }

    public Plugin getPlugin() {
        return plugin;
    }
}