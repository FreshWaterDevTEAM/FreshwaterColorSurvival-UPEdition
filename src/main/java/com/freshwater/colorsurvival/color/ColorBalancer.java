package com.freshwater.colorsurvival.color;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 平衡器：实现三层平衡。作者：淡水岛开发组
 * <ol>
 *   <li>两队颜色镜像：两队使用完全相同的一组颜色，保证队间绝对公平；</li>
 *   <li>按卡平衡：bingo 卡的 25 格所需颜色均匀摊到各颜色，每名玩家在卡上同等重要；</li>
 *   <li>择优色池：选用的颜色都保证能在卡上拥有格子（弱色保留，靠按卡平衡与强色同等重要）。</li>
 * </ol>
 */
public final class ColorBalancer {

    private ColorBalancer() {
    }

    /** 一次开局的颜色编排。 */
    public static final class Plan {
        /** 两队按下标镜像分配用的有序颜色（长度 = 两队较大人数）。 */
        public final List<GameColor> orderedColors;
        /** 卡片相关颜色（两队都拥有，卡片格子只摊到这些颜色上）。 */
        public final List<GameColor> cardColors;

        Plan(List<GameColor> orderedColors, List<GameColor> cardColors) {
            this.orderedColors = orderedColors;
            this.cardColors = cardColors;
        }
    }

    /** 计算每种颜色在物品池中“能持有/获取”的物品集合（按物品自身颜色判定）。 */
    public static Map<GameColor, List<Material>> obtainable(List<Material> pool, BlockColorMapper mapper) {
        Map<GameColor, List<Material>> map = new EnumMap<>(GameColor.class);
        for (GameColor c : GameColor.values()) {
            map.put(c, new ArrayList<>());
        }
        for (Material m : pool) {
            for (GameColor c : mapper.colorsOf(m)) {
                map.get(c).add(m);
            }
        }
        return map;
    }

    /**
     * 编排两队颜色：两队按下标镜像分配；卡片颜色取两队公共部分（= 较小队人数，单队时取该队人数），
     * 且只选“能在物品池中获取到物品”的颜色，保证卡片可完成。
     */
    public static Plan plan(int sizeA, int sizeB, List<Material> pool, BlockColorMapper mapper, Random rng) {
        int maxSize = Math.max(sizeA, sizeB);
        int minSize = Math.min(sizeA, sizeB);
        int cardCount = (minSize == 0) ? maxSize : minSize;

        Map<GameColor, List<Material>> obtain = obtainable(pool, mapper);

        // 能在卡上拥有格子的候选色（在池中至少能获取一个物品）
        List<GameColor> usable = new ArrayList<>();
        List<GameColor> fallback = new ArrayList<>();
        for (GameColor c : GameColor.values()) {
            if (!obtain.get(c).isEmpty()) {
                usable.add(c);
            } else {
                fallback.add(c);
            }
        }
        Collections.shuffle(usable, rng);
        Collections.shuffle(fallback, rng);

        // 先取卡片颜色（必须可获取物品），再补足到 maxSize（补位色可用任意剩余色）
        List<GameColor> ordered = new ArrayList<>();
        for (GameColor c : usable) {
            if (ordered.size() >= cardCount) {
                break;
            }
            ordered.add(c);
        }
        List<GameColor> cardColors = new ArrayList<>(ordered);

        // 补足剩余（较大队多出来的成员所需的额外色，不要求覆盖卡片）
        List<GameColor> rest = new ArrayList<>();
        for (GameColor c : usable) {
            if (!ordered.contains(c)) {
                rest.add(c);
            }
        }
        rest.addAll(fallback);
        for (GameColor c : rest) {
            if (ordered.size() >= maxSize) {
                break;
            }
            ordered.add(c);
        }
        return new Plan(ordered, cardColors);
    }

    /**
     * 生成按卡平衡的物品数组：把 cells 个格子尽量均匀摊到 cardColors 上，
     * 每种颜色优先获得“能被它获取、且被尽量少的在用颜色获取”的物品（更专属 -&gt; 该玩家更关键）。
     */
    public static Material[] buildCard(int cells, List<GameColor> cardColors,
                                       List<Material> pool, BlockColorMapper mapper, Random rng) {
        if (cardColors.isEmpty()) {
            // 兜底：无颜色信息时随机填充
            Material[] arr = new Material[cells];
            for (int i = 0; i < cells; i++) {
                arr[i] = pool.get(rng.nextInt(pool.size()));
            }
            return arr;
        }

        Map<GameColor, List<Material>> obtain = obtainable(pool, mapper);

        // 每个物品被多少个“在用卡片颜色”能获取（越少越专属）
        Map<Material, Integer> ownerCount = new HashMap<>();
        for (Material m : new HashSet<>(pool)) {
            int n = 0;
            Set<GameColor> mc = mapper.colorsOf(m);
            for (GameColor c : cardColors) {
                if (mc.contains(c)) {
                    n++;
                }
            }
            ownerCount.put(m, n);
        }

        int m = cardColors.size();
        int base = cells / m;
        int rem = cells % m;

        List<GameColor> order = new ArrayList<>(cardColors);
        Collections.shuffle(order, rng);

        List<Material> picked = new ArrayList<>();
        Set<Material> used = new HashSet<>();
        for (int idx = 0; idx < order.size(); idx++) {
            GameColor c = order.get(idx);
            int target = base + (idx < rem ? 1 : 0);
            List<Material> cand = new ArrayList<>(obtain.getOrDefault(c, Collections.emptyList()));
            if (cand.isEmpty()) {
                continue;
            }
            // 优先：更专属（ownerCount 小）-> 尚未使用 -> 随机
            Collections.shuffle(cand, rng);
            cand.sort(Comparator
                    .comparingInt((Material mt) -> ownerCount.getOrDefault(mt, 1))
                    .thenComparingInt(mt -> used.contains(mt) ? 1 : 0));
            int got = 0;
            for (Material mt : cand) {
                if (got >= target) {
                    break;
                }
                if (!used.contains(mt)) {
                    picked.add(mt);
                    used.add(mt);
                    got++;
                }
            }
            // 不够不同物品则在该色可获取物品里重复填充
            int ci = 0;
            while (got < target) {
                picked.add(cand.get(ci % cand.size()));
                ci++;
                got++;
            }
        }

        // 校正到 cells 个
        while (picked.size() < cells) {
            picked.add(pool.get(rng.nextInt(pool.size())));
        }
        while (picked.size() > cells) {
            picked.remove(picked.size() - 1);
        }
        Collections.shuffle(picked, rng);
        return picked.toArray(new Material[0]);
    }
}
