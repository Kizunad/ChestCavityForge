package net.tigereye.chestcavity.compat.guzhenren.shockfield.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shockfield 的粒子/音效服务接口。实现方可在客户端或通用侧注入到 {@link ShockfieldFx#set(ShockfieldFxService)}。
 *
 * <p>注意：本接口仅为“预留钩子”，默认不做任何效果。
 */
public interface ShockfieldFxService {

  enum ExtinguishReason {
    DAMPED_OUT, // 振幅低于阈值
    LIFETIME_ENDED, // 达到寿命上限
    OWNER_REMOVED // 归属者清理（卸下/死亡/切换等）
  }

  /** 波源创建。 */
  void onWaveCreate(ServerLevel level, ShockfieldState state);

  /** 每tick更新（已完成半径/振幅/周期演化）。 */
  void onWaveTick(ServerLevel level, ShockfieldState state);

  /** 击中目标（最终伤害 after 软上限）。 */
  void onHit(ServerLevel level, ShockfieldState state, LivingEntity target, double damageApplied);

  /** 二级波包创建（例如主圈触剑触发）。 */
  void onSubwaveCreate(ServerLevel level, ShockfieldState parent, ShockfieldState sub);

  /** 波场熄灭（自然/寿命/被移除）。 */
  void onExtinguish(ServerLevel level, ShockfieldState state, ExtinguishReason reason);
}

