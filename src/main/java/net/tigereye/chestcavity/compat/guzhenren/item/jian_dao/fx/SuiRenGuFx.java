package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 碎刃蛊特效与提示接口。
 *
 * <p>提供视觉/音效入口，便于后续扩展与调参。
 */
public final class SuiRenGuFx {

  private SuiRenGuFx() {}

  /**
   * 播放冷却提示（客户端 toast）。
   *
   * @param player 玩家
   * @param abilityId 技能 ID
   * @param readyAtTick 冷却结束时间
   * @param nowTick 当前时间
   */
  public static void scheduleCooldownToast(
      ServerPlayer player, ResourceLocation abilityId, long readyAtTick, long nowTick) {
    ActiveSkillRegistry.scheduleReadyToast(player, abilityId, readyAtTick, nowTick);
  }

  /**
   * 播放飞剑碎裂特效（粒子与音效）。
   *
   * <p>建议：在飞剑位置生成"刃落如霰"粒子效果。
   *
   * @param player 玩家
   * @param sword 被牺牲的飞剑
   */
  public static void playShardBurst(ServerPlayer player, FlyingSwordEntity sword) {
    // TODO: 实现粒子效果
    // 参考 FlyingSwordFX.spawnRecallEffect
    // 可以使用 ParticleTypes.SWEEP_ATTACK 或自定义粒子
    // 音效可使用 CUSTOM_FLYINGSWORD_SHATTER（需在 CCSoundEvents 中添加）
  }

  /**
   * 播放技能激活完成特效。
   *
   * @param player 玩家
   * @param swordCount 牺牲的飞剑数量
   * @param totalDelta 获得的总道痕增幅
   */
  public static void playActivationComplete(ServerPlayer player, int swordCount, int totalDelta) {
    // TODO: 实现激活成功特效
    // 建议：玩家位置的光效/粒子，表示道痕增幅生效
  }

  /**
   * 播放 buff 结束特效。
   *
   * @param player 玩家
   */
  public static void playBuffExpired(ServerPlayer player) {
    // TODO: 实现 buff 结束提示
    // 建议：淡出粒子效果或音效提示
  }
}
