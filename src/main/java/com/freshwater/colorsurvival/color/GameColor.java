package com.freshwater.colorsurvival.color;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

/**
 * 游戏内使用的 11 种颜色：红、棕、橙、黄、绿、蓝、紫、粉、黑、灰、白。
 * 玩家界面统一用对应颜色的 ■ 显示，不用汉字。作者：淡水岛开发组
 */
public enum GameColor {
    RED("红", ChatColor.RED, DyeColor.RED),
    BROWN("棕", ChatColor.DARK_RED, DyeColor.BROWN),
    ORANGE("橙", ChatColor.GOLD, DyeColor.ORANGE),
    YELLOW("黄", ChatColor.YELLOW, DyeColor.YELLOW),
    GREEN("绿", ChatColor.GREEN, DyeColor.LIME),
    BLUE("蓝", ChatColor.BLUE, DyeColor.BLUE),
    PURPLE("紫", ChatColor.DARK_PURPLE, DyeColor.PURPLE),
    PINK("粉", ChatColor.LIGHT_PURPLE, DyeColor.PINK),
    BLACK("黑", ChatColor.BLACK, DyeColor.BLACK),
    GRAY("灰", ChatColor.GRAY, DyeColor.GRAY),
    WHITE("白", ChatColor.WHITE, DyeColor.WHITE);

    private static final String SQUARE = "\u25A0";

    private final String shortName;
    private final ChatColor chatColor;
    private final DyeColor dyeColor;

    GameColor(String shortName, ChatColor chatColor, DyeColor dyeColor) {
        this.shortName = shortName;
        this.chatColor = chatColor;
        this.dyeColor = dyeColor;
    }

    public String shortName() {
        return shortName;
    }

    public ChatColor chatColor() {
        return chatColor;
    }

    public DyeColor dyeColor() {
        return dyeColor;
    }

    /** 玩家界面显示：对应颜色的 ■。 */
    public String colored() {
        return chatColor + SQUARE + ChatColor.RESET;
    }

    /** 用于 GUI 的代表方块（接近该颜色的羊毛）。 */
    public Material woolMaterial() {
        Material m = Material.matchMaterial(dyeColor.name() + "_WOOL");
        return m != null ? m : Material.WHITE_WOOL;
    }

    public Material glassPaneMaterial() {
        Material m = Material.matchMaterial(dyeColor.name() + "_STAINED_GLASS_PANE");
        return m != null ? m : Material.WHITE_STAINED_GLASS_PANE;
    }

    public static GameColor fromName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
