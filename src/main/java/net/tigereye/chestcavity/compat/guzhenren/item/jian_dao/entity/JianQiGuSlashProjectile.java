package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianQiGuCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianQiGuTuning;
import org.jetbrains.annotations.Nullable;

/**
 * 剑气蛊剑光投射物实体。
 *
 * <p>特性：
 * <ul>
 *   <li>直线高速前进</li>
 *   <li>命中实体造成伤害，每次命中后威能衰减</li>
 *   <li>可破坏部分方块（硬度限制）</li>
 *   <li>视觉威能同步到客户端用于动态渲染</li>
 * </ul>
 */
public class JianQiGuSlashProjectile extends Entity {

  // ========== 数据同步字段 ==========

  /** 视觉威能（归一化到0-1，用于客户端渲染） */
  private static final EntityDataAccessor<Float> DATA_VISUAL_POWER =
      SynchedEntityData.defineId(JianQiGuSlashProjectile.class, EntityDataSerializers.FLOAT);

  // ========== 服务端字段 ==========

  /** 初始威能 */
  private double initialPower;

  /** 当前威能 */
  private double currentPower;

  /** 移动方向 */
  private Vec3 direction = Vec3.ZERO;

  /** 拥有者UUID */
  @Nullable
  private UUID ownerId;

  /** 拥有者缓存 */
  @Nullable
  private LivingEntity cachedOwner;

  /** 已造成伤害的实体（防止重复命中） */
  private final Set<UUID> damagedEntities = new HashSet<>();

  /** 命中次数（用于衰减计算） */
  private int hitCount = 0;

  /** 衰减豁免次数（断势提供） */
  private int decayGrace = 0;

  /** 存活时间（tick） */
  private int age = 0;

  /** 最大存活时间（tick） */
  private int maxAge = 0;

  /** 可破坏的方块白名单 */
  private static final Set<Block> BREAKABLE_BLOCKS =
      Set.of(
          Blocks.GRASS_BLOCK,
          Blocks.DIRT,
          Blocks.COARSE_DIRT,
          Blocks.SAND,
          Blocks.RED_SAND,
          Blocks.GLASS,
          Blocks.GLASS_PANE,
          Blocks.ICE,
          Blocks.SNOW_BLOCK,
          Blocks.TALL_GRASS,
          Blocks.FERN,
          Blocks.DEAD_BUSH);

  public JianQiGuSlashProjectile(EntityType<? extends JianQiGuSlashProjectile> type, Level level) {
    super(type, level);
    this.noPhysics = true;
    this.noCulling = true;
    this.setNoGravity(true);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(DATA_VISUAL_POWER, 1.0f);
  }

  /**
   * 初始化剑气斩击。
   *
   * @param owner 施法者
   * @param position 起始位置
   * @param direction 移动方向
   * @param initialPower 初始威能
   * @param decayGrace 衰减豁免次数
   */
  public void initialize(
      @Nullable LivingEntity owner,
      Vec3 position,
      Vec3 direction,
      double initialPower,
      int decayGrace) {

    if (owner != null) {
      this.cachedOwner = owner;
      this.ownerId = owner.getUUID();
    }

    this.setPos(position.x, position.y, position.z);
    this.direction = direction.normalize();
    this.initialPower = initialPower;
    this.currentPower = initialPower;
    this.decayGrace = decayGrace;

    // 计算最大存活时间（基于射程和速度）
    this.maxAge = (int) Math.ceil(JianQiGuTuning.MAX_RANGE / JianQiGuTuning.SLASH_SPEED);

    // 更新朝向
    updateRotationFromDirection();

    // 同步视觉威能
    updateVisualPower();
  }

  @Override
  public void tick() {
    super.tick();

    // 增加年龄
    this.age++;

    // 超过最大存活时间则销毁
    if (this.age >= this.maxAge) {
      if (!this.level().isClientSide) {
        this.discard();
      }
      return;
    }

    // 计算移动向量
    Vec3 motion = this.direction.scale(JianQiGuTuning.SLASH_SPEED);
    Vec3 currentPos = this.position();
    Vec3 nextPos = currentPos.add(motion);

    // 服务端处理碰撞
    if (!this.level().isClientSide) {
      handleEntityHits(currentPos, nextPos);
      handleBlockBreaks(currentPos, nextPos);

      // 检查是否应该终止
      if (JianQiGuCalc.shouldTerminate(
          this.currentPower, this.initialPower, JianQiGuTuning.MIN_DAMAGE_RATIO)) {
        this.discard();
        return;
      }
    }

    // 移动
    this.move(MoverType.SELF, motion);
  }

  /**
   * 处理实体命中。
   *
   * @param start 起始位置
   * @param end 结束位置
   */
  private void handleEntityHits(Vec3 start, Vec3 end) {
    if (!(this.level() instanceof ServerLevel server)) {
      return;
    }

    // 构建扫描盒子
    AABB sweep = new AABB(start, end).inflate(JianQiGuTuning.SLASH_WIDTH * 0.5);

    // 查找范围内的生物
    for (LivingEntity target :
        server.getEntitiesOfClass(LivingEntity.class, sweep, this::canHitEntity)) {

      // 防止重复命中
      if (damagedEntities.add(target.getUUID())) {
        applyDamage(target);
        this.hitCount++;

        // 计算衰减后的威能
        this.currentPower =
            JianQiGuCalc.computeDamageAfterHit(
                this.initialPower, this.hitCount, JianQiGuTuning.DECAY_RATE, this.decayGrace);

        // 更新视觉威能
        updateVisualPower();
      }
    }
  }

  /**
   * 判断是否可以命中目标实体。
   *
   * @param entity 目标实体
   * @return 是否可以命中
   */
  private boolean canHitEntity(LivingEntity entity) {
    if (!entity.isAlive()) {
      return false;
    }

    LivingEntity owner = getOwner();
    if (owner != null) {
      if (entity == owner) {
        return false;
      }
      if (entity.isAlliedTo(owner)) {
        return false;
      }
    }

    return true;
  }

  /**
   * 对目标造成伤害。
   *
   * @param target 目标实体
   */
  private void applyDamage(LivingEntity target) {
    LivingEntity owner = getOwner();

    // 构建伤害源
    DamageSource source;
    if (owner instanceof Player player) {
      source = this.damageSources().playerAttack(player);
    } else if (owner != null) {
      source = this.damageSources().mobAttack(owner);
    } else {
      source = this.damageSources().magic();
    }

    // 造成伤害
    float amount = (float) Math.max(0.0, this.currentPower);
    if (amount > 0.0f) {
      boolean hurt = target.hurt(source, amount);

      // 击退效果
      if (hurt && this.direction.lengthSqr() > 1.0E-6) {
        Vec3 push = this.direction.normalize();
        target.push(push.x * 0.4, 0.15 + Math.abs(push.y) * 0.2, push.z * 0.4);
        target.hurtMarked = true;
      }
    }
  }

  /**
   * 处理方块破坏。
   *
   * @param start 起始位置
   * @param end 结束位置
   */
  private void handleBlockBreaks(Vec3 start, Vec3 end) {
    if (!(this.level() instanceof ServerLevel server)) {
      return;
    }

    // 检查mobGriefing规则
    LivingEntity owner = getOwner();
    boolean isPlayer = owner instanceof Player;
    if (!isPlayer && !server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
      return;
    }

    // 构建扫描盒子
    AABB box = new AABB(start, end).inflate(JianQiGuTuning.SLASH_WIDTH * 0.5);

    int minX = Mth.floor(box.minX);
    int minY = Mth.floor(box.minY);
    int minZ = Mth.floor(box.minZ);
    int maxX = Mth.floor(box.maxX);
    int maxY = Mth.floor(box.maxY);
    int maxZ = Mth.floor(box.maxZ);

    int brokenCount = 0;

    for (int x = minX; x <= maxX && brokenCount < JianQiGuTuning.BLOCK_BREAK_CAP_PER_TICK; x++) {
      for (int y = minY; y <= maxY && brokenCount < JianQiGuTuning.BLOCK_BREAK_CAP_PER_TICK; y++) {
        for (int z = minZ; z <= maxZ && brokenCount < JianQiGuTuning.BLOCK_BREAK_CAP_PER_TICK;
            z++) {
          BlockPos pos = new BlockPos(x, y, z);
          BlockState state = server.getBlockState(pos);

          if (state.isAir()) {
            continue;
          }

          if (!isBreakableBlock(state)) {
            continue;
          }

          float hardness = state.getDestroySpeed(server, pos);
          if (hardness < 0.0f || hardness > JianQiGuTuning.BLOCK_BREAK_HARDNESS_MAX) {
            continue;
          }

          // 破坏方块
          boolean destroyed = server.destroyBlock(pos, true);
          if (destroyed) {
            brokenCount++;

            // 额外衰减
            this.currentPower *= (1.0 - JianQiGuTuning.BLOCK_BREAK_DECAY);
            updateVisualPower();
          }
        }
      }
    }
  }

  /**
   * 判断方块是否可破坏。
   *
   * @param state 方块状态
   * @return 是否可破坏
   */
  private boolean isBreakableBlock(BlockState state) {
    if (BREAKABLE_BLOCKS.contains(state.getBlock())) {
      return true;
    }

    return state.is(BlockTags.LEAVES)
        || state.is(BlockTags.FLOWERS)
        || state.is(BlockTags.SMALL_FLOWERS)
        || state.is(BlockTags.TALL_FLOWERS)
        || state.is(BlockTags.CROPS)
        || state.is(BlockTags.SAPLINGS);
  }

  /**
   * 更新视觉威能（同步到客户端）。
   */
  private void updateVisualPower() {
    if (this.initialPower <= 0.0) {
      this.entityData.set(DATA_VISUAL_POWER, 0.0f);
    } else {
      float ratio = (float) (this.currentPower / this.initialPower);
      this.entityData.set(DATA_VISUAL_POWER, Mth.clamp(ratio, 0.0f, 1.0f));
    }
  }

  /**
   * 获取视觉威能（客户端渲染用）。
   *
   * @return 视觉威能（0-1）
   */
  public float getVisualPower() {
    return this.entityData.get(DATA_VISUAL_POWER);
  }

  /**
   * 获取移动方向。
   *
   * @return 移动方向
   */
  public Vec3 getDirection() {
    return this.direction;
  }

  /**
   * 根据方向更新实体朝向。
   */
  private void updateRotationFromDirection() {
    if (this.direction.lengthSqr() < 1.0E-6) {
      return;
    }

    float yaw = (float) Math.toDegrees(Math.atan2(this.direction.x, this.direction.z));
    float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(this.direction.y, -1.0, 1.0)));

    this.setYRot(yaw);
    this.setXRot(pitch);
    this.yRotO = yaw;
    this.xRotO = pitch;
  }

  /**
   * 获取拥有者。
   *
   * @return 拥有者实体
   */
  @Nullable
  public LivingEntity getOwner() {
    if (cachedOwner != null && cachedOwner.isAlive()) {
      return cachedOwner;
    }

    if (ownerId != null && this.level() instanceof ServerLevel server) {
      Entity entity = server.getEntity(ownerId);
      if (entity instanceof LivingEntity living) {
        cachedOwner = living;
        return living;
      }
    }

    return null;
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    if (ownerId != null) {
      tag.putUUID("Owner", ownerId);
    }
    tag.putDouble("InitialPower", initialPower);
    tag.putDouble("CurrentPower", currentPower);
    tag.putDouble("DirX", direction.x);
    tag.putDouble("DirY", direction.y);
    tag.putDouble("DirZ", direction.z);
    tag.putInt("HitCount", hitCount);
    tag.putInt("DecayGrace", decayGrace);
    tag.putInt("Age", age);
    tag.putInt("MaxAge", maxAge);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    if (tag.hasUUID("Owner")) {
      ownerId = tag.getUUID("Owner");
    }
    initialPower = tag.getDouble("InitialPower");
    currentPower = tag.getDouble("CurrentPower");
    direction = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
    hitCount = tag.getInt("HitCount");
    decayGrace = tag.getInt("DecayGrace");
    age = tag.getInt("Age");
    maxAge = tag.getInt("MaxAge");

    updateVisualPower();
    updateRotationFromDirection();
  }

  @Override
  public boolean isPickable() {
    return false;
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    return false;
  }

  @Override
  public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
    return new ClientboundAddEntityPacket(this, serverEntity);
  }
}
