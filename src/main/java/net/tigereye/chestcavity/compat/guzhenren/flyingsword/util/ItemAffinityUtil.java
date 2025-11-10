package net.tigereye.chestcavity.compat.guzhenren.flyingsword.util;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordAttributes;

/**
 * 物品属性 → 飞剑属性修正 的纯工具。
 *
 * <p>职责： - 提取物品可用于映射的“度量”（攻击伤害/速度、护甲/韧性、工具Tier与速度、最大耐久、常见附魔等级） - 按传入的系数配置将度量转换为飞剑
 * AttributeModifiers（不内置具体数值）
 *
 * <p>注意：本工具不做任何副作用（不修改物品、不接触世界），便于独立测试。
 */
public final class ItemAffinityUtil {

  private ItemAffinityUtil() {}

  /** 物品抽取的度量集合。 */
  public static final class ItemMetrics {
    public double attackDamage;
    public double attackSpeed;
    public double armor;
    public double armorToughness;
    public int toolTier; // 约定：WOOD=0, STONE=1, IRON=2, DIAMOND=3, NETHERITE=4, GOLD=0
    public double toolSpeed;
    public int maxDamage; // 物品最大耐久（0 表示无耐久）
    public int sharpnessLevel;
    public int unbreakingLevel;
    public int sweepingLevel;
    public int efficiencyLevel;
  }

  /** 转换系数配置（由调用方提供数值）。 */
  public static final class Config {
    public double attackDamageCoef;
    public double attackSpeedAbsCoef;
    public double sharpnessDmgPerLvl;
    public double sharpnessVelPerLvl;
    public double unbreakingLossMultPerLvl; // 每级乘以该系数，<1表示更省耐久
    public double sweepingBase;
    public double sweepingPerLvl;
    public double efficiencyBlockEffPerLvl;
    public double miningSpeedToBlockEffCoef;
    public double maxDamageToMaxDurabilityCoef;
    public double armorToMaxDurabilityCoef;
    public double armorDuraLossMultPerPoint; // 每点护甲乘以该系数
  }

  /** 提取物品度量。若物品为空，返回全0结构。 */
  public static ItemMetrics extract(ServerLevel level, @Nullable ItemStack stack) {
    ItemMetrics m = new ItemMetrics();
    if (level == null || stack == null || stack.isEmpty()) {
      return m;
    }

    // 1) 属性修饰（攻强/攻速/护甲/韧性）
    ItemAttributeModifiers attributeModifiers =
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
    for (var entry : attributeModifiers.modifiers()) {
      if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
        m.attackDamage += entry.modifier().amount();
      } else if (entry.attribute().equals(Attributes.ATTACK_SPEED)) {
        m.attackSpeed += entry.modifier().amount();
      } else if (entry.attribute().equals(Attributes.ARMOR)) {
        m.armor += entry.modifier().amount();
      } else if (entry.attribute().equals(Attributes.ARMOR_TOUGHNESS)) {
        m.armorToughness += entry.modifier().amount();
      }
    }

    // 2) 工具Tier与速度
    if (stack.getItem() instanceof TieredItem tiered) {
      var tier = tiered.getTier();
      if (tier.equals(net.minecraft.world.item.Tiers.WOOD)) m.toolTier = 0;
      else if (tier.equals(net.minecraft.world.item.Tiers.STONE)) m.toolTier = 1;
      else if (tier.equals(net.minecraft.world.item.Tiers.IRON)) m.toolTier = 2;
      else if (tier.equals(net.minecraft.world.item.Tiers.GOLD)) m.toolTier = 0;
      else if (tier.equals(net.minecraft.world.item.Tiers.DIAMOND)) m.toolTier = 3;
      else if (tier.equals(net.minecraft.world.item.Tiers.NETHERITE)) m.toolTier = 4;
      else m.toolTier = 0;
      m.toolSpeed = tier.getSpeed();
    }

    // 3) 最大耐久
    m.maxDamage = stack.getMaxDamage();

    // 4) 常见附魔等级
    HolderLookup<Enchantment> reg = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    Holder<Enchantment> sharpness = reg.getOrThrow(Enchantments.SHARPNESS);
    Holder<Enchantment> unbreaking = reg.getOrThrow(Enchantments.UNBREAKING);
    Holder<Enchantment> sweeping = reg.getOrThrow(Enchantments.SWEEPING_EDGE);
    Holder<Enchantment> efficiency = reg.getOrThrow(Enchantments.EFFICIENCY);
    m.sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(sharpness, stack);
    m.unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(unbreaking, stack);
    m.sweepingLevel = EnchantmentHelper.getItemEnchantmentLevel(sweeping, stack);
    m.efficiencyLevel = EnchantmentHelper.getItemEnchantmentLevel(efficiency, stack);

    return m;
  }

  /** 将度量按配置转换为飞剑属性修正。 */
  public static FlyingSwordAttributes.AttributeModifiers toModifiers(ItemMetrics m, Config cfg) {
    FlyingSwordAttributes.AttributeModifiers mod = FlyingSwordAttributes.AttributeModifiers.empty();
    if (m == null || cfg == null) return mod;

    // 攻击伤害 → 基础伤害
    if (m.attackDamage != 0) {
      mod.damageBase += m.attackDamage * cfg.attackDamageCoef;
    }

    // 攻击速度（负数）→ 最大速度（取绝对值）
    if (m.attackSpeed != 0) {
      mod.speedMax += Math.abs(m.attackSpeed) * cfg.attackSpeedAbsCoef;
    }

    // 工具等级/速度 → 破块能力
    if (m.toolTier > 0) {
      mod.toolTier = Math.max(mod.toolTier, m.toolTier);
    }
    if (m.toolSpeed > 0) {
      mod.blockBreakEff += m.toolSpeed * cfg.miningSpeedToBlockEffCoef;
    }

    // 物品最大耐久 → 飞剑最大耐久
    if (m.maxDamage > 0 && cfg.maxDamageToMaxDurabilityCoef != 0) {
      mod.maxDurability += m.maxDamage * cfg.maxDamageToMaxDurabilityCoef;
    }

    // 护甲/韧性 → 飞剑耐久与耐久损耗倍率
    double armorScore = m.armor + Math.max(0.0, m.armorToughness) * 0.5;
    if (armorScore > 0) {
      mod.maxDurability += armorScore * cfg.armorToMaxDurabilityCoef;
      if (cfg.armorDuraLossMultPerPoint > 0 && cfg.armorDuraLossMultPerPoint != 1.0) {
        mod.duraLossRatioMult *= Math.pow(cfg.armorDuraLossMultPerPoint, armorScore);
      }
    }

    // 锋利 → 伤害与速度²系数
    if (m.sharpnessLevel > 0) {
      mod.damageBase += m.sharpnessLevel * cfg.sharpnessDmgPerLvl;
      mod.velDmgCoef += m.sharpnessLevel * cfg.sharpnessVelPerLvl;
    }

    // 不灭 → 耐久损耗倍率
    if (m.unbreakingLevel > 0 && cfg.unbreakingLossMultPerLvl > 0) {
      mod.duraLossRatioMult *= Math.pow(cfg.unbreakingLossMultPerLvl, m.unbreakingLevel);
    }

    // 横扫 → 启用范围伤害
    if (m.sweepingLevel > 0) {
      mod.enableSweep = true;
      mod.sweepPercent = cfg.sweepingBase + m.sweepingLevel * cfg.sweepingPerLvl;
    }

    // 效率 → 破块效率
    if (m.efficiencyLevel > 0) {
      mod.blockBreakEff += m.efficiencyLevel * cfg.efficiencyBlockEffPerLvl;
    }

    return mod;
  }

  /** 组合流程：提取度量 + 转换修正 + 构建渲染用 initSpec（displayItemId=主手物品）。 */
  public static AffinityResult evaluate(ServerLevel level, @Nullable ItemStack stack, Config cfg) {
    ItemMetrics metrics = extract(level, stack);
    FlyingSwordAttributes.AttributeModifiers modifiers = toModifiers(metrics, cfg);
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.init.FlyingSwordInitSpec spec =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.init.FlyingSwordInitSpec.empty();
    if (stack != null && !stack.isEmpty()) {
      var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
      spec.displayItemId = id; // 渲染采用主手物品ID
    }
    return new AffinityResult(modifiers, spec);
  }

  /** 封装 evaluate 的结果：飞剑属性修正 + 初始化渲染配置。 */
  public static final class AffinityResult {
    public final FlyingSwordAttributes.AttributeModifiers modifiers;
    public final net.tigereye.chestcavity.compat.guzhenren.flyingsword.init.FlyingSwordInitSpec
        initSpec;

    public AffinityResult(
        FlyingSwordAttributes.AttributeModifiers modifiers,
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.init.FlyingSwordInitSpec initSpec) {
      this.modifiers = modifiers;
      this.initSpec = initSpec;
    }
  }
}
