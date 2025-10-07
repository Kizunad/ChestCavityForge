package net.tigereye.chestcavity.soul.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
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
final class VirtualSoulNavigator {

    private final DummyMob dummy;
    private final GroundPathNavigation nav;

    private @Nullable Vec3 target;
    private double stopDistance = 2.0;
    private double speedModifier = 1.0;
    private int stuckTicks;
    private double lastRemain2 = Double.MAX_VALUE;

    VirtualSoulNavigator(ServerLevel level) {
        this.dummy = new DummyMob(level);
        this.nav = new GroundPathNavigation(this.dummy, level);
        // 支持涉水与门/台阶：
        // - canFloat: 允许在水中寻路；
        // - canPassDoors: 把门视作可通过（配合下方 tryOpenDoorIfNeeded 在相邻时打开木门）；
        // - canOpenDoors: 提示导航器可开门（我们也会手动开木门避免停滞）。
        this.nav.setCanFloat(true);
        this.nav.setCanPassDoors(true);
        this.nav.setCanOpenDoors(true);
        // Keep default step height from backing entity type
    }

    void setGoal(SoulPlayer soul, Vec3 target, double speedModifier, double stopDistance) {
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
        this.target = target;
        // start navigation from the soul's current location
        this.dummy.moveTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), soul.getXRot());
        syncSpeedFromSoul(soul);
        this.nav.moveTo(target.x, target.y, target.z, speedModifier);
    }

    void clearGoal() {
        this.target = null;
        this.nav.stop();
        this.stuckTicks = 0;
        this.lastRemain2 = Double.MAX_VALUE;
    }

    /**
     * Advance navigation one tick and apply the resulting movement to the SoulPlayer.
     */
    void tick(SoulPlayer soul) {
        if (this.target == null) return;
        if (soul.isRemoved()) return;
        Level level = soul.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Keep dummy at the same position of soul at start of tick
        this.dummy.setPos(soul.getX(), soul.getY(), soul.getZ());
        this.dummy.setYRot(soul.getYRot());
        this.dummy.setXRot(soul.getXRot());
        // PathNavigation.canUpdatePath 依赖 onGround()；Dummy 不参与真实物理，强制视为在地面
        this.dummy.setOnGround(true);

        // Maintain speed parity before nav tick
        syncSpeedFromSoul(soul);

        // Path maintenance: if nav went idle but target remains far, reissue
        if (this.nav.isDone()) {
            this.nav.moveTo(target.x, target.y, target.z, this.speedModifier);
        }

        // Tick navigation → sets MoveControl intentions (wanted position + speed)
        this.nav.tick();
        // 若下一节点被木门阻挡，尝试打开门（非铁门）以避免寻路卡住
        tryOpenDoorIfNeeded(serverLevel);

        // Tick MoveControl and perform one travel step to advance dummy position in the Level
        Vec3 before = this.dummy.position();
        this.dummy.getMoveControl().tick();
        // travel uses internal xxa/zza/speed set by MoveControl.tick
        this.dummy.travel(Vec3.ZERO);
        Vec3 after = this.dummy.position();
        Vec3 delta = after.subtract(before);

        // If movement is extremely small, try nudging towards the next path point to avoid stall
        if (delta.lengthSqr() < 1.0e-6) {
            Vec3 hint = hintDirectionTowardsNext();
            if (hint != null) {
                double speed = currentBlocksPerTick();
                delta = hint.scale(speed);
                // Move dummy with collisions so stuck detection works
                this.dummy.move(MoverType.SELF, delta);
            }
        }

        // Apply the computed delta to the SoulPlayer using vanilla collision resolution
        if (delta.lengthSqr() > 0) {
            soul.move(MoverType.SELF, delta);
            // Align facing towards movement
            if (delta.horizontalDistanceSqr() > 1.0e-6) {
                float yaw = (float)(Mth.atan2(delta.z, delta.x) * (180F/Math.PI)) - 90f;
                soul.setYRot(yaw);
                soul.setYHeadRot(yaw);
            }
        }

        // Completion & stuck handling
        double remain2 = after.distanceToSqr(this.target);
        boolean close = remain2 <= (this.stopDistance * this.stopDistance);
        boolean navDone = this.nav.isDone();
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

        if (this.stuckTicks >= 40) { // ~2 seconds
            if (fallbackToSafeGround(serverLevel, soul)) {
                this.stuckTicks = 0;
                this.lastRemain2 = Double.MAX_VALUE;
            }
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
        // Match MoveControl: desiredSpeed = speedModifier * entity.getSpeed()
        // Movement per tick roughly equals desiredSpeed for ground walking in vanilla travel.
        AttributeInstance d = this.dummy.getAttribute(Attributes.MOVEMENT_SPEED);
        double base = d != null ? d.getValue() : 0.1; // MOVEMENT_SPEED final value
        return base * this.speedModifier;
    }

    /**
     * Provide a gentle direction hint towards the next path node when MoveControl produces no motion.
     */
    private @Nullable Vec3 hintDirectionTowardsNext() {
        if (this.nav.getPath() == null || this.nav.getPath().isDone()) return null;
        var p = this.nav.getPath();
        var node = p.getNextNodePos();
        Vec3 here = this.dummy.position();
        Vec3 dir = new Vec3(node.getX() + 0.5 - here.x, node.getY() - here.y, node.getZ() + 0.5 - here.z);
        if (dir.lengthSqr() < 1.0e-6) return null;
        return dir.normalize();
    }

    private boolean fallbackToSafeGround(ServerLevel level, SoulPlayer soul) {
        if (this.target == null) return false;
        int[] OFF = new int[]{0, 1, -1, 2, -2};
        for (int dx : OFF) for (int dz : OFF) {
            if (Math.abs(dx) + Math.abs(dz) > 3) continue;
            int x = Mth.floor(this.target.x) + dx;
            int z = Mth.floor(this.target.z) + dz;
            int startY = Math.min(level.getMaxBuildHeight() - 1, Mth.floor(this.target.y) + 2);
            int minY = Math.max(level.getMinBuildHeight(), startY - 24);
            for (int y = startY; y >= minY; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockPos below = pos.below();
                if (level.isEmptyBlock(pos) && !level.isEmptyBlock(below) && level.getBlockState(below).isSolid()) {
                    double fx = x + 0.5;
                    double fy = y + 0.01;
                    double fz = z + 0.5;
                    // snap dummy & soul, then re-issue navigation
                    this.dummy.setPos(fx, fy, fz);
                    soul.teleportTo(level, fx, fy, fz, soul.getYRot(), soul.getXRot());
                    this.nav.moveTo(this.target.x, this.target.y, this.target.z, this.speedModifier);
                    SoulLog.info("[soul][nav] fallback safe-ground soul={} at ({},{},{})", soul.getSoulId(), fx, fy, fz);
                    return true;
                }
            }
        }
        return false;
    }

    // 检查下一节点是否为关闭的木门，若是且靠近则尝试打开
    private void tryOpenDoorIfNeeded(ServerLevel level) {
        var path = this.nav.getPath();
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
}
