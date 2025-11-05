package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.cooldown;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * Phase 4：飞剑冷却统一入口（简化实现）
 *
 * <p>说明：为降低实现复杂度并尽快完成 P4 校验，当前实现使用实体级“兼容镜像”字段存储冷却，
 * 通过本类封装提供统一 API。后续可无缝替换为 owner 附件（MultiCooldown）而无需改动调用方。
 */
public final class FlyingSwordCooldownOps {

  private FlyingSwordCooldownOps() {}

  // ========== 攻击冷却 ==========

  public static int getAttackCooldown(FlyingSwordEntity sword) {
    return sword == null ? 0 : sword.__getAttackCooldownMirror();
  }

  public static boolean setAttackCooldown(FlyingSwordEntity sword, int ticks) {
    if (sword == null) return false;
    sword.__setAttackCooldownMirror(Math.max(0, ticks));
    return true;
    }

  public static boolean isAttackReady(FlyingSwordEntity sword) {
    return getAttackCooldown(sword) <= 0;
  }

  public static int tickDownAttackCooldown(FlyingSwordEntity sword) {
    if (sword == null) return 0;
    int v = Math.max(0, sword.__getAttackCooldownMirror());
    if (v > 0) {
      sword.__setAttackCooldownMirror(v - 1);
      return v - 1;
    }
    return 0;
  }

  // ========== 破块冷却（预留，当前未使用） ==========
  public static int getBlockBreakCooldown(FlyingSwordEntity sword) { return 0; }
  public static boolean setBlockBreakCooldown(FlyingSwordEntity sword, int ticks) { return true; }
  public static boolean isBlockBreakReady(FlyingSwordEntity sword) { return true; }
  public static int tickDownBlockBreakCooldown(FlyingSwordEntity sword) { return 0; }
}
