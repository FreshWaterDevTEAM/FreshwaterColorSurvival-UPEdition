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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 颜色限制：破坏 / 放置 / 右键使用 / 拾取方块（物品）时，玩家颜色必须属于该方块拥有的颜色集合，
 * 否则取消，并提示「可交互颜色」。作者：淡水岛开发组
 */
public final class RestrictionListener implements Listener {

    private final GameManager game;
    private final PluginConfig config;
    private final BlockColorMapper mapper;
    private final java.util.Map<UUID, Long> pickupMsgCooldown = new ConcurrentHashMap<>();

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
            deny(player, type, "break-deny", "破坏");
            game.punishment().applyViolation(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlockPlaced().getType();
        if (isDenied(player, type)) {
            event.setCancelled(true);
            deny(player, type, "place-deny", "放置");
            game.punishment().applyViolation(player);
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
            deny(player, type, "use-deny", "使用");
            game.punishment().applyViolation(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Material type = event.getItem().getItemStack().getType();
        if (isDenied(player, type)) {
            event.setCancelled(true);
            // 站在物品上会反复触发，做节流，不施加惩罚
            long now = System.currentTimeMillis();
            Long until = pickupMsgCooldown.get(player.getUniqueId());
            if (until == null || until <= now) {
                pickupMsgCooldown.put(player.getUniqueId(), now + 1500L);
                deny(player, type, "pickup-deny", "拾取");
            }
        }
    }

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
        Set<GameColor> colors = mapper.colorsOf(type);
        return !colors.contains(playerColor);
    }

    private boolean isExempt(Material type) {
        return config.isUtilityExempt() && config.getExemptMaterials().contains(type);
    }

    private void deny(Player player, Material type, String key, String verb) {
        String colorsStr = BlockColorMapper.describe(mapper.colorsOf(type));
        String raw = config.msg(key);
        if (raw == null || raw.isEmpty()) {
            raw = "&c你不能" + verb + "该方块！可交互颜色: %color%";
        }
        raw = raw.replace("%color%", colorsStr).replace("%verb%", verb);
        if (config.isShowActionBar()) {
            player.sendActionBar(Text.amp(raw));
        } else {
            player.sendMessage(Text.amp(config.prefix() + raw));
        }
    }
}
