package com.freshwater.colorsurvival.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * 文本与颜色代码工具。支持十六进制 RGB（&amp;#RRGGBB）。作者：淡水岛开发组
 */
public final class Text {

    /** 解析 '&amp;' 颜色代码（含 &amp;#RRGGBB 十六进制）。 */
    public static final LegacyComponentSerializer AMP = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    /** 输出为 '§' 代码（十六进制用 §x§R§R§G§G§B§B 格式，客户端可在聊天/记分板渲染）。 */
    public static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character('\u00A7')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private Text() {
    }

    public static Component amp(String legacy) {
        return AMP.deserialize(legacy == null ? "" : legacy);
    }

    public static Component sec(String legacy) {
        return SECTION.deserialize(legacy == null ? "" : legacy);
    }

    /** 将 '&amp;' 代码（含 RGB）转换为 '§' 代码，用于 scoreboard 等仍使用 String 的 API。 */
    public static String legacy(String s) {
        if (s == null) {
            return "";
        }
        return SECTION.serialize(AMP.deserialize(s));
    }
}
