package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.survival;

import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.ConstantMobs;
import net.tigereye.chestcavity.soul.combat.FleeContext;
import net.tigereye.chestcavity.soul.combat.SoulFleeRegistry;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.BrainDebugEvent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.debug.BrainDebugProbe;
import net.tigereye.chestcavity.soul.fakeplayer.brain.model.SurvivalSnapshot;
import net.tigereye.chestcavity.soul.fakeplayer.brain.policy.SafetyWindowPolicy;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.BrainActionStep;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror.GoalPriority;

/**
 * 生存撤退子大脑（SurvivalRetreatSubBrain）
 *
 * 作用
 * - 基于外部计算好的 SurvivalSnapshot（共享内存中提供）决定何时进入/退出“撤退”状态；
 * - 撤退优先尝试调用 SoulFleeRegistry 的逃生策略（例如朝安全点或远离威胁的路径）；
 * - 若逃生策略不可用，则回退为本地计算的“远离威胁向量 + 地形落点”目标，并通过 SoulNavigationMirror 设置高优先级寻路；
 * - 使用 SafetyWindowPolicy 提供的“安全窗口”抖动保护：短时间内的安全/不安全切换不会频繁进出撤退状态。
 *
 * 关键输入
 * - SurvivalSnapshot：来自生存评估模块，包含威胁实体、生命比例、撤退评分等；
 * - SubBrainContext：提供 soul/owner/level/memory 等运行期上下文。
 *
 * 内部状态
 * - State.fleeing：是否处于撤退中；
 * - MultiCooldown safeWindow：安全窗口冷却，配合 SAFETY_WINDOW 判定“可以退出撤退”。
 */
public final class SurvivalRetreatSubBrain extends SubBrain {

    private static final String MEMORY_STATE = "state";
    private static final String MEMORY_COOLDOWNS = "cooldowns";
    private static final String SAFE_WINDOW_KEY = "safe_window";
    private static final String SHARED_SNAPSHOT_KEY = "survival.snapshot";
    private static final SafetyWindowPolicy SAFETY_WINDOW = new SafetyWindowPolicy(40, 0.3);
    public SurvivalRetreatSubBrain() {
        super("survival.retreat");
        addStep(BrainActionStep.always(this::tickRetreat));
    }

    @Override
    public boolean shouldTick(SubBrainContext ctx) {
        // 仅在 Soul 存活时工作
        return ctx.soul().isAlive();
    }

    @Override
    public void onExit(SubBrainContext ctx) {
        // 退出子大脑时清理导航目标与本地状态
        SoulNavigationMirror.clearGoal(ctx.soul());
        ctx.memory().put(MEMORY_STATE, null);
    }

    private void tickRetreat(SubBrainContext ctx) {
        SurvivalSnapshot snapshot = ctx.sharedMemory().getIfPresent(SHARED_SNAPSHOT_KEY);
        if (snapshot == null) {
            // 无快照则不进行撤退逻辑
            return;
        }
        State state = ctx.memory().get(MEMORY_STATE, State::new);
        MultiCooldown cooldowns = ctx.memory().get(MEMORY_COOLDOWNS, SurvivalRetreatSubBrain::createCooldowns);
        MultiCooldown.Entry safeWindow = cooldowns.entry(SAFE_WINDOW_KEY);
        long now = ctx.level().getGameTime();
        if (snapshot.shouldRetreat()) {
            // 标记当前为“非安全”，刷新安全窗口计时，并触发撤退
            SAFETY_WINDOW.refreshUnsafe(safeWindow, now);
            triggerFlee(ctx, snapshot, state);
        } else if (state.fleeing && SAFETY_WINDOW.isSafeToExit(safeWindow, now, snapshot)) {
            // 已在撤退中，但当前满足“可以退出撤退”的安全窗口条件
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
        // 锚点：优先使用 owner 的位置，否则以自身为锚
        Vec3 anchor = ctx.owner() != null ? ctx.owner().position() : soul.position();
        if (threat != null && threat.isAlive()) {
            FleeContext fleeContext = FleeContext.of(soul, threat, anchor);
            boolean started = SoulFleeRegistry.tryFlee(fleeContext);
            if (!started) {
                // 逃生策略未能启动时，使用本地后备目标（基于“远离威胁”的方向并考虑可站立落点）
                Vec3 target = computeFallbackTarget(ctx, snapshot, fleeContext);
                SoulNavigationMirror.setGoal(soul, target, 1.3, 2.5, GoalPriority.HIGH);
            }
        } else {
            // 无威胁时，低优先级靠近锚点（通常是主人）
            SoulNavigationMirror.setGoal(soul, anchor, 1.2, 2.5, GoalPriority.LOW);
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

    private Vec3 computeFallbackTarget(SubBrainContext ctx, SurvivalSnapshot snapshot, FleeContext fleeContext) {
        var soul = ctx.soul();
        var level = ctx.level();
        Vec3 soulPos = soul.position();
        // 收集附近“敌对”实体，用以计算远离向量的合成方向
        List<LivingEntity> hostiles = collectHostiles(ctx, snapshot);
        Vec3 combined = Vec3.ZERO;
        for (LivingEntity hostile : hostiles) {
            Vec3 away = soulPos.subtract(hostile.position());
            double distSq = Math.max(away.lengthSqr(), 1.0);
            double weight = 1.0 / distSq;
            combined = combined.add(away.normalize().scale(weight));
        }
        if (combined.lengthSqr() <= 1.0e-6) {
            // 若无有效方向，随机一个水平向量
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            combined = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        }
        Vec3 dir = combined.normalize();
        // 增加少量抖动（jitter）以避免与障碍物/局部最小值纠缠
        double jitter = (level.random.nextDouble() - 0.5) * (Math.PI / 6.0);
        double cos = Math.cos(jitter);
        double sin = Math.sin(jitter);
        Vec3 rotated = new Vec3(
                dir.x * cos - dir.z * sin,
                0.0,
                dir.x * sin + dir.z * cos);
        if (rotated.lengthSqr() <= 1.0e-6) {
            rotated = dir;
        }
        Vec3 perpendicular = new Vec3(-rotated.z, 0.0, rotated.x).normalize();
        // 期望撤退距离范围（可按需要调参）
        double minDist = 10.0;
        double maxDist = 14.0;
        Vec3 best = null;
        for (int attempt = 0; attempt < 6; attempt++) {
            double forward = Mth.lerp(level.random.nextDouble(), minDist, maxDist);
            double lateral = switch (attempt) {
                case 0 -> 0.0;
                case 1, 2 -> level.random.nextBoolean() ? 1.0 : -1.0;
                default -> (level.random.nextDouble() - 0.5) * 2.0;
            };
            Vec3 candidate = soulPos.add(rotated.scale(forward)).add(perpendicular.scale(lateral));
            BlockPos column = BlockPos.containing(candidate.x, candidate.y, candidate.z);
            // 查找该列的“可站立地面”高度，并验证头顶两格清空
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    column.getX(), column.getZ());
            BlockPos head = new BlockPos(column.getX(), surfaceY + 1, column.getZ());
            BlockPos headTop = head.above();
            boolean clear = level.isEmptyBlock(head) && level.isEmptyBlock(headTop);
            if (clear) {
                double y = surfaceY + 0.1;
                if (y < soulPos.y + 0.5) {
                    y = soulPos.y + 0.5;
                }
                best = new Vec3(candidate.x, y, candidate.z);
                break;
            }
        }
        if (best == null) {
            // 全部尝试失败：退化为“以锚点为参照的远离方向”，并同样对地面高度做一次校准
            Vec3 anchorOffset = soulPos.subtract(fleeContext.anchor());
            if (anchorOffset.lengthSqr() < 1.0e-6) {
                double angle = level.random.nextDouble() * Math.PI * 2.0;
                anchorOffset = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            }
            Vec3 fallback = soulPos.add(anchorOffset.normalize().scale(12.0));
            BlockPos column = BlockPos.containing(fallback.x, fallback.y, fallback.z);
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    column.getX(), column.getZ());
            double y = Math.max(surfaceY + 0.1, soulPos.y + 0.5);
            best = new Vec3(fallback.x, y, fallback.z);
        }
        return best;
    }

    private List<LivingEntity> collectHostiles(SubBrainContext ctx, SurvivalSnapshot snapshot) {
        var soul = ctx.soul();
        var level = ctx.level();
        Vec3 center = soul.position();
        // 搜索半径：18 格，可根据表现再调优
        double radius = 18.0;
        AABB box = new AABB(center, center).inflate(radius);
        // 过滤敌对目标（怪物或在 ConstantMobs 定义为敌对）
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive() && e != soul && (e instanceof Enemy || ConstantMobs.isConsideredHostile(e)));
        List<LivingEntity> hostiles = new ArrayList<>(candidates.size() + 1);
        if (snapshot.threat() != null && snapshot.threat().isAlive()) {
            hostiles.add(snapshot.threat());
        }
        for (LivingEntity entity : candidates) {
            if (!hostiles.contains(entity)) {
                hostiles.add(entity);
            }
        }
        if (hostiles.isEmpty()) {
            // 保底：若没有敌对对象，避免除零/空向量问题，加入自身
            hostiles.add(soul);
        }
        return hostiles;
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
