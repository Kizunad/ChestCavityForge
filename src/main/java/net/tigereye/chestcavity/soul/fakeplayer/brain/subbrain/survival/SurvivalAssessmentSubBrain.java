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

/** Computes the per-tick survival snapshot consumed by other sub-brains. */
public final class SurvivalAssessmentSubBrain extends SubBrain {

    private static final double SCAN_RADIUS = 14.0;
    private static final double MAX_DISTANCE = 18.0;
    private static final String SHARED_SNAPSHOT_KEY = "survival.snapshot";

    private final SurvivalScorecard scorecard;

    public SurvivalAssessmentSubBrain() {
        super("survival.assess");
        this.scorecard = new SurvivalScorecard(new WeightedUtilityScorer(0.35, 0.2, 0.3, -0.15, 0.3), 0.55, 0.35);
        addStep(BrainActionStep.always(this::computeSnapshot));
    }

    @Override
    public boolean shouldTick(SubBrainContext ctx) {
        return ctx.soul().isAlive();
    }

    private void computeSnapshot(SubBrainContext ctx) {
        var soul = ctx.soul();
        LivingEntity threat = pickThreat(ctx);
        double distance = threat == null ? MAX_DISTANCE : Math.min(MAX_DISTANCE, soul.distanceTo(threat));
        double healthRatio = soul.getHealth() / soul.getMaxHealth();
        double absorption = soul.getAbsorptionAmount();
        boolean hasRegen = soul.hasEffect(MobEffects.REGENERATION);
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

    private LivingEntity pickThreat(SubBrainContext ctx) {
        var soul = ctx.soul();
        LivingEntity recent = soul.getLastHurtByMob();
        if (recent != null && recent.isAlive()) {
            return recent;
        }
        Vec3 center = soul.position();
        AABB box = new AABB(center, center).inflate(SCAN_RADIUS);
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
