package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.skills;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianYinGuFx;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 剑引蛊行为占位：封装主动技能与被动周期执行的空壳。
 *
 * <p>后续实现可在此集中处理资源消耗、选取目标与引导逻辑。
 */
public final class JianYinGuSkill {

  private JianYinGuSkill() {}

  /**
   * 主动技能占位：扣除资源并记录最近触发时间。
   *
   * @return 激活成功与否
   */
  public static boolean castGuidance(
      ServerPlayer player, ChestCavityInstance cc, ItemStack organ, long nowTick) {
    if (player == null || cc == null || organ == null || organ.isEmpty()) {
      return false;
    }

    Optional<ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      JianYinGuFx.playResourceFailureFx(player);
      return false;
    }
    ResourceHandle handle = handleOpt.get();

    if (JianYinGuTuning.ACTIVE_ZHENYUAN_COST > 0.0
        && ResourceOps.tryConsumeScaledZhenyuan(handle, JianYinGuTuning.ACTIVE_ZHENYUAN_COST)
            .isEmpty()) {
      JianYinGuFx.playResourceFailureFx(player);
      return false;
    }

    if (JianYinGuTuning.ACTIVE_JINGLI_COST > 0.0
        && ResourceOps.tryAdjustJingli(handle, -JianYinGuTuning.ACTIVE_JINGLI_COST, true)
            .isEmpty()) {
      JianYinGuFx.playResourceFailureFx(player);
      return false;
    }

    OrganStateOps.setLongSync(
        cc,
        organ,
        JianYinGuTuning.STATE_ROOT,
        JianYinGuTuning.KEY_LAST_TRIGGER_TICK,
        nowTick,
        value -> Math.max(0L, value),
        0L);

    JianYinGuFx.playActivationFx(player);
    return true;
  }

  /** 被动占位，每秒收束一次状态，便于后续填充引导逻辑。 */
  public static void tickPassive(
      ServerPlayer player, ChestCavityInstance cc, ItemStack organ, long nowTick) {
    if (player == null || cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    OrganState state = OrganState.of(organ, JianYinGuTuning.STATE_ROOT);
    long lastPulse = state.getLong(JianYinGuTuning.KEY_PASSIVE_PULSE_TICK, 0L);
    if (lastPulse >= nowTick) {
      return;
    }
    OrganStateOps.setLongSync(
        cc,
        organ,
        JianYinGuTuning.STATE_ROOT,
        JianYinGuTuning.KEY_PASSIVE_PULSE_TICK,
        nowTick,
        value -> Math.max(0L, value),
        0L);
  }

  /** 近战命中占位，当前仅刷新被动节奏。 */
  public static void onMeleeHit(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      LivingEntity target,
      float baseDamage) {
    if (player == null || target == null || player.level().isClientSide()) {
      return;
    }
    tickPassive(player, cc, organ, player.level().getGameTime());
  }
}
