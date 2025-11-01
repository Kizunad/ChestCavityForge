package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
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
    // 客户端逻辑：轨迹特效、渲染相关
    // 将在后续FX系统中实现
  }

  private void tickServer() {
    Player owner = getOwner();
    if (owner == null || !owner.isAlive()) {
      this.discard();
      return;
    }

    // 攻击冷却递减
    if (attackCooldown > 0) {
      attackCooldown--;
    }

    // 维持消耗检查
    upkeepTicks++;
    if (upkeepTicks >= FlyingSwordTuning.UPKEEP_CHECK_INTERVAL) {
      upkeepTicks = 0;
      if (!checkUpkeep()) {
        // 维持不足，召回
        recallToOwner();
        return;
      }
    }

    // AI行为逻辑
    tickAI();
  }

  private boolean checkUpkeep() {
    Player owner = getOwner();
    if (owner == null) return false;

    // 计算维持消耗
    double speedPercent = getCurrentSpeed() / attributes.speedMax;
    double upkeepRate = FlyingSwordCalculator.calculateUpkeep(
        attributes.upkeepRate,
        getAIMode(),
        owner.isSprinting(),
        false, // TODO: 破块状态
        speedPercent
    );

    // 转换为每秒消耗 -> 每interval消耗
    double upkeepCost = upkeepRate * (FlyingSwordTuning.UPKEEP_CHECK_INTERVAL / 20.0);

    // 尝试消耗真元
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(owner);
    if (handleOpt.isEmpty()) {
      return false;
    }

    return handleOpt.get().consumeScaledZhenyuan(upkeepCost).isPresent();
  }

  private void tickAI() {
    Player owner = getOwner();
    if (owner == null) {
      return;
    }

    AIMode mode = getAIMode();

    switch (mode) {
      case ORBIT -> tickOrbitMode(owner);
      case GUARD -> tickGuardMode(owner);
      case HUNT -> tickHuntMode(owner);
    }

    // 检测碰撞攻击
    tickCollisionAttack();

    // 更新当前速度
    setCurrentSpeed((float) this.getDeltaMovement().length());
  }

  /**
   * 环绕模式：绕着主人旋转
   */
  private void tickOrbitMode(Player owner) {
    Vec3 ownerPos = owner.getEyePosition();
    Vec3 currentPos = this.position();
    Vec3 toOwner = ownerPos.subtract(currentPos);
    double distance = toOwner.length();

    // 目标距离：3格
    double targetDistance = 3.0;
    double distanceTolerance = 0.5;

    Vec3 desiredVelocity;

    if (distance > targetDistance + distanceTolerance) {
      // 太远，向主人移动
      Vec3 direction = toOwner.normalize();
      desiredVelocity = direction.scale(attributes.speedBase);
    } else if (distance < targetDistance - distanceTolerance) {
      // 太近，远离主人
      Vec3 direction = toOwner.normalize().scale(-1);
      desiredVelocity = direction.scale(attributes.speedBase);
    } else {
      // 距离合适，绕圈飞行
      // 使用垂直于toOwner的切线方向
      Vec3 tangent = new Vec3(-toOwner.z, 0, toOwner.x).normalize();
      // 添加轻微的向内偏移以维持轨道
      Vec3 radial = toOwner.normalize().scale(-0.1);
      desiredVelocity = tangent.add(radial).normalize().scale(attributes.speedBase * 0.8);
    }

    // 应用转向
    applySteeringVelocity(desiredVelocity);
  }

  /**
   * 防守模式：跟随主人并攻击附近的敌对实体
   */
  private void tickGuardMode(Player owner) {
    // 搜索附近的敌对实体
    LivingEntity target = findNearestHostile(owner.position(), 12.0);

    if (target != null) {
      // 调试：显示找到目标
      if (this.tickCount % 20 == 0) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            "[FlyingSword] GUARD mode: Found target {}, distance: {}",
            target.getName().getString(),
            String.format("%.2f", this.distanceTo(target)));
      }

      setTargetEntity(target);
      // 追击目标
      Vec3 targetPos = target.getEyePosition();
      Vec3 direction = targetPos.subtract(this.position()).normalize();
      Vec3 desiredVelocity = direction.scale(attributes.speedMax * 0.9);
      applySteeringVelocity(desiredVelocity);
    } else {
      setTargetEntity(null);
      // 跟随主人
      Vec3 ownerPos = owner.getEyePosition();
      Vec3 toOwner = ownerPos.subtract(this.position());
      double distance = toOwner.length();

      if (distance > 2.0) {
        // 向主人移动
        Vec3 desiredVelocity = toOwner.normalize().scale(attributes.speedBase * 1.2);
        applySteeringVelocity(desiredVelocity);
      } else {
        // 在主人附近缓慢环绕
        Vec3 tangent = new Vec3(-toOwner.z, 0, toOwner.x).normalize();
        Vec3 desiredVelocity = tangent.scale(attributes.speedBase * 0.4);
        applySteeringVelocity(desiredVelocity);
      }
    }
  }

  /**
   * 出击模式：主动搜索并攻击敌对实体
   */
  private void tickHuntMode(Player owner) {
    LivingEntity currentTarget = getTargetEntity();

    // 检查当前目标是否有效
    if (currentTarget != null && currentTarget.isAlive() && this.distanceTo(currentTarget) < 32.0) {
      // 调试：显示追击
      if (this.tickCount % 20 == 0) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            "[FlyingSword] HUNT mode: Chasing target {}, distance: {}",
            currentTarget.getName().getString(),
            String.format("%.2f", this.distanceTo(currentTarget)));
      }

      // 继续追击当前目标
      Vec3 targetPos = currentTarget.getEyePosition();
      Vec3 direction = targetPos.subtract(this.position()).normalize();
      Vec3 desiredVelocity = direction.scale(attributes.speedMax);
      applySteeringVelocity(desiredVelocity);
    } else {
      // 搜索新目标
      LivingEntity newTarget = findNearestHostile(this.position(), 24.0);

      if (newTarget != null) {
        // 调试：显示找到新目标
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            "[FlyingSword] HUNT mode: Found NEW target {}, distance: {}",
            newTarget.getName().getString(),
            String.format("%.2f", this.distanceTo(newTarget)));

        setTargetEntity(newTarget);
        Vec3 targetPos = newTarget.getEyePosition();
        Vec3 direction = targetPos.subtract(this.position()).normalize();
        Vec3 desiredVelocity = direction.scale(attributes.speedMax);
        applySteeringVelocity(desiredVelocity);
      } else {
        // 没有目标，回到主人身边
        setTargetEntity(null);
        Vec3 ownerPos = owner.getEyePosition();
        Vec3 toOwner = ownerPos.subtract(this.position());
        double distance = toOwner.length();

        if (distance > 4.0) {
          Vec3 desiredVelocity = toOwner.normalize().scale(attributes.speedBase);
          applySteeringVelocity(desiredVelocity);
        } else {
          // 在主人附近缓慢巡逻
          Vec3 tangent = new Vec3(-toOwner.z, 0, toOwner.x).normalize();
          Vec3 desiredVelocity = tangent.scale(attributes.speedBase * 0.5);
          applySteeringVelocity(desiredVelocity);
        }
      }
    }
  }

  /**
   * 搜索最近的敌对实体
   */
  @Nullable
  private LivingEntity findNearestHostile(Vec3 center, double range) {
    if (!(this.level() instanceof ServerLevel server)) {
      return null;
    }

    Player owner = getOwner();
    if (owner == null) {
      return null;
    }

    net.minecraft.world.phys.AABB searchBox =
        new net.minecraft.world.phys.AABB(center, center).inflate(range);

    LivingEntity nearest = null;
    double minDistSq = range * range;

    for (net.minecraft.world.entity.Entity entity : server.getEntities(null, searchBox)) {
      if (!(entity instanceof LivingEntity living)) {
        continue;
      }

      // 排除主人、其他玩家、自己
      if (living == owner || living instanceof Player || living == this) {
        continue;
      }

      // 排除已死亡的实体
      if (!living.isAlive()) {
        continue;
      }

      boolean isHostile = false;

      // 检查是否敌对
      if (living instanceof Mob mob) {
        // 正在攻击主人的怪物 - 最高优先级
        if (mob.getTarget() == owner) {
          isHostile = true;
        }
        // 或者是怪物类别的Mob
        else if (mob.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
          isHostile = true;
        }
      }
      // 非Mob但是怪物类别的生物（如末影龙、凋灵）
      else if (living.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
        isHostile = true;
      }

      if (isHostile) {
        double distSq = living.distanceToSqr(center);
        if (distSq < minDistSq) {
          minDistSq = distSq;
          nearest = living;
        }
      }
    }

    return nearest;
  }

  /**
   * 应用转向行为
   */
  private void applySteeringVelocity(Vec3 desiredVelocity) {
    Vec3 currentVelocity = this.getDeltaMovement();

    // 计算转向力
    Vec3 steering = desiredVelocity.subtract(currentVelocity);

    // 限制转向速率
    double steeringMag = steering.length();
    if (steeringMag > attributes.turnRate) {
      steering = steering.normalize().scale(attributes.turnRate);
    }

    // 应用转向
    Vec3 newVelocity = currentVelocity.add(steering);

    // 限制速度
    double speed = newVelocity.length();
    if (speed > attributes.speedMax) {
      newVelocity = newVelocity.normalize().scale(attributes.speedMax);
    }

    this.setDeltaMovement(newVelocity);
  }

  /**
   * 检测碰撞攻击
   */
  private void tickCollisionAttack() {
    if (!(this.level() instanceof ServerLevel server)) {
      return;
    }

    // 检查攻击冷却
    if (attackCooldown > 0) {
      return;
    }

    LivingEntity target = getTargetEntity();

    // 调试：显示目标状态
    if (this.tickCount % 20 == 0) { // 每秒显示一次
      net.tigereye.chestcavity.ChestCavity.LOGGER.info(
          "[FlyingSword] Tick collision check: target={}, cooldown={}",
          target != null ? target.getName().getString() : "NULL",
          attackCooldown);
    }

    if (target == null || !target.isAlive()) {
      return;
    }

    // 检测碰撞 - 使用更宽松的范围
    double distance = this.distanceTo(target);
    double attackRange = 1.5; // 增加攻击范围

    // 调试信息
    if (distance <= attackRange) {
      net.tigereye.chestcavity.ChestCavity.LOGGER.info(
          "[FlyingSword] Collision detected! Distance: {}, attempting attack...",
          String.format("%.2f", distance));
      boolean success = attackTarget(target);
      if (!success) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.warn(
            "[FlyingSword] Attack returned false! Target: {}, Health: {}",
            target.getName().getString(),
            String.format("%.1f", target.getHealth()));
      }
    }
  }

  // ========== 交互逻辑 ==========
  @Override
  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    if (this.level().isClientSide) {
      return InteractionResult.SUCCESS;
    }

    // 右键召回
    if (isOwnedBy(player)) {
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

  // ========== 伤害处理 ==========
  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (this.level().isClientSide) {
      return false;
    }

    // 耐久损耗
    float duraLoss = FlyingSwordCalculator.calculateDurabilityLoss(
        amount,
        attributes.duraLossRatio,
        false
    );
    damageDurability(duraLoss);

    return super.hurt(source, amount);
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

    // Age
    this.age = tag.getInt("Age");
    this.upkeepTicks = tag.getInt("UpkeepTicks");
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);

    // Owner
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
  }

  // ========== 攻击逻辑 ==========
  /**
   * 对目标造成速度²伤害
   */
  public boolean attackTarget(LivingEntity target) {
    if (this.level().isClientSide || target == null || !target.isAlive()) {
      return false;
    }

    Player owner = getOwner();
    if (owner == null) {
      return false;
    }

    // 计算伤害
    Vec3 velocity = this.getDeltaMovement();
    double speed = velocity.length();
    int level = getSwordLevel();
    double levelScale = FlyingSwordCalculator.calculateLevelScale(
        level,
        FlyingSwordTuning.DAMAGE_PER_LEVEL
    );

    double damage = FlyingSwordCalculator.calculateDamage(
        attributes.damageBase,
        speed,
        FlyingSwordTuning.V_REF,
        attributes.velDmgCoef,
        levelScale
    );

    // 调试信息（总是输出到日志）
    net.tigereye.chestcavity.ChestCavity.LOGGER.info(
        "[FlyingSword] Attack START: target={}, speed={}, damage={}, baseDmg={}, vRef={}, velCoef={}, levelScale={}",
        target.getName().getString(),
        String.format("%.3f", speed),
        String.format("%.2f", damage),
        attributes.damageBase,
        FlyingSwordTuning.V_REF,
        attributes.velDmgCoef,
        String.format("%.2f", levelScale));

    // 检查目标状态
    net.tigereye.chestcavity.ChestCavity.LOGGER.info(
        "[FlyingSword] Target status: invulnerableTime={}, health={}/{}, isInvulnerableTo(playerAttack)={}",
        target.invulnerableTime,
        String.format("%.1f", target.getHealth()),
        String.format("%.1f", target.getMaxHealth()),
        target.isInvulnerableTo(this.damageSources().playerAttack(owner)));

    // 造成伤害
    DamageSource damageSource = this.damageSources().playerAttack(owner);
    boolean success = target.hurt(damageSource, (float) damage);

    net.tigereye.chestcavity.ChestCavity.LOGGER.info(
        "[FlyingSword] Attack result: success={}, targetHealthAfter={}",
        success,
        String.format("%.1f", target.getHealth()));

    // 设置攻击冷却 (10 ticks = 0.5秒)
    attackCooldown = 10;

    if (success) {
      // 调试信息
      net.tigereye.chestcavity.ChestCavity.LOGGER.debug(
          "[FlyingSword] Hit success! Target health: {}/{}",
          String.format("%.1f", target.getHealth()),
          String.format("%.1f", target.getMaxHealth()));

      // 耐久损耗
      float duraLoss = FlyingSwordCalculator.calculateDurabilityLoss(
          (float) damage,
          attributes.duraLossRatio,
          false
      );
      damageDurability(duraLoss);

      // 经验获取
      boolean isKill = !target.isAlive();
      boolean isElite = false; // TODO: 判断精英怪
      int expGain = FlyingSwordCalculator.calculateExpGain(
          damage,
          isKill,
          isElite,
          1.0 // TODO: 经验倍率
      );
      addExperience(expGain);

      // 击杀提示
      if (isKill) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            "[FlyingSword] Killed {}! Gained {} exp",
            target.getName().getString(),
            expGain);
      }
    } else {
      // 调试信息
      net.tigereye.chestcavity.ChestCavity.LOGGER.debug(
          "[FlyingSword] Hit failed! Target may be invulnerable or damage was 0");
    }

    return success;
  }

  // ========== 工厂方法 ==========
  /**
   * 创建飞剑实体
   */
  public static FlyingSwordEntity create(
      ServerLevel level,
      Player owner,
      Vec3 spawnPos,
      @Nullable FlyingSwordAttributes.AttributeModifiers modifiers) {

    EntityType<FlyingSwordEntity> type =
        net.tigereye.chestcavity.registration.CCEntities.FLYING_SWORD.get();
    FlyingSwordEntity sword = type.create(level);

    if (sword == null) {
      return null;
    }

    // 设置位置和主人
    sword.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, owner.getYRot(), 0);
    sword.setOwner(owner);

    // 应用释放继承修正
    if (modifiers != null) {
      sword.getSwordAttributes().applyModifiers(modifiers);
    }

    return sword;
  }
}
