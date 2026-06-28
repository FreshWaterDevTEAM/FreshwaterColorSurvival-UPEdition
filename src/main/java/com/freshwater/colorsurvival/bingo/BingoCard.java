package com.freshwater.colorsurvival.bingo;

import com.freshwater.colorsurvival.game.GameTeam;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 5x5 Bingo 卡。两队共用同一张卡，各自记录完成状态。作者：淡水岛开发组
 */
public final class BingoCard {

    public static final int SIZE = 5;
    public static final int CELLS = SIZE * SIZE;

    private final Material[] items = new Material[CELLS];
    private final Map<GameTeam, boolean[]> done = new EnumMap<>(GameTeam.class);

    public BingoCard(List<Material> pool) {
        List<Material> copy = new ArrayList<>(pool);
        Collections.shuffle(copy);
        for (int i = 0; i < CELLS; i++) {
            items[i] = copy.get(i % copy.size());
        }
        for (GameTeam team : GameTeam.values()) {
            done.put(team, new boolean[CELLS]);
        }
    }

    /** 使用预先编排好的 25 个格子（按卡平衡生成）。 */
    public BingoCard(Material[] prebuilt) {
        for (int i = 0; i < CELLS; i++) {
            items[i] = prebuilt[i % prebuilt.length];
        }
        for (GameTeam team : GameTeam.values()) {
            done.put(team, new boolean[CELLS]);
        }
    }

    public Material itemAt(int index) {
        return items[index];
    }

    public boolean isDone(GameTeam team, int index) {
        return done.get(team)[index];
    }

    /** 一次刷新的变化结果。 */
    public static final class Diff {
        public final List<Material> added = new ArrayList<>();
        public final List<Material> removed = new ArrayList<>();

        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty();
        }
    }

    /**
     * 按某队「当前持有的物品集合」刷新整张卡的完成状态：
     * 持有的格子点亮，不再持有的格子熄灭。
     *
     * @return 本次新点亮 / 新熄灭的物品
     */
    public Diff updateHeld(GameTeam team, java.util.Set<Material> held) {
        boolean[] flags = done.get(team);
        Diff diff = new Diff();
        for (int i = 0; i < CELLS; i++) {
            boolean now = held.contains(items[i]);
            if (now && !flags[i]) {
                diff.added.add(items[i]);
            } else if (!now && flags[i]) {
                diff.removed.add(items[i]);
            }
            flags[i] = now;
        }
        return diff;
    }

    public int completedCount(GameTeam team) {
        boolean[] flags = done.get(team);
        int n = 0;
        for (boolean f : flags) {
            if (f) {
                n++;
            }
        }
        return n;
    }

    /** 是否已连成一线（横/竖/斜）。 */
    public boolean hasLine(GameTeam team) {
        boolean[] f = done.get(team);
        // 行
        for (int r = 0; r < SIZE; r++) {
            boolean all = true;
            for (int col = 0; col < SIZE; col++) {
                if (!f[r * SIZE + col]) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return true;
            }
        }
        // 列
        for (int col = 0; col < SIZE; col++) {
            boolean all = true;
            for (int r = 0; r < SIZE; r++) {
                if (!f[r * SIZE + col]) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return true;
            }
        }
        // 主对角线
        boolean diag = true;
        for (int i = 0; i < SIZE; i++) {
            if (!f[i * SIZE + i]) {
                diag = false;
                break;
            }
        }
        if (diag) {
            return true;
        }
        // 副对角线
        boolean anti = true;
        for (int i = 0; i < SIZE; i++) {
            if (!f[i * SIZE + (SIZE - 1 - i)]) {
                anti = false;
                break;
            }
        }
        return anti;
    }

    /** 当前最长连线进度（用于 HUD 展示，0~5）。 */
    public int bestLineProgress(GameTeam team) {
        boolean[] f = done.get(team);
        int best = 0;
        for (int r = 0; r < SIZE; r++) {
            int n = 0;
            for (int col = 0; col < SIZE; col++) {
                if (f[r * SIZE + col]) {
                    n++;
                }
            }
            best = Math.max(best, n);
        }
        for (int col = 0; col < SIZE; col++) {
            int n = 0;
            for (int r = 0; r < SIZE; r++) {
                if (f[r * SIZE + col]) {
                    n++;
                }
            }
            best = Math.max(best, n);
        }
        int d = 0;
        int a = 0;
        for (int i = 0; i < SIZE; i++) {
            if (f[i * SIZE + i]) {
                d++;
            }
            if (f[i * SIZE + (SIZE - 1 - i)]) {
                a++;
            }
        }
        best = Math.max(best, Math.max(d, a));
        return best;
    }
}
