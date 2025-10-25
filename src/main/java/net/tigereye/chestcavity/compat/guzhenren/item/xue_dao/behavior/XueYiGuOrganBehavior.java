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
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.XueFengJiBiSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.XueShuShouJinSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.XueYongPiShenSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.YiXueFanCiSkill;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
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
 * <p>4 个主动技能： - 血涌披身 (Blood Aura): 切换光环，在半径范围内应用出血DoT - 血束收紧 (Blood Bind): 光束攻击，附带减速 + 出血 - 血缝急闭
 * (Blood Seal): 将敌人的出血转化为吸收 - 溢血反刺 (Blood Reflect): 3秒窗口，反射近战伤害为出血
 *
 * <p>6 个被动技能： - 血衣 (Blood Armor): 被近战击中时获得护甲层数 - 渗透 (Penetration): 每2次近战击中应用出血DoT - 越染越坚 (Enraged
 * Defense): 低于50% HP 时获得防御 + 光环提升 - 血偿 (Blood Reward): 杀死出血敌人以恢复真元 - 凝血止创 (Hardened Shield):
 * 承受200点伤害以获得吸收 - 代价 (Cost): 主动技能额外消耗2%当前HP
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
  private static final String BLOOD_REWARD_READY_AT_KEY = "BloodRewardReadyAt";
  private static final String SYNERGY_FORCE_TAKE_READY_AT_KEY = "SynergyForceTakeReadyAt";

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
  private static final int BLOOD_REWARD_COOLDOWN_TICKS = 160; // 8秒
  private static final double BLOOD_REWARD_ZHENYUAN_AMOUNT = 50.0; // 返还真元量

  // 联动技能参数
  private static final int SYNERGY_FORCE_TAKE_COOLDOWN_TICKS = 300; // 15秒
  private static final double SYNERGY_FORCE_TAKE_ZHENYUAN = 30.0; // 返还真元量
  private static final int SYNERGY_FORCE_TAKE_FOOD = 2; // 返还饱食度

  static {
    // 注册血偿被动的击杀监听器
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
        XueYiGuOrganBehavior::onLivingDeath);
  }

  private XueYiGuOrganBehavior() {}

  /** 在装备器官时调用以初始化状态。 */
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
      state.setLong(BLOOD_REWARD_READY_AT_KEY, 0L);
      state.setLong(SYNERGY_FORCE_TAKE_READY_AT_KEY, 0L);
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

    // 联动4: 强取回流（对流血目标首次近战命中吸取饱食和真元）
    handleSynergyForceTake(player, organ, state, cc, target);

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

  /** 被动1: 血衣 - 被近战击中时获得护甲层数。 */
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

  /** tick护甲层数衰减（在10秒无伤害后每秒失去1层）。 */
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

  /** 被动2: 渗透 - 每2次近战击中应用出血DoT。 */
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

  /** 对目标应用渗透出血。 */
  private void applyPenetrationBleed(ServerPlayer player, LivingEntity target) {
    // 应用流血DoT：6伤害/秒持续3秒
    net.tigereye.chestcavity.engine.dot.DoTEngine.schedulePerSecond(
        player,
        target,
        6.0, // 每秒6点伤害
        3, // 持续3秒
        null, // 无音效
        0.0f,
        0.0f,
        net.tigereye.chestcavity.util.DoTTypes.XIE_WANG_BLEED, // 流血DoT类型
        null,
        net.tigereye.chestcavity.engine.dot.DoTEngine.FxAnchor.TARGET,
        net.minecraft.world.phys.Vec3.ZERO,
        1.0f);
  }

  /** 被动3: 狂暴防御 - 检查并激活狂暴状态。 */
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

  /** 维护狂暴视觉效果。 */
  private void tickEnrageStatus(ServerPlayer player, ItemStack organ) {
    OrganState state = organState(organ, STATE_ROOT);
    long now = player.level().getGameTime();
    long enragedUntil = state.getLong(ENRAGED_UNTIL_KEY, 0L);

    if (now < enragedUntil && now % 10 == 0) {
      // 每0.5秒播放维护效果
      // XueYiGuEffects.playEnrageMaintain(...) if needed
    }
  }

  /** 被动5: 凝血盾牌 - 累积伤害以获得吸收。 */
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

  /** 应用凝血盾牌吸收。 */
  private void applyHardenedShield(ServerPlayer player, float amount) {
    // 添加吸收效果
    player.addEffect(
        new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 0, false, false, true));
    player.setAbsorptionAmount(player.getAbsorptionAmount() + amount);
  }

  /** 被动6: 代价 - 为主动技能扣除额外HP代价。 */
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

  // ============================================================
  // 联动技能辅助方法
  // ============================================================

  /** 检查玩家是否装备了指定的器官。 */
  private static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation organId) {
    if (cc == null || cc.inventory == null || organId == null) {
      return false;
    }

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty()) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (organId.equals(itemId)) {
          return true;
        }
      }
    }

    return false;
  }

  // 联动器官的ID定义
  private static final ResourceLocation TIEXUEGU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tiexuegu"); // 铁血蛊
  private static final ResourceLocation LZXQ_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "lizhánxueqiaogu"); // 历战血窍蛊
  private static final ResourceLocation PENXUE_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "penxuegu"); // 喷血蛊
  private static final ResourceLocation QIANGQU_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "qiangqugu"); // 强取蛊
  private static final ResourceLocation HUNDUN_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hundungu"); // 魂盾蛊

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

  // ============================================================
  // 被动4: 血偿 - 击杀流血敌人返还真元
  // 联动2: 历战回转 - 击杀精英时刷新血束收紧冷却
  // ============================================================

  /** 击杀事件监听器：实现血偿被动和历战回转联动。 */
  private static void onLivingDeath(
      net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
    DamageSource source = event.getSource();
    if (source == null) {
      return;
    }

    // 检查击杀者是否为玩家
    if (!(source.getEntity() instanceof ServerPlayer killer)) {
      return;
    }

    LivingEntity victim = event.getEntity();
    if (victim == null || victim.level().isClientSide()) {
      return;
    }

    // 检查玩家是否装备了血衣蛊
    Optional<net.tigereye.chestcavity.interfaces.ChestCavityEntity> ccEntityOpt =
        net.tigereye.chestcavity.interfaces.ChestCavityEntity.of(killer);
    if (ccEntityOpt.isEmpty()) {
      return;
    }

    ChestCavityInstance cc = ccEntityOpt.get().getChestCavityInstance();
    Optional<ItemStack> organOpt = INSTANCE.findOrgan(cc);
    if (organOpt.isEmpty()) {
      return;
    }

    ItemStack organ = organOpt.get();
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    long now = killer.level().getGameTime();

    // 被动4: 血偿 - 击杀流血敌人返还真元
    handleBloodReward(killer, victim, cc, organ, state, now);

    // 联动2: 历战回转 - 击杀精英时刷新血束收紧冷却
    handleEliteKillRefresh(killer, victim, cc, organ, state);
  }

  /** 处理血偿被动：击杀流血敌人返还真元。 */
  private static void handleBloodReward(
      ServerPlayer killer,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      long now) {
    // 检查冷却
    long readyAt = state.getLong(BLOOD_REWARD_READY_AT_KEY, 0L);
    if (now < readyAt) {
      return; // 冷却中
    }

    // 检查受害者是否有流血DoT
    boolean hasBleed = hasBleedingEffect(victim);
    if (!hasBleed) {
      return;
    }

    // 返还真元
    Optional<net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle>
        handleOpt =
            net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.open(killer);
    if (handleOpt.isPresent()) {
      net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle handle =
          handleOpt.get();
      handle.replenishScaledZhenyuan(BLOOD_REWARD_ZHENYUAN_AMOUNT, true);

      // 设置冷却
      state.setLong(BLOOD_REWARD_READY_AT_KEY, now + BLOOD_REWARD_COOLDOWN_TICKS);

      // 播放效果
      if (killer.level() instanceof ServerLevel serverLevel) {
        Vec3 victimPos = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
        XueYiGuEffects.playBloodReward(serverLevel, killer, victimPos);
      }

      NetworkUtil.sendOrganSlotUpdate(cc, organ);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "{} Blood Reward triggered: killed bleeding enemy, restored {} zhenyuan",
            LOG_PREFIX,
            BLOOD_REWARD_ZHENYUAN_AMOUNT);
      }
    }
  }

  /** 联动2: 历战回转 - 击杀精英怪物时刷新血束收紧冷却。 */
  private static void handleEliteKillRefresh(
      ServerPlayer killer, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, OrganState state) {
    // 检查是否装备了历战血窍蛊
    if (!hasOrgan(cc, LZXQ_GU_ID)) {
      return;
    }

    // 检查受害者是否为精英怪物（检测是否有发光效果或Boss标签）
    boolean isElite = victim.hasGlowingTag() || !victim.canChangeDimensions();

    // 或者检查是否为Boss级别的怪物
    if (!isElite && victim.getType().is(net.minecraft.tags.EntityTypeTags.RAIDERS)) {
      // 劫掠者也算作精英
      isElite = true;
    }

    if (!isElite) {
      return; // 不是精英，不触发
    }

    // 刷新血束收紧的冷却时间
    XueShuShouJinSkill.refreshCooldown(cc, organ, state);

    // 播放效果
    if (killer.level() instanceof ServerLevel serverLevel) {
      Vec3 killerPos = killer.position().add(0, killer.getBbHeight() * 0.5, 0);
      // 播放金色粒子效果表示冷却刷新
      serverLevel.sendParticles(
          net.minecraft.core.particles.ParticleTypes.WAX_ON,
          killerPos.x,
          killerPos.y,
          killerPos.z,
          15,
          0.3,
          0.3,
          0.3,
          0.1);
    }

    // 发送消息给玩家
    killer.displayClientMessage(
        net.minecraft.network.chat.Component.literal("历战回转：击杀精英，血束收紧冷却已刷新！"), true);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "{} Elite Kill Refresh triggered: killed elite {}, refreshed Blood Bind cooldown",
          LOG_PREFIX,
          victim.getName().getString());
    }
  }

  /** 检查实体是否有流血DoT效果。 */
  private static boolean hasBleedingEffect(LivingEntity entity) {
    if (entity == null) {
      return false;
    }

    // 检查实体是否有待处理的流血DoT
    List<net.tigereye.chestcavity.engine.dot.DoTEngine.DoTEntry> pendingDoTs =
        net.tigereye.chestcavity.engine.dot.DoTEngine.getPendingForTarget(entity.getUUID());

    for (net.tigereye.chestcavity.engine.dot.DoTEngine.DoTEntry entry : pendingDoTs) {
      if (net.tigereye.chestcavity.util.DoTTypes.XIE_WANG_BLEED.equals(entry.typeId())) {
        return true;
      }
    }

    return false;
  }

  // ============================================================
  // 联动技能实现
  // ============================================================

  /** 联动4: 强取回流 - 对流血目标首次近战命中吸取饱食和真元。 */
  private void handleSynergyForceTake(
      ServerPlayer player,
      ItemStack organ,
      OrganState state,
      ChestCavityInstance cc,
      LivingEntity target) {
    // 检查是否装备了强取蛊
    if (!hasOrgan(cc, QIANGQU_GU_ID)) {
      return;
    }

    long now = player.level().getGameTime();
    long readyAt = state.getLong(SYNERGY_FORCE_TAKE_READY_AT_KEY, 0L);

    if (now < readyAt) {
      return; // 冷却中
    }

    // 检查目标是否有流血DoT
    if (!hasBleedingEffect(target)) {
      return;
    }

    // 吸取饱食
    net.minecraft.world.food.FoodData foodData = player.getFoodData();
    if (foodData != null) {
      foodData.setFoodLevel(Math.min(20, foodData.getFoodLevel() + SYNERGY_FORCE_TAKE_FOOD));
    }

    // 返还真元
    Optional<net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle>
        handleOpt =
            net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.open(player);
    if (handleOpt.isPresent()) {
      handleOpt.get().replenishScaledZhenyuan(SYNERGY_FORCE_TAKE_ZHENYUAN, true);
    }

    // 设置冷却
    state.setLong(SYNERGY_FORCE_TAKE_READY_AT_KEY, now + SYNERGY_FORCE_TAKE_COOLDOWN_TICKS);

    // 播放效果
    if (player.level() instanceof ServerLevel serverLevel) {
      Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
      // 简单的粒子效果
      serverLevel.sendParticles(
          net.minecraft.core.particles.ParticleTypes.HEART,
          targetPos.x,
          targetPos.y,
          targetPos.z,
          3,
          0.3,
          0.3,
          0.3,
          0.1);
    }

    // 发送消息给玩家
    player.displayClientMessage(
        net.minecraft.network.chat.Component.literal(
            "强取回流：吸取了 " + SYNERGY_FORCE_TAKE_FOOD + " 饱食度和 " + SYNERGY_FORCE_TAKE_ZHENYUAN + " 真元！"),
        true);

    NetworkUtil.sendOrganSlotUpdate(cc, organ);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "{} Synergy Force Take triggered: drained {} food and {} zhenyuan from bleeding target",
          LOG_PREFIX,
          SYNERGY_FORCE_TAKE_FOOD,
          SYNERGY_FORCE_TAKE_ZHENYUAN);
    }
  }

  // ============================================================
  // 所有联动技能已实现完成
  // ============================================================
  // 联动1: 铁血披覆 - 已在 XueYongPiShenSkill 中实现（增强光环伤害）
  // 联动2: 历战回转 - 已在 handleEliteKillRefresh 中实现（击杀精英刷新血束收紧冷却）
  // 联动3: 血雾喷涌 - 已在 XueShuShouJinSkill 中实现（血束收紧命中后喷涌血雾）
  // 联动4: 强取回流 - 已在 handleSynergyForceTake 中实现（对流血目标吸取饱食和真元）
  // 联动5: 魂盾叠层 - 已在 XueFengJiBiSkill 中实现（增强吸收量）
}
