package net.tigereye.chestcavity.compat.guzhenren.flyingsword;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.FlyingSwordCalculator;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning;
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
public class FlyingSwordEntity extends PathfinderMob implements OwnableEntity {

  // ========== SynchedEntityData ==========
  private static final EntityDataAccessor<Optional<UUID>> OWNER =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.OPTIONAL_UUID);

  private static final EntityDataAccessor<Integer> AI_MODE =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.INT);

  private static final EntityDataAccessor<Integer> GROUP_ID =
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

  // 是否可被召回（默认true，主动技能生成的飞剑设为false）
  private static final EntityDataAccessor<Boolean> IS_RECALLABLE =
      SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.BOOLEAN);

  // ========== 缓存字段 ==========
  @Nullable
  private LivingEntity cachedOwner;

  @Nullable
  private LivingEntity cachedTarget;

  private FlyingSwordAttributes attributes;

  private int upkeepTicks = 0;

  private int age = 0;

  // Phase 4: 冷却迁移兼容镜像（临时保留）
  // 说明：P4 计划将冷却统一到 owner 附件（MultiCooldown）。为降低实现复杂度并保持行为一致，
  // 先保留实体级倒计时字段作为兼容镜像，由 FlyingSwordCooldownOps 统一读写，后续可无缝替换为附件存储。
  private int attackCooldown = 0;

  /** 仅供 FlyingSwordCooldownOps 访问（P4 兼容镜像）。 */
  public int __getAttackCooldownMirror() { return attackCooldown; }
  /** 仅供 FlyingSwordCooldownOps 访问（P4 兼容镜像）。 */
  public void __setAttackCooldownMirror(int ticks) { this.attackCooldown = Math.max(0, ticks); }

  // 平滑朝向向量（用于渲染，避免抖动）
  private Vec3 smoothedLookAngle = Vec3.ZERO;
  private Vec3 lastVelocity = Vec3.ZERO;
  @Nullable private Vec3 antipodalSlerpBasis = null;

  public static final int SWARM_GROUP_ID = 900;

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
    builder.define(IS_RECALLABLE, true); // 默认可被召回
    builder.define(GROUP_ID, 0);
  }

  @Override
  protected void registerGoals() {
    // AI Goals 将在后续AI系统中实现
  }

  // ========== Owner 管理 ==========
  public void setOwner(@Nullable LivingEntity owner) {
    this.cachedOwner = owner;
    this.entityData.set(OWNER, owner == null ? Optional.empty() : Optional.of(owner.getUUID()));
  }

  @Override
  @Nullable
  public LivingEntity getOwner() {
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
    // 先尝试查询玩家（性能优化）
    Player player = server.getPlayerByUUID(ownerId.get());
    if (player != null) {
      cachedOwner = player;
      return player;
    }
    // 再查询普通实体
    net.minecraft.world.entity.Entity entity = server.getEntity(ownerId.get());
    if (entity instanceof LivingEntity living) {
      cachedOwner = living;
      return living;
    }
    return null;
  }

  @Override
  @Nullable
  public java.util.UUID getOwnerUUID() {
    Optional<java.util.UUID> id = this.entityData.get(OWNER);
    return id.orElse(null);
  }

  public boolean isOwnedBy(Player player) {
    Optional<UUID> id = this.entityData.get(OWNER);
    return id.isPresent() && id.get().equals(player.getUUID());
  }

  public boolean isOwnedBy(LivingEntity entity) {
    Optional<UUID> id = this.entityData.get(OWNER);
    return id.isPresent() && id.get().equals(entity.getUUID());
  }

  // ========== AI Mode 管理 ==========
  public AIMode getAIMode() {
    return AIMode.fromOrdinal(this.entityData.get(AI_MODE));
  }

  public void setAIMode(AIMode mode) {
    if (mode == null) return;
    AIMode old = getAIMode();
    if (old == mode) {
      return;
    }
    // Phase 3: 触发模式切换事件（可取消）
    try {
      var ctx = new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
          .ModeChangeContext(this, old, mode, /*trigger*/ null);
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
          .FlyingSwordEventRegistry.fireModeChange(ctx);
      if (ctx.cancelled) {
        return;
      }
    } catch (Throwable ignored) {}
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
    LivingEntity old = this.getTargetEntity();
    // 若新目标与旧目标不同，按顺序触发丢失/获取事件
    if (old != target) {
      if (old != null) {
        try {
          var lostCtx = new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
              .TargetLostContext(
              this,
              old,
              target == null
                  ? net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
                      .TargetLostContext.LostReason.MANUAL_CANCEL
                  : net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
                      .TargetLostContext.LostReason.OTHER);
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
              .FlyingSwordEventRegistry.fireTargetLost(lostCtx);
        } catch (Throwable ignored) {}
      }

      if (target != null) {
        try {
          var acqCtx = new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
              .TargetAcquiredContext(this, target, getAIMode());
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
              .FlyingSwordEventRegistry.fireTargetAcquired(acqCtx);
          if (acqCtx.cancelled) {
            return; // 取消锁定
          }
        } catch (Throwable ignored) {}
      }
    }

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
      if (!stack.isEmpty() && net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning
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

  public int getGroupId() {
    return this.entityData.get(GROUP_ID);
  }

  public void setGroupId(int groupId) {
    this.entityData.set(GROUP_ID, Math.max(0, groupId));
  }

  /**
   * 获取飞剑是否可被召回
   *
   * @return 是否可被召回
   */
  public boolean isRecallable() {
    return this.entityData.get(IS_RECALLABLE);
  }

  /**
   * 设置飞剑是否可被召回
   *
   * <p>主动技能生成的飞剑应设为false，避免被召回指令影响
   *
   * @param recallable 是否可被召回
   */
  public void setRecallable(boolean recallable) {
    this.entityData.set(IS_RECALLABLE, recallable);
  }

  // ========== Tick 逻辑 ==========
  @Override
  public void tick() {
    super.tick();
    age++;

    // 更新平滑朝向（客户端和服务端都需要）
    updateSmoothedLookAngle();

    if (this.level().isClientSide) {
      tickClient();
    } else {
      tickServer();
    }
  }

  /**
   * 更新平滑朝向向量，避免渲染时抖动
   */
  private void updateSmoothedLookAngle() {
    Vec3 currentVelocity = this.getDeltaMovement();

    // 如果速度太小，保持上次的朝向
    if (currentVelocity.lengthSqr() < 1.0e-4) {
      if (smoothedLookAngle.lengthSqr() < 1.0e-4) {
        smoothedLookAngle = this.getLookAngle();
      }
      return;
    }
    //当环绕轨迹切换东西方向时，目标速度向量会在一帧内接近 180° 翻转
    Vec3 targetLook = currentVelocity.normalize();

    // 如果是第一次，直接使用目标朝向
    if (smoothedLookAngle.lengthSqr() < 1.0e-4) {
      smoothedLookAngle = targetLook;
      lastVelocity = currentVelocity;
      return;
    }

    double smoothFactor = 0.3;

    smoothedLookAngle = slerpRobust(smoothedLookAngle, targetLook, smoothFactor);
    if (!isFinite(smoothedLookAngle) || smoothedLookAngle.lengthSqr() < 1.0e-12) {
      smoothedLookAngle = targetLook;
    }
    lastVelocity = currentVelocity;
  }

  /**
   * 球面线性插值（Slerp）
   *
   * @param from 起始向量（必须归一化）
   * @param to 目标向量（必须归一化）
   * @param t 插值系数 [0, 1]
   * @return 插值后的归一化向量
   */
  private static boolean isFinite(Vec3 vec) {
    return Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z);
  }

  private static Vec3 perpendicularUnit(Vec3 v) {
    Vec3 axis;
    double ax = Math.abs(v.x);
    double ay = Math.abs(v.y);
    double az = Math.abs(v.z);
    if (ax <= ay && ax <= az) {
      axis = new Vec3(1.0, 0.0, 0.0);
    } else if (ay <= az) {
      axis = new Vec3(0.0, 1.0, 0.0);
    } else {
      axis = new Vec3(0.0, 0.0, 1.0);
    }
    Vec3 perp = v.cross(axis);
    double len = perp.length();
    if (len < 1.0e-12) {
      axis = axis.x == 0.0 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      perp = v.cross(axis);
      len = perp.length();
    }
    return perp.scale(1.0 / Math.max(len, 1.0e-12));
  }

  private Vec3 slerpRobust(Vec3 fromRaw, Vec3 toRaw, double t) {
    Vec3 from = fromRaw.normalize();
    Vec3 to = toRaw.normalize();
    double dot = Mth.clamp(from.dot(to), -1.0, 1.0);

    if (dot > 0.9995) {
      Vec3 blended = from.scale(1.0 - t).add(to.scale(t));
      return blended.normalize();
    }

    if (dot < -0.9995) {
      // 对径点情况（180度翻转）：选择旋转轴
      Vec3 basis = this.antipodalSlerpBasis;
      if (basis == null || basis.lengthSqr() < 1.0e-12 || Math.abs(from.dot(basis)) > 0.999) {
        // 关键修复：检测旋转是否主要在水平面进行
        // 通过检查from和to的水平分量来判断
        double fromHorizontalSq = from.x * from.x + from.z * from.z;
        double toHorizontalSq = to.x * to.x + to.z * to.z;

        // 如果from和to都有显著的水平分量（>0.3），说明是水平面的旋转
        // 典型场景：环绕轨迹从东切换到西（即使有垂直分离力）
        if (fromHorizontalSq > 0.3 && toHorizontalSq > 0.3) {
          // 使用Y轴作为旋转轴，保持旋转在水平面
          basis = new Vec3(0, 1, 0);
          // Gram-Schmidt正交化：确保basis垂直于from
          double dotY = from.dot(basis);
          if (Math.abs(dotY) > 0.01) {
            basis = basis.subtract(from.scale(dotY));
            double len = basis.length();
            if (len > 0.01) {
              basis = basis.scale(1.0 / len);
            } else {
              // 如果from几乎垂直，使用水平方向作为basis
              basis = new Vec3(1, 0, 0);
            }
          }
        } else {
          // from或to主要是垂直的，使用perpendicularUnit选择合适的轴
          basis = perpendicularUnit(from);
        }
      }
      double angle = Math.PI * t;
      Vec3 rotated = from.scale(Math.cos(angle)).add(basis.scale(Math.sin(angle)));
      this.antipodalSlerpBasis = basis;
      return rotated.normalize();
    }

    this.antipodalSlerpBasis = null;
    double theta = Math.acos(dot);
    double sinTheta = Math.sin(theta);
    double ratioA = Math.sin((1.0 - t) * theta) / sinTheta;
    double ratioB = Math.sin(t * theta) / sinTheta;
    return from.scale(ratioA).add(to.scale(ratioB)).normalize();
  }

  /**
   * 获取平滑后的朝向向量（用于渲染）
   *
   * @return 平滑后的归一化朝向向量
   */
  public Vec3 getSmoothedLookAngle() {
    if (smoothedLookAngle.lengthSqr() < 1.0e-4) {
      Vec3 velocity = this.getDeltaMovement();
      if (velocity.lengthSqr() > 1.0e-4) {
        return velocity.normalize();
      }
      return this.getLookAngle();
    }
    return smoothedLookAngle;
  }

  private void tickClient() {
    // 客户端逻辑：主要在服务端生成粒子，客户端自动同步
    // 客户端特定的渲染逻辑在Renderer中处理
  }

  private void tickServer() {
    LivingEntity owner = getOwner();
    if (owner == null || !owner.isAlive()) {
      this.discard();
      return;
    }

    // 触发onTick事件钩子
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
        .TickContext tickCtx =
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
            .context.TickContext(
            this, (net.minecraft.server.level.ServerLevel) this.level(), owner, getAIMode(), this.tickCount);
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
        .FlyingSwordEventRegistry.fireTick(tickCtx);

    // Phase 2: 维持系统 (UpkeepSystem) - 集中管理资源消耗
    if (!tickCtx.skipUpkeep) {
      upkeepTicks = net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems
          .UpkeepSystem.tick(this, upkeepTicks);
      // 若维持不足，UpkeepSystem 会召回飞剑，此时实体已被移除
      if (this.isRemoved()) {
        return;
      }
    }

    // Phase 2: 运动系统 (MovementSystem) - 集中管理 AI 行为与速度计算
    if (!tickCtx.skipAI) {
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems
          .MovementSystem.tick(this, owner, getAIMode());
    }

    // Phase 2/4: 战斗系统 (CombatSystem) - 集中管理碰撞检测与伤害
    // Phase 4: 攻击冷却由 MultiCooldown 管理
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems
        .CombatSystem.tick(this);

    // 破块逻辑（速度分段 + 镐子可破范围，可被钩子跳过）
    if (!tickCtx.skipBlockBreak) {
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops.BlockBreakOps
          .tickBlockBreak(this);
    }

    // 粒子特效（每2 tick生成一次，减少性能消耗）
    if (this.tickCount % 2 == 0 && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
      spawnFlightParticles(serverLevel, getAIMode());
    }
  }

  // Phase 2: 维持/运动/战斗逻辑已迁移到 systems/ 目录
  // 原 tickAI() 方法已被 MovementSystem.tick() 替代

  /**
   * 生成飞行粒子特效
   */
  private void spawnFlightParticles(ServerLevel level, AIMode mode) {
    double speed = this.getDeltaMovement().length();

    // 根据AI模式显示不同的粒子
    switch (mode) {
      case ORBIT ->
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx
              .FlyingSwordFX.spawnOrbitTrail(level, this);
      case GUARD ->
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx
              .FlyingSwordFX.spawnGuardTrail(level, this);
      case HUNT ->
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx
              .FlyingSwordFX.spawnHuntTrail(level, this);
      case RECALL ->
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx
              .FlyingSwordFX.spawnRecallTrail(level, this);
    }

    // 高速飞行时的额外特效（SONIC_BOOM 音爆圆环）
    // 只在速度大于1时播放，避免低速时过于杂乱
    if (speed > 1.0) {
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.FlyingSwordFX
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
   * Phase 2: 应用转向行为 (委托给 MovementSystem)
   *
   * <p>保留此方法以兼容外部模块调用 (如 Swarm 集群管理器)
   */
  public void applySteeringVelocity(Vec3 desiredVelocity) {
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems
        .MovementSystem.applySteeringVelocity(this, desiredVelocity);
  }

  /**
   * Phase 2: 应用转向模板 (内部方法，已由 MovementSystem 使用)
   *
   * <p>保留此方法以供 MovementSystem 调用，避免破坏封装
   *
   * @deprecated Phase 2: 考虑在后续阶段移除或私有化
   */
  @Deprecated
  private void applySteeringTemplate(
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion
              .SteeringTemplate template,
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent
              .AIContext ctx,
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent
              .IntentResult intent) {
    var snapshot =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion
            .KinematicsSnapshot.capture(this);
    var command = template.compute(ctx, intent, snapshot);
    Vec3 newVelocity =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion
            .SteeringOps.computeNewVelocity(this, command, snapshot);
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
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
        .InteractContext interactCtx =
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
            .context.InteractContext(this, player, hand, isOwner);
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
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

    LivingEntity owner = getOwner();
    if (owner == null) {
      // 无主人：按基础规则消耗耐久，并与生命同步
      float duraLoss =
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator
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
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
        .HurtContext hurtCtx =
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
            .context.HurtContext(
            this, (net.minecraft.server.level.ServerLevel) this.level(), owner, source, amount);
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
        .FlyingSwordEventRegistry.fireHurt(hurtCtx);

    // 检查是否被钩子取消
    if (hurtCtx.cancelled) {
      return false;
    }

    // 使用钩子修改后的伤害
    amount = hurtCtx.damage;

    // 应用折返效果
    if (hurtCtx.triggerRetreat) {
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.impl
          .DefaultEventHooks.applyRetreat(this);
    }

    // 应用虚弱状态
    if (hurtCtx.triggerWeakened) {
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.impl
          .DefaultEventHooks.applyWeakened(this, hurtCtx.weakenedDuration);
    }

    // 耐久损耗（带上下文，应用流派经验等影响）
    float duraLoss =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator
            .FlyingSwordCalculator.calculateDurabilityLossWithContext(
                amount,
                attributes.duraLossRatio,
                false,
                net.tigereye.chestcavity.compat.guzhenren.flyingsword
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

    // 可召回标记
    if (tag.contains("IsRecallable")) {
      this.entityData.set(IS_RECALLABLE, tag.getBoolean("IsRecallable"));
    }

    if (tag.contains("GroupId")) {
      setGroupId(tag.getInt("GroupId"));
    }

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

    // 可召回标记
    tag.putBoolean("IsRecallable", isRecallable());

    tag.putInt("GroupId", getGroupId());
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
      LivingEntity owner,
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
      LivingEntity owner,
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
