package com.freshwater.colorsurvival.ui;

import com.freshwater.colorsurvival.color.GameColor;
import com.freshwater.colorsurvival.game.GameManager;
import com.freshwater.colorsurvival.game.GameTeam;
import com.freshwater.colorsurvival.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;

/**
 * 侧边栏 HUD：展示玩家颜色、队伍、连线进度、阶段。作者：淡水岛开发组
 */
public final class HudManager {

    private final JavaPlugin plugin;
    private GameManager game;
    private BukkitTask task;

    public HudManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setGame(GameManager game) {
        this.game = game;
    }

    public void onGameStart() {
        stopTask();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, 20L);
    }

    public void onGameStop() {
        stopTask();
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(main);
        }
    }

    /** 数据变化后立即刷新一次。 */
    public void refreshSoon() {
        if (task != null) {
            updateAll();
        }
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void updateAll() {
        if (game == null) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            GameColor color = game.getColor(p.getUniqueId());
            if (color == null) {
                continue;
            }
            updatePlayer(p);
        }
    }

    private void updatePlayer(Player player) {
        UUID id = player.getUniqueId();
        GameColor color = game.getColor(id);
        GameTeam team = game.getTeam(id);
        if (color == null || team == null) {
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("fwc", "dummy",
                Text.amp("&b&l颜色生存"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int progress = game.bingo() != null && game.bingo().getCard() != null
                ? game.bingo().getCard().bestLineProgress(team) : 0;
        int completed = game.bingo() != null && game.bingo().getCard() != null
                ? game.bingo().getCard().completedCount(team) : 0;

        String phaseLine;
        long remain = game.punishment().remainingSecondsToAuto();
        if (game.punishment().isPunishPhase()) {
            phaseLine = ChatColor.RED + "阶段: 惩罚";
        } else if (remain >= 0) {
            phaseLine = ChatColor.GREEN + "阶段: 宽容 " + ChatColor.GRAY + "(" + formatTime(remain) + ")";
        } else {
            phaseLine = ChatColor.GREEN + "阶段: 宽容";
        }

        int score = 8;
        set(obj, ChatColor.DARK_GRAY + "----------------", score--);
        set(obj, ChatColor.WHITE + "颜色: " + color.colored(), score--);
        set(obj, ChatColor.WHITE + "队伍: " + team.colored(), score--);
        set(obj, ChatColor.WHITE + "连线: " + ChatColor.YELLOW + progress + "/5", score--);
        set(obj, ChatColor.WHITE + "完成: " + ChatColor.YELLOW + completed + "/25", score--);
        set(obj, phaseLine, score--);
        set(obj, ChatColor.DARK_GRAY + "=========", score--);
        set(obj, ChatColor.AQUA + "淡水岛开发组", score--);

        player.setScoreboard(board);
    }

    private void set(Objective obj, String entry, int score) {
        // 保证条目唯一，避免重复字符串冲突
        String unique = entry;
        while (obj.getScoreboard().getEntries().contains(unique)) {
            unique += ChatColor.RESET;
        }
        obj.getScore(unique).setScore(score);
    }

    private String formatTime(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }
}
