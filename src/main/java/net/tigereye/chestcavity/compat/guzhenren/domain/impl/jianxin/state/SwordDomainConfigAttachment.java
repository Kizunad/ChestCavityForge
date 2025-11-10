package net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

/**
 * 可调的“剑心域”参数，附着在玩家上。
 *
 * <p>用于统一控制： - 半径缩放（radiusScale） - 效果缩放（effectScale）：统一乘到所有域内效果（增益/减益/飞剑加速与抑制等）
 */
public final class SwordDomainConfigAttachment {

  private double radiusScale = 1.0;
  private double effectScale = 1.0;

  public SwordDomainConfigAttachment() {}

  public double getRadiusScale() {
    return radiusScale;
  }

  public double getEffectScale() {
    return effectScale;
  }

  public void setRadiusScale(double value) {
    radiusScale = clampPos(value, 10000.0);
  }

  public void setEffectScale(double value) {
    effectScale = clampPos(value, 10000.0);
  }

  private static double clampPos(double v, double maxInclusive) {
    // (0, max] 开区间下界，使用极小正数近似避免0
    double eps = 1.0e-6;
    if (!(v > 0.0)) {
      return eps;
    }
    return Math.min(v, maxInclusive);
  }

  public static final class Serializer
      implements IAttachmentSerializer<CompoundTag, SwordDomainConfigAttachment> {
    @Override
    public SwordDomainConfigAttachment read(
        IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
      SwordDomainConfigAttachment cfg = new SwordDomainConfigAttachment();
      if (tag != null && !tag.isEmpty()) {
        if (tag.contains("radiusScale")) {
          cfg.setRadiusScale(tag.getDouble("radiusScale"));
        }
        if (tag.contains("effectScale")) {
          cfg.setEffectScale(tag.getDouble("effectScale"));
        }
      }
      return cfg;
    }

    @Override
    public CompoundTag write(
        SwordDomainConfigAttachment attachment, HolderLookup.Provider provider) {
      CompoundTag tag = new CompoundTag();
      tag.putDouble("radiusScale", attachment.getRadiusScale());
      tag.putDouble("effectScale", attachment.getEffectScale());
      return tag;
    }
  }
}
