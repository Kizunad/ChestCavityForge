package net.tigereye.chestcavity.compat.guzhenren.flyingsword;

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
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.FlyingSwordCalculator;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning;

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
   * 召唤飞剑（默认类型）
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
      net.minecraft.world.entity.LivingEntity owner,
      Vec3 spawnPos,
      @Nullable Vec3 direction,
      @Nullable ItemStack sourceStack) {
    return spawn(level, owner, spawnPos, direction, sourceStack, FlyingSwordType.DEFAULT);
  }

  /**
   * 召唤飞剑（指定类型）
   *
   * @param level 服务端世界
   * @param owner 主人
   * @param spawnPos 生成位置
   * @param direction 初始方向（可选，如果为null则使用主人视线方向）
   * @param sourceStack 源剑ItemStack（用于释放继承，可选）
   * @param swordType 飞剑类型
   * @return 生成的飞剑实体，失败返回null
   */
  @Nullable
  public static FlyingSwordEntity spawn(
      ServerLevel level,
      net.minecraft.world.entity.LivingEntity owner,
      Vec3 spawnPos,
      @Nullable Vec3 direction,
      @Nullable ItemStack sourceStack,
      FlyingSwordType swordType) {
    // 计算释放继承修正（默认基于通用规则）
    FlyingSwordAttributes.AttributeModifiers modifiers =
        calculateReleaseAffinity(level, sourceStack);

    // 根据类型选择对应的EntityType
    net.minecraft.world.entity.EntityType<FlyingSwordEntity> entityType = getEntityTypeForSwordType(swordType);

    // 创建飞剑实体
    FlyingSwordEntity sword = FlyingSwordEntity.create(level, owner, spawnPos, modifiers, entityType);
    if (sword == null) {
      return null;
    }

    // 初始化定制：从源物品读取并应用（属性覆盖/模型/音效档等）
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.init
        .FlyingSwordInit.applyTo(
            sword,
            net.tigereye.chestcavity.compat.guzhenren.flyingsword.init
                .FlyingSwordInit.fromItemStack(sourceStack));

    // 属性可能被定制覆盖，需同步生命-耐久
    sword.syncHealthWithDurability();

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

    // 音效：生成
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops.SoundOps
        .playSpawn(sword);

    // 召唤时的剑阵粒子（玩家脚下三环阵）
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.FlyingSwordFX
        .spawnSummonArrayAt(level, owner.position(), swordType);

    // 触发onSpawn事件钩子
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
        .SpawnContext spawnCtx =
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
            .context.SpawnContext(sword, level, owner, spawnPos, sourceStack);
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
        .FlyingSwordEventRegistry.fireSpawn(spawnCtx);

    // 检查是否被钩子取消
    if (spawnCtx.cancelled) {
      sword.discard();
      return null;
    }

    return sword;
  }

  /**
   * 简化版召唤方法：在主人面前生成，沿视线方向飞行（默认类型）
   */
  @Nullable
  public static FlyingSwordEntity spawnFromOwner(
      ServerLevel level,
      Player owner,
      @Nullable ItemStack sourceStack) {
    return spawnFromOwner(level, owner, sourceStack, FlyingSwordType.DEFAULT);
  }

  /**
   * 简化版召唤方法：在主人面前生成，沿视线方向飞行（指定类型）
   */
  @Nullable
  public static FlyingSwordEntity spawnFromOwner(
      ServerLevel level,
      Player owner,
      @Nullable ItemStack sourceStack,
      FlyingSwordType swordType) {

    // 在主人前方1.5格生成
    Vec3 lookVec = owner.getLookAngle();
    Vec3 spawnPos = owner.getEyePosition().add(lookVec.scale(1.5));

    return spawn(level, owner, spawnPos, lookVec, sourceStack, swordType);
  }

  /**
   * 召唤飞剑（指定类型 + 指定修正）。
   * 用于外部已计算好 AttributeModifiers 的场景（例如 Combo 自定义数值）。
   */
  @Nullable
  public static FlyingSwordEntity spawnWithModifiers(
      ServerLevel level,
      Player owner,
      Vec3 spawnPos,
      @Nullable Vec3 direction,
      @Nullable ItemStack sourceStack,
      FlyingSwordType swordType,
      FlyingSwordAttributes.AttributeModifiers modifiers) {
    return spawnWithModifiersAndSpec(
        level,
        owner,
        spawnPos,
        direction,
        sourceStack,
        swordType,
        modifiers,
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.init
            .FlyingSwordInit.fromItemStack(sourceStack));
  }

  /** 简化：基于主人视线与位置的 spawnWithModifiers 封装。 */
  @Nullable
  public static FlyingSwordEntity spawnFromOwnerWithModifiers(
      ServerLevel level,
      Player owner,
      @Nullable ItemStack sourceStack,
      FlyingSwordType swordType,
      FlyingSwordAttributes.AttributeModifiers modifiers) {
    Vec3 lookVec = owner.getLookAngle();
    Vec3 spawnPos = owner.getEyePosition().add(lookVec.scale(1.5));
    return spawnWithModifiers(level, owner, spawnPos, lookVec, sourceStack, swordType, modifiers);
  }

  /**
   * 召唤飞剑（指定类型 + 指定修正 + 指定初始化Spec）。
   */
  @Nullable
  public static FlyingSwordEntity spawnWithModifiersAndSpec(
      ServerLevel level,
      Player owner,
      Vec3 spawnPos,
      @Nullable Vec3 direction,
      @Nullable ItemStack sourceStack,
      FlyingSwordType swordType,
      FlyingSwordAttributes.AttributeModifiers modifiers,
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.init
          .FlyingSwordInitSpec initSpec) {

    // 根据类型选择对应的EntityType
    net.minecraft.world.entity.EntityType<FlyingSwordEntity> entityType = getEntityTypeForSwordType(swordType);

    // 创建飞剑实体
    FlyingSwordEntity sword = FlyingSwordEntity.create(level, owner, spawnPos, modifiers, entityType);
    if (sword == null) {
      return null;
    }

    // 应用初始化Spec（可设置显示用物品ID/模型/音效）
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.init
        .FlyingSwordInit.applyTo(sword, initSpec);

    // 若提供了源物品，优先使用其完整拷贝作为渲染ItemStack，以保留附魔发光/自定义组件
    if (sourceStack != null && !sourceStack.isEmpty()) {
      // 为源物品打上稳定 UUID（如无），copy 将继承该 UUID。
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.util
          .ItemIdentityUtil.ensureItemUUID(sourceStack);
      net.minecraft.world.item.ItemStack copy = sourceStack.copy();
      copy.setCount(1);
      sword.setDisplayItemStack(copy);
    }

    // 属性可能被定制覆盖，需同步生命-耐久
    sword.syncHealthWithDurability();

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

    // 音效：生成
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops.SoundOps
        .playSpawn(sword);

    // 召唤时的剑阵粒子（玩家脚下三环阵）
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.FlyingSwordFX
        .spawnSummonArrayAt(level, owner.position(), swordType);

    // 触发onSpawn事件钩子
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
        .SpawnContext spawnCtx =
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
            .context.SpawnContext(sword, level, owner, spawnPos, sourceStack);
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
        .FlyingSwordEventRegistry.fireSpawn(spawnCtx);

    return sword;
  }

  /** 简化：基于主人视线与位置的 spawnWithModifiersAndSpec 封装。 */
  @Nullable
  public static FlyingSwordEntity spawnFromOwnerWithModifiersAndSpec(
      ServerLevel level,
      Player owner,
      @Nullable ItemStack sourceStack,
      FlyingSwordType swordType,
      FlyingSwordAttributes.AttributeModifiers modifiers,
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.init
          .FlyingSwordInitSpec initSpec) {
    Vec3 lookVec = owner.getLookAngle();
    Vec3 spawnPos = owner.getEyePosition().add(lookVec.scale(1.5));
    return spawnWithModifiersAndSpec(
        level, owner, spawnPos, lookVec, sourceStack, swordType, modifiers, initSpec);
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
    double armor = 0;
    double armorToughness = 0;

    for (var entry : attributeModifiers.modifiers()) {
      if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
        attackDamage += entry.modifier().amount();
      } else if (entry.attribute().equals(Attributes.ATTACK_SPEED)) {
        attackSpeed += entry.modifier().amount();
      } else if (entry.attribute().equals(Attributes.ARMOR)) {
        armor += entry.modifier().amount();
      } else if (entry.attribute().equals(Attributes.ARMOR_TOUGHNESS)) {
        armorToughness += entry.modifier().amount();
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

    // 物品最大耐久 → 飞剑最大耐久
    int maxDamage = stack.getMaxDamage();
    if (maxDamage > 0) {
      modifiers.maxDurability +=
          maxDamage * FlyingSwordTuning.INHERIT_MAX_DAMAGE_TO_MAX_DURABILITY_COEF;
    }

    // 护甲/韧性 → 飞剑最大耐久与耐久损耗倍率
    double armorScore = armor + Math.max(0.0, armorToughness) * 0.5;
    if (armorScore > 0) {
      modifiers.maxDurability +=
          armorScore * FlyingSwordTuning.INHERIT_ARMOR_TO_MAX_DURABILITY_COEF;
      // 每点护甲按系数降低耐久损耗
      modifiers.duraLossRatioMult *=
          Math.pow(FlyingSwordTuning.INHERIT_ARMOR_DURA_LOSS_MULT_PER_POINT, armorScore);
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

  /**
   * 从存储中恢复单个飞剑
   *
   * @param level 服务端世界
   * @param owner 主人
   * @param recalled 召回的飞剑数据
   * @return 恢复的飞剑实体，失败返回null
   */
  @Nullable
  public static FlyingSwordEntity restore(
      ServerLevel level, Player owner, FlyingSwordStorage.RecalledSword recalled) {
    // 若物品已被拿出（withdrawn），禁止恢复
    if (recalled.itemWithdrawn) {
      owner.sendSystemMessage(
          net.minecraft.network.chat.Component.literal("[飞剑] 该飞剑已取出本体，无法召唤/恢复"));
      return null;
    }
    // 在主人前方1.5格生成
    Vec3 lookVec = owner.getLookAngle();
    Vec3 spawnPos = owner.getEyePosition().add(lookVec.scale(1.5));

    // 选择实体类型：优先使用存储的剑类型，其次默认
    FlyingSwordType type = FlyingSwordType.DEFAULT;
    if (recalled.swordType != null && !recalled.swordType.isEmpty()) {
      type = FlyingSwordType.fromRegistryName(recalled.swordType);
    }
    net.minecraft.world.entity.EntityType<FlyingSwordEntity> entityType = getEntityTypeForSwordType(type);

    // 创建飞剑实体（不使用释放继承修正）
    FlyingSwordEntity sword =
        FlyingSwordEntity.create(
            level, owner, spawnPos, FlyingSwordAttributes.AttributeModifiers.empty(), entityType);

    if (sword == null) {
      return null;
    }

    // 恢复保存的状态
    sword.setSwordAttributes(recalled.attributes);
    sword.setSwordLevel(recalled.level);
    sword.setExperience(recalled.experience);
    sword.setDurability(recalled.durability);

    // 确保生命-耐久绑定
    sword.syncHealthWithDurability();

    // 恢复外观：显示物品/模型键/音效档/类型
    // 1) 优先完整 ItemStack NBT
    if (recalled.displayItem != null && !recalled.displayItem.isEmpty()) {
      net.minecraft.world.item.ItemStack parsed =
          net.minecraft.world.item.ItemStack.parseOptional(level.registryAccess(), recalled.displayItem);
      if (!parsed.isEmpty()) {
        sword.setDisplayItemStack(parsed);
      }
    } else if (recalled.displayItemId != null) {
      // 2) 回退到 itemId
      var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(recalled.displayItemId).orElse(null);
      if (item != null) {
        sword.setDisplayItemStack(new net.minecraft.world.item.ItemStack(item));
      }
    }
    if (recalled.modelKey != null && !recalled.modelKey.isEmpty()) {
      sword.setModelKey(recalled.modelKey);
    }
    if (recalled.soundProfile != null && !recalled.soundProfile.isEmpty()) {
      sword.setSoundProfile(recalled.soundProfile);
    }
    sword.setSwordType(type);

    // 恢复胸腔内容
    try {
      if (recalled.chestCavity != null && !recalled.chestCavity.isEmpty()) {
        var wrapper = new net.minecraft.nbt.CompoundTag();
        wrapper.put("ChestCavity", recalled.chestCavity.copy());
        net.tigereye.chestcavity.registration.CCAttachments
            .getChestCavity(sword)
            .fromTag(wrapper, sword, level.registryAccess());
      }
    } catch (Throwable t) {
      net.tigereye.chestcavity.ChestCavity.LOGGER.warn(
          "[FlyingSword] Failed to restore chest cavity from storage; continuing", t);
    }

    // 设置初始速度
    Vec3 initialVelocity = lookVec.normalize().scale(sword.getSwordAttributes().speedBase);
    sword.setDeltaMovement(initialVelocity);

    // 添加到世界
    if (!level.addFreshEntity(sword)) {
      return null;
    }

    return sword;
  }

  /**
   * 从存储中恢复所有飞剑
   *
   * @param level 服务端世界
   * @param owner 主人
   * @return 成功恢复的飞剑数量
   */
  public static int restoreAll(ServerLevel level, Player owner) {
    FlyingSwordStorage storage =
        net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordStorage(owner);

    int storageCount = storage.getCount();
    if (storageCount == 0) {
      owner.sendSystemMessage(
          net.minecraft.network.chat.Component.literal("[飞剑] 存储中没有召回的飞剑"));
      return 0;
    }

    java.util.List<FlyingSwordStorage.RecalledSword> recalledSwords =
        storage.getRecalledSwords();

    int successCount = 0;
    for (FlyingSwordStorage.RecalledSword recalled : recalledSwords) {
      if (recalled.itemWithdrawn) {
        owner.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("[飞剑] 该飞剑已取出本体，无法恢复/召唤"));
        continue;
      }
      FlyingSwordEntity sword = restore(level, owner, recalled);
      if (sword != null) {
        successCount++;
        owner.sendSystemMessage(
            net.minecraft.network.chat.Component.literal(
                String.format(
                    "[飞剑] 恢复成功 - 等级%d (经验: %d, 耐久: %.1f/%.1f)",
                    recalled.level,
                    recalled.experience,
                    recalled.durability,
                    recalled.attributes.maxDurability)));
      }
    }

    // 清空存储
    storage.clear();

    owner.sendSystemMessage(
        net.minecraft.network.chat.Component.literal(
            String.format("[飞剑] 共恢复 %d/%d 个飞剑", successCount, storageCount)));

    return successCount;
  }

  /** 从存储中按索引恢复一个飞剑（1-based）。成功返回true。 */
  public static boolean restoreOne(ServerLevel level, Player owner, int index1) {
    FlyingSwordStorage storage =
        net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordStorage(owner);
    java.util.List<FlyingSwordStorage.RecalledSword> list = storage.getRecalledSwords();
    if (index1 < 1 || index1 > list.size()) {
      owner.sendSystemMessage(
          net.minecraft.network.chat.Component.literal("[飞剑] 索引无效"));
      return false;
    }
    FlyingSwordStorage.RecalledSword recalled = list.get(index1 - 1);
    if (recalled.itemWithdrawn) {
      owner.sendSystemMessage(
          net.minecraft.network.chat.Component.literal("[飞剑] 该飞剑已取出本体，无法恢复/召唤"));
      return false;
    }
    FlyingSwordEntity sword = restore(level, owner, recalled);
    if (sword != null) {
      storage.remove(index1 - 1);
      owner.sendSystemMessage(
          net.minecraft.network.chat.Component.literal(
              String.format(
                  java.util.Locale.ROOT,
                  "[飞剑] 召唤成功 - 等级%d (经验: %d, 耐久: %.1f/%.1f)",
                  recalled.level,
                  recalled.experience,
                  recalled.durability,
                  recalled.attributes.maxDurability)));
      return true;
    }
    return false;
  }

  /**
   * 根据飞剑类型获取对应的EntityType
   *
   * @param swordType 飞剑类型
   * @return 对应的EntityType
   */
  private static net.minecraft.world.entity.EntityType<FlyingSwordEntity> getEntityTypeForSwordType(
      FlyingSwordType swordType) {
    return switch (swordType) {
      case ZHENG_DAO -> net.tigereye.chestcavity.registration.CCEntities.FLYING_SWORD_ZHENG_DAO.get();
      case REN_SHOU_ZANG_SHENG -> net.tigereye.chestcavity.registration.CCEntities.FLYING_SWORD_REN_SHOU_ZANG_SHENG.get();
      default -> net.tigereye.chestcavity.registration.CCEntities.FLYING_SWORD.get();
    };
  }
}
