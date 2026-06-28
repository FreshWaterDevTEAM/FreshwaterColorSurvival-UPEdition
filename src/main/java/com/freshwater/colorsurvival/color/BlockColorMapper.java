package com.freshwater.colorsurvival.color;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将任意 {@link Material} 映射到一组 {@link GameColor}（一个方块可以同时拥有多种颜色，
 * 取决于其材质里实际出现的颜色）。玩家只要拥有其中任意一种颜色即可交互。
 * 优先级：config 覆盖 &gt; 颜色名前缀 &gt; 精选多色表 &gt; 名称哈希兜底。
 * 作者：淡水岛开发组
 */
public final class BlockColorMapper {

    /** 16 种染料前缀名 -&gt; 对应主色。 */
    private static final Map<String, GameColor> DYE_PREFIX = new LinkedHashMap<>();

    static {
        DYE_PREFIX.put("WHITE", GameColor.WHITE);
        DYE_PREFIX.put("ORANGE", GameColor.ORANGE);
        DYE_PREFIX.put("MAGENTA", GameColor.MAGENTA);
        DYE_PREFIX.put("LIGHT_BLUE", GameColor.LIGHT_BLUE);
        DYE_PREFIX.put("YELLOW", GameColor.YELLOW);
        DYE_PREFIX.put("LIME", GameColor.LIME);
        DYE_PREFIX.put("PINK", GameColor.PINK);
        DYE_PREFIX.put("GRAY", GameColor.GRAY);
        DYE_PREFIX.put("LIGHT_GRAY", GameColor.LIGHT_GRAY);
        DYE_PREFIX.put("CYAN", GameColor.CYAN);
        DYE_PREFIX.put("PURPLE", GameColor.PURPLE);
        DYE_PREFIX.put("BLUE", GameColor.BLUE);
        DYE_PREFIX.put("BROWN", GameColor.BROWN);
        DYE_PREFIX.put("GREEN", GameColor.GREEN);
        DYE_PREFIX.put("RED", GameColor.RED);
        DYE_PREFIX.put("BLACK", GameColor.BLACK);
    }

    private final List<String> prefixSorted = new ArrayList<>();
    private final Map<Material, Set<GameColor>> curated = new EnumMap<>(Material.class);
    private final Map<String, GameColor> keywords = new LinkedHashMap<>();
    private final Map<Material, Set<GameColor>> overrides = new HashMap<>();
    private final Map<Material, Set<GameColor>> cache = new EnumMap<>(Material.class);

    public BlockColorMapper() {
        prefixSorted.addAll(DYE_PREFIX.keySet());
        // 长名优先，保证 LIGHT_BLUE 先于 BLUE、LIGHT_GRAY 先于 GRAY
        prefixSorted.sort((a, b) -> Integer.compare(b.length(), a.length()));
        buildCurated();
        buildKeywords();
    }

    public void setOverrides(Map<Material, Set<GameColor>> map) {
        overrides.clear();
        if (map != null) {
            overrides.putAll(map);
        }
        cache.clear();
    }

    /** 返回某方块/物品拥有的所有颜色（不可变）。 */
    public Set<GameColor> colorsOf(Material material) {
        if (material == null) {
            return EnumSet.of(GameColor.WHITE);
        }
        Set<GameColor> cached = cache.get(material);
        if (cached != null) {
            return cached;
        }
        Set<GameColor> result = compute(material);
        cache.put(material, result);
        return result;
    }

    private Set<GameColor> compute(Material material) {
        Set<GameColor> override = overrides.get(material);
        if (override != null && !override.isEmpty()) {
            return EnumSet.copyOf(override);
        }
        String name = material.name();
        for (String prefix : prefixSorted) {
            if (name.startsWith(prefix + "_") || name.equals(prefix)) {
                return EnumSet.of(DYE_PREFIX.get(prefix));
            }
        }
        Set<GameColor> cur = curated.get(material);
        if (cur != null) {
            return EnumSet.copyOf(cur);
        }
        Set<GameColor> kw = keywordColors(name);
        if (!kw.isEmpty()) {
            return kw;
        }
        GameColor[] values = GameColor.values();
        return EnumSet.of(values[Math.floorMod(name.hashCode(), values.length)]);
    }

    /** 按名称里出现的关键词推断颜色（一个名字可命中多个 -&gt; 取并集）。 */
    private Set<GameColor> keywordColors(String name) {
        EnumSet<GameColor> set = EnumSet.noneOf(GameColor.class);
        for (Map.Entry<String, GameColor> e : keywords.entrySet()) {
            if (name.contains(e.getKey())) {
                set.add(e.getValue());
            }
        }
        return set;
    }

    private void buildKeywords() {
        // 矿物 / 材料
        keywords.put("NETHERITE", GameColor.BLACK);
        keywords.put("DIAMOND", GameColor.BLUE);
        keywords.put("GOLDEN", GameColor.YELLOW);
        keywords.put("GOLD", GameColor.YELLOW);
        keywords.put("IRON", GameColor.WHITE);
        keywords.put("CHAINMAIL", GameColor.GRAY);
        keywords.put("COPPER", GameColor.ORANGE);
        keywords.put("EMERALD", GameColor.GREEN);
        keywords.put("LAPIS", GameColor.BLUE);
        keywords.put("REDSTONE", GameColor.RED);
        keywords.put("AMETHYST", GameColor.PURPLE);
        keywords.put("QUARTZ", GameColor.WHITE);
        keywords.put("CHARCOAL", GameColor.BLACK);
        keywords.put("COAL", GameColor.BLACK);
        keywords.put("OBSIDIAN", GameColor.PURPLE);
        keywords.put("PRISMARINE", GameColor.BLUE);
        keywords.put("FLINT", GameColor.GRAY);
        keywords.put("CLAY", GameColor.GRAY);
        keywords.put("BRICK", GameColor.RED);
        // 生物掉落 / 杂项材料
        keywords.put("LEATHER", GameColor.BROWN);
        keywords.put("BONE", GameColor.WHITE);
        keywords.put("FEATHER", GameColor.WHITE);
        keywords.put("PAPER", GameColor.WHITE);
        keywords.put("EGG", GameColor.WHITE);
        keywords.put("SUGAR", GameColor.WHITE);
        keywords.put("SNOW", GameColor.WHITE);
        keywords.put("MILK", GameColor.WHITE);
        keywords.put("STRING", GameColor.WHITE);
        keywords.put("INK", GameColor.BLACK);
        keywords.put("SLIME", GameColor.GREEN);
        keywords.put("GUNPOWDER", GameColor.GRAY);
        keywords.put("PHANTOM", GameColor.GRAY);
        keywords.put("SHULKER", GameColor.PURPLE);
        keywords.put("ENDER", GameColor.GREEN);
        keywords.put("BLAZE", GameColor.ORANGE);
        keywords.put("MAGMA", GameColor.ORANGE);
        keywords.put("GLOWSTONE", GameColor.YELLOW);
        keywords.put("GLOW", GameColor.YELLOW);
        keywords.put("HONEY", GameColor.ORANGE);
        keywords.put("ROTTEN", GameColor.GREEN);
        keywords.put("ARROW", GameColor.GRAY);
        keywords.put("BUCKET", GameColor.GRAY);
        keywords.put("SHEARS", GameColor.GRAY);
        keywords.put("BELL", GameColor.YELLOW);
        // 工具 / 物品基底
        keywords.put("BOOK", GameColor.BROWN);
        keywords.put("FISHING", GameColor.BROWN);
        keywords.put("BOW", GameColor.BROWN);
        keywords.put("BOWL", GameColor.BROWN);
        keywords.put("TORCH", GameColor.ORANGE);
        keywords.put("STICK", GameColor.BROWN);
        // 植物 / 食物
        keywords.put("WHEAT", GameColor.YELLOW);
        keywords.put("HAY", GameColor.YELLOW);
        keywords.put("CARROT", GameColor.ORANGE);
        keywords.put("POTATO", GameColor.YELLOW);
        keywords.put("BEETROOT", GameColor.RED);
        keywords.put("APPLE", GameColor.RED);
        keywords.put("BERRIES", GameColor.RED);
        keywords.put("BERRY", GameColor.RED);
        keywords.put("PUMPKIN", GameColor.ORANGE);
        keywords.put("MELON", GameColor.GREEN);
        keywords.put("BREAD", GameColor.BROWN);
        keywords.put("COOKIE", GameColor.BROWN);
        keywords.put("CAKE", GameColor.WHITE);
        keywords.put("COOKED", GameColor.BROWN);
        keywords.put("BEEF", GameColor.BROWN);
        keywords.put("PORK", GameColor.BROWN);
        keywords.put("CHICKEN", GameColor.BROWN);
        keywords.put("MUTTON", GameColor.BROWN);
        keywords.put("RABBIT", GameColor.BROWN);
        keywords.put("COD", GameColor.BROWN);
        keywords.put("SALMON", GameColor.RED);
        keywords.put("KELP", GameColor.GREEN);
        keywords.put("SEAGRASS", GameColor.GREEN);
        keywords.put("GRASS", GameColor.GREEN);
        keywords.put("MOSS", GameColor.GREEN);
        keywords.put("VINE", GameColor.GREEN);
        keywords.put("FERN", GameColor.GREEN);
        keywords.put("LEAVES", GameColor.GREEN);
        keywords.put("SAPLING", GameColor.GREEN);
        keywords.put("FLOWER", GameColor.PINK);
        keywords.put("TULIP", GameColor.PINK);
        keywords.put("BAMBOO", GameColor.GREEN);
        keywords.put("SUGAR_CANE", GameColor.GREEN);
        // 地形 / 方块基底
        keywords.put("CHORUS", GameColor.PURPLE);
        keywords.put("PURPUR", GameColor.PURPLE);
        keywords.put("NETHERRACK", GameColor.RED);
        keywords.put("NETHER", GameColor.RED);
        keywords.put("CRIMSON", GameColor.RED);
        keywords.put("WARPED", GameColor.BLUE);
        keywords.put("WART", GameColor.RED);
        keywords.put("BLACKSTONE", GameColor.BLACK);
        keywords.put("DEEPSLATE", GameColor.GRAY);
        keywords.put("COBBLE", GameColor.GRAY);
        keywords.put("STONE", GameColor.GRAY);
        keywords.put("DIRT", GameColor.BROWN);
        keywords.put("SAND", GameColor.YELLOW);
        keywords.put("LOG", GameColor.BROWN);
        keywords.put("PLANK", GameColor.BROWN);
        keywords.put("WOOD", GameColor.BROWN);
        keywords.put("DOOR", GameColor.BROWN);
        keywords.put("FENCE", GameColor.BROWN);
    }

    /** 将一组颜色拼成彩色 ■ 字符串，用于提示玩家可交互颜色。 */
    public static String describe(Set<GameColor> colors) {
        StringBuilder sb = new StringBuilder();
        for (GameColor c : colors) {
            sb.append(c.colored()).append(" ");
        }
        return sb.toString().trim();
    }

    private void put(Material material, GameColor... colors) {
        if (material == null) {
            return;
        }
        EnumSet<GameColor> set = EnumSet.noneOf(GameColor.class);
        for (GameColor c : colors) {
            set.add(c);
        }
        curated.put(material, set);
    }

    private void buildCurated() {
        // 草 / 泥土
        put(Material.GRASS_BLOCK, GameColor.GREEN, GameColor.BROWN);
        put(Material.DIRT, GameColor.BROWN);
        put(Material.COARSE_DIRT, GameColor.BROWN, GameColor.BLACK);
        put(Material.ROOTED_DIRT, GameColor.BROWN);
        put(Material.PODZOL, GameColor.BROWN, GameColor.ORANGE);
        put(Material.MUD, GameColor.BROWN, GameColor.GRAY);
        put(Material.MUDDY_MANGROVE_ROOTS, GameColor.BROWN);
        put(Material.DIRT_PATH, GameColor.BROWN);
        put(Material.MYCELIUM, GameColor.PURPLE, GameColor.BROWN);
        put(Material.MOSS_BLOCK, GameColor.GREEN);
        put(Material.MOSS_CARPET, GameColor.GREEN);

        // 石头类（灰为主，带白/黑斑）
        put(Material.STONE, GameColor.GRAY);
        put(Material.COBBLESTONE, GameColor.GRAY, GameColor.BLACK);
        put(Material.MOSSY_COBBLESTONE, GameColor.GRAY, GameColor.GREEN);
        put(Material.STONE_BRICKS, GameColor.GRAY);
        put(Material.SMOOTH_STONE, GameColor.GRAY, GameColor.WHITE);
        put(Material.ANDESITE, GameColor.GRAY, GameColor.WHITE);
        put(Material.DIORITE, GameColor.WHITE, GameColor.GRAY, GameColor.BLACK);
        put(Material.GRANITE, GameColor.PINK, GameColor.ORANGE, GameColor.GRAY);
        put(Material.DEEPSLATE, GameColor.GRAY, GameColor.BLACK);
        put(Material.COBBLED_DEEPSLATE, GameColor.GRAY, GameColor.BLACK);
        put(Material.POLISHED_DEEPSLATE, GameColor.GRAY, GameColor.BLACK);
        put(Material.DEEPSLATE_BRICKS, GameColor.GRAY, GameColor.BLACK);
        put(Material.TUFF, GameColor.GRAY, GameColor.GREEN);
        put(Material.CALCITE, GameColor.WHITE, GameColor.GRAY);
        put(Material.BASALT, GameColor.GRAY, GameColor.BLACK);
        put(Material.SMOOTH_BASALT, GameColor.BLACK, GameColor.GRAY);
        put(Material.BLACKSTONE, GameColor.BLACK, GameColor.GRAY);
        put(Material.OBSIDIAN, GameColor.PURPLE, GameColor.BLACK);
        put(Material.CRYING_OBSIDIAN, GameColor.PURPLE, GameColor.BLACK);
        put(Material.BEDROCK, GameColor.BLACK, GameColor.GRAY);
        put(Material.GRAVEL, GameColor.GRAY, GameColor.BROWN, GameColor.WHITE);
        put(Material.CLAY, GameColor.GRAY, GameColor.WHITE);

        // 沙
        put(Material.SAND, GameColor.YELLOW, GameColor.WHITE);
        put(Material.SANDSTONE, GameColor.YELLOW, GameColor.WHITE);
        put(Material.SMOOTH_SANDSTONE, GameColor.YELLOW, GameColor.WHITE);
        put(Material.RED_SAND, GameColor.ORANGE, GameColor.RED);
        put(Material.RED_SANDSTONE, GameColor.ORANGE, GameColor.RED);

        // 木：原木 + 木板 + 树叶
        put(Material.OAK_LOG, GameColor.BROWN);
        put(Material.OAK_WOOD, GameColor.BROWN);
        put(Material.OAK_PLANKS, GameColor.BROWN);
        put(Material.STRIPPED_OAK_LOG, GameColor.BROWN);
        put(Material.OAK_LEAVES, GameColor.GREEN);

        put(Material.BIRCH_LOG, GameColor.YELLOW, GameColor.WHITE);
        put(Material.BIRCH_WOOD, GameColor.WHITE, GameColor.BLACK);
        put(Material.BIRCH_PLANKS, GameColor.YELLOW);
        put(Material.STRIPPED_BIRCH_LOG, GameColor.YELLOW, GameColor.WHITE);
        put(Material.BIRCH_LEAVES, GameColor.GREEN);

        put(Material.SPRUCE_LOG, GameColor.BROWN, GameColor.BLACK);
        put(Material.SPRUCE_WOOD, GameColor.BROWN, GameColor.BLACK);
        put(Material.SPRUCE_PLANKS, GameColor.BROWN);
        put(Material.STRIPPED_SPRUCE_LOG, GameColor.BROWN);
        put(Material.SPRUCE_LEAVES, GameColor.GREEN);

        put(Material.DARK_OAK_LOG, GameColor.BROWN, GameColor.BLACK);
        put(Material.DARK_OAK_PLANKS, GameColor.BROWN, GameColor.BLACK);
        put(Material.DARK_OAK_LEAVES, GameColor.GREEN);

        put(Material.JUNGLE_LOG, GameColor.BROWN);
        put(Material.JUNGLE_PLANKS, GameColor.BROWN, GameColor.ORANGE);
        put(Material.JUNGLE_LEAVES, GameColor.GREEN);

        put(Material.ACACIA_LOG, GameColor.GRAY, GameColor.ORANGE);
        put(Material.ACACIA_PLANKS, GameColor.ORANGE, GameColor.RED);
        put(Material.ACACIA_LEAVES, GameColor.GREEN);

        put(Material.CHERRY_LOG, GameColor.PINK, GameColor.GRAY);
        put(Material.CHERRY_PLANKS, GameColor.PINK);
        put(Material.CHERRY_LEAVES, GameColor.PINK);

        put(Material.MANGROVE_LOG, GameColor.RED, GameColor.BROWN);
        put(Material.MANGROVE_PLANKS, GameColor.RED, GameColor.BROWN);
        put(Material.MANGROVE_LEAVES, GameColor.GREEN);

        put(Material.CRIMSON_STEM, GameColor.RED);
        put(Material.CRIMSON_PLANKS, GameColor.RED, GameColor.PURPLE);
        put(Material.WARPED_STEM, GameColor.BLUE, GameColor.GREEN);
        put(Material.WARPED_PLANKS, GameColor.BLUE, GameColor.GREEN);

        put(Material.BAMBOO, GameColor.GREEN);
        put(Material.BAMBOO_BLOCK, GameColor.GREEN, GameColor.YELLOW);
        put(Material.BAMBOO_PLANKS, GameColor.YELLOW);
        put(Material.SUGAR_CANE, GameColor.GREEN);
        put(Material.CACTUS, GameColor.GREEN);
        put(Material.MELON, GameColor.GREEN);
        put(Material.PUMPKIN, GameColor.ORANGE);
        put(Material.CARVED_PUMPKIN, GameColor.ORANGE);
        put(Material.HAY_BLOCK, GameColor.YELLOW);

        // 矿石（灰色基底 + 矿物颜色）
        put(Material.COAL_ORE, GameColor.GRAY, GameColor.BLACK);
        put(Material.DEEPSLATE_COAL_ORE, GameColor.GRAY, GameColor.BLACK);
        put(Material.COAL_BLOCK, GameColor.BLACK);
        put(Material.IRON_ORE, GameColor.GRAY, GameColor.ORANGE);
        put(Material.DEEPSLATE_IRON_ORE, GameColor.GRAY, GameColor.BLACK, GameColor.ORANGE);
        put(Material.IRON_BLOCK, GameColor.WHITE, GameColor.GRAY);
        put(Material.RAW_IRON_BLOCK, GameColor.ORANGE, GameColor.GRAY);
        put(Material.GOLD_ORE, GameColor.GRAY, GameColor.YELLOW);
        put(Material.DEEPSLATE_GOLD_ORE, GameColor.GRAY, GameColor.BLACK, GameColor.YELLOW);
        put(Material.GOLD_BLOCK, GameColor.YELLOW);
        put(Material.RAW_GOLD_BLOCK, GameColor.YELLOW, GameColor.ORANGE);
        put(Material.DIAMOND_ORE, GameColor.GRAY, GameColor.BLUE);
        put(Material.DEEPSLATE_DIAMOND_ORE, GameColor.GRAY, GameColor.BLACK, GameColor.BLUE);
        put(Material.DIAMOND_BLOCK, GameColor.BLUE, GameColor.WHITE);
        put(Material.EMERALD_ORE, GameColor.GRAY, GameColor.GREEN);
        put(Material.DEEPSLATE_EMERALD_ORE, GameColor.GRAY, GameColor.BLACK, GameColor.GREEN);
        put(Material.EMERALD_BLOCK, GameColor.GREEN);
        put(Material.LAPIS_ORE, GameColor.GRAY, GameColor.BLUE);
        put(Material.DEEPSLATE_LAPIS_ORE, GameColor.GRAY, GameColor.BLACK, GameColor.BLUE);
        put(Material.LAPIS_BLOCK, GameColor.BLUE);
        put(Material.REDSTONE_ORE, GameColor.GRAY, GameColor.RED);
        put(Material.DEEPSLATE_REDSTONE_ORE, GameColor.GRAY, GameColor.BLACK, GameColor.RED);
        put(Material.REDSTONE_BLOCK, GameColor.RED);
        put(Material.COPPER_ORE, GameColor.GRAY, GameColor.ORANGE);
        put(Material.DEEPSLATE_COPPER_ORE, GameColor.GRAY, GameColor.BLACK, GameColor.ORANGE);
        put(Material.COPPER_BLOCK, GameColor.ORANGE);
        put(Material.AMETHYST_BLOCK, GameColor.PURPLE);
        put(Material.BUDDING_AMETHYST, GameColor.PURPLE);
        put(Material.QUARTZ_BLOCK, GameColor.WHITE);
        put(Material.NETHER_QUARTZ_ORE, GameColor.RED, GameColor.WHITE);

        // 下界 / 末地
        put(Material.NETHERRACK, GameColor.RED);
        put(Material.NETHER_BRICKS, GameColor.RED, GameColor.BLACK);
        put(Material.NETHER_WART_BLOCK, GameColor.RED);
        put(Material.WARPED_WART_BLOCK, GameColor.BLUE, GameColor.GREEN);
        put(Material.SOUL_SAND, GameColor.BROWN, GameColor.BLACK);
        put(Material.SOUL_SOIL, GameColor.BROWN, GameColor.BLACK);
        put(Material.GLOWSTONE, GameColor.YELLOW, GameColor.ORANGE);
        put(Material.MAGMA_BLOCK, GameColor.ORANGE, GameColor.BLACK);
        put(Material.END_STONE, GameColor.YELLOW, GameColor.WHITE);
        put(Material.END_STONE_BRICKS, GameColor.YELLOW, GameColor.WHITE);
        put(Material.PURPUR_BLOCK, GameColor.PURPLE);
        put(Material.CHORUS_PLANT, GameColor.PURPLE);
        put(Material.CHORUS_FLOWER, GameColor.PURPLE, GameColor.WHITE);

        // 水 / 冰 / 雪
        put(Material.WATER, GameColor.BLUE);
        put(Material.ICE, GameColor.BLUE, GameColor.WHITE);
        put(Material.PACKED_ICE, GameColor.BLUE, GameColor.WHITE);
        put(Material.BLUE_ICE, GameColor.BLUE, GameColor.WHITE);
        put(Material.SNOW, GameColor.WHITE);
        put(Material.SNOW_BLOCK, GameColor.WHITE);
        put(Material.POWDER_SNOW, GameColor.WHITE);

        // 杂项
        put(Material.GLASS, GameColor.WHITE);
        put(Material.GLASS_PANE, GameColor.WHITE);
        put(Material.BRICKS, GameColor.RED, GameColor.ORANGE);
        put(Material.TERRACOTTA, GameColor.ORANGE, GameColor.BROWN);
        put(Material.BONE_BLOCK, GameColor.WHITE);
        put(Material.SPONGE, GameColor.YELLOW);
        put(Material.SLIME_BLOCK, GameColor.GREEN);
        put(Material.HONEY_BLOCK, GameColor.ORANGE, GameColor.YELLOW);
        put(Material.HONEYCOMB_BLOCK, GameColor.ORANGE, GameColor.YELLOW);
    }
}
