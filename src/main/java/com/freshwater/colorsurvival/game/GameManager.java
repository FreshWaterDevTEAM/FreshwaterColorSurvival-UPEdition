package com.freshwater.colorsurvival.game;

import com.freshwater.colorsurvival.bingo.BingoManager;
import com.freshwater.colorsurvival.color.ColorAssigner;
import com.freshwater.colorsurvival.color.GameColor;
import com.freshwater.colorsurvival.config.PluginConfig;
import com.freshwater.colorsurvival.ui.HudManager;
import com.freshwater.colorsurvival.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对局核心：队伍、颜色、状态、开始/结束流程。作者：淡水岛开发组
 */
public final class GameManager {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final ColorAssigner assigner = new ColorAssigner();

    private BingoManager bingoManager;
    private PunishmentManager punishmentManager;
    private HudManager hudManager;

    private GameState state = GameState.LOBBY;
    private GameTeam winner;
    private final Map<UUID, GameTeam> teams = new ConcurrentHashMap<>();
    private final Map<UUID, GameColor> colors = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> bypassOverride = new ConcurrentHashMap<>();
    private final Set<UUID> spectators = ConcurrentHashMap.newKeySet();
    private final Map<UUID, GameMode> previousGameMode = new ConcurrentHashMap<>();
    private BukkitTask fireworkTask;

    public GameManager(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void wire(BingoManager bingoManager, PunishmentManager punishmentManager, HudManager hudManager) {
        this.bingoManager = bingoManager;
        this.punishmentManager = punishmentManager;
        this.hudManager = hudManager;
    }

    public GameState getState() {
        return state;
    }

    public boolean isRunning() {
        return state == GameState.RUNNING;
    }

    public GameTeam getWinner() {
        return winner;
    }

    public PluginConfig config() {
        return config;
    }

    public BingoManager bingo() {
        return bingoManager;
    }

    public PunishmentManager punishment() {
        return punishmentManager;
    }

    public HudManager hud() {
        return hudManager;
    }

    public GameTeam getTeam(UUID uuid) {
        return teams.get(uuid);
    }

    public GameColor getColor(UUID uuid) {
        return colors.get(uuid);
    }

    public List<UUID> teamMembers(GameTeam team) {
        List<UUID> list = new ArrayList<>();
        for (Map.Entry<UUID, GameTeam> e : teams.entrySet()) {
            if (e.getValue() == team) {
                list.add(e.getKey());
            }
        }
        return list;
    }

    // ---- 旁路 ----

    public boolean isBypassing(Player player) {
        // 个人显式开关优先于权限默认值，方便 OP 自测
        Boolean override = bypassOverride.get(player.getUniqueId());
        if (override != null) {
            return override;
        }
        return player.hasPermission("fwfish-colors.bypass");
    }

    public boolean toggleBypass(Player player) {
        boolean next = !isBypassing(player);
        bypassOverride.put(player.getUniqueId(), next);
        return next;
    }

    // ---- 观战 ----

    public boolean isSpectator(UUID uuid) {
        return spectators.contains(uuid);
    }

    /**
     * 切换观战状态。
     *
     * @return 切换后是否为观战者
     */
    public boolean toggleSpectate(Player player) {
        UUID id = player.getUniqueId();
        if (spectators.contains(id)) {
            spectators.remove(id);
            restoreGameMode(player);
            send(player, "&7你已退出观战。");
            return false;
        }
        spectators.add(id);
        // 观战者不参与队伍
        teams.remove(id);
        colors.remove(id);
        if (state == GameState.RUNNING) {
            enterSpectatorMode(player);
        }
        send(player, "&b你已进入观战模式。");
        return true;
    }

    private void enterSpectatorMode(Player player) {
        previousGameMode.putIfAbsent(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);
    }

    private void restoreGameMode(Player player) {
        GameMode prev = previousGameMode.remove(player.getUniqueId());
        if (prev != null) {
            player.setGameMode(prev);
        } else if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    // ---- 组队 ----

    public boolean join(Player player, GameTeam team) {
        if (state == GameState.RUNNING) {
            send(player, "&c对局进行中，无法加入。");
            return false;
        }
        if (teamMembers(team).size() >= ColorAssigner.MAX_PER_TEAM
                && getTeam(player.getUniqueId()) != team) {
            send(player, "&c" + team.displayName() + " 已满（" + ColorAssigner.MAX_PER_TEAM + " 人）。");
            return false;
        }
        if (spectators.remove(player.getUniqueId())) {
            restoreGameMode(player);
        }
        teams.put(player.getUniqueId(), team);
        send(player, "&a你加入了 " + team.colored() + "&a。");
        return true;
    }

    public void leave(Player player) {
        if (state == GameState.RUNNING) {
            send(player, "&c对局进行中，无法离队。");
            return;
        }
        if (teams.remove(player.getUniqueId()) != null) {
            send(player, "&7你离开了队伍。");
        } else {
            send(player, "&7你当前不在任何队伍。");
        }
    }

    // ---- 开始 / 结束 ----

    public boolean start(org.bukkit.command.CommandSender sender) {
        if (state == GameState.RUNNING) {
            sender.sendMessage(Text.amp(config.prefix() + "&c对局已经在进行中。"));
            return false;
        }

        autoBalance();

        Map<UUID, GameColor> assigned = new HashMap<>();
        try {
            for (GameTeam team : GameTeam.values()) {
                assigned.putAll(assigner.assign(team, teamMembers(team)));
            }
        } catch (ColorAssigner.AssignException ex) {
            sender.sendMessage(Text.amp(config.prefix() + "&c无法开始：" + ex.getMessage()));
            return false;
        }

        long totalPlayers = assigned.size();
        if (totalPlayers == 0) {
            sender.sendMessage(Text.amp(config.prefix() + "&c没有任何玩家加入队伍，无法开始。"));
            return false;
        }

        colors.clear();
        colors.putAll(assigned);
        winner = null;

        bingoManager.generateCard();

        state = GameState.RUNNING;
        punishmentManager.onGameStart();
        hudManager.onGameStart();

        for (UUID id : spectators) {
            Player sp = Bukkit.getPlayer(id);
            if (sp != null) {
                enterSpectatorMode(sp);
            }
        }

        Bukkit.broadcast(Text.amp(config.prefix() + "&e对局开始！共 " + totalPlayers
                + " 名玩家。&7(淡水岛开发组)"));

        for (UUID id : colors.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                continue;
            }
            GameColor color = colors.get(id);
            GameTeam team = getTeam(id);
            p.showTitle(Title.title(
                    Text.amp("&f你的颜色：" + color.colored()),
                    Text.amp(team.colored() + " &7| 只能操作 " + color.colored() + " &7的方块"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(800))));
            send(p, "&7你的队伍：" + team.colored() + " &7你的颜色：" + color.colored());
        }
        return true;
    }

    private void autoBalance() {
        List<Player> unassigned = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("fwfish-colors.play")) {
                continue;
            }
            if (spectators.contains(p.getUniqueId())) {
                continue;
            }
            if (!teams.containsKey(p.getUniqueId())) {
                unassigned.add(p);
            }
        }
        Collections.shuffle(unassigned);
        for (Player p : unassigned) {
            int a = teamMembers(GameTeam.TEAM_A).size();
            int b = teamMembers(GameTeam.TEAM_B).size();
            GameTeam target = a <= b ? GameTeam.TEAM_A : GameTeam.TEAM_B;
            if (teamMembers(target).size() >= ColorAssigner.MAX_PER_TEAM) {
                target = target.other();
            }
            if (teamMembers(target).size() >= ColorAssigner.MAX_PER_TEAM) {
                send(p, "&c两队都已满，你本局未被分配。");
                continue;
            }
            teams.put(p.getUniqueId(), target);
        }
    }

    public void stop(org.bukkit.command.CommandSender sender) {
        if (state != GameState.RUNNING && state != GameState.ENDED) {
            sender.sendMessage(Text.amp(config.prefix() + "&c当前没有进行中的对局。"));
            return;
        }
        reset();
        Bukkit.broadcast(Text.amp(config.prefix() + "&e对局已结束。"));
    }

    public void onTeamWin(GameTeam winner) {
        if (state != GameState.RUNNING) {
            return;
        }
        state = GameState.ENDED;
        this.winner = winner;
        punishmentManager.onGameStop();
        if (hudManager != null) {
            hudManager.refreshSoon();
        }
        Bukkit.broadcast(Text.amp(config.prefix() + winner.colored() + " &a完成了一条 Bingo 连线，获得胜利！"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(
                    Text.amp(winner.colored() + " &a获胜！"),
                    Text.amp("&7恭喜！ &8- 淡水岛开发组"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(5), Duration.ofSeconds(1))));
        }
        launchVictoryFireworks(winner);
    }

    // ---- 胜利烟花 ----

    private void launchVictoryFireworks(GameTeam winner) {
        stopFireworks();
        final int totalRounds = 12;
        final int[] round = {0};
        fireworkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (round[0] >= totalRounds) {
                stopFireworks();
                return;
            }
            round[0]++;
            for (UUID id : teamMembers(winner)) {
                Player p = Bukkit.getPlayer(id);
                if (p == null) {
                    continue;
                }
                GameColor color = colors.get(id);
                Color c = color != null ? color.dyeColor().getFireworkColor() : Color.WHITE;
                spawnFirework(p.getLocation(), c, winner.chatColor().isColor());
            }
        }, 0L, 15L);
    }

    private void spawnFirework(Location location, Color color, boolean trail) {
        if (location.getWorld() == null) {
            return;
        }
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        FireworkEffect.Type type = types[(int) (Math.random() * types.length)];
        meta.addEffect(FireworkEffect.builder()
                .with(type)
                .withColor(color)
                .withFade(Color.WHITE)
                .flicker(true)
                .trail(trail)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    private void stopFireworks() {
        if (fireworkTask != null) {
            fireworkTask.cancel();
            fireworkTask = null;
        }
    }

    /** 清空所有对局数据，回到大厅。 */
    public void reset() {
        state = GameState.LOBBY;
        winner = null;
        teams.clear();
        colors.clear();
        stopFireworks();
        for (UUID id : new java.util.ArrayList<>(previousGameMode.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                restoreGameMode(p);
            }
        }
        previousGameMode.clear();
        spectators.clear();
        if (punishmentManager != null) {
            punishmentManager.onGameStop();
        }
        if (hudManager != null) {
            hudManager.onGameStop();
        }
    }

    /** 返回当前队伍快照（用于展示）。 */
    public Map<GameTeam, List<UUID>> snapshotTeams() {
        Map<GameTeam, List<UUID>> map = new LinkedHashMap<>();
        for (GameTeam t : GameTeam.values()) {
            map.put(t, teamMembers(t));
        }
        return map;
    }

    public void send(Player player, String legacy) {
        player.sendMessage(Text.amp(config.prefix() + legacy));
    }
}
