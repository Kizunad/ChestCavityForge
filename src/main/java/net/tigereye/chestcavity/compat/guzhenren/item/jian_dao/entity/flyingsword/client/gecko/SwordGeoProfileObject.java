package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.client.gecko;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

/**
 * 绑定 SwordVisualProfile 的轻量 Geo 渲染对象：
 * - model 固定
 * - texture 可在每层渲染前切换
 * - animation 可为空
 */
public final class SwordGeoProfileObject implements SingletonGeoAnimatable {
  private final ResourceLocation model;
  private final ResourceLocation animation;
  private ResourceLocation texture;
  private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
  private double animationTick;

  public SwordGeoProfileObject(ResourceLocation model, ResourceLocation animation) {
    this.model = model;
    this.animation = animation;
  }

  public void setTexture(ResourceLocation texture) {
    this.texture = texture;
  }

  public ResourceLocation model() { return model; }
  public ResourceLocation texture() { return texture; }
  public ResourceLocation animation() { return animation; }

  public void updateTick(double tick) { this.animationTick = tick; }

  @Override
  public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    // 暂无特定动画控制器；后续可按动画名驱动
  }

  @Override
  public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

  @Override
  public double getTick(Object object) { return animationTick; }
}

