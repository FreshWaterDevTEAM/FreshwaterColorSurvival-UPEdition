package com.freshwater.colorsurvival.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * 文本与颜色代码工具。作者：淡水岛开发组
 */
public final class Text {

    public static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();
    public static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    private Text() {
    }

    /** 解析 '&' 颜色代码字符串为 Component。 */
    public static Component amp(String legacy) {
        return AMP.deserialize(legacy == null ? "" : legacy);
    }

    /** 解析 '§' 颜色代码字符串为 Component。 */
    public static Component sec(String legacy) {
        return SECTION.deserialize(legacy == null ? "" : legacy);
    }

    /** 将 '&' 代码转换为 '§' 代码（用于 scoreboard 等仍使用 String 的 API）。 */
    public static String legacy(String s) {
        if (s == null) {
            return "";
        }
        return SECTION.serialize(AMP.deserialize(s));
    }
}
