package net.tigereye.chestcavity.soul.navigation;

import java.util.EnumMap;
import javax.annotation.Nullable;
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
 * <p>Design - A DummyMob instance is constructed with the same ServerLevel reference but is NOT
 * added to the world. - A standard GroundPathNavigation is attached to the dummy and used to
 * compute/maintain a Path. - Each tick, we advance the DummyMob by running only the minimal
 * navigation/move-control+travel sequence. - We then apply the computed delta movement to the
 * SoulPlayer via Entity.move, preserving collisions.
 *
 * <p>Key properties - No world entity is created; collision and block checks still work as they
 * query Level state. - Movement speed parity comes from copying the SoulPlayer MOVEMENT_SPEED
 * attribute onto the DummyMob.
 */
final class VirtualSoulNavigator implements ISoulNavigator {

  // 调试开关：通过 JVM 参数控制（-Dchestcavity.debugSoul=true 或 -Dchestcavity.debugSoul.nav=true）
  private static final boolean DEBUG_NAV = false;

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
  private Mode currentMode = Mode.GROUND;

  private enum Mode {
    GROUND,
    WATER,
    LAVA,
    FLYING
  }

  enum StepPolicy {
    DEFAULT,
    AGGRESSIVE
  }

  VirtualSoulNavigator(ServerLevel level) {
    this(level, StepPolicy.DEFAULT);
  }

  VirtualSoulNavigator(ServerLevel level, StepPolicy stepPolicy) {
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
    this.modeStrategies = new EnumMap<>(Mode.class);
    this.modeStrategies.put(Mode.GROUND, new GroundPathingStrategy());
    this.modeStrategies.put(Mode.WATER, new WaterPathingStrategy());
    this.modeStrategies.put(Mode.LAVA, new LavaPathingStrategy());
    // Note: step-height tuning relies on navigation/jump control; direct setter may not be
    // available in this mapping.
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

  /** Advance navigation one tick and apply the resulting movement to the SoulPlayer. */
  @Override
  public void tick(SoulPlayer soul) {
    if (!prepareTick(soul)) {
      return;
    }
    Vec3 before = soul.position();
    Vec3 delta =
        switch (this.currentMode) {
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
    return Vec3.ZERO;
  }

  /** 地面模式：依据导航器输出的下一节点，按跑步速度做水平推进，必要时触发一次跳跃。 返回值为本 tick 期望位移向量（用于朝向与进度计算）。 */
  private Vec3 tickGround(SoulPlayer soul) {
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
    // 6) 从当前位置指向下一中心的向量
    Vec3 toNode = nextCenter.subtract(soul.position());
    // 7) 本 tick 允许的最大位移（基于移动速度与冲刺倍率），下限 0.05 以避免被极小数卡住
    double maxStep = Math.max(0.05, currentBlocksPerTick());
    // 8) 交给策略决定实际移动向量
    Vec3 delta = strategy.computeDelta(soul, toNode, maxStep, nextCenter);
    if (delta.lengthSqr() > 0.0) {
      soul.move(MoverType.SELF, delta);
    }
    strategy.afterMove(soul, delta, nextCenter);
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

    protected void beforeNavigationTick(SoulPlayer soul) {}

    protected void afterNavigationTick(SoulPlayer soul) {}

    protected Vec3 computeDelta(SoulPlayer soul, Vec3 toNode, double maxStep, Vec3 nextCenter) {
      double distance = toNode.length();
      if (distance <= 1.0e-6) {
        return Vec3.ZERO;
      }
      double ratio = Math.min(1.0, maxStep / distance);
      return toNode.scale(ratio);
    }

    protected void afterMove(SoulPlayer soul, Vec3 delta, Vec3 nextCenter) {}
  }

  private final class GroundPathingStrategy extends BasePathingStrategy {
    GroundPathingStrategy() {
      super(Mode.GROUND);
    }

    @Override
    protected Vec3 computeDelta(SoulPlayer soul, Vec3 toNode, double maxStep, Vec3 nextCenter) {
      double rise = toNode.y;
      if (soul.onGround() && rise > 0.6) {
        soul.forceJump();
      }
      Vec3 horizontal = new Vec3(toNode.x, 0.0, toNode.z);
      double horizontalLenSq = horizontal.lengthSqr();
      if (horizontalLenSq <= 1.0e-6) {
        Vec3 fallback = new Vec3(target.x - soul.getX(), 0.0, target.z - soul.getZ());
        double fallbackLenSq = fallback.lengthSqr();
        if (fallbackLenSq > 1.0e-6) {
          double fallbackLen = Math.sqrt(fallbackLenSq);
          double step = Math.min(fallbackLen, maxStep);
          horizontal = fallback.scale(step / fallbackLen);
        } else {
          horizontal = Vec3.ZERO;
        }
      } else {
        double horizontalLen = Math.sqrt(horizontalLenSq);
        double step = Math.min(horizontalLen, maxStep);
        horizontal = horizontal.scale(step / horizontalLen);
      }
      double descent = Math.min(0.0, rise);
      return horizontal.add(0.0, descent, 0.0);
    }
  }

  private final class WaterPathingStrategy extends BasePathingStrategy {
    WaterPathingStrategy() {
      super(Mode.WATER);
    }

    @Override
    protected Vec3 computeDelta(SoulPlayer soul, Vec3 toNode, double maxStep, Vec3 nextCenter) {
      return Vec3.ZERO;
    }
  }

  private final class LavaPathingStrategy extends BasePathingStrategy {
    LavaPathingStrategy() {
      super(Mode.LAVA);
    }

    @Override
    protected Vec3 computeDelta(SoulPlayer soul, Vec3 toNode, double maxStep, Vec3 nextCenter) {
      return Vec3.ZERO;
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
      var hit =
          this.dummy
              .level()
              .clip(
                  new net.minecraft.world.level.ClipContext(
                      eye,
                      toTarget,
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
      float yaw = (float) (Mth.atan2(moved.z, moved.x) * (180F / Math.PI)) - 90f;
      soul.setYRot(yaw);
      soul.setYHeadRot(yaw);
    }
    // 调试：当启用 -Dchestcavity.debugSoul=true 时，打印关键进度数据
    if (DEBUG_NAV) {
      double remain2 = (this.target == null) ? -1.0 : soul.position().distanceToSqr(this.target);
      boolean close = (this.target != null) && remain2 <= (this.stopDistance * this.stopDistance);
      boolean navDone = (this.currentMode != Mode.FLYING) && this.navCurrent.isDone();
      var path = this.navCurrent.getPath();
      boolean hasPath = path != null && !path.isDone();
      net.tigereye.chestcavity.ChestCavity.LOGGER.info(
          "[SoulNav] finalize mode={} remain2={} close={} navDone={} stuckTicks={} hasPath={}",
          this.currentMode,
          String.format("%.3f", remain2),
          close,
          navDone,
          this.stuckTicks,
          hasPath);
      // 若几乎未移动，标记一次“可能被阻挡/尝试阶梯辅助”
      if (moved.lengthSqr() < 1.0e-6) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info("[SoulNav] blocked_or_no_delta");
      }
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
