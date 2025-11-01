package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.FlyingSwordCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordTuning;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * 飞剑实体（Flying Sword Entity）
 *
 * <p>独立实体系统，支持：
 * <ul>
 *   <li>速度²伤害公式</li>
 *   <li>三种AI模式（环绕/防守/出击）</li>
 *   <li>经验成长系统</li>
 *   <li>耐久管理</li>
 *   <li>维持消耗</li>
 *   <li>释放继承（从剑物品继承属性）</li>
 * </ul>
 */
public class FlyingSwordEntity extends PathfinderMob {

  // ========== SynchedEntityData ==========
  private static final EntityDataAccessor<Optional<UUID>> OWNER =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.OPTIONAL_UUID);

  private static final EntityDataAccessor<Integer> AI_MODE =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.INT);

  private static final EntityDataAccessor<Optional<UUID>> TARGET =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.OPTIONAL_UUID);

  private static final EntityDataAccessor<Integer> LEVEL =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.INT);

  private static final EntityDataAccessor<Integer> EXPERIENCE =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.INT);

  private static final EntityDataAccessor<Float> DURABILITY =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.FLOAT);

  private static final EntityDataAccessor<Float> SPEED_CURRENT =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.FLOAT);

  // 显示模型与音效档（可运行时定制）
  private static final EntityDataAccessor<ItemStack> DISPLAY_ITEM_STACK =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.ITEM_STACK);
  private static final EntityDataAccessor<String> MODEL_KEY =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<String> SOUND_PROFILE =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.STRING);

  // 飞剑类型（用于区分正道/魔道/默认）
  private static final EntityDataAccessor<String> SWORD_TYPE =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.STRING);

  // ========== 缓存字段 ==========
  @Nullable
  private Player cachedOwner;

  @Nullable
  private LivingEntity cachedTarget;

  private FlyingSwordAttributes attributes;

  private int upkeepTicks = 0;

  private int age = 0;

  private int attackCooldown = 0; // 攻击冷却（tick）

  // ========== 构造函数 ==========
  public FlyingSwordEntity(EntityType<? extends PathfinderMob> type, Level level) {
    super(type, level);
    this.setNoGravity(true);
    this.noCulling = true;
    this.attributes = FlyingSwordAttributes.createDefault();
  }

  // ========== 属性配置 ==========
  public static AttributeSupplier.Builder createAttributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 20.0D)
        .add(Attributes.MOVEMENT_SPEED, 0.0D)
        .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
        .add(Attributes.FOLLOW_RANGE, 64.0D);
  }

  // ========== SynchedEntityData 定义 ==========
  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(OWNER, Optional.empty());
    builder.define(AI_MODE, AIMode.ORBIT.ordinal());
    builder.define(TARGET, Optional.empty());
    builder.define(LEVEL, 1);
    builder.define(EXPERIENCE, 0);
    builder.define(DURABILITY, (float) FlyingSwordTuning.MAX_DURABILITY);
    builder.define(SPEED_CURRENT, (float) FlyingSwordTuning.SPEED_BASE);
    builder.define(DISPLAY_ITEM_STACK, new ItemStack(Items.IRON_SWORD));
    builder.define(MODEL_KEY, "");
    builder.define(SOUND_PROFILE, "");
    builder.define(SWORD_TYPE, FlyingSwordType.DEFAULT.getRegistryName());
  }

  @Override
  protected void registerGoals() {
    // AI Goals 将在后续AI系统中实现
  }

  // ========== Owner 管理 ==========
  public void setOwner(@Nullable Player owner) {
    this.cachedOwner = owner;
    this.entityData.set(OWNER, owner == null ? Optional.empty() : Optional.of(owner.getUUID()));
  }

  @Nullable
  public Player getOwner() {
    if (cachedOwner != null && !cachedOwner.isRemoved()) {
      return cachedOwner;
    }
    if (!(this.level() instanceof ServerLevel server)) {
      return null;
    }
    Optional<UUID> ownerId = this.entityData.get(OWNER);
    if (ownerId.isEmpty()) {
      return null;
    }
    Player owner = server.getPlayerByUUID(ownerId.get());
    if (owner != null) {
      cachedOwner = owner;
    }
    return owner;
  }

  public boolean isOwnedBy(Player player) {
    Optional<UUID> id = this.entityData.get(OWNER);
    return id.isPresent() && id.get().equals(player.getUUID());
  }

  // ========== AI Mode 管理 ==========
  public AIMode getAIMode() {
    return AIMode.fromOrdinal(this.entityData.get(AI_MODE));
  }

  public void setAIMode(AIMode mode) {
    this.entityData.set(AI_MODE, mode.ordinal());
  }

  // ========== Target 管理 ==========
  @Nullable
  public LivingEntity getTargetEntity() {
    if (cachedTarget != null && cachedTarget.isAlive()) {
      return cachedTarget;
    }
    if (!(this.level() instanceof ServerLevel server)) {
      return null;
    }
    Optional<UUID> targetId = this.entityData.get(TARGET);
    if (targetId.isEmpty()) {
      return null;
    }
    if (server.getEntity(targetId.get()) instanceof LivingEntity target) {
      cachedTarget = target;
      return target;
    }
    return null;
  }

  public void setTargetEntity(@Nullable LivingEntity target) {
    this.cachedTarget = target;
    this.entityData.set(TARGET, target == null ? Optional.empty() : Optional.of(target.getUUID()));
  }

  // ========== 属性管理 ==========
  public FlyingSwordAttributes getSwordAttributes() {
    return this.attributes;
  }

  public void setSwordAttributes(FlyingSwordAttributes attributes) {
    this.attributes = attributes;
    // 属性变化后，同步生命-耐久关系
    syncHealthWithDurability();
  }

  // ========== 等级与经验 ==========
  public int getSwordLevel() {
    return this.entityData.get(LEVEL);
  }

  public void setSwordLevel(int level) {
    this.entityData.set(LEVEL, FlyingSwordCalculator.clamp(level, 1, FlyingSwordTuning.MAX_LEVEL));
  }

  public int getExperience() {
    return this.entityData.get(EXPERIENCE);
  }

  public void setExperience(int exp) {
    this.entityData.set(EXPERIENCE, Math.max(0, exp));
  }

  public void addExperience(int amount) {
    if (amount <= 0) return;

    int currentExp = getExperience();
    int currentLevel = getSwordLevel();
    int newExp = currentExp + amount;

    // 检查升级
    while (currentLevel < FlyingSwordTuning.MAX_LEVEL) {
      int expToNext = FlyingSwordCalculator.calculateExpToNext(currentLevel);
      if (newExp >= expToNext) {
        newExp -= expToNext;
        currentLevel++;
      } else {
        break;
      }
    }

    setSwordLevel(currentLevel);
    setExperience(newExp);
  }

  // ========== 耐久管理 ==========
  public float getDurability() {
    return this.entityData.get(DURABILITY);
  }

  public void setDurability(float durability) {
    float clamped = (float) FlyingSwordCalculator.clamp(
        durability, 0.0, attributes.maxDurability);
    this.entityData.set(DURABILITY, clamped);
    // 生命值与耐久绑定：最大生命 = 最大耐久；当前生命 = 当前耐久
    AttributeInstance hp = this.getAttribute(Attributes.MAX_HEALTH);
    if (hp != null) {
      if (hp.getBaseValue() != attributes.maxDurability) {
        hp.setBaseValue(Math.max(1.0, attributes.maxDurability));
      }
    }
    this.setHealth(clamped);
  }

  public void damageDurability(float amount) {
    float current = getDurability();
    setDurability(current - amount);

    // 耐久耗尽时消散
    if (getDurability() <= 0) {
      this.discard();
    }
  }

  // ========== 速度管理 ==========
  public float getCurrentSpeed() {
    return this.entityData.get(SPEED_CURRENT);
  }

  public void setCurrentSpeed(float speed) {
    this.entityData.set(SPEED_CURRENT, speed);
  }

  // ========== 显示模型/音效 档 ==========
  public ItemStack getDisplayItemStack() {
    return this.entityData.get(DISPLAY_ITEM_STACK);
  }

  public void setDisplayItemStack(ItemStack stack) {
    if (stack == null) stack = ItemStack.EMPTY;
    this.entityData.set(DISPLAY_ITEM_STACK, stack);
    // 同步名字到实体（用于名称牌显示）
    try {
      if (!stack.isEmpty() && net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning
          .FlyingSwordModelTuning.SHOW_ITEM_NAME) {
        this.setCustomName(stack.getHoverName());
        this.setCustomNameVisible(true);
      }
    } catch (Throwable ignored) {}
  }

  public String getModelKey() {
    return this.entityData.get(MODEL_KEY);
  }

  public void setModelKey(String key) {
    if (key == null) key = "";
    this.entityData.set(MODEL_KEY, key);
  }

  public String getSoundProfile() {
    return this.entityData.get(SOUND_PROFILE);
  }

  public void setSoundProfile(String profile) {
    if (profile == null) profile = "";
    this.entityData.set(SOUND_PROFILE, profile);
  }

  // ========== 飞剑类型管理 ==========
  public FlyingSwordType getSwordType() {
    String registryName = this.entityData.get(SWORD_TYPE);
    return FlyingSwordType.fromRegistryName(registryName);
  }

  public void setSwordType(FlyingSwordType type) {
    if (type == null) type = FlyingSwordType.DEFAULT;
    this.entityData.set(SWORD_TYPE, type.getRegistryName());
  }

  // ========== Tick 逻辑 ==========
  @Override
  public void tick() {
    super.tick();
    age++;

    if (this.level().isClientSide) {
      tickClient();
    } else {
      tickServer();
    }
  }

  private void tickClient() {
    // 客户端逻辑：主要在服务端生成粒子，客户端自动同步
    // 客户端特定的渲染逻辑在Renderer中处理
  }

  private void tickServer() {
    Player owner = getOwner();
    if (owner == null || !owner.isAlive()) {
      this.discard();
      return;
    }

    // 触发onTick事件钩子
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context
        .TickContext tickCtx =
        new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
            .context.TickContext(
            this, (net.minecraft.server.level.ServerLevel) this.level(), owner, getAIMode(), this.tickCount);
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
        .FlyingSwordEventRegistry.fireTick(tickCtx);

    // 攻击冷却递减
    if (attackCooldown > 0) {
      attackCooldown--;
    }

    // 维持消耗检查（可被钩子跳过）
    if (!tickCtx.skipUpkeep) {
      upkeepTicks++;
    }
    if (!tickCtx.skipUpkeep && upkeepTicks >= FlyingSwordTuning.UPKEEP_CHECK_INTERVAL) {
      upkeepTicks = 0;
      if (!net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ops
          .UpkeepOps.consumeIntervalUpkeep(this, FlyingSwordTuning.UPKEEP_CHECK_INTERVAL)) {
        // 音效：真元不足
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ops.SoundOps
            .playOutOfEnergy(this);
        // 维持不足，召回
        recallToOwner();
        return;
      }
    }

    // AI行为逻辑（可被钩子跳过）
    if (!tickCtx.skipAI) {
      tickAI();
    }

    // 破块逻辑（速度分段 + 镐子可破范围，可被钩子跳过）
    if (!tickCtx.skipBlockBreak) {
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ops.BlockBreakOps
          .tickBlockBreak(this);
    }
  }

  // 维持消耗逻辑已迁移到 ops.UpkeepOps

  private void tickAI() {
    Player owner = getOwner();
    if (owner == null) {
      return;
    }

    AIMode mode = getAIMode();

    switch (mode) {
      case ORBIT ->
          net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior
              .OrbitBehavior.tick(this, owner);
      case GUARD -> {
        var nearest =
            net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior
                .TargetFinder.findNearestHostile(
                    this, owner.position(),
                    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai
                        .behavior.GuardBehavior.getSearchRange());
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior
            .GuardBehavior.tick(this, owner, nearest);
      }
      case HUNT -> {
        var nearest =
            net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior
                .TargetFinder.findNearestHostile(
                    this, this.position(),
                    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai
                        .behavior.HuntBehavior.getSearchRange());
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior
            .HuntBehavior.tick(this, owner, nearest);
      }
      case HOVER ->
          net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior
              .HoverBehavior.tick(this, owner);
    }

    // 检测碰撞攻击（集中在战斗模块）
    this.attackCooldown =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.combat
            .FlyingSwordCombat.tickCollisionAttack(this, this.attackCooldown);

    // 更新当前速度
    setCurrentSpeed((float) this.getDeltaMovement().length());

    // 粒子特效（每2 tick生成一次，减少性能消耗）
    if (this.tickCount % 2 == 0 && this.level() instanceof ServerLevel serverLevel) {
      spawnFlightParticles(serverLevel, mode);
    }
  }

  /**
   * 生成飞行粒子特效
   */
  private void spawnFlightParticles(ServerLevel level, AIMode mode) {
    double speed = this.getDeltaMovement().length();

    // 根据AI模式显示不同的粒子
    switch (mode) {
      case ORBIT ->
          net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.fx
              .FlyingSwordFX.spawnOrbitTrail(level, this);
      case GUARD ->
          net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.fx
              .FlyingSwordFX.spawnGuardTrail(level, this);
      case HUNT ->
          net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.fx
              .FlyingSwordFX.spawnHuntTrail(level, this);
    }

    // 高速飞行时的额外特效
    if (speed > attributes.speedMax * 0.8) {
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.fx.FlyingSwordFX
          .spawnSpeedBoostEffect(level, this);
    }
  }

  /**
   * 环绕模式：绕着主人旋转
   */
  // 旧的环绕/防守/出击行为已迁移到 ai.behavior 包

  /**
   * 防守模式：跟随主人并攻击附近的敌对实体
   */
  

  /**
   * 出击模式：主动搜索并攻击敌对实体
   */
  

  /**
   * 搜索最近的敌对实体
   */
  @Nullable
  // 目标搜索已迁移到 TargetFinder

  /**
   * 应用转向行为
   */
  public void applySteeringVelocity(Vec3 desiredVelocity) {
    Vec3 currentVelocity = this.getDeltaMovement();

    // 依据上下文调整目标速度与最大速度（挂接道痕等因素）
    var ctx =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator
            .context.CalcContexts.from(this);
    double effectiveBase =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator
            .FlyingSwordCalculator.effectiveSpeedBase(this.attributes.speedBase, ctx);
    double effectiveMax =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator
            .FlyingSwordCalculator.effectiveSpeedMax(this.attributes.speedMax, ctx);
    double effectiveAccel =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator
            .FlyingSwordCalculator.effectiveAccel(this.attributes.accel, ctx);

    // 将期望速度按 base 比例缩放（行为层仍以 base 构建）
    double baseScale = this.attributes.speedBase > 1.0e-8 ? (effectiveBase / this.attributes.speedBase) : 1.0;
    Vec3 desired = desiredVelocity.scale(baseScale);

    // 计算转向力
    Vec3 steering = desired.subtract(currentVelocity);

    // 限制转向/加速度（每 tick 最大速度变化）
    double limit = Math.max(1.0e-6, Math.min(attributes.turnRate, effectiveAccel));
    double steeringMag = steering.length();
    if (steeringMag > limit) {
      steering = steering.normalize().scale(limit);
    }

    // 应用转向
    Vec3 newVelocity = currentVelocity.add(steering);

    // 限制速度（使用有效最大速度）
    double speed = newVelocity.length();
    if (speed > effectiveMax && speed > 1.0e-8) {
      newVelocity = newVelocity.normalize().scale(effectiveMax);
    }

    this.setDeltaMovement(newVelocity);
  }

  /**
   * 检测碰撞攻击
   */
  // 碰撞攻击已集中到 FlyingSwordCombat

  // ========== 交互逻辑 ==========
  @Override
  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    // 若手持 chest_opener，放行给物品交互（打开胸腔），不执行默认召回
    var opener = net.tigereye.chestcavity.registration.CCItems.CHEST_OPENER.get();
    if (player.getItemInHand(hand).is(opener) || player.getOffhandItem().is(opener)) {
      return InteractionResult.PASS;
    }
    if (this.level().isClientSide) {
      return InteractionResult.SUCCESS;
    }

    boolean isOwner = isOwnedBy(player);

    // 触发onInteract事件钩子
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context
        .InteractContext interactCtx =
        new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
            .context.InteractContext(this, player, hand, isOwner);
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
        .FlyingSwordEventRegistry.fireInteract(interactCtx);

    // 检查是否被钩子拦截
    if (interactCtx.cancelDefault) {
      return interactCtx.customResult;
    }

    // 右键召回（仅主人）
    if (isOwner) {
      recallToOwner();
      return InteractionResult.SUCCESS;
    }

    return InteractionResult.PASS;
  }

  private void recallToOwner() {
    if (!this.level().isClientSide) {
      // 使用控制器的召回方法，确保逻辑一致
      FlyingSwordController.recall(this);
    }
  }

  /** 将实体生命值与耐久绑定（1:1），并限制到当前上限。 */
  public void syncHealthWithDurability() {
    try {
      AttributeInstance hp = this.getAttribute(Attributes.MAX_HEALTH);
      if (hp != null) {
        hp.setBaseValue(Math.max(1.0, attributes.maxDurability));
      }
      float cur = getDurability();
      float clamped = (float) FlyingSwordCalculator.clamp(cur, 0.0, attributes.maxDurability);
      if (clamped != cur) {
        this.entityData.set(DURABILITY, clamped);
      }
      this.setHealth(clamped);
    } catch (Throwable ignored) {
    }
  }

  // ========== 伤害处理 ==========
  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (this.level().isClientSide) {
      return false;
    }

    Player owner = getOwner();
    if (owner == null) {
      // 无主人：按基础规则消耗耐久，并与生命同步
      float duraLoss =
          net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator
              .FlyingSwordCalculator.calculateDurabilityLoss(
                  amount, attributes.duraLossRatio, false);
      damageDurability(duraLoss);
      if (!this.isRemoved()) {
        this.setHealth(this.getDurability());
      }
      super.hurt(source, 0.0F);
      return true;
    }

    // 触发onHurt事件钩子
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context
        .HurtContext hurtCtx =
        new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
            .context.HurtContext(
            this, (net.minecraft.server.level.ServerLevel) this.level(), owner, source, amount);
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
        .FlyingSwordEventRegistry.fireHurt(hurtCtx);

    // 检查是否被钩子取消
    if (hurtCtx.cancelled) {
      return false;
    }

    // 使用钩子修改后的伤害
    amount = hurtCtx.damage;

    // 应用折返效果
    if (hurtCtx.triggerRetreat) {
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.impl
          .DefaultEventHooks.applyRetreat(this);
    }

    // 应用虚弱状态
    if (hurtCtx.triggerWeakened) {
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.impl
          .DefaultEventHooks.applyWeakened(this, hurtCtx.weakenedDuration);
    }

    // 耐久损耗（带上下文，应用流派经验等影响）
    float duraLoss =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator
            .FlyingSwordCalculator.calculateDurabilityLossWithContext(
                amount,
                attributes.duraLossRatio,
                false,
                net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword
                    .calculator.context.CalcContexts.from(this));
    damageDurability(duraLoss);

    // 保持生命值与耐久一致
    if (!this.isRemoved()) {
      this.setHealth(this.getDurability());
    }

    // 调用父类以保持受击副作用，但避免重复扣血：传入0伤害
    super.hurt(source, 0.0F);
    return true;
  }

  @Override
  public boolean causeFallDamage(
      float fallDistance, float damageMultiplier, DamageSource damageSource) {
    return false; // 飞剑不受坠落伤害
  }

  @Override
  protected void doPush(net.minecraft.world.entity.Entity entity) {
    // 飞剑不推挤其他实体
  }

  @Override
  public boolean isPushable() {
    return false;
  }

  // ========== NBT 序列化 ==========
  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);

    // Owner
    if (tag.hasUUID("Owner")) {
      this.entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
    }

    // AI Mode
    if (tag.contains("AIMode")) {
      String modeId = tag.getString("AIMode");
      AIMode mode = AIMode.fromId(modeId);
      this.entityData.set(AI_MODE, mode.ordinal());
    }

    // Level & Experience
    this.entityData.set(LEVEL, tag.getInt("Level"));
    this.entityData.set(EXPERIENCE, tag.getInt("Experience"));

    // Durability
    this.entityData.set(DURABILITY, tag.getFloat("Durability"));

    // Speed
    this.entityData.set(SPEED_CURRENT, tag.getFloat("SpeedCurrent"));

    // Attributes
    if (tag.contains("Attributes")) {
      this.attributes = FlyingSwordAttributes.loadFromNBT(tag.getCompound("Attributes"));
    }

    // 展示模型与音效档
    if (tag.contains("DisplayItem")) {
      ItemStack stack = ItemStack.parseOptional(this.registryAccess(), tag.getCompound("DisplayItem"));
      if (!stack.isEmpty()) {
        this.setDisplayItemStack(stack);
      }
    }
    if (tag.contains("ModelKey")) {
      setModelKey(tag.getString("ModelKey"));
    }
    if (tag.contains("SoundProfile")) {
      setSoundProfile(tag.getString("SoundProfile"));
    }

    // 飞剑类型
    if (tag.contains("SwordType")) {
      FlyingSwordType type = FlyingSwordType.fromRegistryName(tag.getString("SwordType"));
      setSwordType(type);
    }

    // Age
    this.age = tag.getInt("Age");
    this.upkeepTicks = tag.getInt("UpkeepTicks");

    // 同步生命-耐久
    syncHealthWithDurability();
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);

    // Owner（持久化召唤者，以便重载后关联）
    this.entityData.get(OWNER).ifPresent(uuid -> tag.putUUID("Owner", uuid));

    // AI Mode
    tag.putString("AIMode", getAIMode().getId());

    // Level & Experience
    tag.putInt("Level", getSwordLevel());
    tag.putInt("Experience", getExperience());

    // Durability
    tag.putFloat("Durability", getDurability());

    // Speed
    tag.putFloat("SpeedCurrent", getCurrentSpeed());

    // Attributes
    CompoundTag attrsTag = new CompoundTag();
    this.attributes.saveToNBT(attrsTag);
    tag.put("Attributes", attrsTag);

    // Age
    tag.putInt("Age", this.age);
    tag.putInt("UpkeepTicks", this.upkeepTicks);

    // 展示模型与音效档
    ItemStack display = getDisplayItemStack();
    if (display != null && !display.isEmpty()) {
      tag.put("DisplayItem", display.save(this.registryAccess()));
    }
    if (!getModelKey().isEmpty()) {
      tag.putString("ModelKey", getModelKey());
    }
    if (!getSoundProfile().isEmpty()) {
      tag.putString("SoundProfile", getSoundProfile());
    }

    // 飞剑类型
    tag.putString("SwordType", getSwordType().getRegistryName());
  }

  // ========== 攻击逻辑 ==========
  /**
   * 对目标造成速度²伤害
   */
  // 攻击逻辑已统一集中到 combat.FlyingSwordCombat

  // ========== 工厂方法 ==========
  /**
   * 创建飞剑实体（默认类型）
   */
  public static FlyingSwordEntity create(
      ServerLevel level,
      Player owner,
      Vec3 spawnPos,
      @Nullable FlyingSwordAttributes.AttributeModifiers modifiers) {
    return create(
        level,
        owner,
        spawnPos,
        modifiers,
        net.tigereye.chestcavity.registration.CCEntities.FLYING_SWORD.get());
  }

  /**
   * 创建飞剑实体（指定实体类型）
   *
   * @param level 服务端世界
   * @param owner 主人
   * @param spawnPos 生成位置
   * @param modifiers 释放继承修正
   * @param entityType 实体类型（用于确定飞剑类型）
   * @return 创建的飞剑实体
   */
  public static FlyingSwordEntity create(
      ServerLevel level,
      Player owner,
      Vec3 spawnPos,
      @Nullable FlyingSwordAttributes.AttributeModifiers modifiers,
      EntityType<FlyingSwordEntity> entityType) {

    FlyingSwordEntity sword = entityType.create(level);

    if (sword == null) {
      return null;
    }

    // 设置位置和主人
    sword.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, owner.getYRot(), 0);
    sword.setOwner(owner);

    // 从EntityType推导并设置飞剑类型
    FlyingSwordType swordType = FlyingSwordType.fromEntityTypeId(
        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    sword.setSwordType(swordType);

    // 应用释放继承修正
    if (modifiers != null) {
      sword.getSwordAttributes().applyModifiers(modifiers);
    }

    // 绑定生命与耐久
    sword.syncHealthWithDurability();

    return sword;
  }
}
