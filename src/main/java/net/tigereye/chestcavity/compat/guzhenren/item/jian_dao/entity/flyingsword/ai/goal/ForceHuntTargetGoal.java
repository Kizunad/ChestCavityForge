package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/**
 * 将飞剑强制设定为追击指定目标的 Goal。
 *
 * <p>注意：本 Goal 不直接驱动移动逻辑，移动由 FlyingSwordEntity 的 tickAI(HUNT) 完成。
 * 本 Goal 的职责是“设定与维持”当前目标与 AI 模式，确保行为层按既有 HUNT 逻辑追击。
 */
public class ForceHuntTargetGoal extends Goal {

  private final FlyingSwordEntity sword;

  @Nullable private LivingEntity target;
  private int timeoutTicks;
  private long startedAt;

  public ForceHuntTargetGoal(FlyingSwordEntity sword) {
    this.sword = sword;
    // 与移动/目标相关即可，避免与其它系统抢夺控制
    this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET, Goal.Flag.LOOK));
  }

  /** 指定强制追击目标与超时（tick，<=0 表示不超时）。 */
  public void setTarget(@Nullable LivingEntity target, int timeoutTicks) {
    this.target = target;
    this.timeoutTicks = Math.max(0, timeoutTicks);
  }

  @Override
  public boolean canUse() {
    if (!(sword.level() instanceof ServerLevel)) return false;
    if (target == null || !target.isAlive()) return false;
    if (sword.getOwner() == null) return false;
    // 初始进入：放宽距离，后续 canContinueToUse 再细控
    return true;
  }

  @Override
  public boolean canContinueToUse() {
    if (target == null || !target.isAlive()) return false;
    if (!(sword.level() instanceof ServerLevel level)) return false;
    long now = level.getGameTime();
    if (timeoutTicks > 0 && now - startedAt >= timeoutTicks) return false;
    // 过远则放弃（两倍有效范围避免频繁切换）
    double maxDist = Math.max(48.0, FlyingSwordAITuning.HUNT_TARGET_VALID_RANGE * 2.0);
    return sword.distanceTo(target) <= maxDist;
  }

  @Override
  public void start() {
    if (!(sword.level() instanceof ServerLevel level)) return;
    this.startedAt = level.getGameTime();
    if (target != null && target.isAlive()) {
      sword.setAIMode(AIMode.HUNT);
      sword.setTargetEntity(target);
    }
  }

  @Override
  public void tick() {
    if (target == null || !target.isAlive()) return;
    // 持续维持 HUNT + 目标绑定，剩余移动/攻击由 HUNT 行为完成
    if (sword.getAIMode() != AIMode.HUNT) {
      sword.setAIMode(AIMode.HUNT);
    }
    if (sword.getTargetEntity() != target) {
      sword.setTargetEntity(target);
    }
  }

  @Override
  public void stop() {
    // 结束时不强制清空，交由上层决定是否保留最后目标
    // 可按需拓展为参数化行为
  }
}

