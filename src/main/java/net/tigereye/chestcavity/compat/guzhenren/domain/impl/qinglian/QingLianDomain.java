package net.tigereye.chestcavity.compat.guzhenren.domain.impl.qinglian;

import java.lang.ref.WeakReference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.domain.AbstractDomain;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainHelper;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.qinglian.fx.QingLianDomainFX;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.qinglian.tuning.QingLianDomainTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;

/**
 * 青莲剑域（Qing Lian Domain）
 *
 * <p>蕴剑青莲蛊展开的青莲剑域，提供以下功能：
 *
 * <ul>
 *   <li>友方：防御增幅、跳跃增幅、呼吸恢复加成
 *   <li>视觉：莲花主题粒子特效 + PNG纹理渲染
 * </ul>
 *
 * <p>领域特性：
 *
 * <ul>
 *   <li>基础半径：{@link QingLianDomainTuning#BASE_RADIUS}（受域控系数影响）
 *   <li>等级：{@link QingLianDomainTuning#DOMAIN_LEVEL}（五级）
 *   <li>跟随主人移动
 * </ul>
 */
public class QingLianDomain extends AbstractDomain {

  /** 领域类型标识 */
  public static final String TYPE = "qinglian";

  /** 主人弱引用（避免内存泄漏） */
  private final WeakReference<LivingEntity> ownerRef;

  /** 半径缩放系数（受域控系数影响） */
  private final double radiusScale;

  /** 青莲剑群集群管理器 */
  private final net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.swarm
          .QingLianSwordSwarm
      swarmManager;

  /** 上次粒子特效tick */
  private long lastParticleTick = 0;

  /** 上次同步到客户端的tick */
  private long lastSyncTick = 0;

  /** 上次同步领域标签的tick（限频以降低开销） */
  private long lastTagSyncTick = 0;

  /** 同步间隔（tick） */
  private static final int SYNC_INTERVAL = 20; // 每秒同步一次
  /** 标签同步间隔（tick） */
  private static final int TAG_SYNC_INTERVAL = 20; // 每秒执行一次标签进入/离开判定

  /**
   * 构造青莲剑域
   *
   * @param owner 主人
   * @param center 中心位置
   * @param radiusScale 半径缩放系数（基于域控系数）
   */
  public QingLianDomain(LivingEntity owner, Vec3 center, double radiusScale) {
    super(owner, center, QingLianDomainTuning.DOMAIN_LEVEL);
    this.ownerRef = new WeakReference<>(owner);
    this.radiusScale = Math.max(0.5, Math.min(2.0, radiusScale)); // 限制在0.5-2.0

    // 初始化集群管理器
    this.swarmManager =
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.swarm
            .QingLianSwordSwarm(getDomainId(), owner);
  }

  @Override
  public LivingEntity getOwner() {
    return ownerRef.get();
  }

  @Override
  public String getDomainType() {
    return TYPE;
  }

  @Override
  public double getRadius() {
    return QingLianDomainTuning.BASE_RADIUS * radiusScale;
  }

  @Override
  public void tick(ServerLevel level) {
    // 调用父类tick（应用效果到范围内实体）
    super.tick(level);

    // 同步领域标签（进入/离开）- 限频以降低大范围 AABB 扫描开销
    long currentTick = level.getGameTime();
    if (currentTick - lastTagSyncTick >= TAG_SYNC_INTERVAL) {
      DomainHelper.syncDomainTags(this);
      lastTagSyncTick = currentTick;
    }

    // Phase 1: 集群AI管理器tick（功能开关控制）
    if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.ENABLE_SWARM) {
      swarmManager.tick();
    }

    // 粒子特效
    if (currentTick != lastParticleTick) {
      QingLianDomainFX.tickDomainEffects(level, this, currentTick);
      lastParticleTick = currentTick;
    }

    // 同步到客户端
    if (currentTick - lastSyncTick >= SYNC_INTERVAL) {
      syncToClients(level);
      lastSyncTick = currentTick;
    }
  }

  @Override
  public void applyEffects(ServerLevel level, LivingEntity entity, boolean isFriendly) {
    if (isFriendly) {
      applyFriendlyEffects(level, entity);
    }
    // 青莲剑域对敌方无减益，只有友方增益
  }

  /**
   * 对友方应用增益效果
   *
   * @param level 世界
   * @param entity 友方实体
   */
  private void applyFriendlyEffects(ServerLevel level, LivingEntity entity) {
    // 防御增幅（+15% → 药水等级1-2）
    int defenseLevel =
        Math.max(0, (int) Math.round(QingLianDomainTuning.FRIENDLY_DEFENSE_MULT * 10) - 1);
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            QingLianDomainTuning.EFFECT_DURATION,
            defenseLevel,
            false,
            false,
            false));

    // 跳跃增幅（+30% → 药水等级2-3）
    int jumpLevel =
        Math.max(0, (int) Math.round(QingLianDomainTuning.FRIENDLY_JUMP_MULT * 10) - 1);
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.JUMP,
            QingLianDomainTuning.EFFECT_DURATION,
            jumpLevel,
            false,
            false,
            false));

    // 呼吸恢复加成（精力恢复）
    double regenAmount = QingLianDomainTuning.FRIENDLY_BREATHING_REGEN / 20.0; // 每tick
    ResourceOps.tryAdjustJingli(entity, regenAmount, true);
  }

  @Override
  public void destroy() {
    super.destroy();

    // 销毁特效
    LivingEntity owner = getOwner();
    if (owner != null && owner.level() instanceof ServerLevel serverLevel) {
      // 标签清理：将附近实体上属于本领域主人的标签移除
      var cleanupBounds = getBounds().inflate(8.0);
      var maybeTagged =
          serverLevel.getEntitiesOfClass(LivingEntity.class, cleanupBounds);
      for (var e : maybeTagged) {
        java.util.UUID tagOwner =
            net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags.getSwordDomainOwner(e);
        if (tagOwner != null && tagOwner.equals(getOwnerUUID())) {
          net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags.markLeaveSwordDomain(e);
        }
      }

      QingLianDomainFX.spawnDestructionEffect(serverLevel, getCenter(), getRadius());

      // 通知客户端移除（使用通用系统）
      net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainRemovePayload removePayload =
          new net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainRemovePayload(
              getDomainId());
      net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainNetworkHandler.sendDomainRemove(
          removePayload, getCenter(), serverLevel);
    }
  }

  /**
   * 同步领域数据到客户端（使用通用系统）
   *
   * @param level 服务端世界
   */
  private void syncToClients(ServerLevel level) {
    Vec3 center = getCenter();
    ResourceLocation texturePath = getTexturePath();
    if (texturePath == null) {
      return; // 没有纹理路径，不同步
    }

    net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainSyncPayload payload =
        new net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainSyncPayload(
            getDomainId(),
            getOwnerUUID(),
            center.x,
            center.y,
            center.z,
            getRadius(),
            getLevel(),
            texturePath.toString(),
            getPngHeightOffset(),
            getPngAlpha(),
            getPngRotationSpeed());
    net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainNetworkHandler.sendDomainSync(
        payload, center, level);
  }

  // ========== PNG渲染配置 ==========

  @Override
  public ResourceLocation getTexturePath() {
    return ResourceLocation.fromNamespaceAndPath(
        "guzhenren", QingLianDomainTuning.TEXTURE_PATH);
  }

  @Override
  public double getPngHeightOffset() {
    return QingLianDomainTuning.PNG_HEIGHT_OFFSET;
  }

  @Override
  public float getPngAlpha() {
    return QingLianDomainTuning.PNG_ALPHA;
  }

  @Override
  public float getPngRotationSpeed() {
    return QingLianDomainTuning.PNG_ROTATION_SPEED;
  }

  /**
   * 获取青莲剑群集群管理器
   *
   * @return 集群管理器
   */
  public net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.swarm
          .QingLianSwordSwarm
      getSwarmManager() {
    return swarmManager;
  }
}
