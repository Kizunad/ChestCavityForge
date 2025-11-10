package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 目标丢失事件上下文
 *
 * <p>Phase 3: 当飞剑失去当前目标时触发。
 *
 * <p>用途：
 *
 * <ul>
 *   <li>记录目标丢失原因（统计、调试）
 *   <li>触发搜索新目标逻辑
 *   <li>清理目标相关状态（buff、debuff、追踪标记）
 *   <li>自动切换AI模式（如HUNT→ORBIT）
 * </ul>
 */
public class TargetLostContext {
  public final FlyingSwordEntity sword;

  /** 上一个目标（可能为null，如首次tick） */
  @Nullable public final LivingEntity lastTarget;

  /** 丢失原因 */
  public final LostReason reason;

  public TargetLostContext(
      FlyingSwordEntity sword, @Nullable LivingEntity lastTarget, LostReason reason) {
    this.sword = sword;
    this.lastTarget = lastTarget;
    this.reason = reason;
  }

  /** 目标丢失原因枚举 */
  public enum LostReason {
    /** 目标已死亡 */
    DEAD,
    /** 目标超出追踪范围 */
    OUT_OF_RANGE,
    /** 目标无效（如变为友军、进入无敌状态） */
    INVALID,
    /** 玩家手动取消目标 */
    MANUAL_CANCEL,
    /** AI模式切换导致目标清除 */
    MODE_CHANGE,
    /** 其他未知原因 */
    OTHER
  }
}
