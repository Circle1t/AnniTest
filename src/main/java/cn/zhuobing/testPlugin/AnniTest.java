/**
 * 项目名称：AnniTest
 * 作者：灼冰/Circle1t
 * 作者联系方式：
 *   BiliBili：烧烤蒸馏水
 * 项目开发时间：2025年2月
 * 项目状态：正在持续更新
 *
 * 本项目基于 MIT 协议开源，此项目可免费用于学习交流和游戏体验。使用此项目时需要保留作者声明。
 * 项目开发过程中借助了 AI 辅助。
 *
 * MIT License
 *
 * Copyright (c) 2025 灼冰/Circle1t
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cn.zhuobing.testPlugin;

import cn.zhuobing.testPlugin.anni.*;
import cn.zhuobing.testPlugin.boss.*;
import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.enchant.EnchantManager;
import cn.zhuobing.testPlugin.enchant.EnchantTableListener;
import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
import cn.zhuobing.testPlugin.enderfurnance.EnderFurnaceCommandHandler;
import cn.zhuobing.testPlugin.enderfurnance.EnderFurnaceListener;
import cn.zhuobing.testPlugin.enderfurnance.EnderFurnaceManager;
import cn.zhuobing.testPlugin.game.GameCommandHandler;
import cn.zhuobing.testPlugin.game.GameManager;
import cn.zhuobing.testPlugin.game.GamePlayerJoinListener;
import cn.zhuobing.testPlugin.map.*;
import cn.zhuobing.testPlugin.ore.DiamondDataManager;
import cn.zhuobing.testPlugin.specialitem.listener.*;
import cn.zhuobing.testPlugin.kit.KitManager;
import cn.zhuobing.testPlugin.kit.kits.*;
import cn.zhuobing.testPlugin.nexus.NexusListener;
import cn.zhuobing.testPlugin.nexus.NexusCommandHandler;
import cn.zhuobing.testPlugin.nexus.NexusManager;
import cn.zhuobing.testPlugin.nexus.NexusInfoBoard;
import cn.zhuobing.testPlugin.ore.DiamondCommand;
import cn.zhuobing.testPlugin.ore.OreBreakListener;
import cn.zhuobing.testPlugin.ore.OreManager;
import cn.zhuobing.testPlugin.specialitem.itemCommand.CompassCommand;
import cn.zhuobing.testPlugin.specialitem.itemCommand.TeamSelectorCommand;
import cn.zhuobing.testPlugin.specialitem.manager.MapConfigurerManager;
import cn.zhuobing.testPlugin.specialitem.manager.MapSelectorManager;
import cn.zhuobing.testPlugin.specialitem.manager.TeamSelectorManager;
import cn.zhuobing.testPlugin.store.StoreCommandHandler;
import cn.zhuobing.testPlugin.store.StoreListener;
import cn.zhuobing.testPlugin.store.StoreManager;
import cn.zhuobing.testPlugin.team.TeamChatListener;
import cn.zhuobing.testPlugin.team.TeamCommandHandler;
import cn.zhuobing.testPlugin.team.TeamManager;
import cn.zhuobing.testPlugin.utils.AnniConfigManager;
import cn.zhuobing.testPlugin.utils.MessageRenderer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnniTest extends JavaPlugin {
    private static AnniTest instance;
    private final MessageRenderer messageRenderer = new MessageRenderer(this);

    private final Map<UUID, Long> itemSpawnTimes = new ConcurrentHashMap<>(); // 存储掉落物UUID和生成时间
    private int itemCleanupTaskId = -1; // 定时任务ID

    private final List<CommandHandler> commandHandlers = new ArrayList<>();
    private LobbyManager lobbyManager;
    private TeamManager teamManager;
    private NexusManager nexusManager;
    private NexusInfoBoard nexusInfoBoard;
    private GameManager gameManager;
    private OreManager oreManager;
    private DiamondDataManager diamondDataManager;
    private EnchantManager enchantManager;
    private TeamSelectorManager teamSelectorManager;
    private TeamCommandHandler teamCommandHandler;
    private RespawnDataManager respawnDataManager;
    private BossWorldManager bossWorldManager;
    private BossDataManager bossDataManager;
    private KitManager kitManager;
    private StoreManager storeManager;
    private WitchDataManager witchDataManager;
    private MapSelectorManager mapSelectorManager;
    private MapSelectManager mapSelectManager;
    private BorderManager borderManager;
    private MapConfigurerManager mapConfigurerManager;
    private EnderFurnaceManager enderFurnaceManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("AnniTest 插件启动");

        getLogger().info("初始化核心基础配置信息...");

        // 加载配置
        AnniConfigManager.loadConfig(this);

        // 注册 BungeeCord 通道（如果启用）
        if (AnniConfigManager.BUNGEE_ENABLED) {
            registerBungeeChannels();
        }

        // 初始化核心数据管理类
        initManagers();

        // 注册命令处理器
        initCommandHandlers();

        // 注册事件监听器
        initListeners();

        // 注册职业
        initKits();

        // 初始化完成后检查大厅世界是否加载成功
        if (lobbyManager.getLobbyWorld() == null) {
            getLogger().severe("大厅地图加载失败！");
        }else{
            getLogger().info("大厅地图加载成功！");
        }

        // 开始掉落物清除任务
        startItemCleanupTask();

        getLogger().info("AnniTest 插件初始化完成！");
    }

    private void initManagers() {
        getLogger().info("初始化核心数据管理类...");
        // 初始化核心数据管理类
        lobbyManager = new LobbyManager(this);
        bossWorldManager = new BossWorldManager(lobbyManager,this);
        borderManager = new BorderManager(this);
        teamManager = new TeamManager();
        enderFurnaceManager = new EnderFurnaceManager(this);
        nexusManager = new NexusManager(this);
        witchDataManager = new WitchDataManager(this,teamManager);
        gameManager = new GameManager(teamManager,null,null,null,witchDataManager,null,messageRenderer,this);
        nexusInfoBoard = new NexusInfoBoard(nexusManager, teamManager,gameManager,null);
        kitManager = new KitManager(gameManager,teamManager,this);
        diamondDataManager = new DiamondDataManager(this);
        oreManager = new OreManager(gameManager,diamondDataManager,kitManager);
        enchantManager = new EnchantManager();
        teamSelectorManager = new TeamSelectorManager(teamManager);
        respawnDataManager = new RespawnDataManager(lobbyManager,nexusManager,this);
        storeManager = new StoreManager(this);
        bossDataManager = new BossDataManager(this,gameManager,teamManager,bossWorldManager,messageRenderer);
        mapSelectManager = new MapSelectManager(bossDataManager,borderManager,nexusManager, diamondDataManager, oreManager, respawnDataManager, storeManager,witchDataManager,gameManager,nexusInfoBoard,enderFurnaceManager,this);
        mapSelectorManager = new MapSelectorManager(mapSelectManager);
        mapConfigurerManager = new MapConfigurerManager(mapSelectManager);
    }

    private void initCommandHandlers() {
        getLogger().info("注册命令处理器...");

        // 注册命令处理器
        LobbyCommandHandler lobbyCommandHandler = new LobbyCommandHandler(lobbyManager);
        commandHandlers.add(lobbyCommandHandler);
        getCommand("lobby").setTabCompleter(lobbyCommandHandler);

        teamCommandHandler = new TeamCommandHandler(teamManager, nexusManager, nexusInfoBoard, gameManager, respawnDataManager, kitManager,messageRenderer);
        commandHandlers.add(teamCommandHandler);
        getCommand("team").setTabCompleter(teamCommandHandler);

        EnderFurnaceCommandHandler enderFurnaceCommandHandler = new EnderFurnaceCommandHandler(enderFurnaceManager, teamManager);
        commandHandlers.add(enderFurnaceCommandHandler);
        getCommand("enderfurnace").setTabCompleter(enderFurnaceCommandHandler);

        NexusCommandHandler nexusCommandHandler = new NexusCommandHandler(nexusManager, nexusInfoBoard, teamManager);
        commandHandlers.add(nexusCommandHandler);
        getCommand("nexus").setTabCompleter(nexusCommandHandler);

        GameCommandHandler gameCommandHandler = new GameCommandHandler(gameManager, oreManager);
        commandHandlers.add(gameCommandHandler);
        getCommand("annistart").setTabCompleter(gameCommandHandler);
        getCommand("phase").setTabCompleter(gameCommandHandler);

        BossCommand bossCommand = new BossCommand(bossDataManager, teamManager);
        commandHandlers.add(bossCommand);
        getCommand("boss").setTabCompleter(bossCommand);

        StoreCommandHandler storeCommandHandler = new StoreCommandHandler(storeManager);
        commandHandlers.add(storeCommandHandler);
        getCommand("store").setTabCompleter(storeCommandHandler);


        WitchCommandHandler witchCommandHandler = new WitchCommandHandler(witchDataManager,teamManager);
        commandHandlers.add(witchCommandHandler);
        getCommand("witch").setTabCompleter(witchCommandHandler);

        MapCommandHandler mapCommandHandler = new MapCommandHandler(borderManager, lobbyManager, mapSelectManager);
        commandHandlers.add(mapCommandHandler);
        getCommand("annimap").setTabCompleter(mapCommandHandler);

        commandHandlers.add(new DiamondCommand(oreManager));
        commandHandlers.add(new TeamSelectorCommand());
        commandHandlers.add(new CompassCommand(teamManager));
        commandHandlers.add(new PlayerCommandHandler());
    }

    private void initListeners() {
        getLogger().info("注册事件监听器...");
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new EnchantedAppleListener(),this);
        getServer().getPluginManager().registerEvents(new LaunchPadListener(),this);
        getServer().getPluginManager().registerEvents(new TeamChatListener(teamManager, gameManager), this);
        getServer().getPluginManager().registerEvents(new EnderFurnaceListener(enderFurnaceManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new NexusListener(nexusManager, nexusInfoBoard, gameManager, teamManager,this,messageRenderer), this);
        getServer().getPluginManager().registerEvents(new GamePlayerJoinListener(lobbyManager,teamManager, gameManager, nexusInfoBoard,respawnDataManager,bossDataManager,this), this);
        getServer().getPluginManager().registerEvents(new OreBreakListener(oreManager, gameManager,nexusManager), this);
        getServer().getPluginManager().registerEvents(new EnchantTableListener(enchantManager), this);
        getServer().getPluginManager().registerEvents(new SoulBoundListener(),this);
        getServer().getPluginManager().registerEvents(new TeamSelectorListener(teamSelectorManager,teamCommandHandler), this);
        getServer().getPluginManager().registerEvents(new CompassListener(teamManager, nexusManager,this),this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(teamManager, respawnDataManager,gameManager,nexusManager,kitManager,this), this);
        getServer().getPluginManager().registerEvents(new KitSelectorListener(kitManager),this);
        getServer().getPluginManager().registerEvents(new PlayerListener(teamManager,gameManager,kitManager,nexusManager),this);
        getServer().getPluginManager().registerEvents(new EndPortalListener(teamManager,bossDataManager,gameManager),this);
        getServer().getPluginManager().registerEvents(new BossListener(bossDataManager),this);
        getServer().getPluginManager().registerEvents(new WitherSkullListener(bossDataManager),this);
        getServer().getPluginManager().registerEvents(new HellPortalListener(teamManager, nexusManager,respawnDataManager,bossDataManager,kitManager),this);
        getServer().getPluginManager().registerEvents(new BossStarItem(this),this);
        getServer().getPluginManager().registerEvents(new StoreListener(storeManager,gameManager),this);
        getServer().getPluginManager().registerEvents(new MapSelectorListener(mapSelectorManager,mapSelectManager),this);
        getServer().getPluginManager().registerEvents(new LobbyListener(lobbyManager),this);
        getServer().getPluginManager().registerEvents(new BorderListener(borderManager,mapSelectManager),this);
        getServer().getPluginManager().registerEvents(new MapConfigurerListener(mapConfigurerManager, mapSelectManager),this);
        getServer().getPluginManager().registerEvents(new ItemCleanListener(this,mapSelectManager,bossWorldManager),this);
    }

    private void initKits() {
        getLogger().info("注册职业...");
        // 注册职业
        kitManager.registerKit(new Civilian(teamManager));
        kitManager.registerKit(new Scout(teamManager,kitManager));
        kitManager.registerKit(new Acrobat(teamManager,kitManager));
        kitManager.registerKit(new Miner(teamManager));
        kitManager.registerKit(new Assassin(teamManager,kitManager));
        kitManager.registerKit(new Enchanter(teamManager,kitManager));
        kitManager.registerKit(new Archer(teamManager,kitManager));
        kitManager.registerKit(new Swapper(teamManager,kitManager));
        kitManager.registerKit(new General(teamManager,kitManager));
        kitManager.registerKit(new Builder(teamManager,kitManager));
        kitManager.registerKit(new Dasher(teamManager,kitManager));
        kitManager.registerKit(new Handyman(teamManager, kitManager, nexusManager, gameManager,nexusInfoBoard));
        kitManager.registerKit(new Scorpio(teamManager, kitManager));
        kitManager.registerKit(new Defender(teamManager,kitManager,nexusManager));
        kitManager.registerKit(new Pyro(teamManager,kitManager));
        kitManager.registerKit(new Berserker(teamManager,kitManager));
        kitManager.registerKit(new Alchemist(teamManager,kitManager,gameManager));
        kitManager.registerKit(new Transporter(teamManager,kitManager,nexusManager));
        kitManager.registerKit(new ISO(teamManager,kitManager));
        kitManager.registerKit(new Sova(teamManager,kitManager));
    }

    private void registerBungeeChannels() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("已注册 BungeeCord 通道");
    }


    @Override
    public void onDisable() {
        getLogger().info("AnniTest 插件正在关闭，开始卸载游戏地图副本和大厅副本...");

        // 卸载游戏地图副本
        if (mapSelectManager != null) {
            mapSelectManager.unloadGameWorld();
        }
        // 卸载boss大厅副本
        if(bossWorldManager != null){
            bossWorldManager.unloadBossWorld();
        }
        // 卸载大厅副本
        if (lobbyManager != null) {
            lobbyManager.unloadLobbyWorld();
        }

        // 取消注册 BungeeCord 通道（如果已注册）
        if (AnniConfigManager.BUNGEE_ENABLED) {
            getServer().getMessenger().unregisterOutgoingPluginChannel(this);
            getLogger().info("已取消注册 BungeeCord 通道");
        }

        // 关闭掉落物清除任务
        stopItemCleanupTask();

        getLogger().info("大厅副本和游戏地图副本卸载完成，AnniTest 插件已关闭。");
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        for (CommandHandler handler : commandHandlers) {
            if (handler.onCommand(sender, command, label, args)) {
                return true;
            }
        }
        return false;
    }

    public static AnniTest getInstance() {
        return instance;
    }

    // 获取大厅管理器
    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    // 开启掉落物清除任务
    private void startItemCleanupTask() {
        // 每分钟检查一次（1200 ticks = 60秒）
        Bukkit.getLogger().info("清除掉落物定时任务已开启，物品保留时长：5分钟");
        itemCleanupTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            long currentTime = System.currentTimeMillis();
            long clearMinutes = 5 * 60 * 1000; // 掉落物清除间隔 5分钟的毫秒数

            Iterator<Map.Entry<UUID, Long>> iterator = itemSpawnTimes.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Long> entry = iterator.next();
                // 如果大于设定的 clearMinutes 物品就被清除
                if (currentTime - entry.getValue() >= clearMinutes) {
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (entity != null && entity instanceof Item) {
                        //Bukkit.broadcastMessage(entity.getName() + ChatColor.GREEN + "物品已被清除");
                        entity.remove(); // 移除超时掉落物
                    }
                    iterator.remove(); // 从Map中移除记录
                }
            }
        }, 1200L, 1200L).getTaskId(); // 延迟1分钟执行，每分钟重复一次
    }

    // 关闭掉落物清除任务
    private void stopItemCleanupTask() {
        if (itemCleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(itemCleanupTaskId);
            itemCleanupTaskId = -1;
        }
        itemSpawnTimes.clear();
    }

    public Map<UUID, Long> getItemSpawnTimes() {
        return itemSpawnTimes;
    }
}