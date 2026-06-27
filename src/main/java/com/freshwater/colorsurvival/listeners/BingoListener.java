package com.freshwater.colorsurvival.listeners;

import com.freshwater.colorsurvival.bingo.BingoManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * 玩家背包变化（拾取/合成/界面操作/丢弃）时即时触发 Bingo 重新计算。
 * 配合 {@link BingoManager} 的「按当前持有判定」规则。作者：淡水岛开发组
 */
public final class BingoListener implements Listener {

    private final BingoManager bingo;

    public BingoListener(BingoManager bingo) {
        this.bingo = bingo;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            bingo.recomputeForPlayer(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        bingo.recomputeForPlayer(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            bingo.recomputeForPlayer(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            bingo.recomputeForPlayer(player);
        }
    }
}
