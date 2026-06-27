package com.freshwater.colorsurvival.ui;

import com.freshwater.colorsurvival.bingo.BingoCard;
import com.freshwater.colorsurvival.color.GameColor;
import com.freshwater.colorsurvival.game.GameManager;
import com.freshwater.colorsurvival.game.GameState;
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
 * 常驻侧边栏 HUD：从插件启用起一直显示，并按对局状态（大厅 / 进行中 / 观战 / 结束）切换内容。
 * 作者：淡水岛开发组
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

    /** 插件启用时调用，开始常驻刷新。 */
    public void enable() {
        stopTask();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    /** 插件卸载时调用，停止并还原主记分板。 */
    public void disable() {
        stopTask();
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(main);
        }
    }

    /** 对局状态变化时调用（开始/结束）：立即刷新一次。 */
    public void onGameStart() {
        updateAll();
    }

    public void onGameStop() {
        updateAll();
    }

    public void refreshSoon() {
        updateAll();
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
            render(p);
        }
    }

    private void render(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("fwc", "dummy", Text.amp("&b&l颜色生存"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        GameState state = game.getState();
        int score = 10;

        set(obj, ChatColor.DARK_GRAY + "----------------", score--);
        switch (state) {
            case RUNNING -> score = renderRunning(player, obj, score);
            case ENDED -> score = renderEnded(obj, score);
            default -> score = renderLobby(obj, score);
        }
        set(obj, ChatColor.DARK_GRAY + "=========", score--);
        set(obj, ChatColor.AQUA + "淡水岛开发组", score--);

        player.setScoreboard(board);
    }

    private int renderLobby(Objective obj, int score) {
        set(obj, ChatColor.WHITE + "状态: " + ChatColor.YELLOW + "等待开始", score--);
        set(obj, GameTeam.TEAM_A.colored() + ChatColor.WHITE + ": "
                + game.teamMembers(GameTeam.TEAM_A).size() + " 人", score--);
        set(obj, GameTeam.TEAM_B.colored() + ChatColor.WHITE + ": "
                + game.teamMembers(GameTeam.TEAM_B).size() + " 人", score--);
        set(obj, ChatColor.GRAY + "/fwc join A|B", score--);
        return score;
    }

    private int renderRunning(Player player, Objective obj, int score) {
        UUID id = player.getUniqueId();
        GameColor color = game.getColor(id);
        GameTeam team = game.getTeam(id);
        BingoCard card = game.bingo() != null ? game.bingo().getCard() : null;

        if (color != null && team != null) {
            int progress = card != null ? card.bestLineProgress(team) : 0;
            int completed = card != null ? card.completedCount(team) : 0;
            set(obj, ChatColor.WHITE + "颜色: " + color.colored(), score--);
            set(obj, ChatColor.WHITE + "队伍: " + team.colored(), score--);
            set(obj, ChatColor.WHITE + "连线: " + ChatColor.YELLOW + progress + "/5", score--);
            set(obj, ChatColor.WHITE + "完成: " + ChatColor.YELLOW + completed + "/25", score--);
        } else {
            // 观战者
            set(obj, ChatColor.GRAY + "观战中", score--);
            set(obj, teamProgressLine(GameTeam.TEAM_A, card), score--);
            set(obj, teamProgressLine(GameTeam.TEAM_B, card), score--);
        }
        set(obj, phaseLine(), score--);
        return score;
    }

    private int renderEnded(Objective obj, int score) {
        BingoCard card = game.bingo() != null ? game.bingo().getCard() : null;
        GameTeam w = game.getWinner();
        set(obj, ChatColor.GOLD + "对局结束", score--);
        set(obj, ChatColor.WHITE + "胜者: " + (w != null ? w.colored() : ChatColor.GRAY + "无"), score--);
        set(obj, teamProgressLine(GameTeam.TEAM_A, card), score--);
        set(obj, teamProgressLine(GameTeam.TEAM_B, card), score--);
        set(obj, ChatColor.GRAY + "/fwc stop 重置", score--);
        return score;
    }

    private String teamProgressLine(GameTeam team, BingoCard card) {
        int progress = card != null ? card.bestLineProgress(team) : 0;
        int completed = card != null ? card.completedCount(team) : 0;
        return team.colored() + ChatColor.WHITE + ": " + completed + "/25 "
                + ChatColor.GRAY + "(连" + progress + ")";
    }

    private String phaseLine() {
        long remain = game.punishment().remainingSecondsToAuto();
        if (game.punishment().isPunishPhase()) {
            return ChatColor.RED + "阶段: 惩罚";
        } else if (remain >= 0) {
            return ChatColor.GREEN + "阶段: 宽容 " + ChatColor.GRAY + "(" + formatTime(remain) + ")";
        }
        return ChatColor.GREEN + "阶段: 宽容";
    }

    private void set(Objective obj, String entry, int score) {
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
