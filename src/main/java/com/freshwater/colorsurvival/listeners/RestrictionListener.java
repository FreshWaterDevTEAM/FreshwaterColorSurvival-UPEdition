package com.freshwater.colorsurvival.listeners;

import com.freshwater.colorsurvival.color.BlockColorMapper;
import com.freshwater.colorsurvival.color.GameColor;
import com.freshwater.colorsurvival.config.PluginConfig;
import com.freshwater.colorsurvival.game.GameManager;
import com.freshwater.colorsurvival.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 颜色限制：破坏/放置/右键使用方块必须与玩家颜色一致，否则取消并（在惩罚阶段）受罚。
 * 作者：淡水岛开发组
 */
public final class RestrictionListener implements Listener {

    private final GameManager game;
    private final PluginConfig config;
    private final BlockColorMapper mapper;

    public RestrictionListener(GameManager game, PluginConfig config, BlockColorMapper mapper) {
        this.game = game;
        this.config = config;
        this.mapper = mapper;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        if (isDenied(player, type)) {
            event.setCancelled(true);
            deny(player, type, "break-deny");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlockPlaced().getType();
        if (isDenied(player, type)) {
            event.setCancelled(true);
            deny(player, type, "place-deny");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Material type = event.getClickedBlock().getType();
        if (!type.isInteractable()) {
            return;
        }
        Player player = event.getPlayer();
        if (isDenied(player, type)) {
            event.setCancelled(true);
            deny(player, type, "use-deny");
        }
    }

    /** 是否应当拒绝该玩家对该方块的操作。 */
    private boolean isDenied(Player player, Material type) {
        if (!game.isRunning()) {
            return false;
        }
        if (game.isBypassing(player)) {
            return false;
        }
        GameColor playerColor = game.getColor(player.getUniqueId());
        if (playerColor == null) {
            return false;
        }
        if (isExempt(type)) {
            return false;
        }
        GameColor blockColor = mapper.colorOf(type);
        return blockColor != playerColor;
    }

    private boolean isExempt(Material type) {
        return config.isUtilityExempt() && config.getExemptMaterials().contains(type);
    }

    private void deny(Player player, Material type, String key) {
        GameColor blockColor = mapper.colorOf(type);
        String raw = config.msg(key).replace("%color%", blockColor.colored());
        if (raw.isEmpty()) {
            raw = "&c你不能操作 " + blockColor.colored() + " &c的方块！";
        }
        if (config.isShowActionBar()) {
            player.sendActionBar(Text.amp(raw));
        } else {
            player.sendMessage(Text.amp(config.prefix() + raw));
        }
        game.punishment().applyViolation(player);
    }
}
