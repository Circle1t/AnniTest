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

import cn.zhuobing.testPlugin.anniPlayer.PlayerCommandHandler;
import cn.zhuobing.testPlugin.anniPlayer.PlayerListener;
import cn.zhuobing.testPlugin.anniPlayer.PlayerRespawnListener;
import cn.zhuobing.testPlugin.anniPlayer.RespawnDataManager;
import cn.zhuobing.testPlugin.boss.*;
import cn.zhuobing.testPlugin.command.CommandHandler;
import cn.zhuobing.testPlugin.enchant.BrewingStandListener;
import cn.zhuobing.testPlugin.enchant.EnchantManager;
import cn.zhuobing.testPlugin.enchant.EnchantTableListener;
import cn.zhuobing.testPlugin.enchant.SoulBoundListener;
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
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class AnniTest extends JavaPlugin {
    private static AnniTest instance;

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
    private BossDataManager bossDataManager;
    private KitManager kitManager;
    private StoreManager storeManager;
    private WitchDataManager witchDataManager;
    private MapSelectorManager mapSelectorManager;
    private MapSelectManager mapSelectManager;
    private BorderManager borderManager;
    private MapConfigurerManager mapConfigurerManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("AnniTest 插件启动");


        getLogger().info("初始化核心数据管理类...");
        // 初始化核心数据管理类
        lobbyManager = new LobbyManager(this);
        teamManager = new TeamManager();
        nexusManager = new NexusManager(this);
        witchDataManager = new WitchDataManager(this,teamManager);
        gameManager = new GameManager(teamManager,null,null,null,witchDataManager,null);
        nexusInfoBoard = new NexusInfoBoard(nexusManager, teamManager,gameManager,null);
        kitManager = new KitManager(gameManager,teamManager,this);
        diamondDataManager = new DiamondDataManager(this);
        oreManager = new OreManager(gameManager,diamondDataManager,kitManager);
        enchantManager = new EnchantManager();
        teamSelectorManager = new TeamSelectorManager(teamManager);
        respawnDataManager = new RespawnDataManager(lobbyManager,nexusManager,this);
        bossDataManager = new BossDataManager(this,gameManager,teamManager);
        storeManager = new StoreManager(this);
        borderManager = new BorderManager(this);
        mapSelectManager = new MapSelectManager(bossDataManager,borderManager,nexusManager, diamondDataManager, respawnDataManager, storeManager,witchDataManager,gameManager,nexusInfoBoard,this);
        mapSelectorManager = new MapSelectorManager(mapSelectManager);
        mapConfigurerManager = new MapConfigurerManager(mapSelectManager);

        getLogger().info("注册命令处理器...");
        // 注册命令处理器
        commandHandlers.add(new LobbyCommandHandler(lobbyManager));
        teamCommandHandler = new TeamCommandHandler(teamManager, nexusManager,nexusInfoBoard, gameManager,respawnDataManager,kitManager);
        commandHandlers.add(teamCommandHandler);
        commandHandlers.add(new NexusCommandHandler(nexusManager, nexusInfoBoard, teamManager));
        commandHandlers.add(new GameCommandHandler(gameManager, oreManager));
        commandHandlers.add(new DiamondCommand(oreManager));
        commandHandlers.add(new TeamSelectorCommand());
        commandHandlers.add(new CompassCommand(teamManager));
        commandHandlers.add(new BossCommand(bossDataManager, teamManager));
        commandHandlers.add(new StoreCommandHandler(storeManager));
        commandHandlers.add(new PlayerCommandHandler());
        commandHandlers.add(new WitchCommandHandler(witchDataManager));
        commandHandlers.add(new MapCommandHandler(borderManager,lobbyManager,mapSelectManager));

        getLogger().info("注册事件监听器...");
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new TeamChatListener(teamManager, gameManager), this);
        getServer().getPluginManager().registerEvents(new NexusListener(nexusManager, nexusInfoBoard, gameManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new GamePlayerJoinListener(lobbyManager,teamManager, gameManager, nexusInfoBoard,respawnDataManager,bossDataManager,this), this);
        getServer().getPluginManager().registerEvents(new OreBreakListener(oreManager, gameManager), this);
        getServer().getPluginManager().registerEvents(new EnchantTableListener(enchantManager), this);
        getServer().getPluginManager().registerEvents(new SoulBoundListener(),this);
        getServer().getPluginManager().registerEvents(new TeamSelectorListener(teamSelectorManager,teamCommandHandler), this);
        getServer().getPluginManager().registerEvents(new CompassListener(teamManager, nexusManager,this),this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(teamManager, respawnDataManager,gameManager,nexusManager,kitManager,this), this);
        getServer().getPluginManager().registerEvents(new KitSelectorListener(kitManager),this);
        getServer().getPluginManager().registerEvents(new PlayerListener(teamManager,gameManager,kitManager),this);
        getServer().getPluginManager().registerEvents(new EndPortalListener(teamManager,bossDataManager,gameManager),this);
        getServer().getPluginManager().registerEvents(new BossListener(bossDataManager),this);
        getServer().getPluginManager().registerEvents(new WitherSkullListener(bossDataManager),this);
        getServer().getPluginManager().registerEvents(new HellPortalListener(teamManager, nexusManager,respawnDataManager,bossDataManager,kitManager),this);
        getServer().getPluginManager().registerEvents(new BossStarItem(this),this);
        getServer().getPluginManager().registerEvents(new StoreListener(storeManager,gameManager),this);
        getServer().getPluginManager().registerEvents(new BrewingStandListener(this),this);
        getServer().getPluginManager().registerEvents(new MapSelectorListener(mapSelectorManager,mapSelectManager),this);
        getServer().getPluginManager().registerEvents(new LobbyListener(lobbyManager),this);
        getServer().getPluginManager().registerEvents(new BorderListener(borderManager,mapSelectManager),this);
        getServer().getPluginManager().registerEvents(new MapConfigurerListener(mapConfigurerManager, mapSelectManager),this);

        getLogger().info("注册职业...");
        // 注册职业
        kitManager.registerKit(new Civilian(teamManager));
        kitManager.registerKit(new Scout(teamManager));
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


        // 初始化完成后加载大厅世界
        if (lobbyManager.getLobbyWorld() == null) {
            getLogger().severe("大厅地图加载失败！");
        }else{
            getLogger().info("大厅地图加载成功！");
        }

        getLogger().info("AnniTest 插件初始化完成！");
    }

    @Override
    public void onDisable() {
        getLogger().info("AnniTest 插件正在关闭，开始卸载游戏地图副本和大厅副本...");

        // 卸载游戏地图副本
        if (mapSelectManager != null) {
            mapSelectManager.unloadGameWorld();
        }
        // 卸载大厅副本
        if (lobbyManager != null) {
            lobbyManager.unloadLobbyWorld();
        }


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
}