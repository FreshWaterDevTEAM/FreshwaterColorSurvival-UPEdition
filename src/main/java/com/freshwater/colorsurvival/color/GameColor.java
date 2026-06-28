package com.freshwater.colorsurvival.color;

import com.freshwater.colorsurvival.util.Text;
import org.bukkit.DyeColor;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家可分配的 16 种主色（真·十六进制 RGB，配色取自 ColorBound 调色板）。
 * 另支持 21 个细分类别名作为别名（如 DARK_OAK、SANDSTONE、TEAL ...），
 * 它们在解析时会归并到最接近的主色——这样方块对照表可以用更细的 37 类标注，
 * 但玩家只会被分配到这 16 个覆盖面广的主色，避免出现"没方块可破坏"的颜色。
 * 界面统一用对应颜色的 ■。作者：淡水岛开发组
 */
public enum GameColor {
    RED("红", "A02E26", DyeColor.RED),
    ORANGE("橙", "F27A10", DyeColor.ORANGE),
    YELLOW("黄", "FAD64A", DyeColor.YELLOW),
    LIME("黄绿", "AEDE5A", DyeColor.LIME),
    GREEN("绿", "00A400", DyeColor.GREEN),
    CYAN("青", "00FFFF", DyeColor.CYAN),
    LIGHT_BLUE("淡蓝", "3AB3DA", DyeColor.LIGHT_BLUE),
    BLUE("蓝", "2848AE", DyeColor.BLUE),
    PURPLE("紫", "7A2AAC", DyeColor.PURPLE),
    MAGENTA("品红", "BD44B3", DyeColor.MAGENTA),
    PINK("粉", "F38BAA", DyeColor.PINK),
    BROWN("棕", "5E3A21", DyeColor.BROWN),
    WHITE("白", "FFFFFF", DyeColor.WHITE),
    LIGHT_GRAY("浅灰", "ADADAD", DyeColor.LIGHT_GRAY),
    GRAY("灰", "696969", DyeColor.GRAY),
    BLACK("黑", "1A1A1A", DyeColor.BLACK);

    private static final String SQUARE = "\u25A0";

    /** 细分类别别名 -> 归并到的主色。键为 ColorBound 颜色类别名。 */
    private static final Map<String, GameColor> ALIASES = new HashMap<>();

    static {
        ALIASES.put("SANDSTONE", YELLOW);
        ALIASES.put("BAMBOO", LIME);
        ALIASES.put("HONEY", ORANGE);
        ALIASES.put("COPPER", ORANGE);
        ALIASES.put("ACACIA", ORANGE);
        ALIASES.put("EXPOSED_COPPER", BROWN);
        ALIASES.put("DARK_OAK", BROWN);
        ALIASES.put("LIGHT_RED", RED);
        ALIASES.put("DARK_RED", RED);
        ALIASES.put("CRIMSON_RED", RED);
        ALIASES.put("DARK_GREEN", GREEN);
        ALIASES.put("LIGHT_GREEN", GREEN);
        ALIASES.put("TEAL", CYAN);
        ALIASES.put("GRAY_BLUE", BLUE);
        ALIASES.put("LIGHT_GRAY_BLUE", LIGHT_BLUE);
        ALIASES.put("PURPUR", PURPLE);
        ALIASES.put("PURPLE_BLUE", PURPLE);
        ALIASES.put("GRAY_PURPLE", PURPLE);
        ALIASES.put("CHERRY_PINK", PINK);
        ALIASES.put("CORAL_PINK", PINK);
        ALIASES.put("ENCHANTED", MAGENTA);
    }

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

    /** 解析颜色名：先匹配 16 主色，再匹配 37 细分类别别名。 */
    public static GameColor fromName(String name) {
        if (name == null) {
            return null;
        }
        String key = name.trim().toUpperCase();
        try {
            return valueOf(key);
        } catch (IllegalArgumentException ex) {
            return ALIASES.get(key);
        }
    }
}
