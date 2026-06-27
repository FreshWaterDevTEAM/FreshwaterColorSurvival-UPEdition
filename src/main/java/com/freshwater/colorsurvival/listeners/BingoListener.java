package com.freshwater.colorsurvival.listeners;

import com.freshwater.colorsurvival.bingo.BingoManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 即时检测玩家获取物品（拾取/合成/取出），交给 {@link BingoManager}。作者：淡水岛开发组
 */
public final class BingoListener implements Listener {

    private final BingoManager bingo;

    public BingoListener(BingoManager bingo) {
        this.bingo = bingo;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            bingo.handleObtain(player, event.getItem().getItemStack().getType());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack result = event.getRecipe().getResult();
            if (result != null) {
                bingo.handleObtain(player, result.getType());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current != null && !current.getType().isAir()) {
            bingo.handleObtain(player, current.getType());
        }
    }
}
