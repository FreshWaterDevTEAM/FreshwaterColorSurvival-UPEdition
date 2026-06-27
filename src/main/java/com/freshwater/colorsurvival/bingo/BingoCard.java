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

    public Material itemAt(int index) {
        return items[index];
    }

    public boolean isDone(GameTeam team, int index) {
        return done.get(team)[index];
    }

    /**
     * 标记某队获得了某物品。
     *
     * @return 是否为新标记（之前未完成）
     */
    public boolean mark(GameTeam team, Material material) {
        boolean[] flags = done.get(team);
        boolean changed = false;
        for (int i = 0; i < CELLS; i++) {
            if (items[i] == material && !flags[i]) {
                flags[i] = true;
                changed = true;
            }
        }
        return changed;
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
