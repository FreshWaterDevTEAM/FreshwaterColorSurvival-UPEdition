package com.freshwater.colorsurvival.color;

import com.freshwater.colorsurvival.game.GameTeam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 为每队成员从各自 8 色池中分配唯一颜色。作者：淡水岛开发组
 */
public final class ColorAssigner {

    /** 单队人数上限（= 色池大小）。 */
    public static final int MAX_PER_TEAM = 8;

    public static class AssignException extends Exception {
        public AssignException(String message) {
            super(message);
        }
    }

    /**
     * @return 玩家 UUID -> 分配到的颜色
     */
    public Map<UUID, GameColor> assign(GameTeam team, List<UUID> members) throws AssignException {
        List<GameColor> pool = new ArrayList<>(team.pool());
        if (members.size() > pool.size()) {
            throw new AssignException(team.displayName() + " 人数为 " + members.size()
                    + "，超过颜色上限 " + pool.size() + " 人。");
        }
        Collections.shuffle(pool);
        Map<UUID, GameColor> result = new HashMap<>();
        for (int i = 0; i < members.size(); i++) {
            result.put(members.get(i), pool.get(i));
        }
        return result;
    }
}
