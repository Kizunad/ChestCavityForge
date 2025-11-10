package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.gecko;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.override.SwordModelOverrideDef;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

/** 轻量的 Geckolib 可渲染对象（非实体），用于在实体渲染器中嵌画 Blockbench 模型。 */
public final class SwordModelObject implements SingletonGeoAnimatable {
  private final SwordModelOverrideDef def;
  private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
  private double animationTick;

  public SwordModelObject(SwordModelOverrideDef def) {
    this.def = def;
  }

  public SwordModelOverrideDef def() {
    return def;
  }

  public void updateTick(double tick) {
    this.animationTick = tick;
  }

  @Override
  public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    // 暂无动画控制器（静态展示或由未来动画JSON驱动）
  }

  @Override
  public AnimatableInstanceCache getAnimatableInstanceCache() {
    return cache;
  }

  @Override
  public double getTick(Object object) {
    return animationTick;
  }
}
