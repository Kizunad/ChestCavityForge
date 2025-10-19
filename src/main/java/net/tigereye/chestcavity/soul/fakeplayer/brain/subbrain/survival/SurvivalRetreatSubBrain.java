package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.survival;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.soul.combat.FleeContext;
import net.tigereye.chestcavity.soul.combat.SoulFleeRegistry;
import net.tigereye.chestcavity.soul.combat.handlers.SimpleFleeHandler;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.BrainDebugEvent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.BrainDebugProbe;
import net.tigereye.chestcavity.soul.fakeplayer.brain.model.SurvivalSnapshot;
import net.tigereye.chestcavity.soul.fakeplayer.brain.policy.SafetyWindowPolicy;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.BrainActionStep;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;

/** Drives retreat behaviour based on the computed survival snapshot. */
public final class SurvivalRetreatSubBrain extends SubBrain {

    private static final String MEMORY_STATE = "state";
    private static final String MEMORY_COOLDOWNS = "cooldowns";
    private static final String SAFE_WINDOW_KEY = "safe_window";
    private static final SafetyWindowPolicy SAFETY_WINDOW = new SafetyWindowPolicy(40, 0.3);
    private static final SimpleFleeHandler FALLBACK_FLEE = new SimpleFleeHandler();

    public SurvivalRetreatSubBrain() {
        super("survival.retreat");
        addStep(BrainActionStep.always(this::tickRetreat));
    }

    @Override
    public boolean shouldTick(SubBrainContext ctx) {
        return ctx.soul().isAlive();
    }

    @Override
    public void onExit(SubBrainContext ctx) {
        SoulNavigationMirror.clearGoal(ctx.soul());
        ctx.memory().put(MEMORY_STATE, null);
    }

    private void tickRetreat(SubBrainContext ctx) {
        SurvivalSnapshot snapshot = ctx.memory().getIfPresent("snapshot");
        if (snapshot == null) {
            return;
        }
        State state = ctx.memory().get(MEMORY_STATE, State::new);
        MultiCooldown cooldowns = ctx.memory().get(MEMORY_COOLDOWNS, SurvivalRetreatSubBrain::createCooldowns);
        MultiCooldown.Entry safeWindow = cooldowns.entry(SAFE_WINDOW_KEY);
        long now = ctx.level().getGameTime();
        if (snapshot.shouldRetreat()) {
            SAFETY_WINDOW.refreshUnsafe(safeWindow, now);
            triggerFlee(ctx, snapshot, state);
        } else if (state.fleeing && SAFETY_WINDOW.isSafeToExit(safeWindow, now, snapshot)) {
            state.fleeing = false;
            SoulNavigationMirror.clearGoal(ctx.soul());
            BrainDebugProbe.emit(BrainDebugEvent.builder("survival")
                    .message("retreat_end")
                    .attribute("score", snapshot.fleeScore())
                    .build());
        }
        ctx.memory().put(MEMORY_STATE, state);
    }

    private void triggerFlee(SubBrainContext ctx, SurvivalSnapshot snapshot, State state) {
        var soul = ctx.soul();
        var threat = snapshot.threat();
        Vec3 anchor = ctx.owner() != null ? ctx.owner().position() : soul.position();
        if (threat != null && threat.isAlive()) {
            FleeContext fleeContext = FleeContext.of(soul, threat, anchor);
            boolean started = SoulFleeRegistry.tryFlee(fleeContext);
            if (!started) {
                FALLBACK_FLEE.tryFlee(fleeContext);
            }
        } else {
            SoulNavigationMirror.setGoal(soul, anchor, 1.2, 2.5);
        }
        if (!state.fleeing) {
            BrainDebugProbe.emit(BrainDebugEvent.builder("survival")
                    .message("retreat_start")
                    .attribute("score", snapshot.fleeScore())
                    .attribute("health", snapshot.healthRatio())
                    .build());
        }
        state.fleeing = true;
    }

    private static MultiCooldown createCooldowns() {
        OrganState state = OrganState.of(new ItemStack(Items.PAPER), "brain.survival");
        return MultiCooldown.builder(state)
                .withLongClamp(value -> Math.max(0L, value), 0L)
                .build();
    }

    private static final class State {
        boolean fleeing;
    }
}
