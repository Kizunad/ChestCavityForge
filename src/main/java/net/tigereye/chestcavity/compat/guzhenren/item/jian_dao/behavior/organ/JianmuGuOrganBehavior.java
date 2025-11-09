package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward.DefaultWardSwordService;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmuGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * 剑幕蛊器官行为（枚举单例）
 *
 * <p>核心功能：
 *
 * <ul>
 *   <li>被动：提供 +10 盔甲值
 *   <li>主动：召唤护幕飞剑进行拦截防御
 *   <li>玩家：手动开关，消耗精力与真元维持
 *   <li>非玩家：受伤自动开启（无消耗），脱战自动关闭
 * </ul>
 *
 * <p>注意：实际的护幕拦截逻辑由 BlockShield 系统（WardSwordService）提供，
 * 本类仅负责器官的激活/关闭与资源消耗。
 */
public enum JianmuGuOrganBehavior
    implements OrganSlowTickListener, OrganIncomingDamageListener {

  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  /** 器官物品ID */
  public static final ResourceLocation ORGAN_ID = JianmuGuTuning.ORGAN_ID;

  /** 主动技能ID */
  public static final ResourceLocation ABILITY_ID = JianmuGuTuning.ABILITY_ID;

  /** 剑道增幅效果ID */
  private static final ResourceLocation JIAN_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jian_dao_increase_effect");

  /** 盔甲值增幅效果ID */
  private static final ResourceLocation ARMOR_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/armor_increase_effect");

  // ========== 主动技能注册 ==========
  static {
    OrganActivationListeners.register(ABILITY_ID, JianmuGuOrganBehavior::activateAbility);
  }

  // ========== 主动技能：开关护幕 ==========

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

    if (!(entity instanceof Player player)) {
      // 非玩家不能手动激活
      return;
    }

    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    OrganState state = OrganState.of(organ, JianmuGuTuning.STATE_ROOT);
    boolean active = state.getBoolean(JianmuGuTuning.KEY_WARD_ACTIVE, false);

    if (active) {
      // 关闭护幕
      deactivateWard(entity, state, level);
    } else {
      // 开启护幕
      activateWard(entity, state, level, cc);
    }
  }

  /**
   * 开启护幕
   *
   * @param entity 实体
   * @param state 器官状态
   * @param level 世界
   * @param cc 胸腔实例
   */
  private static void activateWard(
      LivingEntity entity, OrganState state, ServerLevel level, ChestCavityInstance cc) {

    state.setBoolean(JianmuGuTuning.KEY_WARD_ACTIVE, true);

    // 生成护幕飞剑
    if (entity instanceof Player player) {
      DefaultWardSwordService.getInstance().ensureWardSwords(player);

      player.displayClientMessage(
          net.minecraft.network.chat.Component.literal("§6[剑幕蛊] §a护幕已开启"), true);
    }
  }

  /**
   * 关闭护幕
   *
   * @param entity 实体
   * @param state 器官状态
   * @param level 世界
   */
  private static void deactivateWard(LivingEntity entity, OrganState state, ServerLevel level) {

    state.setBoolean(JianmuGuTuning.KEY_WARD_ACTIVE, false);

    // 移除护幕飞剑
    if (entity instanceof Player player) {
      DefaultWardSwordService.getInstance().disposeWardSwords(player);

      player.displayClientMessage(
          net.minecraft.network.chat.Component.literal("§6[剑幕蛊] §c护幕已关闭"), true);
    }
  }

  // ========== 慢Tick监听：资源消耗与非玩家AI ==========

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity.level().isClientSide()) {
      return;
    }

    if (!(entity.level() instanceof ServerLevel level)) {
      return;
    }

    OrganState state = OrganState.of(organ, JianmuGuTuning.STATE_ROOT);
    boolean active = state.getBoolean(JianmuGuTuning.KEY_WARD_ACTIVE, false);

    if (!active) {
      return;
    }

    long now = level.getGameTime();

    if (entity instanceof Player player) {
      // 玩家：消耗精力与真元维持
      handlePlayerUpkeep(player, state, level, now, cc, organ);
    } else {
      // 非玩家：脱战检测
      handleNonPlayerDisengage(entity, state, level, now);
    }
  }

  /**
   * 处理玩家的资源消耗
   *
   * @param player 玩家
   * @param state 器官状态
   * @param level 世界
   * @param now 当前时间
   * @param cc 胸腔实例
   * @param organ 器官物品
   */
  private static void handlePlayerUpkeep(
      Player player,
      OrganState state,
      ServerLevel level,
      long now,
      ChestCavityInstance cc,
      ItemStack organ) {

    // 每 tick 消耗精力与真元（使用分级真元消耗）
    // 精力采用固定消耗，真元采用 3转1阶段 BURST 分级消耗
    java.util.OptionalDouble jingliResult =
        ResourceOps.tryAdjustJingli(player, -JianmuGuTuning.UPKEEP_JINGLI_PER_TICK, false);

    // 3转1阶段 BURST 真元消耗
    java.util.OptionalDouble zhenyuanResult =
        ResourceOps.tryConsumeTieredZhenyuan(player, 3, 1, Tier.BURST);

    boolean success = jingliResult.isPresent() && zhenyuanResult.isPresent();

    if (!success) {
      // 资源不足，自动关闭
      deactivateWard(player, state, level);
      player.displayClientMessage(
          net.minecraft.network.chat.Component.literal("§6[剑幕蛊] §c资源不足，护幕关闭"),
          true);
    }
  }

  /**
   * 处理非玩家的脱战检测
   *
   * @param entity 实体
   * @param state 器官状态
   * @param level 世界
   * @param now 当前时间
   */
  private static void handleNonPlayerDisengage(
      LivingEntity entity, OrganState state, ServerLevel level, long now) {

    // 检测是否脱战：无攻击目标 + 10秒内未受伤
    boolean hasTarget = false;
    if (entity instanceof Mob mob) {
      hasTarget = mob.getTarget() != null;
    }

    long lastHurt = state.getLong(JianmuGuTuning.KEY_LAST_HURT_TICK, 0L);
    long timeSinceHurt = now - lastHurt;

    boolean disengaged =
        !hasTarget && timeSinceHurt >= JianmuGuTuning.AI_DISENGAGE_NO_DAMAGE_TIME;

    if (disengaged) {
      // 脱战，关闭护幕
      deactivateWard(entity, state, level);
    }
  }

  // ========== 受击监听：非玩家自动开启 ==========

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {

    LivingEntity target = victim; // 重命名以保持逻辑清晰

    if (target.level().isClientSide()) {
      return damage;
    }

    if (!(target.level() instanceof ServerLevel level)) {
      return damage;
    }

    OrganState state = OrganState.of(organ, JianmuGuTuning.STATE_ROOT);
    long now = level.getGameTime();

    // 更新上次受伤时间
    state.setLong(JianmuGuTuning.KEY_LAST_HURT_TICK, now);

    // 非玩家：受到伤害时自动开启护幕
    if (!(target instanceof Player)) {
      boolean active = state.getBoolean(JianmuGuTuning.KEY_WARD_ACTIVE, false);
      if (!active) {
        activateWard(target, state, level, cc);
      }
    }

    return damage;
  }

  // ========== 确保附着：盔甲值增幅 ==========

  /**
   * 确保器官附着时初始化链接通道
   *
   * @param cc 胸腔实例
   */
  public void ensureAttached(ChestCavityInstance cc) {
    // 确保剑道增幅通道存在
    LedgerOps.ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT, NON_NEGATIVE);

    // 确保盔甲值增幅通道存在并设置为 +10
    LedgerOps.ensureChannel(cc, ARMOR_INCREASE_EFFECT, NON_NEGATIVE)
        .adjust(JianmuGuTuning.PASSIVE_ARMOR_BONUS);
  }

  // ========== 工具方法 ==========

  /**
   * 查找匹配的器官物品
   *
   * @param cc 胸腔实例
   * @return 器官物品，若不存在则返回空物品
   */
  private static ItemStack findMatchingOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }

      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        return stack;
      }
    }

    return ItemStack.EMPTY;
  }

  /**
   * 检查是否拥有器官
   *
   * @param cc 胸腔实例
   * @return 是否拥有
   */
  public static boolean hasOrgan(ChestCavityInstance cc) {
    return !findMatchingOrgan(cc).isEmpty();
  }

  /**
   * 检查护幕当前是否处于激活状态。
   *
   * @param cc 胸腔实例
   * @return true 表示护幕开启
   */
  public static boolean isWardActive(ChestCavityInstance cc) {
    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) {
      return false;
    }

    OrganState state = OrganState.of(organ, JianmuGuTuning.STATE_ROOT);
    return state.getBoolean(JianmuGuTuning.KEY_WARD_ACTIVE, false);
  }
}
