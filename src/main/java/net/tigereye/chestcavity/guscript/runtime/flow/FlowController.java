package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.sync.FlowSyncDispatcher;

import java.util.Map;
import java.util.Objects;

/**
 * Runtime controller binding a {@link FlowProgram} to a performer.
 */
public final class FlowController {

    private final ServerPlayer performer;
    private final Map<String, Long> cooldowns = new java.util.HashMap<>();
    private FlowInstance instance;

    public FlowController(ServerPlayer performer) {
        this.performer = Objects.requireNonNull(performer, "performer");
    }

    public ServerPlayer performer() {
        return performer;
    }

    public boolean isRunning() {
        return instance != null && !instance.isFinished();
    }

    public void tick(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (instance != null) {
            instance.tick(serverLevel);
            if (instance.isFinished()) {
                FlowSyncDispatcher.syncStopped(performer, instance);
                instance = null;
            }
        }
    }

    public void start(FlowProgram program, LivingEntity target, long gameTime) {
        if (program == null) {
            return;
        }
        if (isRunning()) {
            ChestCavity.LOGGER.debug("[Flow] Ignoring start for {} because flow {} already running", performer.getGameProfile().getName(), instance.program().id());
            return;
        }
        instance = new FlowInstance(program, performer, target, this, gameTime);
        FlowSyncDispatcher.syncState(performer, instance);
        if (!instance.attemptStart(gameTime)) {
            ChestCavity.LOGGER.debug("[Flow] Flow {} stayed in {} after start trigger", program.id(), instance.state());
        }
    }

    public void handleInput(FlowInput input, long gameTime) {
        if (instance == null || input == null) {
            return;
        }
        instance.handleInput(input, gameTime);
    }

    public void handleStateChanged(FlowInstance flowInstance) {
        FlowSyncDispatcher.syncState(performer, flowInstance);
    }

    public boolean isCooldownReady(String key, long gameTime) {
        Long cooldown = cooldowns.get(key);
        return cooldown == null || cooldown <= gameTime;
    }

    public void setCooldown(String key, long readyTick) {
        cooldowns.put(key, readyTick);
    }
}
