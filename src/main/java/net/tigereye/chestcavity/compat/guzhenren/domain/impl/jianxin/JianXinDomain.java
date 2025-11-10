package net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin;

import java.lang.ref.WeakReference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.domain.AbstractDomain;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.fx.JianXinDomainFX;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning;

/**
 * 剑心域（Sword Heart Domain）
 *
 * <p>剑道流派的核心领域技能，提供以下功能：
 *
 * <ul>
 *   <li>友方：资源恢复
 *   <li>敌方：移动速度/攻击速度减慢
 *   <li>敌方剑修：剑气反噬
 * </ul>
 *
 * <p>领域特性：
 *
 * <ul>
 *   <li>基础半径：{@link JianXinDomainTuning#BASE_RADIUS}
 *   <li>等级范围：{@link JianXinDomainTuning#MIN_LEVEL}-{@link JianXinDomainTuning#MAX_LEVEL}
 *   <li>跟随主人移动
 * </ul>
 */
public class JianXinDomain extends AbstractDomain {

  /** 领域类型标识 */
  public static final String TYPE = "jianxin";

  /** 主人弱引用（避免内存泄漏） */
  private final WeakReference<LivingEntity> ownerRef;

  /** 剑道道痕值 */
  private int jiandaoDaohen;

  /** 流派经验值 */
  private int schoolExperience;

  /** 是否强化状态（定心返本触发后翻倍） */
  private boolean enhanced = false;

  /** 强化剩余时间（tick） */
  private int enhancedTicks = 0;

  /** 上次粒子特效tick */
  private long lastParticleTick = 0;

  /** 上次同步到客户端的tick */
  private long lastSyncTick = 0;

  /** 同步间隔（tick） */
  private static final int SYNC_INTERVAL = 20; // 每秒同步一次

  /**
   * 构造剑心域
   *
   * @param owner 主人
   * @param center 中心位置
   * @param level 领域等级（5-6）
   * @param jiandaoDaohen 剑道道痕值
   * @param schoolExperience 流派经验值
   */
  public JianXinDomain(
      LivingEntity owner, Vec3 center, int level, int jiandaoDaohen, int schoolExperience) {
    super(
        owner,
        center,
        Math.max(JianXinDomainTuning.MIN_LEVEL, Math.min(JianXinDomainTuning.MAX_LEVEL, level)));
    this.ownerRef = new WeakReference<>(owner);
    this.jiandaoDaohen = jiandaoDaohen;
    this.schoolExperience = schoolExperience;
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
    var owner = getOwner();
    if (owner instanceof net.minecraft.world.entity.player.Player player) {
      double scale =
          net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.state.DomainConfigOps
              .radiusScale(player);
      return JianXinDomainTuning.BASE_RADIUS * Math.max(1.0e-6, scale);
    }
    return JianXinDomainTuning.BASE_RADIUS;
  }

  @Override
  public void tick(ServerLevel level) {
    // 更新强化状态
    if (enhanced) {
      enhancedTicks--;
      if (enhancedTicks <= 0) {
        enhanced = false;
      }
    }

    // 调用父类tick（应用效果到范围内实体）
    super.tick(level);

    // 同步领域标签（进入/离开）
    net.tigereye.chestcavity.compat.guzhenren.domain.DomainHelper.syncDomainTags(this);

    // 粒子特效
    long currentTick = level.getGameTime();
    if (currentTick != lastParticleTick) {
      JianXinDomainFX.tickDomainEffects(level, this, currentTick);
      lastParticleTick = currentTick;
    }

    // 同步到客户端
    if (currentTick - lastSyncTick >= SYNC_INTERVAL) {
      syncToClients(level);
      lastSyncTick = currentTick;
    }

    // 域控系数广播改为通用系统（SwordDomainOps）统一处理，避免与具体领域耦合。
  }

  @Override
  public void applyEffects(ServerLevel level, LivingEntity entity, boolean isFriendly) {
    if (isFriendly) {
      applyFriendlyEffects(level, entity);
    } else {
      applyEnemyEffects(level, entity);
    }
  }

  /**
   * 对友方应用增益效果
   *
   * @param level 世界
   * @param entity 友方实体
   */
  private void applyFriendlyEffects(ServerLevel level, LivingEntity entity) {
    double enhancedMult = enhanced ? JianXinDomainTuning.ENHANCED_REGEN_MULT : 1.0;
    double effectScale = 1.0;
    var own = getOwner();
    if (own instanceof net.minecraft.world.entity.player.Player p) {
      effectScale =
          net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.state.DomainConfigOps
              .effectScale(p);
    }

    // 资源恢复（精力）：每tick = 1.0 / 20 * 乘积缩放
    double regenAmount = (1.0 / 20.0) * enhancedMult * effectScale;
    net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps.tryAdjustJingli(
        entity, regenAmount, true);

    // 剑势恢复（通过LinkageChannel实现）
    // TODO: 实现剑势层数恢复逻辑
  }

  /**
   * 对敌方应用减益效果
   *
   * @param level 世界
   * @param entity 敌方实体
   */
  private void applyEnemyEffects(ServerLevel level, LivingEntity entity) {
    double enhancedMult = enhanced ? JianXinDomainTuning.ENHANCED_DEBUFF_MULT : 1.0;
    double effectScale = 1.0;
    var own = getOwner();
    if (own instanceof net.minecraft.world.entity.player.Player p) {
      effectScale =
          net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.state.DomainConfigOps
              .effectScale(p);
    }

    // 计算效果强度缩放
    double intensityScale =
        jiandaoDaohen * JianXinDomainTuning.DAOHEN_INTENSITY_COEF
            + schoolExperience * JianXinDomainTuning.SCHOOL_EXP_INTENSITY_COEF;

    double slowFactor = JianXinDomainTuning.ENEMY_SLOW_FACTOR * enhancedMult;
    double attackSlowFactor = JianXinDomainTuning.ENEMY_ATTACK_SLOW_FACTOR * enhancedMult;

    // 应用强度缩放
    slowFactor *= (1.0 + intensityScale) * effectScale;
    attackSlowFactor *= (1.0 + intensityScale) * effectScale;

    // 应用缓慢效果（移动速度）
    int slowAmplifier = Math.min(4, (int) (slowFactor * 10)); // 转换为药水等级
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN,
            JianXinDomainTuning.EFFECT_DURATION,
            slowAmplifier,
            false,
            false,
            false));

    // 应用挖掘疲劳（攻击速度减慢）
    int miningFatigueAmplifier = Math.min(2, (int) (attackSlowFactor * 10));
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.DIG_SLOWDOWN,
            JianXinDomainTuning.EFFECT_DURATION,
            miningFatigueAmplifier,
            false,
            false,
            false));

    // 剑气反噬判定
    if (entity instanceof Player enemyPlayer) {
      checkSwordCounterAttack(level, enemyPlayer);
    }
  }

  /**
   * 检查剑气反噬
   *
   * <p>当敌方剑修使用剑道技能时，如果实力不足会被反噬
   *
   * @param level 世界
   * @param enemy 敌方玩家
   */
  private void checkSwordCounterAttack(ServerLevel level, Player enemy) {
    // 检查敌方是否正在使用“剑道”物品（tag: guzhenren:jiandao）
    if (!net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver.isUsingJiandao(
        enemy)) {
      return;
    }

    // 获取敌方实力（道痕 + 流派经验）
    double enemyDaohen =
        net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps.openHandle(enemy)
            .map(h -> h.read("daohen_jiandao").orElse(0.0))
            .orElse(0.0);
    double enemySchoolExp =
        net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps.openHandle(enemy)
            .map(h -> h.read("liupai_jiandao").orElse(0.0))
            .orElse(0.0);

    // 计算实力对比
    double ownerPower =
        jiandaoDaohen * JianXinDomainTuning.DAOHEN_WEIGHT
            + schoolExperience * JianXinDomainTuning.SCHOOL_EXP_WEIGHT;
    double enemyPower = enemyDaohen + enemySchoolExp;

    if (ownerPower > enemyPower) {
      // 计算反噬伤害
      double powerDiff = ownerPower - enemyPower;
      float damage =
          JianXinDomainTuning.SWORD_COUNTER_BASE_DAMAGE
              + (float) (powerDiff * JianXinDomainTuning.POWER_DIFF_DAMAGE_COEF);

      // 触发剑气反噬：取消当前引导
      enemy.stopUsingItem();
      enemy.hurt(level.damageSources().magic(), damage);

      // 反噬特效
      JianXinDomainFX.spawnCounterAttackEffect(level, enemy.position());
    }
  }

  /**
   * 触发强化状态（定心返本效果）
   *
   * <p>域内增益与减益翻倍，持续2秒
   */
  public void triggerEnhancement() {
    if (!this.enhanced) {
      this.enhanced = true;
      this.enhancedTicks = JianXinDomainTuning.ENHANCED_DURATION;

      // 强化特效
      LivingEntity owner = getOwner();
      if (owner != null && owner.level() instanceof ServerLevel serverLevel) {
        JianXinDomainFX.spawnEnhancementEffect(serverLevel, getCenter(), getRadius());
      }
    }
  }

  /**
   * 检查是否处于强化状态
   *
   * @return 是否强化
   */
  public boolean isEnhanced() {
    return enhanced;
  }

  /**
   * 更新剑道数值（动态调整领域强度）
   *
   * @param jiandaoDaohen 新的剑道道痕值
   * @param schoolExperience 新的流派经验值
   */
  public void updateJiandaoStats(int jiandaoDaohen, int schoolExperience) {
    this.jiandaoDaohen = jiandaoDaohen;
    this.schoolExperience = schoolExperience;

    // 根据实力重新计算等级
    int newLevel = calculateLevel(jiandaoDaohen, schoolExperience);
    setLevel(newLevel);
  }

  /**
   * 根据剑道道痕和流派经验计算领域等级
   *
   * @param jiandaoDaohen 剑道道痕
   * @param schoolExperience 流派经验
   * @return 领域等级（5-6）
   */
  private static int calculateLevel(int jiandaoDaohen, int schoolExperience) {
    double totalPower =
        jiandaoDaohen * JianXinDomainTuning.DAOHEN_WEIGHT
            + schoolExperience * JianXinDomainTuning.SCHOOL_EXP_WEIGHT;
    return totalPower > JianXinDomainTuning.LEVEL_THRESHOLD
        ? JianXinDomainTuning.MAX_LEVEL
        : JianXinDomainTuning.MIN_LEVEL;
  }

  @Override
  public void destroy() {
    super.destroy();

    // 销毁特效
    LivingEntity owner = getOwner();
    if (owner != null && owner.level() instanceof ServerLevel serverLevel) {
      // 标签清理：将附近实体上属于本领域主人的剑域标签移除
      var cleanupBounds = getBounds().inflate(8.0);
      var maybeTagged =
          serverLevel.getEntitiesOfClass(
              net.minecraft.world.entity.LivingEntity.class, cleanupBounds);
      for (var e : maybeTagged) {
        java.util.UUID tagOwner =
            net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags.getSwordDomainOwner(e);
        if (tagOwner != null && tagOwner.equals(getOwnerUUID())) {
          net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags.markLeaveSwordDomain(e);
        }
      }

      JianXinDomainFX.spawnDestructionEffect(serverLevel, getCenter(), getRadius());

      // 通知客户端移除（使用通用系统）
      net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainRemovePayload removePayload =
          new net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainRemovePayload(
              getDomainId());
      net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainNetworkHandler
          .sendDomainRemove(removePayload, getCenter(), serverLevel);
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
        "guzhenren", "textures/domain/jianxinyu_transparent_hardcut.png");
  }

  @Override
  public double getPngHeightOffset() {
    return 20.0; // 领域中心上方20格
  }

  @Override
  public float getPngAlpha() {
    return enhanced ? 0.8f : 0.5f; // 强化状态下更不透明
  }

  @Override
  public float getPngRotationSpeed() {
    return 0.5f; // 缓慢旋转
  }
}
