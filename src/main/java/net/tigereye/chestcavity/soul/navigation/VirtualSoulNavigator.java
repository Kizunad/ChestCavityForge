package net.tigereye.chestcavity.soul.navigation;

import java.util.EnumMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/**
 * Virtual pathfinding driver that never spawns a Mob into the world.
 *
 * Design
 * - A DummyMob instance is constructed with the same ServerLevel reference but is NOT added to the world.
 * - A standard GroundPathNavigation is attached to the dummy and used to compute/maintain a Path.
 * - Each tick, we advance the DummyMob by running only the minimal navigation/move-control+travel sequence.
 * - We then apply the computed delta movement to the SoulPlayer via Entity.move, preserving collisions.
 *
 * Key properties
 * - No world entity is created; collision and block checks still work as they query Level state.
 * - Movement speed parity comes from copying the SoulPlayer MOVEMENT_SPEED attribute onto the DummyMob.
 */
final class VirtualSoulNavigator implements ISoulNavigator {

    private final DummyMob dummy;
    private final GroundPathNavigation navGround;
    private final WaterBoundPathNavigation navWater;
    private PathNavigation navCurrent;
    private final EnumMap<Mode, ModePathingStrategy> modeStrategies;

    private @Nullable Vec3 target;
    private double stopDistance = 2.0;
    private double speedModifier = 1.0;
    private int stuckTicks;
    private double lastRemain2 = Double.MAX_VALUE;
    private static final float MAX_UP_STEP = 1.5f; // allow stepping up ~1.5 blocks
    // Auto step-up cooldown (similar to vanilla auto-jump debounce)
    private int stepAssistCooldown = 1; // ticks remaining
    private final StepPolicy stepPolicy;
    private Mode currentMode = Mode.GROUND;

    private enum Mode { GROUND, WATER, LAVA, FLYING }

    enum StepPolicy { DEFAULT, AGGRESSIVE }

    VirtualSoulNavigator(ServerLevel level) {
        this(level, StepPolicy.DEFAULT);
    }

    VirtualSoulNavigator(ServerLevel level, StepPolicy stepPolicy) {
        this.dummy = new DummyMob(level);
        this.navGround = new GroundPathNavigation(this.dummy, level);
        this.navWater = new WaterBoundPathNavigation(this.dummy, level);
        this.stepPolicy = stepPolicy == null ? StepPolicy.DEFAULT : stepPolicy;
        // Ground defaults
        this.navGround.setCanPassDoors(true);
        this.navGround.setCanOpenDoors(true);
        this.navGround.setCanFloat(true);
        // Water defaults
        this.navWater.setCanFloat(true);
        this.navCurrent = this.navGround;
        this.modeStrategies = new EnumMap<>(Mode.class);
        this.modeStrategies.put(Mode.GROUND, new GroundPathingStrategy());
        this.modeStrategies.put(Mode.WATER, new WaterPathingStrategy());
        this.modeStrategies.put(Mode.LAVA, new LavaPathingStrategy());
        // Note: step-height tuning relies on navigation/jump control; direct setter may not be available in this mapping.
    }

    @Override
    public void setGoal(SoulPlayer soul, Vec3 target, double speedModifier, double stopDistance) {
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
        this.target = target;
        // start navigation from the soul's current location
        this.dummy.moveTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), soul.getXRot());
        syncSpeedFromSoul(soul);
        selectNavFor(soul); // ensure nav matches current environment
        this.navCurrent.moveTo(target.x, target.y, target.z, speedModifier);
    }

    @Override
    public void clearGoal() {
        this.target = null;
        this.navCurrent.stop();
        this.stuckTicks = 0;
        this.lastRemain2 = Double.MAX_VALUE;
    }

    /**
     * Advance navigation one tick and apply the resulting movement to the SoulPlayer.
     */
    @Override
    public void tick(SoulPlayer soul) {
        if (!prepareTick(soul)) {
            return;
        }
        Vec3 before = soul.position();
        Vec3 delta = switch (this.currentMode) {
            case FLYING -> tickFlying(soul);
            case GROUND -> tickGround(soul);
            case WATER -> tickWater(soul);
            case LAVA -> tickLava(soul);
        };
        finalizeTick(soul, before, delta);
    }

    private boolean prepareTick(SoulPlayer soul) {
        if (this.target == null) {
            if (soul.isSprinting()) {
                soul.setSprinting(false);
            }
            return false;
        }
        if (soul.isRemoved()) {
            return false;
        }
        if (!(soul.level() instanceof ServerLevel)) {
            return false;
        }
        if (stepAssistCooldown > 0) {
            stepAssistCooldown--;
        }
        this.dummy.setPos(soul.getX(), soul.getY(), soul.getZ());
        this.dummy.setYRot(soul.getYRot());
        this.dummy.setXRot(soul.getXRot());
        syncSpeedFromSoul(soul);
        Mode mode = selectNavFor(soul);
        this.currentMode = mode;
        soul.setSprinting(mode == Mode.GROUND);
        return true;
    }

    private Vec3 tickFlying(SoulPlayer soul) {
        soul.setNoGravity(true);
        this.dummy.setNoGravity(true);
        Vec3 to = new Vec3(target.x - soul.getX(), target.y - soul.getY(), target.z - soul.getZ());
        double dist = Math.sqrt(to.lengthSqr());
        double maxStep = Math.max(0.05, currentBlocksPerTick());
        Vec3 step = dist > maxStep ? to.scale(maxStep / dist) : to;
        soul.move(MoverType.SELF, step);
        return step;
    }

    /**
     * 地面/水中位移推进：
     * - 同步/修正重力状态（确保不在飞行无重力状态）
     * - 维护并推进底层 PathNavigation（必要时重新下达 moveTo）
     * - 地面模式尝试开门以避免路径被木门阻挡
     * - 计算“下一目标中心”并按速度限制生成本 tick 的位移向量
     * - 应用位移；若被阻挡则尝试阶梯辅助（小台阶抬升）
     * 返回值为期望位移向量（用于后续 finalizeTick 计算朝向/进度）
     */
    private Vec3 tickGround(SoulPlayer soul) {
        /*
         * 地面模式：
         * - 确保不处于无重力状态
         * - 若导航报告“已完成”但仍有目标则补一次 moveTo
         * - 推进导航器内部状态（包含寻路推进/节点前进/地面行走控制）
         * - 计算下一步要靠拢的“节点中心”（若路径不可用则回退为最终目标）
         * - 从当前位置指向下一中心的向量与距离
         * - 本 tick 允许的最大位移（基于移动速度与冲刺倍率），下限 0.05 以避免被极小数卡住
         * - 若距离大于可移动步长，则按比例缩放；否则一次到位
         * - 应用移动；如被方块边沿阻挡，启用“阶梯辅助”尝试抬升通过
         */
        return tickWithStrategy(soul, Mode.GROUND);
    }

    private Vec3 tickWater(SoulPlayer soul) {
        return tickWithStrategy(soul, Mode.WATER);
    }

    private Vec3 tickLava(SoulPlayer soul) {
        return tickWithStrategy(soul, Mode.LAVA);
    }

    private Vec3 tickWithStrategy(SoulPlayer soul, Mode mode) {
        ModePathingStrategy strategy = this.modeStrategies.get(mode);
        if (strategy == null) {
            return Vec3.ZERO;
        }
        return strategy.tick(soul);
    }

    private Vec3 tickCommonPathing(SoulPlayer soul, BasePathingStrategy strategy) {
        Mode mode = strategy.mode();
        // 1) 确保不处于无重力：地面/水中模式下应受重力影响
        if (soul.isNoGravity()) {
            soul.setNoGravity(false);
        }
        if (this.dummy.isNoGravity()) {
            this.dummy.setNoGravity(false);
        }
        // 2) 若当前导航报告“已完成”，但仍有目标则补一次 moveTo，避免停在终点附近后不再前进
        if (this.navCurrent.isDone()) {
            this.navCurrent.moveTo(target.x, target.y, target.z, this.speedModifier);
        }
        // 3) 推进导航器内部状态（包含寻路推进/节点前进/游泳或地面行走控制）
        strategy.beforeNavigationTick(soul);
        this.navCurrent.tick();
        strategy.afterNavigationTick(soul);
        // 5) 计算下一步要靠拢的“节点中心”（若路径不可用则回退为最终目标）
        Vec3 nextCenter = computeNextCenter(soul);
        // 6) 从当前位置指向下一中心的向量与距离
        Vec3 toNode = nextCenter.subtract(soul.position());
        double dist = toNode.length();
        // 7) 本 tick 允许的最大位移（基于移动速度与冲刺倍率），下限 0.05 以避免被极小数卡住
        double maxStep = Math.max(0.05, currentBlocksPerTick());
        // 8) 若距离大于可移动步长，则按比例缩放；否则一次到位
        Vec3 delta = dist > maxStep ? toNode.scale(maxStep / dist) : toNode;
        delta = strategy.adjustDelta(soul, delta, nextCenter);
        // 9) 应用移动；如被方块边沿阻挡，启用“阶梯辅助”尝试抬升通过
        applyMoveWithStepAssist(soul, delta, mode, nextCenter);
        strategy.afterMoveApplied(soul, delta, nextCenter);
        return delta;
    }

    private interface ModePathingStrategy {
        Vec3 tick(SoulPlayer soul);
    }

    private abstract class BasePathingStrategy implements ModePathingStrategy {
        private final Mode mode;

        BasePathingStrategy(Mode mode) {
            this.mode = mode;
        }

        Mode mode() {
            return mode;
        }

        @Override
        public final Vec3 tick(SoulPlayer soul) {
            return tickCommonPathing(soul, this);
        }

        protected void beforeNavigationTick(SoulPlayer soul) {
        }

        protected void afterNavigationTick(SoulPlayer soul) {
        }

        protected Vec3 adjustDelta(SoulPlayer soul, Vec3 delta, Vec3 nextCenter) {
            return delta;
        }

        protected void afterMoveApplied(SoulPlayer soul, Vec3 delta, Vec3 nextCenter) {
        }
    }

    private final class GroundPathingStrategy extends BasePathingStrategy {
        GroundPathingStrategy() {
            super(Mode.GROUND);
        }

        @Override
        protected void afterNavigationTick(SoulPlayer soul) {
            if (soul.level() instanceof ServerLevel level) {
                tryOpenDoorIfNeeded(level);
            }
        }
    }

    private final class WaterPathingStrategy extends BasePathingStrategy {
        WaterPathingStrategy() {
            super(Mode.WATER);
        }
    }

    private final class LavaPathingStrategy extends BasePathingStrategy {
        LavaPathingStrategy() {
            super(Mode.LAVA);
        }
    }

    private Vec3 computeNextCenter(SoulPlayer soul) {
        var path = this.navCurrent.getPath();
        Vec3 nextCenter = null;
        if (path != null && !path.isDone()) {
            var nodePos = path.getNextNodePos();
            if (nodePos != null) {
                nextCenter = new Vec3(nodePos.getX() + 0.5, nodePos.getY(), nodePos.getZ() + 0.5);
            }
        }
        if (nextCenter == null) {
            nextCenter = this.target;
        }
        if (this.target != null) {
            Vec3 eye = this.dummy.getEyePosition();
            Vec3 toTarget = new Vec3(this.target.x, this.target.y + 0.5, this.target.z);
            var hit = this.dummy.level().clip(new net.minecraft.world.level.ClipContext(
                    eye, toTarget,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    this.dummy));
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
                nextCenter = this.target;
            }
        }
        return nextCenter;
    }

    private void finalizeTick(SoulPlayer soul, Vec3 before, Vec3 delta) {
        Vec3 moved = soul.position().subtract(before);
        /*
         * 判断条件 moved.horizontalDistanceSqr() > 1.0e-6：先看本次移动在水平面（X/Z）上的距离平方是否几乎为零。
         * 若几乎没动（例如只上下浮动或完全静止），就不更新朝向，避免抖动。
         */
        if (moved.horizontalDistanceSqr() > 1.0e-6) {
            float yaw = (float)(Mth.atan2(moved.z, moved.x) * (180F / Math.PI)) - 90f;
            soul.setYRot(yaw);
            soul.setYHeadRot(yaw);
        }
        updateProgressAndCompletion(soul);
    }
    
    /*
     * 整体作用：持续跟踪到目标的进展；到达则清理，未到达但疑似卡住则定期重算路径，长时间卡住时重置观察基线，防止抖动与无效重算。
     */
    private void updateProgressAndCompletion(SoulPlayer soul) {
        // 当前位置到目标点的距离平方。
        double remain2 = soul.position().distanceToSqr(this.target);
        boolean close = remain2 <= (this.stopDistance * this.stopDistance);
        // 在非飞行模式下，底层导航器是否报告完成（isDone()）。
        boolean navDone = (this.currentMode != Mode.FLYING) && this.navCurrent.isDone();
        if (remain2 < this.lastRemain2 - 0.25) {
            this.lastRemain2 = remain2;
            this.stuckTicks = 0;
        } else {
            this.stuckTicks++;
        }
        if (close || navDone) {
            clearGoal();
            return;
        }
        if (this.currentMode != Mode.FLYING && (this.stuckTicks % 40 == 0)) {
            this.navCurrent.recomputePath();
        }
        if (this.stuckTicks >= 200) {
            this.stuckTicks = 0;
            this.lastRemain2 = remain2;
        }
    }


    private void syncSpeedFromSoul(SoulPlayer soul) {
        AttributeInstance s = soul.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance d = this.dummy.getAttribute(Attributes.MOVEMENT_SPEED);
        if (s == null || d == null) return;
        double v = s.getValue(); // final value including effects and equipment
        if (Math.abs(d.getBaseValue() - v) > 1e-4) {
            d.setBaseValue(v);
        }
    }

    private double currentBlocksPerTick() {
        AttributeInstance d = this.dummy.getAttribute(Attributes.MOVEMENT_SPEED);
        double base = d != null ? d.getValue() : 0.1;
        double v = base * this.speedModifier;
        if (this.currentMode == Mode.GROUND) {
            v *= getRunMultiplier();
        }
        return v;
    }

    private Mode selectNavFor(SoulPlayer soul) {
        if (this.target == null) return Mode.GROUND;
        // FLY when abilities report flying (granted via organ or command)
        if (soul.getAbilities().flying) {
            this.navCurrent.stop();
            return Mode.FLYING;
        }
        // Swimming / lava handling
        boolean inWater = soul.isInWaterOrBubble();
        boolean inLava = soul.isInLava();
        PathNavigation desired;
        Mode mode;
        if (inWater) {
            desired = this.navWater;
            mode = Mode.WATER;
        } else if (inLava) {
            desired = this.navGround;
            mode = Mode.LAVA;
        } else {
            desired = this.navGround;
            mode = Mode.GROUND;
        }
        if (this.navCurrent != desired) {
            this.navCurrent.stop();
            // Move dummy to current soul pos before issuing new moveTo
            this.dummy.moveTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), soul.getXRot());
            this.navCurrent = desired;
            this.navCurrent.moveTo(target.x, target.y, target.z, this.speedModifier);
        }
        return mode;
    }

    // 检查下一节点是否为关闭的木门，若是且靠近则尝试打开
    private void tryOpenDoorIfNeeded(ServerLevel level) {
        var path = this.navCurrent.getPath();
        if (path == null || path.isDone()) return;
        var nodePos = path.getNextNodePos();
        // 检查当前与下一格（下/上半门）
        openDoorAtIfBlocked(level, nodePos);
        openDoorAtIfBlocked(level, nodePos.above());
    }

    private void openDoorAtIfBlocked(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock door)) return;
        if (state.is(net.minecraft.world.level.block.Blocks.IRON_DOOR)) return; // 不处理铁门
        // 只有在接近门时再尝试开门，避免远程频繁改动
        if (this.dummy.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 3.0) return;
        var openProp = net.minecraft.world.level.block.DoorBlock.OPEN;
        if (!state.hasProperty(openProp)) return;
        if (state.getValue(openProp)) return; // 已打开
        // 打开当前半门
        level.setBlock(pos, state.setValue(openProp, true), 10);
        // 同步另一半
        var halfProp = net.minecraft.world.level.block.DoorBlock.HALF;
        if (state.hasProperty(halfProp)) {
            var half = state.getValue(halfProp);
            BlockPos other = (half == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) ? pos.above() : pos.below();
            var otherState = level.getBlockState(other);
            if (otherState.getBlock() instanceof net.minecraft.world.level.block.DoorBlock && otherState.hasProperty(openProp) && !otherState.getValue(openProp)) {
                if (!otherState.is(net.minecraft.world.level.block.Blocks.IRON_DOOR)) {
                    level.setBlock(other, otherState.setValue(openProp, true), 10);
                }
            }
        }
    }

    /**
     * Apply movement to the SoulPlayer. If on ground mode and the move seems blocked,
     * attempt an auto-jump style step-up up to 2 blocks high by splitting the move into
     * vertical then horizontal components.
     */
    private void applyMoveWithStepAssist(SoulPlayer soul, Vec3 delta, Mode mode, @Nullable Vec3 nextCenter) {
        if (delta.lengthSqr() <= 0) {
            return;
        }
        Vec3 start = soul.position();
        boolean jumped = maybeJumpBeforeMove(soul, mode, nextCenter);
        Vec3 after = performPrimaryMove(soul, delta);
        double intended2 = delta.lengthSqr();
        if (!shouldAttemptStepAssist(mode, soul, jumped, start, after, intended2)) {
            return;
        }
        revertToPosition(soul, start);
        attemptStepAssist(soul, start, delta, intended2);
    }

    private boolean maybeJumpBeforeMove(SoulPlayer soul, Mode mode, @Nullable Vec3 nextCenter) {
        if (mode == Mode.GROUND && soul.onGround() && shouldJumpToClimb(soul, nextCenter)) {
            Vec3 dir = nextCenter != null ? nextCenter.subtract(soul.position()).normalize() : Vec3.ZERO;
            soul.forceJump();
            if (dir.lengthSqr() > 0) soul.move(MoverType.SELF, dir.scale(0.3));
            this.stepAssistCooldown = Math.max(this.stepAssistCooldown, 4);
            return true;
        }
        return false;
    }



    private Vec3 performPrimaryMove(SoulPlayer soul, Vec3 delta) {
        soul.move(MoverType.SELF, delta);
        return soul.position();
    }

    private boolean shouldAttemptStepAssist(Mode mode, SoulPlayer soul, boolean jumped, Vec3 start, Vec3 after, double intended2) {
        if (mode != Mode.GROUND) {
            return false;
        }
        if (!soul.onGround()) {
            return false;
        }
        if (jumped) {
            return false;
        }
        if (!isStepUpNeeded(soul)) {
            return false;
        }
        double moved2 = after.distanceToSqr(start);
        if (moved2 >= Math.min(intended2, 0.25)) {
            return false;
        }
        if (stepAssistCooldown > 0) {
            return false;
        }
        return true;
    }

    private void revertToPosition(SoulPlayer soul, Vec3 targetPos) {
        Vec3 revert = targetPos.subtract(soul.position());
        if (revert.lengthSqr() > 0) {
            soul.move(MoverType.SELF, revert);
        }
    }

    private void attemptStepAssist(SoulPlayer soul, Vec3 start, Vec3 delta, double intended2) {
        double[] heights = buildStepHeights(getStepMaxUpBlocks());
        for (double h : heights) {
            Vec3 up = new Vec3(0.0, h, 0.0);
            soul.move(MoverType.SELF, up);
            Vec3 horiz = new Vec3(delta.x, 0.0, delta.z);
            soul.move(MoverType.SELF, horiz);
            Vec3 pos = soul.position();
            double movedTry2 = pos.distanceToSqr(start);
            if (movedTry2 >= Math.min(intended2, 0.25)) {
                int maxCd = getStepAssistCooldownMax(this.stepPolicy);
                if (maxCd < 0) maxCd = 0;
                if (maxCd > 200) maxCd = 200;
                this.stepAssistCooldown = maxCd;
                return;
            }
            revertToPosition(soul, start);
        }
    }

    /**
     * 满足以下条件时建议触发跳跃：
     * - 存在有效的 nextCenter（或最终目标），且其 Y 高于当前脚下（上坡）且不超过允许的最大抬升；
     * - 面前脚前半格~一格范围内存在“可碰撞”的方块（阻挡前进）。
     */

    private boolean shouldJumpToClimb(SoulPlayer soul, @Nullable Vec3 nextCenter) {
        if (nextCenter == null) return false;
        final var level = soul.level();
        if (!(level instanceof ServerLevel)) return false;
        int footY = Mth.floor(soul.getY());
        double yDiff = nextCenter.y - footY;
        if (yDiff <= 0.1) return false; // 容忍更小坡度
        if (yDiff > getStepMaxUpBlocks() + 1e-3) return false;

        Vec3 dir = nextCenter.subtract(soul.position());
        double lenH = Math.hypot(dir.x, dir.z);
        if (lenH < 1.0e-6) return false;
        double nx = dir.x / lenH;
        double nz = dir.z / lenH;

        double ahead = 0.6;
        // 检测脚前上半格是否阻挡
        BlockPos front = BlockPos.containing(soul.getX() + nx * ahead, footY + 1, soul.getZ() + nz * ahead);
        var state = level.getBlockState(front);
        boolean blocking = !state.getCollisionShape(level, front).isEmpty();
        if (!blocking) return false;

        // 确认头顶空间足够
        BlockPos above = front.above();
        if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) return false;
        if (yDiff > 1.0) {
            BlockPos above2 = above.above();
            if (!level.getBlockState(above2).getCollisionShape(level, above2).isEmpty()) return false;
        }
        return true;
    }
    /**
     * 判断是否需要进行“上台阶”式的抬升：
     * - 存在有效的下一路径节点，且该节点的 Y 明显高于当前脚下（>0.5 格）。
     * 这样可以与 Baritone 的路径规划更一致：只有需要抬升时才尝试“跳跃/上台阶”。
     */
    private boolean isStepUpNeeded(SoulPlayer soul) {
        var path = this.navCurrent.getPath();
        if (path == null || path.isDone()) return false;
        var nodePos = path.getNextNodePos();
        if (nodePos == null) return false;
        int currentFootY = Mth.floor(soul.getY());
        double threshold = (this.stepPolicy == StepPolicy.AGGRESSIVE) ? 0.25 : 0.5;
        return (nodePos.getY() - currentFootY) > threshold;
    }

    private static int getStepAssistCooldownMax(StepPolicy policy) {
        String v = System.getProperty("chestcavity.soul.stepAssistCooldown");
        if (v == null) {
            // 默认：普通 8t，AGGRESSIVE 0t（更贴近 autostep）
            return policy == StepPolicy.AGGRESSIVE ? 0 : 8; // ~0.4s at 20 TPS
        }
        try {
            int parsed = Integer.parseInt(v);
            if (parsed < 0) return 0;
            if (parsed > 200) return 200;
            return parsed;
        } catch (NumberFormatException e) {
            return policy == StepPolicy.AGGRESSIVE ? 0 : 8;
        }
    }

    private static double getStepMaxUpBlocks() {
        // 可通过 -Dchestcavity.soul.stepMaxUpBlocks 或 stepMaxUp 配置（单位：方块数，默认 1.5）
        String v = System.getProperty("chestcavity.soul.stepMaxUpBlocks");
        if (v == null) v = System.getProperty("chestcavity.soul.stepMaxUp");
        double def = 1.5;
        if (v == null) return def;
        try {
            double d = Double.parseDouble(v);
            if (d < 1.0) d = 1.0; // 最小 1 格
            if (d > 2.0) d = 2.0; // 最大 2 格（安全）
            return d;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double[] buildStepHeights(double maxUp) {
        // 以 0.5 递增，起点 1.0，直至不超过 maxUp（并上限到 2.0）
        double cap = Math.min(2.0, Math.max(1.0, maxUp));
        java.util.List<Double> hs = new java.util.ArrayList<>();
        for (double h = 1.0; h <= cap + 1e-6; h += 0.5) {
            // 避免浮点误差带来的 1.5000000002
            double rounded = Math.round(h * 2.0) / 2.0;
            if (rounded > cap + 1e-9) break;
            hs.add(rounded);
        }
        if (hs.isEmpty()) hs.add(1.0);
        double[] arr = new double[hs.size()];
        for (int i = 0; i < hs.size(); i++) arr[i] = hs.get(i);
        return arr;
    }

    private static double getRunMultiplier() {
        // 近似原版冲刺倍率（约 1.3），可通过 JVM 配置调整
        String v = System.getProperty("chestcavity.soul.runMultiplier", "1.3");
        try {
            double d = Double.parseDouble(v);
            if (d < 1.0) d = 1.0; // 不低于行走速度
            if (d > 2.0) d = 2.0; // 避免过高
            return d;
        } catch (NumberFormatException e) {
            return 1.3;
        }
    }
}
