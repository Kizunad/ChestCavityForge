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

    private enum Mode { GROUND, SWIMMING, FLYING }

    VirtualSoulNavigator(ServerLevel level) {
        this.dummy = new DummyMob(level);
        this.navGround = new GroundPathNavigation(this.dummy, level);
        this.navWater = new WaterBoundPathNavigation(this.dummy, level);
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
        if (this.target == null) return;
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
            // Apply move with step assist when grounded
            applyMoveWithStepAssist(soul, delta, mode);
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
        return base * this.speedModifier;
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
    private void applyMoveWithStepAssist(SoulPlayer soul, Vec3 delta, Mode mode) {
        if (delta.lengthSqr() <= 0) return;
        Vec3 start = soul.position();
        // Try plain move first
        soul.move(MoverType.SELF, delta);
        Vec3 after = soul.position();
        double intended2 = delta.lengthSqr();
        double moved2 = after.distanceToSqr(start);
        if (mode != Mode.GROUND) return; // only assist on ground
        if (moved2 >= Math.min(intended2, 0.25)) return; // moved enough

        // Respect step assist cooldown
        if (stepAssistCooldown > 0) {
            // On cooldown: do not attempt step-up; keep current (possibly blocked) result
            return;
        }

        // Revert to start and try step-up attempts
        // Move back by negative of actual displacement to avoid teleport
        Vec3 revert = start.subtract(after);
        if (revert.lengthSqr() > 0) soul.move(MoverType.SELF, revert);

        double[] heights = new double[]{1.0, 1.5, 2.0};
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
                int maxCd = getStepAssistCooldownMax();
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

    private static int getStepAssistCooldownMax() {
        String v = System.getProperty("chestcavity.soul.stepAssistCooldown");
        if (v == null) return 8; // ~0.4s at 20 TPS
        try {
            int parsed = Integer.parseInt(v);
            if (parsed < 0) return 0;
            if (parsed > 200) return 200;
            return parsed;
        } catch (NumberFormatException e) {
            return 8;
        }
    }
}
