package com.freshwater.colorsurvival.color;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将任意 {@link Material} 映射到一种 {@link GameColor}。
 * 优先级：config 覆盖 &gt; 颜色名前缀 &gt; 精选自然方块表 &gt; 名称哈希兜底。
 * 作者：淡水岛开发组
 */
public final class BlockColorMapper {

    private final List<GameColor> prefixSorted = new ArrayList<>();
    private final Map<Material, GameColor> curated = new EnumMap<>(Material.class);
    private final Map<Material, GameColor> overrides = new HashMap<>();
    private final Map<Material, GameColor> cache = new EnumMap<>(Material.class);

    public BlockColorMapper() {
        // 颜色名前缀按长度倒序，保证 LIGHT_BLUE 先于 BLUE、LIGHT_GRAY 先于 GRAY 匹配
        prefixSorted.addAll(List.of(GameColor.values()));
        prefixSorted.sort(Comparator.comparingInt((GameColor c) -> c.name().length()).reversed());
        buildCurated();
    }

    /** 由配置覆盖映射。 */
    public void setOverrides(Map<Material, GameColor> map) {
        overrides.clear();
        if (map != null) {
            overrides.putAll(map);
        }
        cache.clear();
    }

    public GameColor colorOf(Material material) {
        if (material == null) {
            return GameColor.WHITE;
        }
        GameColor cached = cache.get(material);
        if (cached != null) {
            return cached;
        }
        GameColor result = compute(material);
        cache.put(material, result);
        return result;
    }

    private GameColor compute(Material material) {
        GameColor override = overrides.get(material);
        if (override != null) {
            return override;
        }
        String name = material.name();
        for (GameColor c : prefixSorted) {
            if (name.startsWith(c.name() + "_") || name.equals(c.name())) {
                return c;
            }
        }
        GameColor cur = curated.get(material);
        if (cur != null) {
            return cur;
        }
        GameColor[] values = GameColor.values();
        return values[Math.floorMod(name.hashCode(), values.length)];
    }

    private void put(GameColor color, Material... materials) {
        for (Material m : materials) {
            if (m != null) {
                curated.put(m, color);
            }
        }
    }

    private void buildCurated() {
        // 石头/岩石类 -> 灰
        put(GameColor.LIGHT_GRAY,
                Material.STONE, Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.STONE_BRICKS,
                Material.SMOOTH_STONE, Material.ANDESITE, Material.DIORITE, Material.GRAVEL,
                Material.COBBLED_DEEPSLATE, Material.TUFF, Material.CALCITE, Material.CLAY);
        put(GameColor.GRAY,
                Material.DEEPSLATE, Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_BRICKS,
                Material.DEEPSLATE_TILES, Material.BASALT, Material.SMOOTH_BASALT, Material.BLACKSTONE,
                Material.GUNPOWDER);
        // 泥土/沙/木 -> 棕
        put(GameColor.BROWN,
                Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT, Material.PODZOL,
                Material.MUD, Material.PACKED_MUD, Material.DIRT_PATH, Material.GRANITE,
                Material.OAK_LOG, Material.SPRUCE_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG,
                Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS, Material.JUNGLE_PLANKS,
                Material.OAK_WOOD, Material.STRIPPED_OAK_LOG, Material.DRIED_KELP_BLOCK,
                Material.COCOA_BEANS, Material.STICK);
        // 草/植物 -> 绿
        put(GameColor.GREEN,
                Material.GRASS_BLOCK, Material.MOSS_BLOCK, Material.MOSS_CARPET,
                Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.DARK_OAK_LEAVES,
                Material.JUNGLE_LEAVES, Material.VINE, Material.CACTUS, Material.MELON,
                Material.SEAGRASS, Material.KELP, Material.EMERALD, Material.EMERALD_BLOCK,
                Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE);
        put(GameColor.LIME,
                Material.SLIME_BLOCK, Material.SLIME_BALL, Material.BAMBOO, Material.SUGAR_CANE,
                Material.SHORT_GRASS, Material.FERN, Material.LILY_PAD, Material.WHEAT);
        // 沙/黄 -> 黄
        put(GameColor.YELLOW,
                Material.SAND, Material.SANDSTONE, Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE,
                Material.GLOWSTONE, Material.GLOWSTONE_DUST, Material.SPONGE, Material.HAY_BLOCK,
                Material.GOLD_BLOCK, Material.GOLD_INGOT, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
                Material.RAW_GOLD);
        put(GameColor.ORANGE,
                Material.RED_SAND, Material.TERRACOTTA, Material.PUMPKIN, Material.CARVED_PUMPKIN,
                Material.JACK_O_LANTERN, Material.COPPER_BLOCK, Material.COPPER_INGOT, Material.COPPER_ORE,
                Material.DEEPSLATE_COPPER_ORE, Material.RAW_COPPER, Material.HONEY_BLOCK,
                Material.HONEYCOMB, Material.CARROT);
        // 红色系
        put(GameColor.RED,
                Material.NETHERRACK, Material.NETHER_BRICKS, Material.NETHER_WART_BLOCK,
                Material.REDSTONE, Material.REDSTONE_BLOCK, Material.REDSTONE_ORE,
                Material.DEEPSLATE_REDSTONE_ORE, Material.APPLE, Material.BEETROOT, Material.POPPY,
                Material.CRIMSON_PLANKS, Material.CRIMSON_STEM, Material.CRIMSON_HYPHAE);
        put(GameColor.PINK,
                Material.CHERRY_LOG, Material.CHERRY_PLANKS, Material.CHERRY_LEAVES,
                Material.PINK_PETALS, Material.BRAIN_CORAL, Material.BRAIN_CORAL_BLOCK);
        // 青/蓝
        put(GameColor.CYAN,
                Material.DIAMOND, Material.DIAMOND_BLOCK, Material.DIAMOND_ORE,
                Material.DEEPSLATE_DIAMOND_ORE, Material.PRISMARINE, Material.PRISMARINE_BRICKS,
                Material.DARK_PRISMARINE, Material.WARPED_PLANKS, Material.WARPED_STEM);
        put(GameColor.LIGHT_BLUE,
                Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.SNOW_BLOCK,
                Material.POWDER_SNOW, Material.DIAMOND_HORSE_ARMOR);
        put(GameColor.BLUE,
                Material.WATER, Material.LAPIS_BLOCK, Material.LAPIS_LAZULI, Material.LAPIS_ORE,
                Material.DEEPSLATE_LAPIS_ORE, Material.TUBE_CORAL, Material.TUBE_CORAL_BLOCK);
        // 紫/品红
        put(GameColor.PURPLE,
                Material.AMETHYST_BLOCK, Material.AMETHYST_SHARD, Material.BUDDING_AMETHYST,
                Material.CHORUS_PLANT, Material.CHORUS_FLOWER, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
                Material.END_STONE, Material.END_STONE_BRICKS);
        put(GameColor.MAGENTA,
                Material.PURPUR_BLOCK, Material.PURPUR_PILLAR, Material.ALLIUM, Material.LILAC);
        // 黑
        put(GameColor.BLACK,
                Material.COAL, Material.COAL_BLOCK, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
                Material.INK_SAC, Material.BEDROCK, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE);
        // 白
        put(GameColor.WHITE,
                Material.SNOW, Material.QUARTZ_BLOCK, Material.QUARTZ, Material.SMOOTH_QUARTZ,
                Material.IRON_BLOCK, Material.IRON_INGOT, Material.RAW_IRON, Material.BONE_BLOCK,
                Material.BONE, Material.SUGAR, Material.PAPER, Material.MILK_BUCKET, Material.EGG);
    }
}
