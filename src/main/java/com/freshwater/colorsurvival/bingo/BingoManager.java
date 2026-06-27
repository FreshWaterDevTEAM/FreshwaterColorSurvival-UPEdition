package com.freshwater.colorsurvival.bingo;

import com.freshwater.colorsurvival.config.PluginConfig;
import com.freshwater.colorsurvival.game.GameManager;
import com.freshwater.colorsurvival.game.GameTeam;
import com.freshwater.colorsurvival.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 管理 Bingo 卡：生成、检测获取、判胜。作者：淡水岛开发组
 */
public final class BingoManager {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private GameManager game;

    private BingoCard card;
    private BukkitTask scanTask;

    public BingoManager(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void setGame(GameManager game) {
        this.game = game;
    }

    public BingoCard getCard() {
        return card;
    }

    public void generateCard() {
        List<Material> pool = config.getBingoItems();
        if (pool.isEmpty()) {
            plugin.getLogger().warning("bingo-items 为空，使用默认物品 STONE 填充。");
            pool = List.of(Material.STONE);
        }
        card = new BingoCard(pool);
        startScanTask();
    }

    private void startScanTask() {
        stopScanTask();
        scanTask = Bukkit.getScheduler().runTaskTimer(plugin, this::scanAll, 40L, 20L);
    }

    public void stopScanTask() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    private void scanAll() {
        recomputeAll();
    }

    /** 重新计算所有队伍的持有状态。 */
    public void recomputeAll() {
        if (game == null || !game.isRunning() || card == null) {
            return;
        }
        for (GameTeam team : GameTeam.values()) {
            recompute(team);
        }
    }

    /** 玩家背包发生变化时，下一 tick 重新计算其队伍（确保背包已更新）。 */
    public void recomputeForPlayer(Player player) {
        if (game == null || !game.isRunning() || card == null) {
            return;
        }
        GameTeam team = game.getTeam(player.getUniqueId());
        if (team == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (game.isRunning() && card != null) {
                recompute(team);
            }
        });
    }

    private void recompute(GameTeam team) {
        Set<Material> held = EnumSet.noneOf(Material.class);
        for (UUID id : game.teamMembers(team)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                continue;
            }
            for (ItemStack it : p.getInventory().getStorageContents()) {
                if (it != null && !it.getType().isAir()) {
                    held.add(it.getType());
                }
            }
        }

        BingoCard.Diff diff = card.updateHeld(team, held);
        if (!diff.isEmpty()) {
            for (Material m : diff.added) {
                Bukkit.broadcast(Text.amp(config.prefix() + team.colored() + " &7集齐 &f"
                        + m.name() + " &7(" + card.completedCount(team) + "/" + BingoCard.CELLS + ")"));
            }
            for (Material m : diff.removed) {
                Bukkit.broadcast(Text.amp(config.prefix() + team.colored() + " &c失去 &f"
                        + m.name() + " &7(" + card.completedCount(team) + "/" + BingoCard.CELLS + ")"));
            }
            if (game.hud() != null) {
                game.hud().refreshSoon();
            }
        }
        if (card.hasLine(team)) {
            game.onTeamWin(team);
        }
    }
}
