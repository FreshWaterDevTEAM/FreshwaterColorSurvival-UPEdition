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

import java.util.List;
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
        if (game == null || !game.isRunning() || card == null) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            GameTeam team = game.getTeam(p.getUniqueId());
            if (team == null) {
                continue;
            }
            for (ItemStack it : p.getInventory().getStorageContents()) {
                if (it != null && !it.getType().isAir()) {
                    handleObtain(team, it.getType());
                }
            }
        }
    }

    public void handleObtain(Player player, Material material) {
        if (game == null || !game.isRunning() || card == null) {
            return;
        }
        GameTeam team = game.getTeam(player.getUniqueId());
        if (team == null) {
            return;
        }
        handleObtain(team, material);
    }

    private void handleObtain(GameTeam team, Material material) {
        if (card == null || material == null) {
            return;
        }
        boolean changed = card.mark(team, material);
        if (!changed) {
            return;
        }
        Bukkit.broadcast(Text.amp(config.prefix() + team.colored() + " &7收集到 &f"
                + material.name() + " &7(" + card.completedCount(team) + "/" + BingoCard.CELLS + ")"));
        if (game.hud() != null) {
            game.hud().refreshSoon();
        }
        if (card.hasLine(team)) {
            game.onTeamWin(team);
        }
    }
}
