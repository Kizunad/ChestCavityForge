package net.tigereye.chestcavity.soul.fakeplayer.brain;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager;
import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.BrainIntent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatIntent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.IntentSnapshot;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.LLMIntent;

/**
 * A lightweight coordinator that behaves like a "brain": selects the appropriate
 * top-level {@link Brain} implementation per soul (e.g., Combat) which in turn
 * orchestrates its registered sub-brains and actions. Modes can be AUTO or forced.
 */
public final class BrainController implements SoulRuntimeHandler {

    private static final BrainController INSTANCE = new BrainController();
    public static BrainController get() { return INSTANCE; }

    private final Map<UUID, BrainMode> modes = new ConcurrentHashMap<>();
    private final Map<UUID, Brain> active = new ConcurrentHashMap<>();
    // 单意图槽（最小实现）：每个 soul 仅保留一个主动意图与其 TTL
    private final Map<UUID, IntentRecord> intents = new ConcurrentHashMap<>();

    private final Brain combat = new net.tigereye.chestcavity.soul.fakeplayer.brain.brains.CombatBrain();
    private final Brain llm = new net.tigereye.chestcavity.soul.fakeplayer.brain.brains.LLMBrain();
    private final Brain idle = new net.tigereye.chestcavity.soul.fakeplayer.brain.brains.IdleBrain();

    private BrainController() {}

    public BrainMode getMode(UUID soulId) {
        return modes.getOrDefault(soulId, BrainMode.AUTO);
    }

    public void setMode(UUID soulId, BrainMode mode) {
        if (mode == null) mode = BrainMode.AUTO;
        modes.put(soulId, mode);
        // brain swap handled on next tick
    }

    /** 设置/覆盖当前 Soul 的主动意图。由命令层调用。 */
    public void pushIntent(UUID soulId, BrainIntent intent) {
        if (soulId == null || intent == null) return;
        intents.put(soulId, new IntentRecord(intent, Math.max(0, intent.ttlTicks())));
    }

    /** 清空 Soul 的意图（回落到 AUTO 推断）。 */
    public void clearIntents(UUID soulId) {
        if (soulId == null) return;
        intents.remove(soulId);
    }

    /** 获取该 soul 的意图快照（本 tick 内冻结）。 */
    public IntentSnapshot getSnapshot(UUID soulId) {
        IntentRecord rec = intents.get(soulId);
        if (rec == null || rec.remaining <= 0) return IntentSnapshot.empty();
        return new IntentSnapshot(rec.intent, rec.remaining);
    }

    @Override
    public void onTickEnd(SoulPlayer player) {
        var level = player.serverLevel();
        var owner = ownerOf(player);
        var mgr = ActionStateManager.of(player);

        // TTL 衰减（每 tick）
        decayIntent(player.getUUID());
        IntentSnapshot snapshot = getSnapshot(player.getUUID());

        BrainMode desired = pickMode(player, owner, snapshot);
        Brain current = active.get(player.getUUID());
        Brain target = selectBrain(desired);
        if (current != target) {
            if (current != null) current.onExit(new BrainContext(level, player, owner, mgr, snapshot));
            active.put(player.getUUID(), target);
            if (target != null) target.onEnter(new BrainContext(level, player, owner, mgr, snapshot));
        }
        if (target != null) target.tick(new BrainContext(level, player, owner, mgr, snapshot));
    }

    private BrainMode pickMode(SoulPlayer soul, ServerPlayer owner, IntentSnapshot snapshot) {
        BrainMode forced = modes.get(soul.getUUID());
        if (forced != null && forced != BrainMode.AUTO) return forced;
        // 若存在显式意图，则据此映射子脑（当前支持 Combat 与 LLM 意图）
        if (snapshot != null && snapshot.isPresent() && snapshot.intent() instanceof CombatIntent) {
            return BrainMode.COMBAT;
        }
        if (snapshot != null && snapshot.isPresent() && snapshot.intent() instanceof LLMIntent) {
            return BrainMode.LLM;
        }
        // AUTO: derive from orders or simple context cues.
        var order = net.tigereye.chestcavity.soul.ai.SoulAIOrders.get(soul.getSoulId());
        if (order == net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.FORCE_FIGHT ||
            order == net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.GUARD) {
            return BrainMode.COMBAT;
        }
        return BrainMode.IDLE;
    }

    private Brain selectBrain(BrainMode mode) {
        return switch (mode) {
            case COMBAT -> combat;
            case LLM -> llm;
            case IDLE -> idle;
            case SURVIVAL, AUTO -> null; // AUTO handled earlier; SURVIVAL to be filled later
        };
    }

    private static ServerPlayer ownerOf(SoulPlayer soul) {
        var server = soul.serverLevel().getServer();
        var opt = soul.getOwnerId();
        if (opt == null || opt.isEmpty()) return null;
        UUID ownerId = opt.get();
        return ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
    }

    /** 简单的意图存储（单槽 + TTL） */
    private static final class IntentRecord {
        final BrainIntent intent;
        int remaining;
        IntentRecord(BrainIntent intent, int ttl) {
            this.intent = intent;
            this.remaining = Math.max(0, ttl);
        }
    }

    private void decayIntent(UUID soulId) {
        if (soulId == null) return;
        IntentRecord rec = intents.get(soulId);
        if (rec == null) return;
        if (rec.remaining <= 0) {
            intents.remove(soulId);
            return;
        }
        rec.remaining -= 1;
        if (rec.remaining <= 0) {
            intents.remove(soulId);
        }
    }
}
