package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.survival;

import java.util.List;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.util.ConstantMobs;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.BrainDebugEvent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.BrainDebugProbe;
import net.tigereye.chestcavity.soul.fakeplayer.brain.model.SurvivalSnapshot;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.ScoreInputs;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.SurvivalScorecard;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.WeightedUtilityScorer;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.BrainActionStep;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;
import net.tigereye.chestcavity.soul.fakeplayer.brain.personality.BrainTuningKeys;

/**
 * 生存评估子大脑（SurvivalAssessmentSubBrain）
 *
 * 作用
 * - 每 tick 计算一次 SurvivalSnapshot（包含 fleeScore、shouldRetreat、healthRatio、威胁等），
 *   提供给其他子大脑（如 SurvivalRetreatSubBrain）作为决策依据。
 * - 评分由 SurvivalScorecard + WeightedUtilityScorer 完成，通过多指标加权得到撤退倾向。
 *
 * 关键输入
 * - 最近伤害来源或扫描半径内最近的敌对实体作为“威胁”；
 * - 自身生命比例、吸收护盾、是否有再生等；
 * - 是否处于“危险”粗判（有威胁/半血/吸收低）。
 */
public final class SurvivalAssessmentSubBrain extends SubBrain {
    private static final String SHARED_SNAPSHOT_KEY = "survival.snapshot";

    private final SurvivalScorecard scorecard;

    public SurvivalAssessmentSubBrain() {
        super("survival.assess");
        // WeightedUtilityScorer 参数含义（按实现顺序）：
        // 距离、生命值、吸收、负向因素、Buff 影响 权重 等。此处仅注释用途，具体见 scorer 实现。
        this.scorecard = new SurvivalScorecard(new WeightedUtilityScorer(0.35, 0.2, 0.3, -0.15, 0.3), 0.55, 0.35);
        addStep(BrainActionStep.always(this::computeSnapshot));
    }

    @Override
    public boolean shouldTick(SubBrainContext ctx) {
        // 仅在 Soul 存活时执行评估
        return ctx.soul().isAlive();
    }

    private void computeSnapshot(SubBrainContext ctx) {
        var soul = ctx.soul();
        double scanRadius = Math.max(6.0, ctx.personality().getDouble(BrainTuningKeys.SURVIVAL_THREAT_SCAN_RADIUS, 18.0));
        double maxDistance = Math.max(scanRadius, ctx.personality().getDouble(BrainTuningKeys.SURVIVAL_RETREAT_MAX_DISTANCE, 18.0));
        LivingEntity threat = pickThreat(ctx, scanRadius);
        double distance = threat == null ? maxDistance : Math.min(maxDistance, soul.distanceTo(threat));
        double healthRatio = soul.getHealth() / soul.getMaxHealth();
        double absorption = soul.getAbsorptionAmount();
        boolean hasRegen = soul.hasEffect(MobEffects.REGENERATION);
        // 危险粗判：有威胁 或 半血以下 或 护盾过低
        boolean inDanger = threat != null || healthRatio < 0.5 || absorption <= 1.0;
        Vec3 ownerPos = ctx.owner() != null ? ctx.owner().position() : soul.position();
        ScoreInputs inputs = ScoreInputs.builder(soul)
                .target(threat, distance)
                .health(healthRatio, absorption)
                .effects(hasRegen)
                .danger(inDanger)
                .owner(ownerPos)
                .build();
        SurvivalSnapshot snapshot = scorecard.evaluate(inputs);
        ctx.sharedMemory().put(SHARED_SNAPSHOT_KEY, snapshot);
        BrainDebugProbe.emit(BrainDebugEvent.builder("survival")
                .message("assess")
                .attribute("score", snapshot.fleeScore())
                .attribute("retreat", snapshot.shouldRetreat())
                .attribute("health", snapshot.healthRatio())
                .build());
    }

    private LivingEntity pickThreat(SubBrainContext ctx, double scanRadius) {
        var soul = ctx.soul();
        // 1) 优先使用“最近伤害来源”
        LivingEntity recent = soul.getLastHurtByMob();
        if (recent != null && recent.isAlive()) {
            return recent;
        }
        Vec3 center = soul.position();
        AABB box = new AABB(center, center).inflate(scanRadius);
        // 2) 否则在扫描半径内寻找最近的敌对
        List<LivingEntity> hostiles = ctx.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive() && e != soul && (e instanceof Enemy || ConstantMobs.isConsideredHostile(e)));
        if (hostiles.isEmpty()) {
            return null;
        }
        return hostiles.stream()
                .min(java.util.Comparator.comparingDouble(soul::distanceToSqr))
                .orElse(null);
    }
}
