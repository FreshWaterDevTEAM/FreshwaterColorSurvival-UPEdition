package com.freshwater.colorsurvival.color;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

/**
 * 游戏内使用的 16 种颜色。枚举名与 {@link DyeColor} 对应，方便按方块名前缀匹配。
 * 作者：淡水岛开发组
 */
public enum GameColor {
    WHITE("白色", ChatColor.WHITE, DyeColor.WHITE),
    ORANGE("橙色", ChatColor.GOLD, DyeColor.ORANGE),
    MAGENTA("品红", ChatColor.LIGHT_PURPLE, DyeColor.MAGENTA),
    LIGHT_BLUE("淡蓝", ChatColor.AQUA, DyeColor.LIGHT_BLUE),
    YELLOW("黄色", ChatColor.YELLOW, DyeColor.YELLOW),
    LIME("黄绿", ChatColor.GREEN, DyeColor.LIME),
    PINK("粉红", ChatColor.LIGHT_PURPLE, DyeColor.PINK),
    GRAY("灰色", ChatColor.DARK_GRAY, DyeColor.GRAY),
    LIGHT_GRAY("淡灰", ChatColor.GRAY, DyeColor.LIGHT_GRAY),
    CYAN("青色", ChatColor.DARK_AQUA, DyeColor.CYAN),
    PURPLE("紫色", ChatColor.DARK_PURPLE, DyeColor.PURPLE),
    BLUE("蓝色", ChatColor.BLUE, DyeColor.BLUE),
    BROWN("棕色", ChatColor.GOLD, DyeColor.BROWN),
    GREEN("绿色", ChatColor.DARK_GREEN, DyeColor.GREEN),
    RED("红色", ChatColor.RED, DyeColor.RED),
    BLACK("黑色", ChatColor.BLACK, DyeColor.BLACK);

    private final String displayName;
    private final ChatColor chatColor;
    private final DyeColor dyeColor;

    GameColor(String displayName, ChatColor chatColor, DyeColor dyeColor) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.dyeColor = dyeColor;
    }

    public String displayName() {
        return displayName;
    }

    public ChatColor chatColor() {
        return chatColor;
    }

    public DyeColor dyeColor() {
        return dyeColor;
    }

    /** 带颜色的中文显示名（'§' 代码）。 */
    public String colored() {
        return chatColor + displayName + ChatColor.RESET;
    }

    /** 用于 GUI / 展示的代表方块（该颜色的羊毛）。 */
    public Material woolMaterial() {
        Material m = Material.matchMaterial(name() + "_WOOL");
        return m != null ? m : Material.WHITE_WOOL;
    }

    /** 用于 HUD 装饰的玻璃板。 */
    public Material glassPaneMaterial() {
        Material m = Material.matchMaterial(name() + "_STAINED_GLASS_PANE");
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
