package net.tigereye.chestcavity.soul.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.util.SoulLog;

import javax.annotation.Nullable;

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

    private @Nullable Vec3 target;
    private double stopDistance = 2.0;
    private double speedModifier = 1.0;
    private int stuckTicks;
    private double lastRemain2 = Double.MAX_VALUE;
    private static final float MAX_UP_STEP = 1.5f; // allow stepping up ~1.5 blocks
    // Auto step-up cooldown (similar to vanilla auto-jump debounce)
    private int stepAssistCooldown = 0; // ticks remaining
    private final StepPolicy stepPolicy;
    private Mode currentMode = Mode.GROUND;

    private enum Mode { GROUND, SWIMMING, FLYING }

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
        if (this.target == null) {
            // 关闭冲刺状态，避免在无目标时保持冲刺
            if (soul.isSprinting()) soul.setSprinting(false);
            return;
        }
        if (soul.isRemoved()) return;
        Level level = soul.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        // Cooldown reduces once per tick
        if (stepAssistCooldown > 0) stepAssistCooldown--;

        // Keep dummy at the same position of soul at start of tick
        this.dummy.setPos(soul.getX(), soul.getY(), soul.getZ());
        this.dummy.setYRot(soul.getYRot());
        this.dummy.setXRot(soul.getXRot());
        // Keep grounded flag for ground nav; step height is governed by pathing and jump control

        // Maintain speed parity before nav tick
        syncSpeedFromSoul(soul);

        // Mode selection and navigation tick
        Mode mode = selectNavFor(soul);
        this.currentMode = mode;
        // 在地面模式时打开冲刺，其它模式关闭（如游泳/飞行）
        soul.setSprinting(mode == Mode.GROUND);

        Vec3 beforePlayer = soul.position();
        Vec3 delta;

        if (mode == Mode.FLYING) {
            // Direct-flight steering: move straight towards target, ignore collisions gently
            soul.setNoGravity(true);
            this.dummy.setNoGravity(true);
            Vec3 to = new Vec3(target.x - soul.getX(), target.y - soul.getY(), target.z - soul.getZ());
            double dist = Math.sqrt(to.lengthSqr());
            double maxStep = Math.max(0.05, currentBlocksPerTick());
            Vec3 step = dist > maxStep ? to.scale(maxStep / dist) : to;
            delta = step;
            // Apply flight move directly to the SoulPlayer
            soul.move(MoverType.SELF, delta);
        } else {
            // Ensure gravity restored outside flying mode
            if (soul.isNoGravity()) soul.setNoGravity(false);
            if (this.dummy.isNoGravity()) this.dummy.setNoGravity(false);
            // Path maintenance: if nav went idle but target remains far, reissue
            if (this.navCurrent.isDone()) {
                this.navCurrent.moveTo(target.x, target.y, target.z, this.speedModifier);
            }
            // Advance pathfinding one step
            this.navCurrent.tick();
            if (mode == Mode.GROUND) {
                tryOpenDoorIfNeeded(serverLevel);
            }
            // Directly step towards the next node center to avoid relying on internal travel mechanics
            var path = this.navCurrent.getPath();
            Vec3 nextCenter = null;
            if (path != null && !path.isDone()) {
                var nodePos = path.getNextNodePos();
                if (nodePos != null) {
                    nextCenter = new Vec3(nodePos.getX() + 0.5, nodePos.getY(), nodePos.getZ() + 0.5);
                }
            }
            if (nextCenter == null) {
                // fallback: go straight to final target smoothly
                nextCenter = this.target;
            }
            // Line-of-sight smoothing: if no solid block between current eye and target, prefer target directly
            if (this.target != null) {
                Vec3 eye = soul.getEyePosition();
                Vec3 toTarget = new Vec3(this.target.x, this.target.y + 0.5, this.target.z);
                var hit = soul.level().clip(new net.minecraft.world.level.ClipContext(
                        eye, toTarget,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        soul));
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    nextCenter = this.target;
                }
            }
            Vec3 toNode = nextCenter.subtract(soul.position());
            double dist = toNode.length();
            double maxStep = Math.max(0.05, currentBlocksPerTick());
            if (dist > maxStep) {
                delta = toNode.scale(maxStep / dist);
            } else {
                delta = toNode;
            }
            // Apply move；在需要时先触发“跳跃”来跨越前方阻挡，再走步进辅助
            applyMoveWithStepAssist(soul, delta, mode, nextCenter);
        }

        // Align facing towards actual movement
        Vec3 moved = soul.position().subtract(beforePlayer);
        if (moved.horizontalDistanceSqr() > 1.0e-6) {
            float yaw = (float)(Mth.atan2(moved.z, moved.x) * (180F/Math.PI)) - 90f;
            soul.setYRot(yaw);
            soul.setYHeadRot(yaw);
        }

        // Completion & stuck handling
        double remain2 = soul.position().distanceToSqr(this.target);
        boolean close = remain2 <= (this.stopDistance * this.stopDistance);
        boolean navDone = (mode != Mode.FLYING) && this.navCurrent.isDone();
        if (remain2 < this.lastRemain2 - 0.25) { // progressed ~0.5 blocks in distance
            this.lastRemain2 = remain2;
            this.stuckTicks = 0;
        } else {
            this.stuckTicks++;
        }

        if (close || navDone) {
            clearGoal();
            return;
        }

        // Recompute path periodically if we appear stuck
        if (mode != Mode.FLYING && (this.stuckTicks % 40 == 0)) {
            this.navCurrent.recomputePath();
        }
        // No teleport fallback; keep behavior minimal and predictable
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
        // SWIMMING when in water or bubble column
        boolean inWater = soul.isInWaterOrBubble();
        PathNavigation desired = inWater ? this.navWater : this.navGround;
        if (this.navCurrent != desired) {
            this.navCurrent.stop();
            // Move dummy to current soul pos before issuing new moveTo
            this.dummy.moveTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), soul.getXRot());
            this.navCurrent = desired;
            this.navCurrent.moveTo(target.x, target.y, target.z, this.speedModifier);
        }
        return inWater ? Mode.SWIMMING : Mode.GROUND;
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
        if (delta.lengthSqr() <= 0) return;
        Vec3 start = soul.position();
        boolean jumped = false;
        // 若目标明显在斜上方，且脚前确有阻挡，先触发一次原版跳跃以取得更自然的抬升效果
        if (mode == Mode.GROUND && soul.onGround() && shouldJumpToClimb(soul, nextCenter)) {
            soul.forceJump();
            jumped = true;
            // 简短冷却避免连跳
            this.stepAssistCooldown = Math.max(this.stepAssistCooldown, 4);
        }
        // Try plain move first
        soul.move(MoverType.SELF, delta);
        Vec3 after = soul.position();
        double intended2 = delta.lengthSqr();
        double moved2 = after.distanceToSqr(start);
        if (mode != Mode.GROUND) return; // only assist on ground
        // 仅在玩家明确“踩在地上”且下一路径节点确实上台阶时，才进行步进辅助，避免“频繁小跳”。
        if (!soul.onGround()) return;
        // 如果本 tick 已经执行过“跳跃”，则不再做垂直抬升尝试
        if (jumped) return;
        if (!isStepUpNeeded(soul)) return;
        if (moved2 >= Math.min(intended2, 0.25)) return; // moved enough

        // Respect step assist cooldown (AGGRESSIVE 模式可更短甚至为 0)
        if (stepAssistCooldown > 0) {
            // On cooldown: do not attempt step-up; keep current (possibly blocked) result
            return;
        }

        // Revert to start and try step-up attempts
        // Move back by negative of actual displacement to avoid teleport
        Vec3 revert = start.subtract(after);
        if (revert.lengthSqr() > 0) soul.move(MoverType.SELF, revert);

        double[] heights = buildStepHeights(getStepMaxUpBlocks());
        for (double h : heights) {
            // Raise up by h, then apply only horizontal component
            Vec3 up = new Vec3(0.0, h, 0.0);
            soul.move(MoverType.SELF, up);
            // Horizontal only
            Vec3 horiz = new Vec3(delta.x, 0.0, delta.z);
            soul.move(MoverType.SELF, horiz);

            Vec3 pos = soul.position();
            double movedTry2 = pos.distanceToSqr(start);
            if (movedTry2 >= Math.min(intended2, 0.25)) {
                // Success: start cooldown
                int maxCd = getStepAssistCooldownMax(this.stepPolicy);
                if (maxCd < 0) maxCd = 0;
                if (maxCd > 200) maxCd = 200;
                this.stepAssistCooldown = maxCd;
                return; // success
            }
            // Revert and try next height
            Vec3 back = start.subtract(pos);
            if (back.lengthSqr() > 0) soul.move(MoverType.SELF, back);
        }
        // All failed: stay at start (already reverted)
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
        if (yDiff <= 0.25) return false; // 非上坡
        if (yDiff > getStepMaxUpBlocks() + 1e-3) return false; // 超过允许抬升高度

        // 取朝向 nextCenter 的单位水平向量
        Vec3 dir = nextCenter.subtract(soul.position());
        double lenH = Math.hypot(dir.x, dir.z);
        if (lenH < 1.0e-6) return false;
        double nx = dir.x / lenH;
        double nz = dir.z / lenH;
        // 在脚前约 0.6 格处采样阻挡（略大于半格，避免贴脸误判）
        double ahead = 0.6;
        BlockPos front = BlockPos.containing(soul.getX() + nx * ahead, footY, soul.getZ() + nz * ahead);
        var state = level.getBlockState(front);
        boolean blocking = !state.getCollisionShape(level, front).isEmpty();
        if (!blocking) return false;
        // 头顶空间需有余量
        BlockPos above = front.above();
        if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) return false;
        // 若需要 1.5 格抬升，再多查一格空间
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
