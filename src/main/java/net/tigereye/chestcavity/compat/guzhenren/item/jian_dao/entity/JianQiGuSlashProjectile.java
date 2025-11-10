package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianQiGuCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianQiGuTuning;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOGGER = LoggerFactory.getLogger(JianQiGuSlashProjectile.class);

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, JianQiGuTuning.ORGAN_ID);
  private static final String STATE_ROOT = "JianQiGu";
  private static final String KEY_DUANSHI_STACKS = "DuanshiStacks";
  private static final String KEY_LAST_CAST_TICK = "LastCastTick";

  // ========== 数据同步字段 ==========
 
  /** 视觉威能（归一化到0-1，用于客户端渲染与规模插值） */
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

    // 基于当前威能动态扩展判定宽度，实现随修为无限成长的命中/破坏范围
    double baseScale = Math.max(this.initialPower * 0.25, 1.0);
    double ratio = Math.max(this.currentPower / baseScale, 0.0);
    // 与视觉类似使用对数放大，保持单调且无硬上限；最低保证略大于原始宽度
    double dynamicWidth = JianQiGuTuning.SLASH_WIDTH * (1.0 + Math.log1p(ratio));

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

    // 构建扫描盒子（宽度随 currentPower 动态扩张）
    double baseScale = Math.max(this.initialPower * 0.25, 1.0);
    double ratio = Math.max(this.currentPower / baseScale, 0.0);
    double dynamicWidth = JianQiGuTuning.SLASH_WIDTH * (1.0 + Math.log1p(ratio));
    AABB sweep = new AABB(start, end).inflate(dynamicWidth * 0.5);

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

      // 成功命中后增加断势层数
      if (hurt && owner != null) {
        incrementDuanshiStack(owner);
      }
    }
  }

  /**
   * 增加断势层数（仅在斩击命中时触发）。
   *
   * @param owner 斩击的拥有者
   */
  private void incrementDuanshiStack(LivingEntity owner) {
    if (!(this.level() instanceof ServerLevel level)) {
      return;
    }

    // 获取胸腔实例
    ChestCavityInstance cc =
        ChestCavityEntity.of(owner).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null) {
      return;
    }

    // 查找剑气蛊器官
    ItemStack organ = findJianQiGuOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    // 更新断势层数
    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = level.getGameTime();
    int currentStacks = state.getInt(KEY_DUANSHI_STACKS, 0);
    state.setInt(KEY_DUANSHI_STACKS, currentStacks + 1);
    state.setLong(KEY_LAST_CAST_TICK, now);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianQiGuSlashProjectile] {} duanshi stacks: {} -> {} (from slash hit)",
          owner.getName().getString(),
          currentStacks,
          currentStacks + 1);
    }
  }

  /**
   * 在胸腔中查找剑气蛊器官。
   *
   * @param cc 胸腔实例
   * @return 剑气蛊物品栈，若未找到返回 EMPTY
   */
  private ItemStack findJianQiGuOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }

    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }

      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (itemId != null && itemId.equals(ORGAN_ID)) {
        return stack;
      }
    }

    return ItemStack.EMPTY;
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

    // 构建扫描盒子（宽度随 currentPower 动态扩张）
    double baseScale = Math.max(this.initialPower * 0.25, 1.0);
    double ratio = Math.max(this.currentPower / baseScale, 0.0);
    double dynamicWidth = JianQiGuTuning.SLASH_WIDTH * (1.0 + Math.log1p(ratio));
    AABB box = new AABB(start, end).inflate(dynamicWidth * 0.5);

    int minX = Mth.floor(box.minX);
    int minY = Mth.floor(box.minY);
    int minZ = Mth.floor(box.minZ);
    int maxX = Mth.floor(box.maxX);
    int maxY = Mth.floor(box.maxY);
    int maxZ = Mth.floor(box.maxZ);

    // 动态破坏预算：随威能范围增加
    int breakBudget = Mth.clamp(
        (int) Math.ceil(JianQiGuTuning.BLOCK_BREAK_CAP_BASE + dynamicWidth * JianQiGuTuning.BLOCK_BREAK_CAP_SCALE),
        JianQiGuTuning.BLOCK_BREAK_CAP_BASE,
        JianQiGuTuning.BLOCK_BREAK_CAP_MAX);
    int brokenCount = 0;

    for (int x = minX; x <= maxX && brokenCount < breakBudget; x++) {
      for (int y = minY; y <= maxY && brokenCount < breakBudget; y++) {
        for (int z = minZ; z <= maxZ && brokenCount < breakBudget;
            z++) {
          BlockPos pos = new BlockPos(x, y, z);
          BlockState state = server.getBlockState(pos);

          if (state.isAir()) {
            continue;
          }

          if (!isBreakableBlock(server, pos, state)) {
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
   * <p>根据需求：允许破坏所有硬度不为 -1 的方块（即排除不可破坏方块，如基岩）。
   *
   * @param state 方块状态
   * @return 是否可破坏
   */
  private boolean isBreakableBlock(ServerLevel server, BlockPos pos, BlockState state) {
    // 仅排除不可破坏（硬度为 -1）的方块，其余均允许
    float hardness = state.getDestroySpeed(server, pos);
    return hardness >= 0.0f;
  }

 /**
  * 更新视觉威能（同步到客户端）。
  *
  * <p>规则（无限上限版）：
  * - 基于 currentPower / baseScale 的对数映射，DATA_VISUAL_POWER 单调递增且无上限硬截断。
  * - 渲染层可直接将该值用于尺度/特效放大（例如 size = 0.5F + DATA_VISUAL_POWER）。
  *
  * <p>这样高道痕/高威能不会被强行压平，上限仅由渲染实现自行决定。
  */
 private void updateVisualPower() {
   if (this.initialPower <= 0.0) {
     this.entityData.set(DATA_VISUAL_POWER, 0.0f);
     return;
   }

   // 以初始威能的一部分作为基准，防止低配时过高
   double baseScale = Math.max(this.initialPower * 0.25, 1.0);
   double ratio = Math.max(this.currentPower / baseScale, 0.0);

   // 使用对数放大：ratio 越大，值越大，且无固定上限
   // +1 防止 log(0)，0.5F 控制增长速度，可根据体感调整
   float visual = (float) (Math.log1p(ratio) * 0.5F);

   // 不再做上限截断，仅避免负值
   if (visual < 0.0f) {
     visual = 0.0f;
   }

   this.entityData.set(DATA_VISUAL_POWER, visual);
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
  public Vec3 getSlashDirection() {
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
