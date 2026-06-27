package com.freshwater.colorsurvival.color;

import com.freshwater.colorsurvival.util.Text;
import org.bukkit.DyeColor;
import org.bukkit.Material;

/**
 * 游戏内使用的 11 种颜色：红、棕、橙、黄、绿、蓝、紫、粉、黑、灰、白。
 * 采用真·十六进制 RGB 显示，界面统一用对应颜色的 ■。作者：淡水岛开发组
 */
public enum GameColor {
    RED("红", "E53935", DyeColor.RED),
    BROWN("棕", "8B5A2B", DyeColor.BROWN),
    ORANGE("橙", "FB8C00", DyeColor.ORANGE),
    YELLOW("黄", "FDD835", DyeColor.YELLOW),
    GREEN("绿", "43A047", DyeColor.LIME),
    BLUE("蓝", "1E88E5", DyeColor.BLUE),
    PURPLE("紫", "8E24AA", DyeColor.PURPLE),
    PINK("粉", "FF6FB5", DyeColor.PINK),
    BLACK("黑", "1A1A1A", DyeColor.BLACK),
    GRAY("灰", "9E9E9E", DyeColor.GRAY),
    WHITE("白", "FFFFFF", DyeColor.WHITE);

    private static final String SQUARE = "\u25A0";

    private final String shortName;
    private final String hex;
    private final DyeColor dyeColor;

    GameColor(String shortName, String hex, DyeColor dyeColor) {
        this.shortName = shortName;
        this.hex = hex;
        this.dyeColor = dyeColor;
    }

    public String shortName() {
        return shortName;
    }

    public String hex() {
        return hex;
    }

    public DyeColor dyeColor() {
        return dyeColor;
    }

    /** 用于聊天/消息（& 形式，经 Text.amp 解析）。 */
    public String colored() {
        return "&#" + hex + SQUARE + "&r";
    }

    /** 用于 scoreboard 等需要 '§' 字符串的场景。 */
    public String coloredSection() {
        return Text.legacy(colored());
    }

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
