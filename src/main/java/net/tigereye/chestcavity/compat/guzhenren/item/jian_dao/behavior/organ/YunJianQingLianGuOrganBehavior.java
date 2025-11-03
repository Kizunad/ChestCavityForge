package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainManager;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.qinglian.QingLianDomain;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.qinglian.fx.QingLianDomainFX;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.YunJianQingLianGuCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordSpawner;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordType;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.YunJianQingLianGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
// 非玩家自动化逻辑暂不启用；如需后续支持再引入对应工具类
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * 蕴剑青莲蛊器官行为（枚举单例）
 *
 * <p>五转剑道核心器官：
 *
 * <ul>
 *   <li>主动：蕴剑化莲（环绕飞剑+青莲剑域+无敌焦点）
 *   <li>被动1：持续消耗（真元+精力+念头）
 *   <li>被动2：缓慢恢复（魂魄+生命）
 *   <li>被动3：青莲护体（致命一击格挡）
 * </ul>
 */
public enum YunJianQingLianGuOrganBehavior
    implements OrganSlowTickListener, OrganIncomingDamageListener {

  INSTANCE;

  private static final String MOD_ID = "guzhenren";

  /** 器官物品ID */
  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yun_jian_qing_lian");

  /** 主动技能ID */
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yun_jian_qing_lian_activate");

  // ========== OrganState字段键 ==========
  private static final String STATE_ROOT = "YunJianQingLian";
  private static final String K_ACTIVE = "Active"; // boolean - 是否激活
  private static final String K_SWORDS = "Swords"; // ListTag - 飞剑UUID列表
  private static final String K_DOMAIN_ID_MOST = "DomainIdMost"; // long - 领域UUID高位
  private static final String K_DOMAIN_ID_LEAST = "DomainIdLeast"; // long - 领域UUID低位
  private static final String K_LAST_DRAIN = "LastDrainAt"; // long - 上次扣费时间
  private static final String K_SHIELD_READY = "ShieldReadyAt"; // long - 护体可用时间

  // ========== 主动技能注册 ==========
  static {
    OrganActivationListeners.register(ABILITY_ID, YunJianQingLianGuOrganBehavior::activateAbility);
  }

  // ========== 主动技能：蕴剑化莲 ==========

  /**
   * 主动技能激活入口
   *
   * @param entity 实体
   * @param cc 胸腔实例
   */
  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (entity.level().isClientSide()) {
      return;
    }

    if (!(entity.level() instanceof ServerLevel level)) {
      return;
    }

    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = level.getGameTime();

    boolean active = state.getBoolean(K_ACTIVE, false);

    if (active) {
      // 关闭技能
      deactivateAbility(entity, state, level, cc, organ);
    } else {
      // 开启技能
      activateAbilityImpl(entity, state, level, now, cc, organ);
    }
  }

  /**
   * 开启蕴剑化莲
   *
   * @param entity 实体
   * @param state 器官状态
   * @param level 世界
   * @param now 当前时间
   * @param cc 胸腔实例
   * @param organ 器官物品
   */
  private static void activateAbilityImpl(
      LivingEntity entity,
      OrganState state,
      ServerLevel level,
      long now,
      ChestCavityInstance cc,
      ItemStack organ) {

    // 1. 读取域控系数（剑域蛊增幅）
    double pOut = DomainTags.getDoubleTag(entity, DomainTags.TAG_SD_P_OUT);
    if (pOut < 1.0) {
      pOut = YunJianQingLianGuTuning.DEFAULT_P_OUT; // 默认值
    }

    // 2. 计算飞剑数量与环绕半径
    int swordCount = YunJianQingLianGuCalc.calculateSwordCount(pOut);
    double orbitRadius = YunJianQingLianGuCalc.calculateOrbitRadius(swordCount);

    // 3. 先创建青莲剑域（飞剑需要注册到集群管理器）
    Vec3 entityPos = entity.position();
    double domainRadiusScale = YunJianQingLianGuCalc.calculateDomainRadiusScale(pOut);
    QingLianDomain domain = new QingLianDomain(entity, entityPos, domainRadiusScale);
    DomainManager.getInstance().registerDomain(domain);

    // 裂剑蛊：剑域激活时，排列附近裂隙成环状剑阵
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.rift.RiftDomainSynergy
        .onDomainActivated(entity, level);

    // 4. 生成环绕飞剑（莲瓣形态），并注册到集群管理器
    ListTag swordList = new ListTag();

    for (int i = 0; i < swordCount; i++) {
      double angle = (i / (double) swordCount) * Math.PI * 2.0;
      Vec3 offset =
          new Vec3(
              Math.cos(angle) * orbitRadius,
              1.5, // 悬浮高度
              Math.sin(angle) * orbitRadius);
      Vec3 spawnPos = entityPos.add(offset);
      Vec3 direction = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)); // 朝外

      // 创建飞剑（青莲蛊生成的飞剑使用钻石剑模型）
      ItemStack swordItem = new ItemStack(Items.DIAMOND_SWORD);

      // 添加耐久I附魔
      HolderLookup<Enchantment> enchantments =
          level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
      Holder<Enchantment> unbreakingHolder = enchantments.getOrThrow(Enchantments.UNBREAKING);
      swordItem.enchant(unbreakingHolder, 1);

      // 重命名为"青莲剑"
      swordItem.set(
          net.minecraft.core.component.DataComponents.CUSTOM_NAME,
          net.minecraft.network.chat.Component.literal("青莲剑"));

      FlyingSwordEntity sword =
          FlyingSwordSpawner.spawn(level, entity, spawnPos, direction, swordItem);

      if (sword != null) {
        // 显式设置显示物品为钻石剑（确保渲染正确）
        sword.setDisplayItemStack(swordItem.copy());

        // 标记模型键：用于客户端覆盖渲染（例如使用Blockbench模型）
        sword.setModelKey("qinglian");

        // 设置集群AI模式（由青莲剑域的集群管理器统一调度）
        sword.setAIMode(AIMode.SWARM);

        // 设置为不可召回（主动技能生成的飞剑）
        sword.setRecallable(false);

        // 应用域控系数增幅（伤害）
        double damageMult = YunJianQingLianGuCalc.calculateSwordDamageMult(pOut);
        // 飞剑伤害会自动应用pOut系数（通过域控标签）

        // 注册到青莲剑群集群管理器
        domain.getSwarmManager().addSword(sword);

        // 存储UUID
        CompoundTag swordTag = new CompoundTag();
        swordTag.putUUID("UUID", sword.getUUID());
        swordList.add(swordTag);
      }
    }

    state.setList(K_SWORDS, swordList);

    // 5. 存储领域UUID（使用两个long）
    UUID domainId = domain.getDomainId();
    state.setLong(K_DOMAIN_ID_MOST, domainId.getMostSignificantBits());
    state.setLong(K_DOMAIN_ID_LEAST, domainId.getLeastSignificantBits());

    // 领域创建特效
    QingLianDomainFX.spawnCreationEffect(level, entityPos, domain.getRadius());

    // 6. 设置 unbreakable_focus（剑心蛊联动）
    DomainTags.addTag(entity, DomainTags.TAG_UNBREAKABLE_FOCUS);

    // 7. 标记激活
    state.setBoolean(K_ACTIVE, true);
    state.setLong(K_LAST_DRAIN, now);

    // 提示（仅对玩家）
    if (entity instanceof ServerPlayer player) {
      player.displayClientMessage(Component.literal("§a§l蕴剑青莲·开"), true);
      player.displayClientMessage(
          Component.literal("§7" + swordCount + "片莲瓣环护，无敌焦点已开启"), false);
    }
  }

  /**
   * 关闭蕴剑化莲
   *
   * @param entity 实体
   * @param state 器官状态
   * @param level 世界
   * @param cc 胸腔实例
   * @param organ 器官物品
   */
  private static void deactivateAbility(
      LivingEntity entity,
      OrganState state,
      ServerLevel level,
      ChestCavityInstance cc,
      ItemStack organ) {

    // 1. 移除飞剑
    ListTag swordList = state.getList(K_SWORDS, Tag.TAG_COMPOUND);
    int removed = 0;
    for (int i = 0; i < swordList.size(); i++) {
      CompoundTag tag = swordList.getCompound(i);
      UUID uuid = tag.getUUID("UUID");
      Entity swordEntity = level.getEntity(uuid);
      if (swordEntity instanceof FlyingSwordEntity sword) {
        sword.discard();
        removed++;
      }
    }

    // 2. 移除领域
    long most = state.getLong(K_DOMAIN_ID_MOST, 0L);
    long least = state.getLong(K_DOMAIN_ID_LEAST, 0L);
    if (most != 0L || least != 0L) {
      UUID domainId = new UUID(most, least);
      DomainManager.getInstance().unregisterDomain(domainId);
    }

    // 3. 移除 unbreakable_focus
    DomainTags.removeTag(entity, DomainTags.TAG_UNBREAKABLE_FOCUS);

    // 4. 清空状态
    state.setBoolean(K_ACTIVE, false);
    state.setList(K_SWORDS, new ListTag());
    state.setLong(K_DOMAIN_ID_MOST, 0L);
    state.setLong(K_DOMAIN_ID_LEAST, 0L);

    // 提示（仅对玩家）
    if (entity instanceof ServerPlayer player) {
      player.displayClientMessage(Component.literal("§c§l蕴剑青莲·收"), true);
      if (removed > 0) {
        player.displayClientMessage(Component.literal("§7" + removed + "片莲瓣已归鞘"), false);
      }
    }
  }

  // ========== 被动技能：持续消耗 + 缓慢恢复 ==========

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity instanceof ServerPlayer player) {
      // 玩家逻辑：主动技能持续消耗
      OrganState state = OrganState.of(organ, STATE_ROOT);
      ServerLevel level = player.serverLevel();
      long now = level.getGameTime();

      boolean active = state.getBoolean(K_ACTIVE, false);

      if (active) {
        // 持续消耗（每秒）
        processActiveDrain(player, state, level, now, cc, organ);
      }

      // 被动恢复（总是生效）
      processPassiveRegen(player);
    } else if (entity instanceof net.minecraft.world.entity.Mob mob) {
      // 非玩家逻辑：AI Goal 变更自动化
      if (mob.level() instanceof ServerLevel serverLevel) {
        handleNonPlayerAutomation(mob, serverLevel, cc, organ);
      }
    }
  }


  /**
   * 处理主动技能持续消耗
   *
   * @param player 玩家
   * @param state 器官状态
   * @param level 世界
   * @param now 当前时间
   * @param cc 胸腔实例
   * @param organ 器官物品
   */
  private static void processActiveDrain(
      ServerPlayer player,
      OrganState state,
      ServerLevel level,
      long now,
      ChestCavityInstance cc,
      ItemStack organ) {

    long lastDrain = state.getLong(K_LAST_DRAIN, 0L);

    // 每秒（20 tick）扣费一次
    if (now - lastDrain >= 20L) {
      state.setLong(K_LAST_DRAIN, now);

      double zhenyuan = YunJianQingLianGuCalc.zhenyuanPerSecond();
      double jingli = YunJianQingLianGuTuning.JINGLI_PER_SEC;
      double niantou = YunJianQingLianGuTuning.NIANTOU_PER_SEC;

      boolean success = true;

      // 消耗真元
      if (ResourceOps.tryConsumeScaledZhenyuan(player, zhenyuan).isEmpty()) {
        success = false;
      }

      // 消耗精力
      if (ResourceOps.tryAdjustJingli(player, -jingli, true).isEmpty()) {
        success = false;
      }

      // 消耗念头
      if (ResourceOps.tryAdjustDouble(player, "niantou", -niantou, true, null).isEmpty()) {
        success = false;
      }

      if (!success) {
        // 资源耗尽，关闭技能
        deactivateAbility(player, state, level, cc, organ);
        player.displayClientMessage(Component.literal("§c资源耗尽，青莲收拢"), true);
      }
    }
  }

  /**
   * 处理被动恢复（总是生效）
   *
   * @param player 玩家
   */
  private static void processPassiveRegen(ServerPlayer player) {
    // 魂魄恢复（每秒0.5 → 每tick 0.025）
    double hunpoRegen = YunJianQingLianGuTuning.HUNPO_REGEN_PER_SEC / 20.0;
    ResourceOps.tryAdjustDouble(player, "hunpo", hunpoRegen, true, "max_hunpo");

    // 生命恢复（每秒0.1 → 每tick 0.005）
    float healthRegen = YunJianQingLianGuTuning.HEALTH_REGEN_PER_SEC / 20.0f;
    player.heal(healthRegen);
  }

  // ========== 非玩家自动化逻辑 ==========

  /** 器官状态字段：上次检测到的攻击Goal列表 */
  private static final String K_LAST_ATTACK_GOALS = "LastAttackGoals";
  /** 器官状态字段：脱战时间戳（worldTime） */
  private static final String K_DISENGAGED_AT = "DisengagedAt";
  /** 延迟关闭时间（ticks）：脱战5秒后 */
  private static final int DISENGAGE_DELAY_TICKS = 100;

  /**
   * 处理非玩家的自动化逻辑（Goal 变更检测 + 自动激活青莲剑群）
   *
   * @param mob 非玩家生物
   * @param level 服务端世界
   * @param cc 胸腔实例
   * @param organ 器官物品
   */
  private static void handleNonPlayerAutomation(
      net.minecraft.world.entity.Mob mob,
      ServerLevel level,
      ChestCavityInstance cc,
      ItemStack organ) {

    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = level.getGameTime();
    boolean active = state.getBoolean(K_ACTIVE, false);

    // 1. 读取当前运行的攻击Goal
    java.util.List<String> currentAttackGoals =
        net.tigereye.chestcavity.compat.guzhenren.util.behavior.AIIntrospection
            .getRunningAttackGoalNames(mob);

    // 2. 读取上次记录的Goal快照
    ListTag lastGoalsTag = state.getList(K_LAST_ATTACK_GOALS, Tag.TAG_STRING);
    java.util.List<String> lastGoals = new java.util.ArrayList<>();
    for (int i = 0; i < lastGoalsTag.size(); i++) {
      lastGoals.add(lastGoalsTag.getString(i));
    }

    // 3. 检测Goal变更
    boolean goalChanged = !currentAttackGoals.equals(lastGoals);
    boolean hasTarget = mob.getTarget() != null && mob.getTarget().isAlive() && !mob.getTarget().isAlliedTo(mob);
    boolean isAttacking = !currentAttackGoals.isEmpty() || hasTarget;

    // 4. 状态机逻辑
    if (!active && isAttacking && (goalChanged || hasTarget)) {
      // ========== 进入战斗 → 激活青莲剑群 ==========
      boolean success =
          net.tigereye.chestcavity.compat.guzhenren.util.behavior.ActiveSkillOps.activateFor(
              mob, ABILITY_ID);

      if (success) {
        // 获取目标并让飞剑群攻击
        LivingEntity target = mob.getTarget();
        if (target != null) {
          // 获取青莲剑域
          long most = state.getLong(K_DOMAIN_ID_MOST, 0L);
          long least = state.getLong(K_DOMAIN_ID_LEAST, 0L);
          if (most != 0L || least != 0L) {
            UUID domainId = new UUID(most, least);
            var domain = DomainManager.getInstance().getDomain(domainId);
            if (domain instanceof QingLianDomain qingLianDomain) {
              // 指令飞剑群攻击目标
              qingLianDomain.getSwarmManager().commandAttack(target);
            }
          }
        }

        // 清除脱战时间戳
        state.setLong(K_DISENGAGED_AT, 0L);
      }
    } else if (active && isAttacking) {
      // ========== 持续战斗 → 更新目标 ==========
      LivingEntity target = mob.getTarget();
      if (target != null) {
        long most = state.getLong(K_DOMAIN_ID_MOST, 0L);
        long least = state.getLong(K_DOMAIN_ID_LEAST, 0L);
        if (most != 0L || least != 0L) {
          UUID domainId = new UUID(most, least);
          var domain = DomainManager.getInstance().getDomain(domainId);
          if (domain instanceof QingLianDomain qingLianDomain) {
            qingLianDomain.getSwarmManager().commandAttack(target);
          }
        }
      }

      // 清除脱战时间戳（重新进入战斗）
      state.setLong(K_DISENGAGED_AT, 0L);
    } else if (active && !isAttacking) {
      // ========== 脱战 → 记录时间戳或延迟关闭 ==========
      long disengagedAt = state.getLong(K_DISENGAGED_AT, 0L);

      if (disengagedAt == 0L) {
        // 首次脱战，记录时间戳
        state.setLong(K_DISENGAGED_AT, now);
      } else if (now - disengagedAt >= DISENGAGE_DELAY_TICKS) {
        // 脱战超过5秒 → 自动关闭
        net.tigereye.chestcavity.compat.guzhenren.util.behavior.ActiveSkillOps.activateFor(
            mob, ABILITY_ID); // 切换关闭
        state.setLong(K_DISENGAGED_AT, 0L);
      }
    }

    // 5. 保存当前Goal快照到器官状态
    ListTag newGoalsTag = new ListTag();
    for (String goalName : currentAttackGoals) {
      newGoalsTag.add(net.minecraft.nbt.StringTag.valueOf(goalName));
    }
    state.setList(K_LAST_ATTACK_GOALS, newGoalsTag);
  }

  // ========== 被动技能：青莲护体（致命一击格挡） ==========

  @Override
  public float onIncomingDamage(
      DamageSource source, LivingEntity owner, ChestCavityInstance cc, ItemStack organ, float dmg) {
    if (!(owner instanceof ServerPlayer player)) {
      return dmg;
    }

    OrganState state = OrganState.of(organ, STATE_ROOT);
    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();

    // 检测是否致命一击
    boolean lethal = YunJianQingLianGuCalc.isLethalDamage(player.getHealth(), dmg);
    if (!lethal) {
      return dmg;
    }

    // 检查冷却
    long shieldReady = state.getLong(K_SHIELD_READY, 0L);
    if (now < shieldReady) {
      return dmg; // 冷却中
    }

    // 计算消耗
    double cost = YunJianQingLianGuCalc.calculateShieldCost(dmg);

    // 尝试消耗真元
    if (ResourceOps.tryConsumeScaledZhenyuan(player, cost).isPresent()) {
      // 成功格挡
      state.setLong(K_SHIELD_READY, now + YunJianQingLianGuTuning.SHIELD_COOLDOWN_SEC * 20L);

      // 特效：青莲护体（发光粒子）
      spawnShieldEffect(level, player);

      // 提示
      player.displayClientMessage(Component.literal("§b§l青莲护体！"), true);
      player.displayClientMessage(
          Component.literal("§7消耗 " + String.format("%.1f", cost) + " 真元，完全格挡致命伤"), false);

      return 0.0f; // 完全免疫伤害
    }

    // 真元不足，无法格挡
    player.displayClientMessage(Component.literal("§c真元不足，青莲护体失效！"), false);
    return dmg;
  }

  /**
   * 生成青莲护体特效
   *
   * @param level 世界
   * @param player 玩家
   */
  private static void spawnShieldEffect(ServerLevel level, ServerPlayer player) {
    Vec3 pos = player.position();

    // 发光粒子环绕（青莲绽放）
    level.sendParticles(
        net.minecraft.core.particles.ParticleTypes.GLOW,
        pos.x,
        pos.y + 1.0,
        pos.z,
        30,
        0.5,
        0.5,
        0.5,
        0.1);

    // 末影烛光向上爆发（莲瓣升华）
    level.sendParticles(
        net.minecraft.core.particles.ParticleTypes.END_ROD,
        pos.x,
        pos.y + 0.5,
        pos.z,
        20,
        0.3,
        0.0,
        0.3,
        0.15);

    // 青色魂焰环绕
    for (int i = 0; i < 16; i++) {
      double angle = (i / 16.0) * Math.PI * 2.0;
      double x = pos.x + Math.cos(angle) * 1.5;
      double z = pos.z + Math.sin(angle) * 1.5;
      double y = pos.y + 1.0;

      level.sendParticles(
          net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
          x,
          y,
          z,
          1,
          0.0,
          0.0,
          0.0,
          0.0);
    }
  }

  // ========== 辅助方法 ==========

  /**
   * 查找匹配的器官物品
   *
   * @param cc 胸腔实例
   * @return 器官物品，未找到则返回EMPTY
   */
  private static ItemStack findMatchingOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (s.isEmpty()) {
        continue;
      }

      ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
      if (id == null) {
        continue;
      }
      // 兼容部分物品命名差异：同时接受 yun_jian_qing_lian 与 yun_jian_qing_lian_gu
      if (id.equals(ORGAN_ID)
          || id.equals(ResourceLocation.parse("guzhenren:yun_jian_qing_lian_gu"))) {
        return s;
      }
    }

    return ItemStack.EMPTY;
  }
}
