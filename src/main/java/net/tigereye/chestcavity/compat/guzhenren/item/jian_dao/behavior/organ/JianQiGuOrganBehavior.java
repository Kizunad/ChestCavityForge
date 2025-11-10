package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianQiGuCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.JianQiGuSlashProjectile;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianQiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 剑气蛊（四转·剑道主动+被动器官）行为实现。
 *
 * <p>主动技能「一斩开天」：
 * <ul>
 *   <li>消耗真元/精力/念头，发射强力直线剑气斩击</li>
 *   <li>可命中多个实体并破坏受控方块</li>
 *   <li>每次命中后威能衰减</li>
 * </ul>
 *
 * <p>被动技能「气断山河」：
 * <ul>
 *   <li>每次有效命中增加断势层数</li>
 *   <li>每满3层为下一次「一斩开天」提供威能加成和衰减豁免</li>
 *   <li>10秒未施放则层数清空</li>
 * </ul>
 *
 * <p>非玩家生物 OnHit 触发：
 * <ul>
 *   <li>近战命中时有15%概率触发「一斩开天」</li>
 *   <li>触发后进入1分钟冷却</li>
 * </ul>
 */
public enum JianQiGuOrganBehavior
    implements OrganOnHitListener, OrganSlowTickListener {
  INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger(JianQiGuOrganBehavior.class);

  public static final String MOD_ID = "guzhenren";
  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, JianQiGuTuning.ORGAN_ID);

  /** 主动技能 ID（用于注册与客户端热键绑定）。*/
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, JianQiGuTuning.ABILITY_ID);

  private static final String STATE_ROOT = "JianQiGu";
  private static final String KEY_DUANSHI_STACKS = "DuanshiStacks";
  private static final String KEY_LAST_CAST_TICK = "LastCastTick";
  private static final String KEY_NPC_TRIGGER_COOLDOWN_UNTIL = "NpcTriggerCooldownUntil";

  static {
    // 注册主动技能激活入口
    OrganActivationListeners.register(ABILITY_ID, JianQiGuOrganBehavior::activateAbility);
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

    // 查找剑气蛊器官
    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    // 构建状态与冷却管理器
    OrganState state = OrganState.of(organ, STATE_ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();

    long now = player.serverLevel().getGameTime();

    // 执行主动技
    performYiZhanKaiTian(player, cc, organ, state, cooldown, now);
  }

  /**
   * 在玩家胸腔中查找剑气蛊器官。
   *
   * @param cc 胸腔实例
   * @return 剑气蛊物品栈，若未找到返回 EMPTY
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
      if (itemId != null && itemId.equals(ORGAN_ID)) {
        return stack;
      }
    }

    return ItemStack.EMPTY;
  }

  /**
   * 执行主动技「一斩开天」。
   *
   * @param caster 施法者
   * @param cc 胸腔实例
   * @param organ 器官物品
   * @param state 器官状态
   * @param cooldown 冷却管理器
   * @param now 当前游戏时间
   */
  private static void performYiZhanKaiTian(
      LivingEntity caster,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {

    if (!(caster.level() instanceof ServerLevel level)) {
      return;
    }

    // 检查冷却
    MultiCooldown.Entry mainCooldown = cooldown.entry("main_ability").withDefault(0L);
    if (now < mainCooldown.getReadyTick()) {
      // TODO: 向玩家发送冷却中的消息
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "[JianQiGuOrganBehavior] Ability on cooldown for {}",
            caster.getName().getString());
      }
      return;
    }

    // 检查并消耗资源
    if (caster instanceof ServerPlayer player) {
      boolean consumed =
          ResourceOps.openHandle(player)
              .map(
                  h -> {
                    // 检查真元（BURST）
                    double zhenyuan = h.read("zhenyuan_burst").orElse(0.0);
                    if (zhenyuan < JianQiGuTuning.COST_ZHENYUAN_BURST) {
                      return false;
                    }

                    // 检查精力
                    double jingli = h.read("jingli").orElse(0.0);
                    if (jingli < JianQiGuTuning.COST_JINGLI) {
                      return false;
                    }

                    // 检查念头
                    double niantou = h.read("niantou").orElse(0.0);
                    if (niantou < JianQiGuTuning.COST_NIANTOU) {
                      return false;
                    }

                    // 消耗资源
                    h.add("zhenyuan_burst", -JianQiGuTuning.COST_ZHENYUAN_BURST);
                    h.add("jingli", -JianQiGuTuning.COST_JINGLI);
                    h.add("niantou", -JianQiGuTuning.COST_NIANTOU);
                    return true;
                  })
              .orElse(false);

      if (!consumed) {
        // TODO: 向玩家发送资源不足的消息
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "[JianQiGuOrganBehavior] Insufficient resources for {}",
              player.getName().getString());
        }
        return;
      }
    }

    // 读取道痕和流派经验
    double daohen =
        ResourceOps.openHandle(caster)
            .flatMap(h -> h.read("daohen_jiandao"))
            .orElse(0.0);

    double liupaiExp =
        ResourceOps.openHandle(caster)
            .flatMap(h -> h.read("liupai_jiandao"))
            .orElse(0.0);

    // 读取并消耗断势层数
    int currentStacks = state.getInt(KEY_DUANSHI_STACKS, 0);
    int triggers = JianQiGuCalc.computeDuanshiTriggers(currentStacks);
    int decayGrace = JianQiGuCalc.computeDecayGrace(triggers);

    // 消耗断势层数（使用后清空）
    if (triggers > 0) {
      state.setInt(KEY_DUANSHI_STACKS, 0);
    }

    // 计算初始威能
    double initialPower = JianQiGuCalc.computeInitialDamage(daohen, liupaiExp, triggers);

    // 生成剑气斩击投射物
    spawnSlashProjectile(caster, level, initialPower, decayGrace);

    // 更新施放时间
    state.setLong(KEY_LAST_CAST_TICK, now);

    // 设置冷却
    mainCooldown.setReadyAt(now + JianQiGuTuning.COOLDOWN_TICKS);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianQiGuOrganBehavior] {} cast Yi Zhan Kai Tian: power={}, triggers={}, grace={}",
          caster.getName().getString(),
          initialPower,
          triggers,
          decayGrace);
    }
  }

  /**
   * 生成剑气斩击投射物。
   *
   * @param caster 施法者
   * @param level 服务端世界
   * @param initialPower 初始威能
   * @param decayGrace 衰减豁免次数
   */
  private static void spawnSlashProjectile(
      LivingEntity caster, ServerLevel level, double initialPower, int decayGrace) {

    // 计算起始位置（施法者眼睛位置）
    Vec3 eyePos = caster.getEyePosition(1.0f);

    // 计算方向（施法者视线方向）
    Vec3 direction = caster.getLookAngle().normalize();

    // 创建投射物
    JianQiGuSlashProjectile projectile =
        new JianQiGuSlashProjectile(CCEntities.JIAN_QI_GU_SLASH.get(), level);

    // 初始化投射物
    projectile.initialize(caster, eyePos, direction, initialPower, decayGrace);

    // 添加到世界
    level.addFreshEntity(projectile);
  }

  /**
   * OnHit 钩子：非玩家触发逻辑。
   *
   * <p>注意：断势层数的增加已移至 JianQiGuSlashProjectile 的命中回调中，
   * 以确保只有主动斩击的有效命中才会叠层，而非普通攻击。
   *
   * @param source 伤害源
   * @param attacker 攻击者（宿主）
   * @param target 目标
   * @param cc 胸腔实例
   * @param organ 剑气蛊器官物品
   * @param damage 原始伤害
   * @return 原始伤害（不修改）
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

    // 非玩家触发逻辑
    if (!(attacker instanceof net.minecraft.world.entity.player.Player)) {
      tryTriggerNpcActiveOnHit(attacker, target, cc, organ, state, now);
    }

    return damage;
  }

  /**
   * 非玩家生物 OnHit 触发主动技逻辑。
   *
   * @param attacker 攻击者（非玩家）
   * @param target 目标
   * @param cc 胸腔实例
   * @param organ 器官物品
   * @param state 器官状态
   * @param now 当前游戏时间
   */
  private void tryTriggerNpcActiveOnHit(
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      long now) {

    // 检查冷却
    long cooldownUntil = state.getLong(KEY_NPC_TRIGGER_COOLDOWN_UNTIL, 0L);
    if (now < cooldownUntil) {
      return; // 冷却中
    }

    // 随机判定
    if (attacker.getRandom().nextFloat() >= JianQiGuTuning.NPC_ACTIVE_CHANCE) {
      return; // 未触发
    }

    // 触发主动技
    if (!(attacker.level() instanceof ServerLevel level)) {
      return;
    }

    // 读取道痕和流派经验（非玩家可能没有，使用默认值）
    double daohen =
        ResourceOps.openHandle(attacker)
            .flatMap(h -> h.read("daohen_jiandao"))
            .orElse(0.0);

    double liupaiExp =
        ResourceOps.openHandle(attacker)
            .flatMap(h -> h.read("liupai_jiandao"))
            .orElse(0.0);

    // 读取并消耗断势层数
    int currentStacks = state.getInt(KEY_DUANSHI_STACKS, 0);
    int triggers = JianQiGuCalc.computeDuanshiTriggers(currentStacks);
    int decayGrace = JianQiGuCalc.computeDecayGrace(triggers);

    if (triggers > 0) {
      state.setInt(KEY_DUANSHI_STACKS, 0);
    }

    // 计算初始威能
    double initialPower = JianQiGuCalc.computeInitialDamage(daohen, liupaiExp, triggers);

    // 生成剑气斩击投射物
    spawnSlashProjectile(attacker, level, initialPower, decayGrace);

    // 设置冷却
    state.setLong(KEY_NPC_TRIGGER_COOLDOWN_UNTIL, now + JianQiGuTuning.NPC_ONHIT_COOLDOWN_TICKS);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianQiGuOrganBehavior] NPC {} auto-triggered Yi Zhan Kai Tian: power={}, cooldownUntil={}",
          attacker.getName().getString(),
          initialPower,
          now + JianQiGuTuning.NPC_ONHIT_COOLDOWN_TICKS);
    }
  }

  /**
   * SlowTick 钩子：检查断势层数是否过期。
   *
   * @param entity 宿主实体
   * @param cc 胸腔实例
   * @param organ 剑气蛊器官物品
   */
  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    // 仅服务端处理
    if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) {
      return;
    }

    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = level.getGameTime();

    // 检查断势层数是否过期
    long lastCastTick = state.getLong(KEY_LAST_CAST_TICK, 0L);
    int currentStacks = state.getInt(KEY_DUANSHI_STACKS, 0);

    if (currentStacks > 0 && (now - lastCastTick) >= JianQiGuTuning.STACK_EXPIRE_TICKS) {
      // 过期，清空层数
      state.setInt(KEY_DUANSHI_STACKS, 0);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "[JianQiGuOrganBehavior] {} duanshi stacks expired and cleared",
            entity.getName().getString());
      }
    }
  }

  /**
   * 确保附着逻辑（用于玩家）。
   *
   * @param entity 宿主实体
   * @param cc 胸腔实例
   * @param organ 器官物品
   */
  public void ensureAttached(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    // 初始化状态（如果需要）
    OrganState state = OrganState.of(organ, STATE_ROOT);

    // 确保关键字段存在
    if (!state.contains(KEY_DUANSHI_STACKS)) {
      state.setInt(KEY_DUANSHI_STACKS, 0);
    }

    if (!state.contains(KEY_LAST_CAST_TICK)) {
      state.setLong(KEY_LAST_CAST_TICK, 0L);
    }

    if (!state.contains(KEY_NPC_TRIGGER_COOLDOWN_UNTIL)) {
      state.setLong(KEY_NPC_TRIGGER_COOLDOWN_UNTIL, 0L);
    }
  }
}
