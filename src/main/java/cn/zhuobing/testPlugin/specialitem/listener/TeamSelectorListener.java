package cn.zhuobing.testPlugin.specialitem.listener;

import cn.zhuobing.testPlugin.specialitem.manager.TeamSelectorManager;
import cn.zhuobing.testPlugin.specialitem.items.TeamSelectorItem;
import cn.zhuobing.testPlugin.team.TeamCommandHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

// 团队选择监听器类，用于处理团队选择之星物品的相关事件
public class TeamSelectorListener implements Listener {
    // 特殊物品管理器
    private final TeamSelectorManager itemManager;
    // 团队命令处理器
    private final TeamCommandHandler teamHandler;
    // 槽位到队伍指令的映射
    private static final Map<Integer, String[]> SLOT_TO_COMMAND_MAP = new HashMap<>();

    static {
//        // 初始化槽位到队伍指令的映射
//        SLOT_TO_COMMAND_MAP.put(0, new String[]{"red"});
//        SLOT_TO_COMMAND_MAP.put(1, new String[]{"green"});
//        SLOT_TO_COMMAND_MAP.put(2, new String[]{"blue"});
//        SLOT_TO_COMMAND_MAP.put(3, new String[]{"yellow"});
//        SLOT_TO_COMMAND_MAP.put(7, new String[]{"random"});
//        SLOT_TO_COMMAND_MAP.put(8, new String[]{"leave"});

        // 强制随机队伍
        SLOT_TO_COMMAND_MAP.put(0, new String[]{"random"});
        SLOT_TO_COMMAND_MAP.put(1, new String[]{"random"});
        SLOT_TO_COMMAND_MAP.put(2, new String[]{"random"});
        SLOT_TO_COMMAND_MAP.put(3, new String[]{"random"});
        SLOT_TO_COMMAND_MAP.put(7, new String[]{"random"});
        SLOT_TO_COMMAND_MAP.put(8, new String[]{"leave"});
    }

    /**
     * 构造函数
     * @param itemManager 特殊物品管理器
     * @param teamHandler 团队命令处理器
     */
    public TeamSelectorListener(TeamSelectorManager itemManager, TeamCommandHandler teamHandler) {
        this.itemManager = itemManager;
        this.teamHandler = teamHandler;
    }

    /**
     * 处理玩家右键点击物品的事件
     * @param event 玩家交互事件
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // 获取玩家手中的物品
            ItemStack item = event.getItem();
            if (TeamSelectorItem.isTeamStar(item)) {
                // 如果是团队选择之星物品，则取消该事件
                event.setCancelled(true);
                // 获取玩家对象
                Player player = event.getPlayer();
                // 为玩家打开队伍选择界面
                player.openInventory(itemManager.createTeamSelectorGUI(player));
            }
        }
    }

    /**
     * 处理玩家在库存中点击物品的事件
     * @param event 库存点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("队伍选择")) {
            // 如果是在队伍选择界面点击，则取消该事件
            event.setCancelled(true);
            // 获取点击的玩家对象
            Player player = (Player) event.getWhoClicked();

            // 获取点击的槽位
            int slot = event.getRawSlot();
            if (slot < 0 || slot > 8) return;

            // 根据槽位获取对应的队伍指令
            String[] commandArgs = SLOT_TO_COMMAND_MAP.get(slot);
            if (commandArgs != null) {
                // 执行队伍指令
                teamHandler.useTeamCommand(player, commandArgs);
                // 关闭玩家的库存界面
                player.closeInventory();
            }
        }
    }
}