package net.tigereye.chestcavity.soul.fakeplayer.brain.brains;

import java.util.List;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.HierarchicalBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.BrainDebugProbe;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.SurvivalTelemetry;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.ScoreInputs;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.BrainActionStep;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;

/**
 * 生存模式：关注危险评估与撤退，保持与主人相对安全的距离。
 */
public final class SurvivalBrain extends HierarchicalBrain {

    private static final String INPUTS_KEY = "survival.inputs";

    private static final List<SubBrain> PIPELINE = List.of(
            new SenseSubBrain(),
            new RetreatSubBrain()
    );

    public SurvivalBrain() {
        super("survival", BrainMode.SURVIVAL, PIPELINE);
    }

    private static final class SenseSubBrain extends SubBrain {
        private SenseSubBrain() {
            super("survival.sense");
            addStep(BrainActionStep.always(this::captureInputs));
        }

        private void captureInputs(SubBrainContext ctx) {
            var soul = ctx.soul();
            double maxHealth = Math.max(1.0f, soul.getMaxHealth());
            double healthRatio = soul.getHealth() / maxHealth;
            double absorption = soul.getAbsorptionAmount();
            boolean hasRegen = soul.hasEffect(MobEffects.REGENERATION);
            LivingEntity lastAttacker = soul.getLastHurtByMob();
            double distance = lastAttacker == null ? Double.POSITIVE_INFINITY : soul.distanceTo(lastAttacker);
            boolean inDanger = lastAttacker != null || soul.getLastDamageSource() != null || healthRatio < 0.6;

            ScoreInputs.Builder builder = ScoreInputs.builder(soul)
                    .health(healthRatio, absorption)
                    .effects(hasRegen)
                    .danger(inDanger)
                    .target(lastAttacker, distance);
            var owner = ctx.owner();
            if (owner != null) {
                builder.owner(owner.position());
            }
            ctx.memory().put(INPUTS_KEY, builder.build());
        }
    }

    private static final class RetreatSubBrain extends SubBrain {
        private RetreatSubBrain() {
            super("survival.retreat");
            addStep(BrainActionStep.always(this::retreatIfNeeded));
        }

        @Override
        public boolean shouldTick(SubBrainContext ctx) {
            ScoreInputs inputs = ctx.memory().getIfPresent(INPUTS_KEY);
            return inputs != null && (inputs.inDanger() || inputs.healthRatio() < 0.45);
        }

        private void retreatIfNeeded(SubBrainContext ctx) {
            ScoreInputs inputs = ctx.memory().getIfPresent(INPUTS_KEY);
            if (inputs == null) {
                return;
            }
            var soul = ctx.soul();
            Vec3 anchor = inputs.ownerPosition();
            if (anchor == null) {
                anchor = soul.position();
            }
            Vec3 retreatPoint = anchor.add(
                    soul.getRandom().nextGaussian() * 2.5,
                    0.0,
                    soul.getRandom().nextGaussian() * 2.5
            );
            SoulNavigationMirror.setGoal(soul, retreatPoint, 1.0, 2.0);
            BrainDebugProbe.recordSurvival(soul, new SurvivalTelemetry(retreatPoint, inputs.healthRatio(), true,
                    soul.level().getGameTime()));
        }
    }
}
