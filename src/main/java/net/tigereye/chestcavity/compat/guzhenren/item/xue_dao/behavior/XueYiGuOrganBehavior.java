package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.XueFengJiBiSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.XueShuShouJinSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.XueYongPiShenSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.YiXueFanCiSkill;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageContext;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;

/**
 * 血衣蛊 (Xue Yi Gu) - Blood Robe Gu
 *
 * <p>以防御为导向的器官，具有血液操纵能力。
 *
 * <p>4 个主动技能：
 * - 血涌披身 (Blood Aura): 切换光环，在半径范围内应用出血DoT
 * - 血束收紧 (Blood Bind): 光束攻击，附带减速 + 出血
 * - 血缝急闭 (Blood Seal): 将敌人的出血转化为吸收
 * - 溢血反刺 (Blood Reflect): 3秒窗口，反射近战伤害为出血
 *
 * <p>6 个被动技能：
 * - 血衣 (Blood Armor): 被近战击中时获得护甲层数
 * - 渗透 (Penetration): 每2次近战击中应用出血DoT
 * - 越染越坚 (Enraged Defense): 低于50% HP 时获得防御 + 光环提升
 * - 血偿 (Blood Reward): 杀死出血敌人以恢复真元
 * - 凝血止创 (Hardened Shield): 承受200点伤害以获得吸收
 * - 代价 (Cost): 主动技能额外消耗2%当前HP
 *
 * <p>5 个协同技能（与其他器官）
 */
public final class XueYiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener,
        OrganIncomingDamageListener,
        OrganOnHitListener,
        OrganRemovalListener {

  public static final XueYiGuOrganBehavior INSTANCE = new XueYiGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String LOG_PREFIX = "[Xue Yi Gu]";

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xueyigu");
  private static final String STATE_ROOT = "XueYiGu";

  // 被动技能的状态键
  private static final String ARMOR_STACKS_KEY = "ArmorStacks";
  private static final String LAST_DAMAGE_TICK_KEY = "LastDamageTick";
  private static final String HIT_COUNTER_KEY = "HitCounter";
  private static final String PENETRATE_READY_AT_KEY = "PenetrateReadyAt";
  private static final String ENRAGED_UNTIL_KEY = "EnragedUntil";
  private static final String DAMAGE_ACCUMULATOR_KEY = "DamageAccum";
  private static final String ABSORPTION_READY_AT_KEY = "AbsorptionReadyAt";

  // 被动参数
  private static final int ARMOR_MAX_STACKS = 10;
  private static final float ARMOR_DAMAGE_REDUCTION_PER_STACK = 0.01f; // 每层1%
  private static final int ARMOR_DECAY_DELAY_TICKS = 200; // 10秒
  private static final int PENETRATE_COOLDOWN_TICKS = 40; // 2秒
  private static final int ENRAGE_COOLDOWN_TICKS = 400; // 20秒
  private static final float ENRAGE_HEALTH_THRESHOLD = 0.5f; // 50% HP
  private static final float ENRAGE_DAMAGE_REDUCTION = 0.1f; // +10% DR
  private static final float ENRAGE_AURA_BOOST = 0.2f; // +20% 光环伤害
  private static final float HARDENED_SHIELD_DAMAGE_THRESHOLD = 200.0f;
  private static final int HARDENED_SHIELD_COOLDOWN_TICKS = 600; // 30秒
  private static final float LIFE_COST_PERCENTAGE = 0.02f; // 2%当前HP

  private XueYiGuOrganBehavior() {}

  /**
   * 在装备器官时调用以初始化状态。
   */
  public void onEquip(
      ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
    if (cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    // 注册移除监听器
    registerRemovalHook(cc, organ, this, staleRemovalContexts);

    // 如需初始化状态
    OrganState state = organState(organ, STATE_ROOT);
    if (state.getInt(ARMOR_STACKS_KEY, -1) == -1) {
      state.setInt(ARMOR_STACKS_KEY, 0);
      state.setLong(LAST_DAMAGE_TICK_KEY, 0L);
      state.setInt(HIT_COUNTER_KEY, 0);
      state.setLong(PENETRATE_READY_AT_KEY, 0L);
      state.setLong(ENRAGED_UNTIL_KEY, 0L);
      state.setDouble(DAMAGE_ACCUMULATOR_KEY, 0.0);
      state.setLong(ABSORPTION_READY_AT_KEY, 0L);
    }

    sendSlotUpdate(cc, organ);
  }

  // ============================================================
  // 慢速 tick - 主动技能和被动维护
  // ============================================================

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player) || cc == null || organ == null) {
      return;
    }
    if (entity.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    // tick主动技能
    XueYongPiShenSkill.tickAura(player, cc, organ);
    YiXueFanCiSkill.tickReflectWindow(player, cc, organ);

    // tick被动: 护甲层数衰减
    tickArmorStackDecay(player, organ, cc);

    // tick被动: 狂暴状态维护
    tickEnrageStatus(player, organ);
  }

  // ============================================================
  // 受伤监听器 - 被动和反刺
  // ============================================================

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(victim instanceof ServerPlayer player)) {
      return damage;
    }

    if (cc == null) {
      return damage;
    }

    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);

    float finalDamage = damage;

    // 被动: 血衣（基于层数减少伤害）
    if (isMeleeDamage(source)) {
      int armorStacks = state.getInt(ARMOR_STACKS_KEY, 0);
      if (armorStacks > 0) {
        float reduction = armorStacks * ARMOR_DAMAGE_REDUCTION_PER_STACK;
        finalDamage *= (1.0f - reduction);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "{} Armor stacks {} reduced damage from {} to {}",
              LOG_PREFIX,
              armorStacks,
              damage,
              finalDamage);
        }
      }

      // 从近战击中获得护甲层数
      gainArmorStack(player, organ, state, cc);
    }

    // 被动: 狂暴防御（低于50% HP）
    if (isEnraged(player, state)) {
      finalDamage *= (1.0f - ENRAGE_DAMAGE_REDUCTION);
    }

    // 主动: 血反刺（如果窗口激活且近战）
    if (isMeleeDamage(source) && source.getEntity() instanceof LivingEntity attacker) {
      YiXueFanCiSkill.handleReflectDamage(player, attacker, finalDamage, cc);
    }

    // 被动: 凝血盾牌（累积伤害）
    accumulateDamageForShield(player, organ, state, cc, finalDamage);

    return finalDamage;
  }

  // ============================================================
  // 击中监听器 - 渗透被动
  // ============================================================

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof ServerPlayer player)) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);

    // 被动: 渗透（每2次击中应用出血）
    handlePenetration(player, organ, state, cc, target);

    return damage;
  }

  // ============================================================
  // 移除监听器
  // ============================================================

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    // 停用所有主动技能
    XueYongPiShenSkill.forceDeactivate(organ);
    YiXueFanCiSkill.forceDeactivate(organ);

    // 清除所有被动状态
    OrganState state = organState(organ, STATE_ROOT);
    state.setInt(ARMOR_STACKS_KEY, 0);
    state.setInt(HIT_COUNTER_KEY, 0);
    state.setDouble(DAMAGE_ACCUMULATOR_KEY, 0.0);
  }

  // ============================================================
  // 被动技能实现
  // ============================================================

  /**
   * 被动1: 血衣 - 被近战击中时获得护甲层数。
   */
  private void gainArmorStack(
      ServerPlayer player, ItemStack organ, OrganState state, ChestCavityInstance cc) {
    int currentStacks = state.getInt(ARMOR_STACKS_KEY, 0);
    if (currentStacks >= ARMOR_MAX_STACKS) {
      return;
    }

    currentStacks++;
    state.setInt(ARMOR_STACKS_KEY, currentStacks);
    state.setLong(LAST_DAMAGE_TICK_KEY, player.level().getGameTime());

    // 播放视觉效果
    if (player.level() instanceof ServerLevel serverLevel) {
      XueYiGuEffects.playArmorStackGain(serverLevel, player, currentStacks);
    }

    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  /**
   * tick护甲层数衰减（在10秒无伤害后每秒失去1层）。
   */
  private void tickArmorStackDecay(ServerPlayer player, ItemStack organ, ChestCavityInstance cc) {
    OrganState state = organState(organ, STATE_ROOT);
    int currentStacks = state.getInt(ARMOR_STACKS_KEY, 0);
    if (currentStacks == 0) {
      return;
    }

    long lastDamageTick = state.getLong(LAST_DAMAGE_TICK_KEY, 0L);
    long now = player.level().getGameTime();
    long timeSinceLastDamage = now - lastDamageTick;

    if (timeSinceLastDamage >= ARMOR_DECAY_DELAY_TICKS) {
      // 开始衰减: 每秒失去1层（20 ticks）
      long ticksSinceDecayStart = timeSinceLastDamage - ARMOR_DECAY_DELAY_TICKS;
      int stacksToLose = (int) (ticksSinceDecayStart / 20);

      if (stacksToLose > 0) {
        currentStacks = Math.max(0, currentStacks - stacksToLose);
        state.setInt(ARMOR_STACKS_KEY, currentStacks);
        state.setLong(
            LAST_DAMAGE_TICK_KEY, now - ARMOR_DECAY_DELAY_TICKS - (ticksSinceDecayStart % 20));
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
      }
    }
  }

  /**
   * 被动2: 渗透 - 每2次近战击中应用出血DoT。
   */
  private void handlePenetration(
      ServerPlayer player,
      ItemStack organ,
      OrganState state,
      ChestCavityInstance cc,
      LivingEntity target) {
    long now = player.level().getGameTime();
    long readyAt = state.getLong(PENETRATE_READY_AT_KEY, 0L);

    if (now < readyAt) {
      return; // 冷却中
    }

    int hitCount = state.getInt(HIT_COUNTER_KEY, 0);
    hitCount++;

    if (hitCount >= 2) {
      // 应用渗透出血
      applyPenetrationBleed(player, target);

      // 播放效果
      if (player.level() instanceof ServerLevel serverLevel) {
        XueYiGuEffects.playPenetrateEffect(serverLevel, target);
      }

      // 重置计数器并设置冷却
      hitCount = 0;
      state.setLong(PENETRATE_READY_AT_KEY, now + PENETRATE_COOLDOWN_TICKS);
    }

    state.setInt(HIT_COUNTER_KEY, hitCount);
  }

  /**
   * 对目标应用渗透出血。
   */
  private void applyPenetrationBleed(ServerPlayer player, LivingEntity target) {
    // TODO: 应用实际出血DoT（6伤害/秒持续3秒）
    // 目前，应用瞬时伤害
    target.hurt(player.damageSources().magic(), 6.0f);
  }

  /**
   * 被动3: 狂暴防御 - 检查并激活狂暴状态。
   */
  private boolean isEnraged(ServerPlayer player, OrganState state) {
    long now = player.level().getGameTime();
    long enragedUntil = state.getLong(ENRAGED_UNTIL_KEY, 0L);

    if (now < enragedUntil) {
      return true; // 已经狂暴
    }

    // 检查是否应触发狂暴
    float healthPercent = player.getHealth() / player.getMaxHealth();
    if (healthPercent <= ENRAGE_HEALTH_THRESHOLD) {
      // 触发狂暴
      long enrageEnd = now + ENRAGE_COOLDOWN_TICKS;
      state.setLong(ENRAGED_UNTIL_KEY, enrageEnd);

      // 播放激活效果
      if (player.level() instanceof ServerLevel serverLevel) {
        XueYiGuEffects.playEnrageActivation(serverLevel, player);
      }

      return true;
    }

    return false;
  }

  /**
   * 维护狂暴视觉效果。
   */
  private void tickEnrageStatus(ServerPlayer player, ItemStack organ) {
    OrganState state = organState(organ, STATE_ROOT);
    long now = player.level().getGameTime();
    long enragedUntil = state.getLong(ENRAGED_UNTIL_KEY, 0L);

    if (now < enragedUntil && now % 10 == 0) {
      // 每0.5秒播放维护效果
      // XueYiGuEffects.playEnrageMaintain(...) if needed
    }
  }

  /**
   * 被动5: 凝血盾牌 - 累积伤害以获得吸收。
   */
  private void accumulateDamageForShield(
      ServerPlayer player,
      ItemStack organ,
      OrganState state,
      ChestCavityInstance cc,
      float damage) {
    long now = player.level().getGameTime();
    long readyAt = state.getLong(ABSORPTION_READY_AT_KEY, 0L);

    if (now < readyAt) {
      return; // 冷却中
    }

    double accumulated = state.getDouble(DAMAGE_ACCUMULATOR_KEY, 0.0);
    accumulated += damage;

    if (accumulated >= HARDENED_SHIELD_DAMAGE_THRESHOLD) {
      // 触发盾牌
      float absorptionAmount = 30.0f; // 固定金额
      applyHardenedShield(player, absorptionAmount);

      // 播放效果
      if (player.level() instanceof ServerLevel serverLevel) {
        XueYiGuEffects.playHardenedBloodShield(serverLevel, player, absorptionAmount);
      }

      // 重置并设置冷却
      accumulated = 0.0;
      state.setLong(ABSORPTION_READY_AT_KEY, now + HARDENED_SHIELD_COOLDOWN_TICKS);
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    state.setDouble(DAMAGE_ACCUMULATOR_KEY, accumulated);
  }

  /**
   * 应用凝血盾牌吸收。
   */
  private void applyHardenedShield(ServerPlayer player, float amount) {
    // 添加吸收效果
    player.addEffect(
        new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 0, false, false, true));
    player.setAbsorptionAmount(player.getAbsorptionAmount() + amount);
  }

  /**
   * 被动6: 代价 - 为主动技能扣除额外HP代价。
   */
  public static void applyLifeCost(ServerPlayer player, ChestCavityInstance cc) {
    float currentHealth = player.getHealth();
    float cost = currentHealth * LIFE_COST_PERCENTAGE;

    if (currentHealth - cost <= 1.0f) {
      // 不要让玩家因代价而死
      cost = currentHealth - 1.0f;
    }

    if (cost > 0) {
      GuzhenrenResourceCostHelper.drainHealth(player, cost, 1.0f, player.damageSources().generic());

      // 播放效果
      if (player.level() instanceof ServerLevel serverLevel) {
        XueYiGuEffects.playLifeCost(serverLevel, player);
      }
    }
  }

  // ============================================================
  // 工具方法
  // ============================================================

  private boolean isMeleeDamage(DamageSource source) {
    return source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) == false
        && source.getDirectEntity() instanceof LivingEntity;
  }

  private Optional<ItemStack> findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return Optional.empty();
    }

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty()) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (ORGAN_ID.equals(itemId)) {
          return Optional.of(stack);
        }
      }
    }

    return Optional.empty();
  }

  // 引导所有主动技能
  public static void bootstrapSkills() {
    XueYongPiShenSkill.bootstrap();
    XueShuShouJinSkill.bootstrap();
    XueFengJiBiSkill.bootstrap();
    YiXueFanCiSkill.bootstrap();
  }
}
