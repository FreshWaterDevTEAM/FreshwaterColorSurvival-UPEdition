package com.freshwater.colorsurvival.game;

/**
 * 违规处理阶段。作者：淡水岛开发组
 */
public enum PunishPhase {
    /** 宽容阶段：仅取消操作 + 提示。 */
    GRACE("宽容阶段"),
    /** 惩罚阶段：取消操作 + 扣血 + 随机清除物品。 */
    PUNISH("惩罚阶段");

    private final String displayName;

    PunishPhase(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
