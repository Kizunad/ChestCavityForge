package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.FlyingSwordCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordTuning;

/**
 * 飞剑召唤接口（Flying Sword Spawner）
 *
 * <p>提供给Organ和Skill调用的统一召唤接口。
 * 负责：
 * <ul>
 *   <li>从ItemStack计算释放继承修正</li>
 *   <li>生成飞剑实体</li>
 *   <li>应用初始速度和方向</li>
 * </ul>
 */
public final class FlyingSwordSpawner {

  private FlyingSwordSpawner() {}

  /**
   * 召唤飞剑
   *
   * @param level 服务端世界
   * @param owner 主人
   * @param spawnPos 生成位置
   * @param direction 初始方向（可选，如果为null则使用主人视线方向）
   * @param sourceStack 源剑ItemStack（用于释放继承，可选）
   * @return 生成的飞剑实体，失败返回null
   */
  @Nullable
  public static FlyingSwordEntity spawn(
      ServerLevel level,
      Player owner,
      Vec3 spawnPos,
      @Nullable Vec3 direction,
      @Nullable ItemStack sourceStack) {

    // 计算释放继承修正
    FlyingSwordAttributes.AttributeModifiers modifiers =
        calculateReleaseAffinity(level, sourceStack);

    // 创建飞剑实体
    FlyingSwordEntity sword = FlyingSwordEntity.create(level, owner, spawnPos, modifiers);
    if (sword == null) {
      return null;
    }

    // 设置初始方向和速度
    Vec3 flyDirection = direction;
    if (flyDirection == null) {
      flyDirection = owner.getLookAngle();
    }
    flyDirection = flyDirection.normalize();

    Vec3 initialVelocity = flyDirection.scale(sword.getSwordAttributes().speedBase);
    sword.setDeltaMovement(initialVelocity);

    // 添加到世界
    if (!level.addFreshEntity(sword)) {
      return null;
    }

    return sword;
  }

  /**
   * 简化版召唤方法：在主人面前生成，沿视线方向飞行
   */
  @Nullable
  public static FlyingSwordEntity spawnFromOwner(
      ServerLevel level,
      Player owner,
      @Nullable ItemStack sourceStack) {

    // 在主人前方1.5格生成
    Vec3 lookVec = owner.getLookAngle();
    Vec3 spawnPos = owner.getEyePosition().add(lookVec.scale(1.5));

    return spawn(level, owner, spawnPos, lookVec, sourceStack);
  }

  /**
   * 从ItemStack计算释放继承修正
   *
   * <p>根据武器属性和附魔计算飞剑的属性加成：
   * <ul>
   *   <li>攻击伤害 → 基础伤害</li>
   *   <li>攻击速度 → 最大速度</li>
   *   <li>锋利附魔 → 伤害和速度²系数</li>
   *   <li>工具等级 → 破块等级</li>
   * </ul>
   */
  public static FlyingSwordAttributes.AttributeModifiers calculateReleaseAffinity(
      ServerLevel level, @Nullable ItemStack stack) {

    FlyingSwordAttributes.AttributeModifiers modifiers =
        FlyingSwordAttributes.AttributeModifiers.empty();

    if (stack == null || stack.isEmpty()) {
      return modifiers;
    }

    // 获取附魔 Holder
    HolderLookup<Enchantment> enchantments =
        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

    // 从ItemStack的attribute modifiers获取攻击伤害和攻击速度
    ItemAttributeModifiers attributeModifiers =
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

    double attackDamage = 0;
    double attackSpeed = 0;

    for (var entry : attributeModifiers.modifiers()) {
      if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
        attackDamage += entry.modifier().amount();
      } else if (entry.attribute().equals(Attributes.ATTACK_SPEED)) {
        attackSpeed += entry.modifier().amount();
      }
    }

    // 攻击伤害 → 基础伤害
    if (attackDamage > 0) {
      modifiers.damageBase += attackDamage * FlyingSwordTuning.INHERIT_ATTACK_DAMAGE_COEF;
    }

    // 攻击速度 → 最大速度（攻击速度是负数，转换为正数）
    if (attackSpeed != 0) {
      modifiers.speedMax += Math.abs(attackSpeed) * FlyingSwordTuning.INHERIT_ATTACK_SPEED_COEF;
    }

    // 工具等级
    if (stack.getItem() instanceof TieredItem tiered) {
      // Tier in 1.21+ doesn't have getLevel(), using compareTo to estimate
      var tier = tiered.getTier();
      // Use the tier directly as an integer representation (Tiers enum ordinal-like)
      // Iron = 2, Diamond = 3, Netherite = 4
      int tierLevel = 2; // Default to iron
      if (tier.equals(net.minecraft.world.item.Tiers.WOOD)) tierLevel = 0;
      else if (tier.equals(net.minecraft.world.item.Tiers.STONE)) tierLevel = 1;
      else if (tier.equals(net.minecraft.world.item.Tiers.IRON)) tierLevel = 2;
      else if (tier.equals(net.minecraft.world.item.Tiers.GOLD)) tierLevel = 0; // Gold is weak
      else if (tier.equals(net.minecraft.world.item.Tiers.DIAMOND)) tierLevel = 3;
      else if (tier.equals(net.minecraft.world.item.Tiers.NETHERITE)) tierLevel = 4;

      modifiers.toolTier = Math.max(modifiers.toolTier, tierLevel);
      modifiers.blockBreakEff += tier.getSpeed() * 0.05;
    }

    // 锋利附魔
    Holder<Enchantment> sharpnessHolder = enchantments.getOrThrow(Enchantments.SHARPNESS);
    int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(sharpnessHolder, stack);
    if (sharpnessLevel > 0) {
      modifiers.damageBase += sharpnessLevel * FlyingSwordTuning.INHERIT_SHARPNESS_DMG;
      modifiers.velDmgCoef += sharpnessLevel * FlyingSwordTuning.INHERIT_SHARPNESS_VEL;
    }

    // 耐久不灭 → 降低耐久损耗
    Holder<Enchantment> unbreakingHolder = enchantments.getOrThrow(Enchantments.UNBREAKING);
    int unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(unbreakingHolder, stack);
    if (unbreakingLevel > 0) {
      // 每级降低10%损耗
      modifiers.duraLossRatioMult *= Math.pow(0.9, unbreakingLevel);
    }

    // 横扫之刃 → 启用范围攻击
    Holder<Enchantment> sweepingHolder = enchantments.getOrThrow(Enchantments.SWEEPING_EDGE);
    int sweepingLevel = EnchantmentHelper.getItemEnchantmentLevel(sweepingHolder, stack);
    if (sweepingLevel > 0) {
      modifiers.enableSweep = true;
      modifiers.sweepPercent = 0.3 + sweepingLevel * 0.15; // 30% + 15% per level
    }

    // 效率 → 破块效率（如果有）
    Holder<Enchantment> efficiencyHolder = enchantments.getOrThrow(Enchantments.EFFICIENCY);
    int efficiencyLevel = EnchantmentHelper.getItemEnchantmentLevel(efficiencyHolder, stack);
    if (efficiencyLevel > 0) {
      modifiers.blockBreakEff += efficiencyLevel * 0.5;
    }

    return modifiers;
  }

  /**
   * 检查玩家是否能召唤飞剑
   *
   * @param owner 主人
   * @param baseCost 基础真元消耗
   * @return 是否有足够真元
   */
  public static boolean canSpawn(Player owner, double baseCost) {
    if (owner == null) {
      return false;
    }

    // 检查真元
    return net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.open(owner)
        .map(handle -> {
          var estimate = handle.estimateScaledZhenyuanCost(baseCost);
          if (estimate.isEmpty()) {
            return false;
          }
          double required = estimate.getAsDouble();
          var current = handle.getZhenyuan();
          return current.isPresent() && current.getAsDouble() >= required;
        })
        .orElse(false);
  }

  /**
   * 消耗召唤成本
   *
   * @param owner 主人
   * @param baseCost 基础真元消耗
   * @return 是否成功消耗
   */
  public static boolean consumeSpawnCost(Player owner, double baseCost) {
    if (owner == null) {
      return false;
    }

    return net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.open(owner)
        .map(handle -> handle.consumeScaledZhenyuan(baseCost).isPresent())
        .orElse(false);
  }
}
