package net.tigereye.chestcavity.soul.fakeplayer.brain.brains;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.HierarchicalBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.arbitration.Arbitrator;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.BrainDebugProbe;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.ExplorationTelemetry;
import net.tigereye.chestcavity.soul.fakeplayer.brain.model.ExplorationPlan;
import net.tigereye.chestcavity.soul.fakeplayer.brain.model.ExplorationTarget;
import net.tigereye.chestcavity.soul.fakeplayer.brain.policy.BudgetPolicy;
import net.tigereye.chestcavity.soul.fakeplayer.brain.policy.ExplorationBudgetTracker;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.ExplorationOpportunityScorer;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.ScoreInputs;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.BrainActionStep;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;

/**
 * 探索模式：围绕主人巡游与调查，保持活跃移动并输出调试数据。
 */
public final class ExplorationBrain extends HierarchicalBrain {

    private static final String INPUTS_KEY = "exploration.inputs";
    private static final String PLAN_KEY = "exploration.plan";
    private static final String LAST_TARGET_KEY = "exploration.lastTarget";

    private static final List<SubBrain> PIPELINE = List.of(
            new SenseSubBrain(),
            new PlanSubBrain(),
            new ExecuteSubBrain()
    );

    public ExplorationBrain() {
        super("exploration", BrainMode.IDLE, PIPELINE); // TODO: replace with dedicated mode when available
    }

    private static final class SenseSubBrain extends SubBrain {
        private SenseSubBrain() {
            super("exploration.sense");
            addStep(BrainActionStep.always(this::captureInputs));
        }

        private void captureInputs(SubBrainContext ctx) {
            var soul = ctx.soul();
            double maxHealth = Math.max(1.0f, soul.getMaxHealth());
            double healthRatio = soul.getHealth() / maxHealth;
            double absorption = soul.getAbsorptionAmount();
            boolean hasRegen = soul.hasEffect(MobEffects.REGENERATION);
            boolean inDanger = soul.getLastDamageSource() != null;
            var builder = ScoreInputs.builder(soul)
                    .health(healthRatio, absorption)
                    .effects(hasRegen)
                    .danger(inDanger);
            var owner = ctx.owner();
            if (owner != null) {
                builder.owner(owner.position());
            }
            var target = soul.getLastHurtByMob();
            if (target != null) {
                builder.target(target, soul.distanceTo(target));
            }
            ctx.memory().put(INPUTS_KEY, builder.build());
        }
    }

    private static final class PlanSubBrain extends SubBrain {
        private final Arbitrator<ExplorationTarget> arbitrator = new Arbitrator<>(0, 0.0);
        private final ExplorationOpportunityScorer scorer = new ExplorationOpportunityScorer();
        private final ExplorationBudgetTracker budget = new ExplorationBudgetTracker(new BudgetPolicy(4, 2));

        private PlanSubBrain() {
            super("exploration.plan");
            addStep(BrainActionStep.always(this::plan));
        }

        @Override
        public boolean shouldTick(SubBrainContext ctx) {
            return ctx.memory().getIfPresent(INPUTS_KEY) != null;
        }

        @Override
        public void onExit(SubBrainContext ctx) {
            budget.clear(ctx.soul().getUUID());
            ctx.memory().put(PLAN_KEY, null);
        }

        private void plan(SubBrainContext ctx) {
            ScoreInputs inputs = ctx.memory().getIfPresent(INPUTS_KEY);
            if (inputs == null) {
                return;
            }
            long now = ctx.level().getGameTime();
            if (!budget.tryConsumePlan(ctx.soul().getUUID(), now)) {
                return;
            }
            List<ExplorationTarget> candidates = generateCandidates(ctx, inputs);
            if (candidates.isEmpty()) {
                ctx.memory().put(PLAN_KEY, null);
                return;
            }
            var result = arbitrator.decide(candidates, candidate -> scorer.score(inputs, candidate));
            ExplorationTarget target = result.exclusive();
            if (target == null) {
                ctx.memory().put(PLAN_KEY, null);
                return;
            }
            double score = scorer.score(inputs, target);
            ctx.memory().put(PLAN_KEY, new ExplorationPlan(target, score));
        }

        private List<ExplorationTarget> generateCandidates(SubBrainContext ctx, ScoreInputs inputs) {
            List<ExplorationTarget> list = new ArrayList<>();
            Vec3 anchor = inputs.ownerPosition();
            if (anchor == null) {
                anchor = ctx.soul().position();
            }
            long time = ctx.level().getGameTime();
            double baseRadius = inputs.inDanger() ? 4.0 : 7.0;
            for (int i = 0; i < 3; i++) {
                double angle = Mth.DEG_TO_RAD * ((time + i * 40L) % 360);
                double radius = baseRadius + i * 1.5;
                Vec3 pos = anchor.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
                list.add(new ExplorationTarget(pos, 0.55 + 0.15 * i, radius, "orbit" + i));
            }
            if (inputs.primaryTarget() != null) {
                Vec3 targetPos = inputs.primaryTarget().position();
                double distanceHint = Math.max(1.0, ctx.soul().distanceTo(inputs.primaryTarget()));
                list.add(new ExplorationTarget(targetPos, 0.35, distanceHint, "investigate_target"));
            }
            return list;
        }
    }

    private static final class ExecuteSubBrain extends SubBrain {
        private ExecuteSubBrain() {
            super("exploration.execute");
            addStep(BrainActionStep.always(this::executePlan));
        }

        @Override
        public boolean shouldTick(SubBrainContext ctx) {
            return ctx.memory().getIfPresent(PLAN_KEY) != null;
        }

        private void executePlan(SubBrainContext ctx) {
            ExplorationPlan plan = ctx.memory().getIfPresent(PLAN_KEY);
            ScoreInputs inputs = ctx.memory().getIfPresent(INPUTS_KEY);
            if (plan == null || inputs == null) {
                return;
            }
            Vec3 target = plan.target().position();
            Vec3 last = ctx.memory().getIfPresent(LAST_TARGET_KEY);
            if (last == null || last.distanceTo(target) > 0.5) {
                SoulNavigationMirror.setGoal(ctx.soul(), target, 1.1, 2.5);
                ctx.memory().put(LAST_TARGET_KEY, target);
            }
            BrainDebugProbe.recordExploration(ctx.soul(), new ExplorationTelemetry(
                    target,
                    plan.score(),
                    plan.target().rationale(),
                    inputs.healthRatio(),
                    inputs.inDanger(),
                    ctx.level().getGameTime()
            ));
        }
    }
}
