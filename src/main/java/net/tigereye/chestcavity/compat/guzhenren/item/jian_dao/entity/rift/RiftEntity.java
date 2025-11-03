package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.rift;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.RiftFx;

/**
 * 裂隙实体（Rift Entity）
 *
 * <p>裂剑蛊的核心实体，表示一个空间裂隙。
 *
 * <p>功能：
 * <ul>
 *   <li>周期性穿刺伤害（每秒1次）</li>
 *   <li>随时间衰减（主裂隙）或固定伤害（微型裂隙）</li>
 *   <li>共鸣激活（接收剑气触发）</li>
 *   <li>吸纳微型裂隙（主裂隙）</li>
 *   <li>支持剑域加成</li>
 * </ul>
 */
public class RiftEntity extends Entity {

  // ========== SynchedEntityData ==========
  private static final EntityDataAccessor<Integer> RIFT_TYPE =
      SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);

  private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
      SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.OPTIONAL_UUID);

  private static final EntityDataAccessor<Float> DAMAGE_MULTIPLIER =
      SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.FLOAT);

  private static final EntityDataAccessor<Integer> REMAINING_TICKS =
      SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);

  // ========== 字段 ==========
  @Nullable private LivingEntity cachedOwner;

  private int age = 0; // 总存活时间（tick）
  private int lastDamageTick = 0; // 上次造成伤害的tick
  private int decayCount = 0; // 衰减次数
  private boolean inDomain = false; // 是否在剑域内（影响衰减速率）
  private int resonanceDecayMultiplier = 1; // 共鸣衰减倍数（每次共鸣后×2）
  private long nextDecayGameTick = -1L; // 下一次衰减的世界时间（tick）

  // ========== 构造函数 ==========
  public RiftEntity(EntityType<? extends RiftEntity> type, Level level) {
    super(type, level);
    this.noPhysics = true; // 无碰撞
    this.noCulling = true; // 总是渲染
  }

  /**
   * 工厂方法：创建裂隙
   *
   * @param level 世界
   * @param pos 位置
   * @param owner 所有者
   * @param type 裂隙类型
   * @param extraDuration 额外持续时间（tick）
   * @return 裂隙实体
   */
  public static RiftEntity create(
      ServerLevel level,
      Vec3 pos,
      @Nullable LivingEntity owner,
      RiftType type,
      int extraDuration) {
    RiftEntity rift = new RiftEntity(
        net.tigereye.chestcavity.registration.CCEntities.RIFT.get(), level);

    rift.setPos(pos);
    rift.setRiftType(type);
    if (owner != null) {
      rift.setOwner(owner);
    }

    // 设置初始伤害倍率
    rift.setDamageMultiplier((float) type.initialDamageMultiplier);

    // 设置持续时间
    int totalDuration = type.baseDuration + extraDuration;
    rift.setRemainingTicks(totalDuration);

    // 计划第一次衰减时间（仅主裂隙需要衰减）
    if (type == RiftType.MAJOR && type.decayInterval > 0) {
      rift.nextDecayGameTick = level.getGameTime() + type.decayInterval;
    }

    return rift;
  }

  // ========== Entity覆写 ==========
  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(RIFT_TYPE, RiftType.MAJOR.ordinal());
    builder.define(OWNER_UUID, Optional.empty());
    builder.define(DAMAGE_MULTIPLIER, 1.0f);
    builder.define(REMAINING_TICKS, 0);
  }

  @Override
  public void tick() {
    super.tick();

    if (level().isClientSide) {
      // 客户端：粒子效果
      spawnParticles();
      return;
    }

    // 服务端逻辑
    ServerLevel serverLevel = (ServerLevel) level();

    // 首次tick播放生成特效
    if (age == 0) {
      RiftFx.spawnFx(serverLevel, position(), getRiftType() == RiftType.MAJOR);
    }

    age++;

    // 环境粒子（定期播放）
    RiftFx.ambientFx(this);

    // 检查是否超时
    int remaining = getRemainingTicks();
    if (remaining <= 0) {
      discard();
      return;
    }
    setRemainingTicks(remaining - 1);

    // 每20 ticks执行一次逻辑
    if (age % 20 == 0) {
      onSlowTick(serverLevel);
    }
  }

  @Override
  public void remove(RemovalReason reason) {
    // 播放消失特效
    if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
      RiftFx.despawnFx(serverLevel, position(), getRiftType() == RiftType.MAJOR);

      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.rift.RiftManager
          .getInstance()
          .unregisterRift(this);
    }
    super.remove(reason);
  }

  /**
   * 慢速Tick（每秒1次）
   */
  private void onSlowTick(ServerLevel level) {
    // 1. 穿刺伤害
    dealPierceDamage(level);

    // 2. 检查衰减
    RiftType type = getRiftType();
    if (type == RiftType.MAJOR && type.decayInterval > 0) {
      long now = level.getGameTime();

      // 计算有效衰减间隔：域内减半速率 => 间隔×2；每次共鸣后加倍速率 => 间隔/倍数
      int domainFactor = inDomain ? 2 : 1;
      int effectiveInterval = Math.max(20, (type.decayInterval * domainFactor) / Math.max(1, resonanceDecayMultiplier));

      if (nextDecayGameTick < 0) {
        nextDecayGameTick = now + effectiveInterval;
      }

      if (now >= nextDecayGameTick) {
        applyDecay();
        decayCount++;
        nextDecayGameTick = now + effectiveInterval;
      }
    }

    // 3. 检查吸纳附近的微型裂隙
    if (type.canAbsorb) {
      absorbNearbyMinorRifts(level);
    }
  }

  /**
   * 造成穿刺伤害
   */
  private void dealPierceDamage(ServerLevel level) {
    RiftType type = getRiftType();
    double radius = type.damageRadius;

    // 获取范围内的所有生物
    AABB box =
        new AABB(
            position().add(-radius, -1, -radius), position().add(radius, type.displayHeight, radius));
    List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box);

    LivingEntity owner = getOwner();
    float damageMultiplier = getDamageMultiplier();

    // 道痕增幅：按裂隙类型应用独立倍率（不随衰减重置）
    double daoHen = 0.0;
    if (owner != null) {
      daoHen =
          net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps
              .openHandle(owner)
              .map(
                  h ->
                      net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps.get(
                          h, "daohen_jiandao"))
              .orElse(0.0);
    }
    double per10k =
        (type == RiftType.MAJOR)
            ? net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.RiftTuning
                .DAMAGE_PER_10K_MAJOR
            : net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.RiftTuning
                .DAMAGE_PER_10K_MINOR;
    double daoHenScale = 1.0 + (daoHen / 10000.0) * per10k;

    for (LivingEntity target : targets) {
      // 跳过所有者
      if (target == owner) {
        continue;
      }

      // 计算基础伤害（TODO: 根据实际设计调整）
      float baseDamage =
          (type == RiftType.MAJOR)
              ? net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.RiftTuning
                  .MAJOR_PIERCE_BASE_DAMAGE
              : net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.RiftTuning
                  .MINOR_PIERCE_BASE_DAMAGE;

      // 多裂隙共鸣链增益（每个+10%）
      double chainBonus =
          net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.rift.RiftManager
              .getInstance()
              .getResonanceChainBonus(this);

      float finalDamage = (float) (baseDamage * damageMultiplier * chainBonus * daoHenScale);

      // 造成伤害
      DamageSource damageSource;
      if (owner != null) {
        damageSource = level.damageSources().indirectMagic(this, owner);
      } else {
        damageSource = level.damageSources().magic();
      }

      target.hurt(damageSource, finalDamage);

      // 伤害特效（每次命中播放）
      RiftFx.damageFx(level, target.position());
    }

    lastDamageTick = age;
  }

  /**
   * 应用伤害衰减
   */
  private void applyDecay() {
    RiftType type = getRiftType();
    float current = getDamageMultiplier();
    float newMultiplier = current - (float) type.decayStep;

    // 限制最低值
    if (newMultiplier < type.minDamageMultiplier) {
      newMultiplier = (float) type.minDamageMultiplier;
    }

    setDamageMultiplier(newMultiplier);
  }

  /**
   * 吸纳附近的微型裂隙
   */
  private void absorbNearbyMinorRifts(ServerLevel level) {
    double absorpRadius = 3.0; // 吸纳范围3格
    AABB box =
        new AABB(
            position().add(-absorpRadius, -2, -absorpRadius),
            position().add(absorpRadius, 4, absorpRadius));

    List<RiftEntity> nearbyRifts =
        level.getEntitiesOfClass(
            RiftEntity.class,
            box,
            rift -> rift != this && rift.getRiftType() == RiftType.MINOR && rift.isAlive());

    for (RiftEntity minorRift : nearbyRifts) {
      absorbRift(minorRift);
    }
  }

  /**
   * 吸纳一个微型裂隙
   *
   * @param minorRift 微型裂隙
   */
  public void absorbRift(RiftEntity minorRift) {
    if (minorRift.getRiftType() != RiftType.MINOR) {
      return;
    }

    // 效果1：延长若干秒
    int remaining = getRemainingTicks();
    setRemainingTicks(
        remaining
            + net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.RiftTuning
                .ABSORB_ADD_SECONDS
                * 20);

    // 效果2：伤害倍率上调（上限1.0）
    float current = getDamageMultiplier();
    float boosted =
        (float)
            Math.min(
                1.0,
                current
                    + net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.RiftTuning
                        .ABSORB_DAMAGE_BOOST);
    setDamageMultiplier(boosted);

    // 吸收特效
    if (level() instanceof ServerLevel serverLevel) {
      RiftFx.absorbFx(serverLevel, position(), minorRift.position());
    }

    // 移除微型裂隙
    minorRift.discard();
  }

  /**
   * 触发共鸣
   *
   * <p>由外部调用（剑气攻击触发）
   */
  public void triggerResonance() {
    // 共鸣后，衰减速率加倍
    resonanceDecayMultiplier *= 2;

    // 共鸣特效
    if (level() instanceof ServerLevel serverLevel) {
      RiftFx.resonanceFx(serverLevel, position());
    }
  }

  /**
   * 设置是否在剑域内
   */
  public void setInDomain(boolean inDomain) {
    this.inDomain = inDomain;
  }

  // ========== 粒子效果 ==========
  private void spawnParticles() {
    // 客户端粒子效果
    if (random.nextInt(4) == 0) {
      RiftType type = getRiftType();
      double offsetX = (random.nextDouble() - 0.5) * type.displayWidth;
      double offsetY = random.nextDouble() * type.displayHeight;
      double offsetZ = (random.nextDouble() - 0.5) * type.displayWidth * 0.1;

      level()
          .addParticle(
              ParticleTypes.PORTAL,
              getX() + offsetX,
              getY() + offsetY,
              getZ() + offsetZ,
              0,
              0,
              0);
    }
  }

  // ========== Getter/Setter ==========
  public RiftType getRiftType() {
    return RiftType.values()[entityData.get(RIFT_TYPE)];
  }

  public void setRiftType(RiftType type) {
    entityData.set(RIFT_TYPE, type.ordinal());
  }

  @Nullable
  public LivingEntity getOwner() {
    if (cachedOwner != null && !cachedOwner.isRemoved()) {
      return cachedOwner;
    }

    Optional<UUID> uuid = entityData.get(OWNER_UUID);
    if (uuid.isEmpty()) {
      return null;
    }

    if (level() instanceof ServerLevel serverLevel) {
      Entity entity = serverLevel.getEntity(uuid.get());
      if (entity instanceof LivingEntity living) {
        cachedOwner = living;
        return living;
      }
    }

    return null;
  }

  public void setOwner(@Nullable LivingEntity owner) {
    if (owner != null) {
      entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
      cachedOwner = owner;
    } else {
      entityData.set(OWNER_UUID, Optional.empty());
      cachedOwner = null;
    }
  }

  public float getDamageMultiplier() {
    return entityData.get(DAMAGE_MULTIPLIER);
  }

  public void setDamageMultiplier(float multiplier) {
    entityData.set(DAMAGE_MULTIPLIER, multiplier);
  }

  public int getRemainingTicks() {
    return entityData.get(REMAINING_TICKS);
  }

  public void setRemainingTicks(int ticks) {
    entityData.set(REMAINING_TICKS, ticks);
  }

  public int getAge() {
    return age;
  }

  // ========== NBT序列化 ==========
  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    if (tag.contains("RiftType")) {
      setRiftType(RiftType.values()[tag.getInt("RiftType")]);
    }
    if (tag.hasUUID("Owner")) {
      UUID ownerUUID = tag.getUUID("Owner");
      entityData.set(OWNER_UUID, Optional.of(ownerUUID));
    }
    if (tag.contains("DamageMultiplier")) {
      setDamageMultiplier(tag.getFloat("DamageMultiplier"));
    }
    if (tag.contains("RemainingTicks")) {
      setRemainingTicks(tag.getInt("RemainingTicks"));
    }
    if (tag.contains("Age")) {
      age = tag.getInt("Age");
    }
    if (tag.contains("DecayCount")) {
      decayCount = tag.getInt("DecayCount");
    }
    if (tag.contains("ResonanceDecayMultiplier")) {
      resonanceDecayMultiplier = tag.getInt("ResonanceDecayMultiplier");
    }
    if (tag.contains("NextDecayAt")) {
      nextDecayGameTick = tag.getLong("NextDecayAt");
    }
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    tag.putInt("RiftType", getRiftType().ordinal());

    Optional<UUID> ownerUUID = entityData.get(OWNER_UUID);
    if (ownerUUID.isPresent()) {
      tag.putUUID("Owner", ownerUUID.get());
    }

    tag.putFloat("DamageMultiplier", getDamageMultiplier());
    tag.putInt("RemainingTicks", getRemainingTicks());
    tag.putInt("Age", age);
    tag.putInt("DecayCount", decayCount);
    tag.putInt("ResonanceDecayMultiplier", resonanceDecayMultiplier);
    tag.putLong("NextDecayAt", nextDecayGameTick);
  }
}
