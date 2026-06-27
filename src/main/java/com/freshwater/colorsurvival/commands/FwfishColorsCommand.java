package com.freshwater.colorsurvival.commands;

import com.freshwater.colorsurvival.color.BlockColorMapper;
import com.freshwater.colorsurvival.color.GameColor;
import com.freshwater.colorsurvival.config.PluginConfig;
import com.freshwater.colorsurvival.game.GameManager;
import com.freshwater.colorsurvival.game.GameTeam;
import com.freshwater.colorsurvival.ui.CardGui;
import com.freshwater.colorsurvival.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /fwfish-colors 主命令与 Tab 补全。作者：淡水岛开发组
 */
public final class FwfishColorsCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN = "fwfish-colors.admin";

    private final JavaPlugin plugin;
    private final GameManager game;
    private final PluginConfig config;
    private final BlockColorMapper mapper;
    private final CardGui cardGui;

    private static final List<String> SUBS = List.of(
            "help", "about", "join", "leave", "spectate", "color", "card", "whatcolor", "team",
            "start", "stop", "bypass", "reload", "punish", "config");

    public FwfishColorsCommand(JavaPlugin plugin, GameManager game, PluginConfig config,
                               BlockColorMapper mapper, CardGui cardGui) {
        this.plugin = plugin;
        this.game = game;
        this.config = config;
        this.mapper = mapper;
        this.cardGui = cardGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> help(sender);
            case "about" -> about(sender);
            case "join" -> join(sender, args);
            case "leave" -> leave(sender);
            case "spectate", "spec" -> spectate(sender);
            case "color" -> color(sender);
            case "card" -> card(sender);
            case "whatcolor" -> whatColor(sender);
            case "team" -> team(sender);
            case "start" -> start(sender);
            case "stop" -> stop(sender);
            case "bypass" -> bypass(sender);
            case "reload" -> reload(sender);
            case "punish" -> punish(sender, args);
            case "config" -> config(sender, args);
            default -> {
                msg(sender, "&c未知子命令，使用 /fwfish-colors help 查看帮助。");
            }
        }
        return true;
    }

    // ---- 子命令 ----

    private void help(CommandSender sender) {
        msg(sender, "&b===== &f颜色生存 FreshwaterColorSurvival &b=====");
        msg(sender, "&e/fwc join <A/B> &7- 加入队伍");
        msg(sender, "&e/fwc leave &7- 离开队伍");
        msg(sender, "&e/fwc spectate &7- 切换观战模式");
        msg(sender, "&e/fwc color &7- 查看你的颜色");
        msg(sender, "&e/fwc card &7- 查看 Bingo 卡");
        msg(sender, "&e/fwc whatcolor &7- 查看视线/手中方块颜色");
        msg(sender, "&e/fwc team &7- 查看队伍信息");
        if (sender.hasPermission(ADMIN)) {
            msg(sender, "&c[管理] /fwc start | stop &7- 开始/结束对局");
            msg(sender, "&c[管理] /fwc punish <on/off/status> &7- 惩罚阶段");
            msg(sender, "&c[管理] /fwc config <...> &7- 配置豁免");
            msg(sender, "&c[管理] /fwc reload &7- 重载配置");
            msg(sender, "&c[管理] /fwc bypass &7- 切换个人无视限制");
        }
        msg(sender, "&8作者：淡水岛开发组");
    }

    private void about(CommandSender sender) {
        msg(sender, "&b颜色生存 FreshwaterColorSurvival");
        msg(sender, "&7版本: &f" + plugin.getDescription().getVersion());
        msg(sender, "&7作者: &f淡水岛开发组");
        msg(sender, "&7玩法: 每人一种颜色，只能操作本色方块 + 双队 5x5 Bingo 连线竞速");
    }

    private void join(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            msg(sender, "&c用法: /fwc join <A/B>");
            return;
        }
        GameTeam team = GameTeam.fromArg(args[1]);
        if (team == null) {
            msg(sender, "&c未知队伍，请使用 A 或 B。");
            return;
        }
        game.join(player, team);
    }

    private void leave(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        game.leave(player);
    }

    private void spectate(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        game.toggleSpectate(player);
    }

    private void color(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        GameColor color = game.getColor(player.getUniqueId());
        GameTeam team = game.getTeam(player.getUniqueId());
        if (color == null) {
            msg(sender, "&7你当前没有分配颜色" + (team != null ? "（队伍：" + team.colored() + "&7，对局开始后分配）" : "。"));
            return;
        }
        msg(sender, "&f你的颜色：" + color.colored() + " &7| 队伍：" + (team != null ? team.colored() : "&7无"));
    }

    private void card(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        cardGui.open(player);
    }

    private void whatColor(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        Block target = player.getTargetBlockExact(8);
        Material type;
        String source;
        if (target != null && !target.getType().isAir()) {
            type = target.getType();
            source = "视线方块";
        } else {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                msg(sender, "&c请看向一个方块，或手持一个方块。");
                return;
            }
            type = hand.getType();
            source = "手中物品";
        }
        String colors = com.freshwater.colorsurvival.color.BlockColorMapper.describe(mapper.colorsOf(type));
        msg(sender, "&7" + source + " &f" + type.name() + " &7的颜色: " + colors);
    }

    private void team(CommandSender sender) {
        msg(sender, "&b===== 队伍信息 =====");
        Map<GameTeam, List<UUID>> snapshot = game.snapshotTeams();
        for (Map.Entry<GameTeam, List<UUID>> e : snapshot.entrySet()) {
            GameTeam t = e.getKey();
            StringBuilder sb = new StringBuilder();
            for (UUID id : e.getValue()) {
                Player p = Bukkit.getPlayer(id);
                String pname = p != null ? p.getName() : id.toString().substring(0, 8);
                GameColor col = game.getColor(id);
                sb.append("&7").append(pname);
                if (col != null) {
                    sb.append("(").append(col.colored()).append("&7)");
                }
                sb.append(" ");
            }
            msg(sender, t.colored() + " &7[" + e.getValue().size() + "]: "
                    + (sb.length() == 0 ? "&8(空)" : sb.toString()));
        }
    }

    private void start(CommandSender sender) {
        if (notAdmin(sender)) {
            return;
        }
        game.start(sender);
    }

    private void stop(CommandSender sender) {
        if (notAdmin(sender)) {
            return;
        }
        game.stop(sender);
    }

    private void bypass(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        if (!sender.hasPermission(ADMIN) && !sender.hasPermission("fwfish-colors.bypass")) {
            msg(sender, config.msg("no-permission"));
            return;
        }
        boolean now = game.toggleBypass(player);
        msg(sender, now ? "&a已开启个人无视颜色限制。" : "&7已关闭个人无视颜色限制。");
    }

    private void reload(CommandSender sender) {
        if (notAdmin(sender)) {
            return;
        }
        config.load();
        mapper.setOverrides(config.getBlockColorOverrides());
        msg(sender, "&a配置已重载。");
    }

    private void punish(CommandSender sender, String[] args) {
        if (notAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            msg(sender, "&c用法: /fwc punish <on/off/status>");
            return;
        }
        if (!game.isRunning()) {
            msg(sender, "&c对局未开始，无法调整惩罚阶段。");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "on" -> game.punishment().enablePunish(true);
            case "off" -> game.punishment().disablePunish();
            case "status" -> {
                if (game.punishment().isPunishPhase()) {
                    msg(sender, "&7当前阶段：&c惩罚阶段");
                } else {
                    long remain = game.punishment().remainingSecondsToAuto();
                    msg(sender, "&7当前阶段：&a宽容阶段" + (remain >= 0
                            ? " &7（" + (remain / 60) + "分" + (remain % 60) + "秒后自动开启惩罚）" : ""));
                }
            }
            default -> msg(sender, "&c未知参数，使用 on/off/status。");
        }
    }

    private void config(CommandSender sender, String[] args) {
        if (notAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            msg(sender, "&c用法: /fwc config <utility-exempt <true/false> | sidebar <true/false> | exempt-add <方块> | exempt-remove <方块> | show>");
            return;
        }
        String key = args[1].toLowerCase(Locale.ROOT);
        switch (key) {
            case "sidebar" -> {
                if (args.length < 3) {
                    msg(sender, "&7sidebar 当前为：" + (config.isSidebarEnabled() ? "&a开启" : "&c关闭"));
                    return;
                }
                boolean value = Boolean.parseBoolean(args[2]);
                config.setSidebarEnabled(value);
                game.hud().refreshSoon();
                msg(sender, "&a已设置 侧边栏 sidebar = " + value);
            }
            case "utility-exempt" -> {
                if (args.length < 3) {
                    msg(sender, "&7utility-exempt 当前为：" + (config.isUtilityExempt() ? "&a开启" : "&c关闭"));
                    return;
                }
                boolean value = Boolean.parseBoolean(args[2]);
                config.setUtilityExempt(value);
                msg(sender, "&a已设置 utility-exempt = " + value);
            }
            case "exempt-add" -> {
                Material m = matchMaterial(sender, args);
                if (m == null) {
                    return;
                }
                boolean ok = config.addExemptMaterial(m);
                msg(sender, ok ? "&a已添加豁免方块：&f" + m.name() : "&7该方块已在豁免列表中。");
            }
            case "exempt-remove" -> {
                Material m = matchMaterial(sender, args);
                if (m == null) {
                    return;
                }
                boolean ok = config.removeExemptMaterial(m);
                msg(sender, ok ? "&a已移除豁免方块：&f" + m.name() : "&7该方块不在豁免列表中。");
            }
            case "show" -> {
                msg(sender, "&7sidebar 侧边栏: " + (config.isSidebarEnabled() ? "&a开启" : "&c关闭"));
                msg(sender, "&7utility-exempt: " + (config.isUtilityExempt() ? "&a开启" : "&c关闭"));
                msg(sender, "&7豁免方块数量: &f" + config.getExemptMaterials().size());
                msg(sender, "&7自动惩罚: &f" + config.getAutoEnableAfterMinutes() + " 分钟后");
            }
            default -> msg(sender, "&c未知配置项。");
        }
    }

    // ---- 工具 ----

    private Material matchMaterial(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg(sender, "&c请指定方块名称。");
            return null;
        }
        Material m = Material.matchMaterial(args[2]);
        if (m == null) {
            msg(sender, "&c未知方块：" + args[2]);
        }
        return m;
    }

    private Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(Text.amp("&c该命令只能由玩家执行。"));
        return null;
    }

    private boolean notAdmin(CommandSender sender) {
        if (!sender.hasPermission(ADMIN)) {
            msg(sender, config.msg("no-permission").isEmpty() ? "&c你没有权限。" : config.msg("no-permission"));
            return true;
        }
        return false;
    }

    private void msg(CommandSender sender, String legacy) {
        sender.sendMessage(Text.amp(config.prefix() + legacy));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "join" -> {
                    return filter(List.of("A", "B"), args[1]);
                }
                case "punish" -> {
                    return filter(List.of("on", "off", "status"), args[1]);
                }
                case "config" -> {
                    return filter(List.of("sidebar", "utility-exempt", "exempt-add", "exempt-remove", "show"), args[1]);
                }
                default -> {
                    return List.of();
                }
            }
        }
        if (args.length == 3 && sub.equals("config")) {
            String key = args[1].toLowerCase(Locale.ROOT);
            if (key.equals("utility-exempt") || key.equals("sidebar")) {
                return filter(List.of("true", "false"), args[2]);
            }
            if (key.equals("exempt-add") || key.equals("exempt-remove")) {
                return Arrays.stream(Material.values())
                        .filter(Material::isBlock)
                        .map(m -> m.name().toLowerCase(Locale.ROOT))
                        .filter(n -> n.startsWith(args[2].toLowerCase(Locale.ROOT)))
                        .limit(40)
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
