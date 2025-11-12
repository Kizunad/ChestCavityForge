package kizuna.guzhenren_event_ext.common.util;

import net.minecraft.world.entity.LivingEntity;

/**
 * 通用的 NPC 蛊修持久化键写入工具。
 * 仅设置当前需要的键，遵循 KISS/YAGNI。
 */
public final class GuCultivatorPersistentUtil {

    private GuCultivatorPersistentUtil() {}

    /**
     * 写入“转数/阶段/野外蛊师转数/野外蛊师阶段”到实体 PersistentData。
     * stageNullable: 1..4（初/中/高/巅）；null 则使用默认“中阶(2)”。
     */
    public static void setTierAndStage(LivingEntity target, int tier, Integer stageNullable) {
        var tag = target.getPersistentData();
        int safeTier = Math.max(1, Math.min(5, tier));
        tag.putDouble("野外蛊师转数", safeTier);

        String zhuanStr = switch (safeTier) {
            case 1 -> "一转";
            case 2 -> "二转";
            case 3 -> "三转";
            case 4 -> "四转";
            default -> "五转";
        };
        tag.putString("转数", zhuanStr);

        int stage = stageNullable == null ? 2 : Math.max(1, Math.min(4, stageNullable));
        tag.putDouble("野外蛊师阶段", stage);
        String stageStr = switch (stage) {
            case 1 -> "初阶";
            case 2 -> "中阶";
            case 3 -> "高阶";
            default -> "巅峰";
        };
        tag.putString("阶段", stageStr);
    }
}

