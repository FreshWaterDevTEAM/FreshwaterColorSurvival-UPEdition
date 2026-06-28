package com.freshwater.colorsurvival.game;

import com.freshwater.colorsurvival.color.GameColor;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 两支队伍。两队都从全部主色中取色（同队内唯一，跨队可重复）。作者：淡水岛开发组
 */
public enum GameTeam {
    TEAM_A("A队", ChatColor.RED),
    TEAM_B("B队", ChatColor.AQUA);

    private final String displayName;
    private final ChatColor chatColor;
    private final List<GameColor> pool;

    GameTeam(String displayName, ChatColor chatColor) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.pool = Collections.unmodifiableList(Arrays.asList(GameColor.values()));
    }

    public String displayName() {
        return displayName;
    }

    public ChatColor chatColor() {
        return chatColor;
    }

    public String colored() {
        return chatColor + displayName + ChatColor.RESET;
    }

    /** 该队的色池（全部主色）。 */
    public List<GameColor> pool() {
        return pool;
    }

    public GameTeam other() {
        return this == TEAM_A ? TEAM_B : TEAM_A;
    }

    public static GameTeam fromArg(String arg) {
        if (arg == null) {
            return null;
        }
        String a = arg.trim().toLowerCase();
        return switch (a) {
            case "a", "team_a", "红", "红队", "1" -> TEAM_A;
            case "b", "team_b", "蓝", "蓝队", "2" -> TEAM_B;
            default -> null;
        };
    }
}
