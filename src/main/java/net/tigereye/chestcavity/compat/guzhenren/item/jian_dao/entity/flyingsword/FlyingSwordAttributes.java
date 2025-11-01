package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword;

import net.minecraft.nbt.CompoundTag;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.FlyingSwordCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordTuning;

/**
 * 飞剑属性数据类
 * 包含所有可配置的属性值
 */
public class FlyingSwordAttributes {
  // ========== 速度属性 ==========
  public double speedBase;
  public double speedMax;
  public double accel;
  public double turnRate;

  // ========== 伤害属性 ==========
  public double damageBase;
  public double velDmgCoef; // 速度²系数

  // ========== 耐久属性 ==========
  public double maxDurability;
  public double duraLossRatio;

  // ========== 维持属性 ==========
  public double upkeepRate;

  // ========== 破块属性 ==========
  public int toolTier;
  public double blockBreakEff;

  // ========== 额外特性 ==========
  public boolean hasGlowLayer;
  public boolean enableSweep;
  public double sweepPercent;

  /**
   * 创建默认属性（从Tuning读取）
   */
  public static FlyingSwordAttributes createDefault() {
    FlyingSwordAttributes attrs = new FlyingSwordAttributes();
    attrs.speedBase = FlyingSwordTuning.SPEED_BASE;
    attrs.speedMax = FlyingSwordTuning.SPEED_MAX;
    attrs.accel = FlyingSwordTuning.ACCEL;
    attrs.turnRate = FlyingSwordTuning.TURN_RATE;

    attrs.damageBase = FlyingSwordTuning.DAMAGE_BASE;
    attrs.velDmgCoef = FlyingSwordTuning.VEL_DMG_COEF;

    attrs.maxDurability = FlyingSwordTuning.MAX_DURABILITY;
    attrs.duraLossRatio = FlyingSwordTuning.DURA_LOSS_RATIO;

    attrs.upkeepRate = FlyingSwordTuning.UPKEEP_BASE_RATE;

    attrs.toolTier = 2; // 默认铁镐等级
    attrs.blockBreakEff = FlyingSwordTuning.BLOCK_BREAK_EFF_BASE;

    attrs.hasGlowLayer = false;
    attrs.enableSweep = false;
    attrs.sweepPercent = 0.0;

    return attrs;
  }

  /**
   * 应用释放继承修正
   */
  public void applyModifiers(AttributeModifiers modifiers) {
    if (modifiers == null) return;

    // 应用修正并限制在合理范围
    this.damageBase += modifiers.damageBase;
    this.damageBase =
        FlyingSwordCalculator.clamp(
            this.damageBase, FlyingSwordTuning.INHERIT_DMG_MIN, FlyingSwordTuning.INHERIT_DMG_MAX);

    this.speedMax += modifiers.speedMax;
    this.speedMax =
        FlyingSwordCalculator.clamp(
            this.speedMax,
            FlyingSwordTuning.INHERIT_SPEED_MIN,
            FlyingSwordTuning.INHERIT_SPEED_MAX);

    this.accel += modifiers.accel;
    this.turnRate += modifiers.turnRate;
    this.velDmgCoef += modifiers.velDmgCoef;
    this.duraLossRatio *= modifiers.duraLossRatioMult;
    this.upkeepRate += modifiers.upkeepRate;

    // 最大耐久增量（受物品耐久/护甲映射）
    if (modifiers.maxDurability > 0) {
      this.maxDurability += modifiers.maxDurability;
      // 下限保护，避免异常为0
      this.maxDurability = Math.max(1.0, this.maxDurability);
    }

    if (modifiers.toolTier > 0) {
      this.toolTier = Math.max(this.toolTier, modifiers.toolTier);
    }
    this.blockBreakEff += modifiers.blockBreakEff;

    if (modifiers.enableSweep) {
      this.enableSweep = true;
      this.sweepPercent = modifiers.sweepPercent;
    }
  }

  /**
   * 保存到NBT
   */
  public void saveToNBT(CompoundTag tag) {
    tag.putDouble("speedBase", speedBase);
    tag.putDouble("speedMax", speedMax);
    tag.putDouble("accel", accel);
    tag.putDouble("turnRate", turnRate);

    tag.putDouble("damageBase", damageBase);
    tag.putDouble("velDmgCoef", velDmgCoef);

    tag.putDouble("maxDurability", maxDurability);
    tag.putDouble("duraLossRatio", duraLossRatio);

    tag.putDouble("upkeepRate", upkeepRate);

    tag.putInt("toolTier", toolTier);
    tag.putDouble("blockBreakEff", blockBreakEff);

    tag.putBoolean("hasGlowLayer", hasGlowLayer);
    tag.putBoolean("enableSweep", enableSweep);
    tag.putDouble("sweepPercent", sweepPercent);
  }

  /**
   * 从NBT加载
   */
  public static FlyingSwordAttributes loadFromNBT(CompoundTag tag) {
    FlyingSwordAttributes attrs = new FlyingSwordAttributes();
    attrs.speedBase = tag.getDouble("speedBase");
    attrs.speedMax = tag.getDouble("speedMax");
    attrs.accel = tag.getDouble("accel");
    attrs.turnRate = tag.getDouble("turnRate");

    attrs.damageBase = tag.getDouble("damageBase");
    attrs.velDmgCoef = tag.getDouble("velDmgCoef");

    attrs.maxDurability = tag.getDouble("maxDurability");
    attrs.duraLossRatio = tag.getDouble("duraLossRatio");

    attrs.upkeepRate = tag.getDouble("upkeepRate");

    attrs.toolTier = tag.getInt("toolTier");
    attrs.blockBreakEff = tag.getDouble("blockBreakEff");

    attrs.hasGlowLayer = tag.getBoolean("hasGlowLayer");
    attrs.enableSweep = tag.getBoolean("enableSweep");
    attrs.sweepPercent = tag.getDouble("sweepPercent");

    return attrs;
  }

  /**
   * 属性修正器（用于释放继承）
   */
  public static class AttributeModifiers {
    public double damageBase = 0;
    public double speedMax = 0;
    public double accel = 0;
    public double turnRate = 0;
    public double velDmgCoef = 0;
    public double duraLossRatioMult = 1.0;
    public double upkeepRate = 0;
    public double maxDurability = 0;
    public int toolTier = 0;
    public double blockBreakEff = 0;
    public boolean enableSweep = false;
    public double sweepPercent = 0;

    public static AttributeModifiers empty() {
      return new AttributeModifiers();
    }
  }
}
