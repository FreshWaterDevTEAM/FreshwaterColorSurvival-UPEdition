package com.freshwater.colorsurvival.ui;

import com.freshwater.colorsurvival.bingo.BingoCard;
import com.freshwater.colorsurvival.game.GameManager;
import com.freshwater.colorsurvival.game.GameTeam;
import com.freshwater.colorsurvival.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 以箱子 GUI 展示 5x5 Bingo 卡（只读）。作者：淡水岛开发组
 */
public final class CardGui implements Listener {

    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;

    private final GameManager game;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    public CardGui(GameManager game) {
        this.game = game;
    }

    public void open(Player player) {
        BingoCard card = game.bingo() != null ? game.bingo().getCard() : null;
        if (card == null) {
            player.sendMessage(Text.amp(game.config().prefix() + "&c当前没有 Bingo 卡（对局未开始）。"));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, SLOTS, Text.amp("&9Bingo 卡 &7- 淡水岛开发组"));
        for (int r = 0; r < BingoCard.SIZE; r++) {
            for (int c = 0; c < BingoCard.SIZE; c++) {
                int index = r * BingoCard.SIZE + c;
                int slot = r * 9 + (c + 2);
                inv.setItem(slot, buildIcon(card, index));
            }
        }
        viewers.add(player.getUniqueId());
        player.openInventory(inv);
    }

    private ItemStack buildIcon(BingoCard card, int index) {
        Material material = card.itemAt(index);
        boolean aDone = card.isDone(GameTeam.TEAM_A, index);
        boolean bDone = card.isDone(GameTeam.TEAM_B, index);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.amp("&f" + material.name()).decoration(
                    net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Text.amp(GameTeam.TEAM_A.colored() + ": " + (aDone ? "&a已收集" : "&7未收集")));
            lore.add(Text.amp(GameTeam.TEAM_B.colored() + ": " + (bDone ? "&a已收集" : "&7未收集")));
            meta.lore(lore);
            if (aDone || bDone) {
                meta.setEnchantmentGlintOverride(true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (viewers.contains(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        HumanEntity who = event.getPlayer();
        viewers.remove(who.getUniqueId());
    }
}
