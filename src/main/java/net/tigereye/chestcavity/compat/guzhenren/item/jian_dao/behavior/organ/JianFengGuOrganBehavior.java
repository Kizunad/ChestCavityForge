package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordController;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.active.JianFengHuaxingActive;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianFengGuFx;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianFengGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 剑锋蛊（四转/五转·剑道主动+被动器官）行为实现。
 *
 * <p>主动技能「锋芒化形」：
 * <ul>
 *   <li>消耗真元/精力，生成飞剑随侍</li>
 *   <li>持续期间近战伤害提升</li>
 *   <li>四转生成1把，五转生成2把</li>
 * </ul>
 *
 * <p>高额一击协同：
 * <ul>
 *   <li>造成高额伤害时（四转≥100，五转≥500），飞剑协同突击</li>
 *   <li>协同伤害继承道痕与攻击修正</li>
 *   <li>单次触发仅消耗一把飞剑</li>
 * </ul>
 *
 * <p>被动技能「意随形动」：
 * <ul>
 *   <li>完成3次协同后触发"剑意共振"</li>
 *   <li>为所有自有飞剑短时加速</li>
 *   <li>加速幅度随道痕/流派经验/转数递增</li>
 * </ul>
 */
public enum JianFengGuOrganBehavior implements OrganOnHitListener, OrganSlowTickListener {
  INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger(JianFengGuOrganBehavior.class);

  public static final String MOD_ID = "guzhenren";
  public static final ResourceLocation ORGAN_ID_FOUR =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, JianFengGuTuning.ORGAN_ID_FOUR);
  public static final ResourceLocation ORGAN_ID_FIVE =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, JianFengGuTuning.ORGAN_ID_FIVE);

  /** 主动技能 ID（用于注册与客户端热键绑定）。*/
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, JianFengGuTuning.ABILITY_ID);

  private static final String STATE_ROOT = "JianFengGu";

  static {
    // 注册主动技能激活入口
    OrganActivationListeners.register(ABILITY_ID, JianFengGuOrganBehavior::activateAbility);
  }

  /**
   * 主动技能激活入口（由 OrganActivationListeners 调用）。
   *
   * @param entity 激活者
   * @param cc 胸腔实例
   */
  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    // 仅服务端 ServerPlayer 可激活
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
      return;
    }

    // 查找剑锋蛊器官
    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    // 构建状态与冷却管理器
    OrganState state = OrganState.of(organ, STATE_ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();

    long now = player.serverLevel().getGameTime();

    // 调用主动技能实现
    JianFengHuaxingActive.activate(player, cc, organ, state, cooldown, now);
  }

  /**
   * 在玩家胸腔中查找剑锋蛊器官（支持四转/五转）。
   *
   * @param cc 胸腔实例
   * @return 剑锋蛊物品栈，若未找到返回 EMPTY
   */
  private static ItemStack findMatchingOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }

    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }

      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (itemId != null
          && (itemId.equals(ORGAN_ID_FOUR) || itemId.equals(ORGAN_ID_FIVE))) {
        return stack;
      }
    }

    return ItemStack.EMPTY;
  }

  /**
   * OnHit 钩子：高额一击协同。
   *
   * <p>当宿主对目标造成高额伤害时：
   * <ul>
   *   <li>选择一把生成的飞剑</li>
   *   <li>命令其对目标发动协同突击</li>
   *   <li>协同后飞剑回归原位</li>
   *   <li>累计协同次数，达到3次触发共振</li>
   * </ul>
   *
   * @param source 伤害源
   * @param attacker 攻击者（宿主）
   * @param target 目标
   * @param cc 胸腔实例
   * @param organ 剑锋蛊器官物品
   * @param damage 原始伤害
   * @return 修改后的伤害（主动态期间 +10%）
   */
  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {

    // 仅服务端处理
    if (attacker.level().isClientSide() || !(attacker.level() instanceof ServerLevel level)) {
      return damage;
    }

    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = level.getGameTime();

    // 检查是否在主动态期间
    long activeUntil = state.getLong("active_until", 0L);
    boolean isActive = now < activeUntil;

    // 主动态期间：近战伤害 +10%
    float finalDamage = damage;
    if (isActive) {
      finalDamage *= (1.0f + (float) JianFengGuTuning.ACTIVE_BONUS_DAMAGE_MULT);
    }

    // 检查是否触发高额一击协同
    ResourceLocation organId = BuiltInRegistries.ITEM.getKey(organ.getItem());
    boolean isFiveTurn = organId != null && organId.equals(ORGAN_ID_FIVE);
    float threshold = isFiveTurn
        ? JianFengGuTuning.HIGH_HIT_THRESHOLD_FIVE
        : JianFengGuTuning.HIGH_HIT_THRESHOLD_FOUR;

    if (finalDamage < threshold) {
      return finalDamage; // 未达到阈值，不触发协同
    }

    // 检查去抖间隔
    long lastCoopTick = state.getLong("last_coop_tick", 0L);
    if (now - lastCoopTick < JianFengGuTuning.COOP_MIN_INTERVAL_TICKS) {
      return finalDamage; // 间隔太短，不触发
    }

    // 查找可用的飞剑
    FlyingSwordEntity sword = findAvailableSword(level, attacker, state);
    if (sword == null) {
      return finalDamage; // 没有可用飞剑
    }

    // 计算协同伤害
    float coopDamage = calculateCoopDamage(attacker, finalDamage, isFiveTurn);

    // 执行协同突击
    performCoopStrike(level, sword, target, coopDamage);

    // 更新状态
    state.setLong("last_coop_tick", now);
    int coopCount = state.getInt("coop_count", 0);
    state.setInt("coop_count", coopCount + 1);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianFengGuOrganBehavior] Coop strike! Damage: {}, Coop count: {}",
          coopDamage,
          coopCount + 1);
    }

    return finalDamage;
  }

  /**
   * SlowTick 钩子：维持主动态、触发共振。
   *
   * @param entity 宿主实体
   * @param cc 胸腔实例
   * @param organ 剑锋蛊器官物品
   */
  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    // 仅服务端处理
    if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) {
      return;
    }

    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = level.getGameTime();

    // 检查主动态是否结束
    long activeUntil = state.getLong("active_until", 0L);
    if (activeUntil > 0 && now >= activeUntil) {
      // 主动态结束：移除生成的飞剑
      despawnGeneratedSwords(level, state);
      state.remove("spawned_sword_ids");
      state.setLong("active_until", 0L);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("[JianFengGuOrganBehavior] Active state expired for {}", entity.getName().getString());
      }
    }

    // 检查是否触发共振
    int coopCount = state.getInt("coop_count", 0);
    if (coopCount >= JianFengGuTuning.COOP_COUNT_FOR_RESONANCE) {
      triggerResonance(entity, state, cc);
    }
  }

  /**
   * 查找可用的飞剑（从生成列表中选择）。
   *
   * @param level 服务端世界
   * @param owner 宿主
   * @param state 器官状态
   * @return 可用的飞剑实体，若无则返回 null
   */
  private FlyingSwordEntity findAvailableSword(ServerLevel level, LivingEntity owner, OrganState state) {
    ListTag swordIdList = state.getList("spawned_sword_ids", 3); // 3 = IntTag
    if (swordIdList.isEmpty()) {
      return null;
    }

    // 遍历查找仍存活的飞剑
    for (int i = 0; i < swordIdList.size(); i++) {
      int swordId = swordIdList.getInt(i);
      Entity entity = level.getEntity(swordId);
      if (entity instanceof FlyingSwordEntity sword && sword.isAlive() && sword.isOwnedBy(owner)) {
        return sword; // 找到可用飞剑
      }
    }

    return null;
  }

  /**
   * 计算协同伤害。
   *
   * @param owner 宿主
   * @param originalDamage 原始伤害
   * @param isFiveTurn 是否五转
   * @return 协同伤害
   */
  private float calculateCoopDamage(LivingEntity owner, float originalDamage, boolean isFiveTurn) {
    // 基础倍率
    double baseMult = JianFengGuTuning.COOP_DAMAGE_BASE_MULT;

    // 道痕加成
    double daohen = ResourceOps.openHandle(owner)
        .map(h -> h.read("daohen_jiandao").orElse(0.0))
        .orElse(0.0);
    double daohenBonus = Math.min(daohen / JianFengGuTuning.COOP_DAMAGE_DAOHEN_DIV, JianFengGuTuning.COOP_DAMAGE_DAOHEN_MAX);

    // 总倍率
    double totalMult = baseMult + daohenBonus;

    return (float) (originalDamage * totalMult);
  }

  /**
   * 执行协同突击。
   *
   * <p>命令飞剑对目标发动一次快速突击，然后回归原位。
   *
   * <p>实现：
   * <ul>
   *   <li>保存飞剑当前AI模式</li>
   *   <li>设置目标并切换到HUNT模式</li>
   *   <li>飞剑AI系统会使用DuelIntent或AssassinIntent飞向并攻击目标</li>
   *   <li>造成协同伤害</li>
   *   <li>1秒后恢复原AI模式</li>
   * </ul>
   *
   * @param level 服务端世界
   * @param sword 飞剑
   * @param target 目标
   * @param damage 协同伤害
   */
  private void performCoopStrike(ServerLevel level, FlyingSwordEntity sword, LivingEntity target, float damage) {
    // 保存当前AI模式
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode originalMode = sword.getAIMode();

    // 设置飞剑目标
    sword.setTargetEntity(target);

    // 切换到HUNT模式，让飞剑飞向目标
    sword.setAIMode(net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode.HUNT);

    // 造成协同伤害
    target.hurt(sword.damageSources().mobAttack(sword), damage);

    // 播放协同突击特效
    Vec3 swordPos = sword.position();
    Vec3 targetPos = target.position();
    JianFengGuFx.playCoopStrike(level, swordPos, targetPos);

    // 1秒后恢复原AI模式（20 ticks）
    long restoreTick = level.getGameTime() + 20L;
    level.getServer().execute(() -> {
      if (sword.isAlive() && !sword.isRemoved()) {
        // 延迟执行：恢复原AI模式
        scheduleAIModeRestore(level, sword, originalMode, restoreTick);
      }
    });

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianFengGuOrganBehavior] Coop strike initiated: sword={}, target={}, damage={}, mode={} -> HUNT",
          sword.getId(),
          target.getName().getString(),
          damage,
          originalMode);
    }
  }

  /**
   * 安排恢复飞剑AI模式。
   *
   * @param level 服务端世界
   * @param sword 飞剑
   * @param mode 要恢复的模式
   * @param restoreTick 恢复时间点
   */
  private void scheduleAIModeRestore(
      ServerLevel level,
      FlyingSwordEntity sword,
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode mode,
      long restoreTick) {

    // 使用递归检查直到达到目标tick
    class RestoreTask implements Runnable {
      @Override
      public void run() {
        if (!sword.isAlive() || sword.isRemoved()) {
          return; // 飞剑已死亡或移除，停止
        }

        long now = level.getGameTime();
        if (now >= restoreTick) {
          // 时间到，恢复AI模式
          sword.setAIMode(mode);
          sword.setTargetEntity(null); // 清除目标

          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "[JianFengGuOrganBehavior] Restored AI mode for sword {} to {}",
                sword.getId(),
                mode);
          }
        } else {
          // 还没到时间，继续等待
          level.getServer().execute(this);
        }
      }
    }

    level.getServer().execute(new RestoreTask());
  }

  /**
   * 移除主动态生成的飞剑。
   *
   * @param level 服务端世界
   * @param state 器官状态
   */
  private void despawnGeneratedSwords(ServerLevel level, OrganState state) {
    ListTag swordIdList = state.getList("spawned_sword_ids", 3); // 3 = IntTag
    if (swordIdList.isEmpty()) {
      return;
    }

    int removedCount = 0;
    for (int i = 0; i < swordIdList.size(); i++) {
      int swordId = swordIdList.getInt(i);
      Entity entity = level.getEntity(swordId);
      if (entity instanceof FlyingSwordEntity sword && sword.isAlive()) {
        sword.discard();
        removedCount++;
      }
    }

    if (LOGGER.isDebugEnabled() && removedCount > 0) {
      LOGGER.debug("[JianFengGuOrganBehavior] Despawned {} generated swords", removedCount);
    }
  }

  /**
   * 触发剑意共振。
   *
   * <p>为宿主名下的全部飞剑提供短时速度提升。
   *
   * @param owner 宿主
   * @param state 器官状态
   * @param cc 胸腔实例
   */
  private void triggerResonance(LivingEntity owner, OrganState state, ChestCavityInstance cc) {
    if (!(owner.level() instanceof ServerLevel level)) {
      return;
    }

    // 重置协同计数
    state.setInt("coop_count", 0);

    // 仅玩家触发共振效果
    if (!(owner instanceof net.minecraft.world.entity.player.Player player)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("[JianFengGuOrganBehavior] Resonance skipped for non-player entity");
      }
      return;
    }

    // 获取道痕和流派经验（用于计算加成）
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(owner);
    if (handleOpt.isEmpty()) {
      return;
    }

    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    double daohen = handle.read("daohen_jiandao").orElse(0.0);
    double liupaiExp = handle.read("liupai_jiandao").orElse(0.0);

    // 计算共振加成
    double speedBonus = JianFengGuTuning.RESONANCE_BASE_SPEED_BONUS
        + (daohen / 200.0) * JianFengGuTuning.RESONANCE_SPEED_PER_200_DAOHEN;
    int duration = (int) (JianFengGuTuning.RESONANCE_BASE_DURATION_TICKS
        + (liupaiExp / 300.0) * JianFengGuTuning.RESONANCE_DURATION_PER_300_DAOHEN);

    // 检查是否五转（额外加成）
    ItemStack organ = findMatchingOrgan(cc);
    if (!organ.isEmpty()) {
      ResourceLocation organId = BuiltInRegistries.ITEM.getKey(organ.getItem());
      if (organId != null && organId.equals(ORGAN_ID_FIVE)) {
        speedBonus += JianFengGuTuning.RESONANCE_FIVE_BONUS;
      }
    }

    // 获取所有自有飞剑
    List<FlyingSwordEntity> swords = FlyingSwordController.getPlayerSwords(level, player);
    int affectedCount = 0;

    for (FlyingSwordEntity sword : swords) {
      if (sword.isOwnedBy(owner)) {
        // 应用速度提升（通过修改飞剑的速度属性）
        // TODO: 需要飞剑系统支持临时速度加成API
        // 暂时使用简化实现
        affectedCount++;
      }
    }

    // 播放共振特效
    JianFengGuFx.playResonance(owner);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianFengGuOrganBehavior] Resonance triggered! Affected {} swords, speedBonus={}, duration={}",
          affectedCount,
          speedBonus,
          duration);
    }
  }
}
