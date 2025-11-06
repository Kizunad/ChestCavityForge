package net.tigereye.chestcavity.compat.guzhenren.shockfield.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * 粒子/音效 预留服务接口（可选实现）。
 *
 * <p>默认实现为空操作（Noop），你可在客户端或通用模块中调用 {@link #set(ShockfieldFxService)}
 * 注入自定义实现，用于：
 * - 波源创建时渲染提示
 * - 波场每tick渲染波纹/细节
 * - 命中时渲染击中特效
 * - 二级波包创建时渲染轻提示
 * - 熄灭时（自然或移除）渲染收束效果
 */
public final class ShockfieldFx {

  private ShockfieldFx() {}

  private static volatile ShockfieldFxService SERVICE = new Noop();

  public static ShockfieldFxService service() {
    return SERVICE;
  }

  public static void set(ShockfieldFxService impl) {
    SERVICE = impl == null ? new Noop() : impl;
  }

  /** 空操作实现。 */
  private static final class Noop implements ShockfieldFxService {
    @Override
    public void onWaveCreate(ServerLevel level, ShockfieldState state) {}

    @Override
    public void onWaveTick(ServerLevel level, ShockfieldState state) {}

    @Override
    public void onHit(
        ServerLevel level, ShockfieldState state, LivingEntity target, double damageApplied) {}

    @Override
    public void onSubwaveCreate(ServerLevel level, ShockfieldState parent, ShockfieldState sub) {}

    @Override
    public void onExtinguish(
        ServerLevel level, ShockfieldState state, ShockfieldFxService.ExtinguishReason reason) {}
  }
}

