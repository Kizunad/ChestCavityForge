package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainHelper;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.JianXinDomain;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 剑心蛊（剑道·体质）
 *
 * <p>实现主动技能「心定冥想」的进入/退出/冷却与每秒循环效果（资源回复、剑势层累积）。
 *
 * <p>按约定：该器官的运行时状态不走 LinkageChannel，全部存储到 OrganState NBT 下的 "JianXinGu" 根键。
 */
public enum JianXinGuOrganBehavior
    implements OrganSlowTickListener,
        net.tigereye.chestcavity.listeners.OrganOnHitListener,
        OrganIncomingDamageListener {
  INSTANCE;

  public static final String MOD_ID = "guzhenren";
  public static final net.minecraft.resources.ResourceLocation ORGAN_ID =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_xin_gu");

  // 主动技能 ID（客户端仅以字面量加入热键列表）
  public static final net.minecraft.resources.ResourceLocation ABILITY_ID =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_xin_mingxiang");

  // OrganState 根键与字段
  private static final String STATE_ROOT = "JianXinGu";
  private static final String K_MEDITATING = "Meditating"; // boolean
  private static final String K_READY_AT = "MeditationReadyAt"; // long tick
  private static final String K_FREEZE_TICKS = "FreezeTicks"; // int ticks
  private static final String K_SWORD_MOMENTUM = "SwordMomentum"; // int [0,5]
  private static final String K_FOCUS_METER = "FocusMeter"; // int [0,100]
  private static final String K_FOCUS_LOCK_UNTIL = "FocusLockUntil"; // long tick
  private static final String K_DOMAIN_LEVEL = "DomainLevel"; // int {5,6}
  private static final String K_LAST_FOCUS_GAIN_AT = "FocusLastGainAt"; // long tick

  // 调参：迁移至 Tuning
  public static final int MEDITATION_COOLDOWN_T =
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning
          .JianXinGuTuning.MEDITATION_COOLDOWN_T;
  private static final int FREEZE_ON_BREAK_T =
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning
          .JianXinGuTuning.FREEZE_ON_BREAK_T;
  private static final double REGEN_JINGLI_PER_SEC =
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning
          .JianXinGuTuning.REGEN_JINGLI_PER_SEC;
  private static final int MAX_MOMENTUM =
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning
          .JianXinGuTuning.MAX_MOMENTUM;

  static {
    // 注册主动技触发入口
    OrganActivationListeners.register(ABILITY_ID, JianXinGuOrganBehavior::activateAbility);
  }

  // -------- 激活入口 --------
  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
      return;
    }
    ItemStack organ = findMatchingOrgan(cc);
    OrganState state = OrganState.of(organ, STATE_ROOT);
    MultiCooldown cd = MultiCooldown.builder(state).withSync(cc, organ).build();

    long now = player.serverLevel().getGameTime();
    boolean meditating = state.getBoolean(K_MEDITATING, false);
    long readyAt = state.getLong(K_READY_AT, 0L);

    if (meditating) {
      // 提前结束：退出冥想但不改变冷却（保留原始起始点）
      exitMeditation(player, state);
      return;
    }

    if (now < readyAt) {
      // 冷却中：直接提示
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx
          .JianXinGuFx.scheduleCooldownToast(player, ABILITY_ID, readyAt, now);
      return;
    }

    // 进入冥想：立即开始冷却（按规格：进入即开始 CD）
    state.setBoolean(K_MEDITATING, true);
    state.setLong(K_READY_AT, now + MEDITATION_COOLDOWN_T);
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx
        .JianXinGuFx.scheduleCooldownToast(
            player, ABILITY_ID, now + MEDITATION_COOLDOWN_T, now);

    // 应用冥想期移动减速（玩家侧属性 + NBT 存储）
    double dec =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning
            .JianXinGuTuning.JIAN_XIN_DOMAIN_VELOCITY_DECREASEMENT;
    net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags
        .setJianxinVelocityDecreasement(player, dec);
    net.tigereye.chestcavity.compat.guzhenren.domain.DomainMovementOps
        .applyMeditationSlow(player, dec);

    // 计算域等级与创建/刷新剑心域
    int domainLevel =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator
            .JianXinGuCalc.computeDomainLevel(player);
    state.setInt(K_DOMAIN_LEVEL, domainLevel);
    spawnOrRefreshDomain(player, domainLevel);
  }

  // -------- 事件：慢 Tick（每秒） --------
  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
      return;
    }
    OrganState state = OrganState.of(organ, STATE_ROOT);

    // 冻结衰减与“冻结期不增长剑势”
    int freeze = state.getInt(K_FREEZE_TICKS, 0);
    if (freeze > 0) {
      freeze = Math.max(0, freeze - 20);
      state.setInt(K_FREEZE_TICKS, freeze);
    }

    // 定心值自然衰减（1秒内未再受控）
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator
        .JianXinGuPassiveCalc.decayFocusIfIdle(player, state);

    boolean meditating = state.getBoolean(K_MEDITATING, false);
    if (!meditating) {
      return;
    }

    // 资源恢复（精力）
    ResourceOps.openHandle(player)
        .ifPresent(h -> h.adjustJingli(+REGEN_JINGLI_PER_SEC, true));

    // 剑势层 +1/s（冻结期不增长）
    if (freeze <= 0) {
      int momentum = state.getInt(K_SWORD_MOMENTUM, 0);
      if (momentum < MAX_MOMENTUM) {
        state.setInt(K_SWORD_MOMENTUM, momentum + 1);
      }
    }

    // 确保领域持续存在并跟随（若被外部移除则重建）
    int level =
        state.getInt(
            K_DOMAIN_LEVEL,
            net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator
                .JianXinGuCalc.computeDomainLevel(player));
    spawnOrRefreshDomain(player, level);
  }

  // -------- 事件：受到伤害（打断） --------
  @Override
  public float onIncomingDamage(
      DamageSource source, LivingEntity owner, ChestCavityInstance cc, ItemStack organ, float dmg) {
    if (!(owner instanceof ServerPlayer player) || owner.level().isClientSide()) {
      return dmg;
    }
    OrganState state = OrganState.of(organ, STATE_ROOT);
    boolean meditating = state.getBoolean(K_MEDITATING, false);
    if (!meditating) {
      return dmg;
    }
    // 若拥有“无视打断”，不退出
    if (DomainTags.hasUnbreakableFocus(player)) {
      return dmg;
    }
    // 退出冥想并赋予“失心惩罚”：冻结 2s + 技能冻结标签
    exitMeditation(player, state);
    state.setInt(K_FREEZE_TICKS, FREEZE_ON_BREAK_T);
    DomainTags.setJiandaoFrozen(player, FREEZE_ON_BREAK_T);
    // 冥想被打断也要继续应用域内防御倍率
    double pIn = net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags.getSwordDomainPin(player);
    return (float) (dmg * (pIn > 0.0 ? pIn : 1.0));
  }

  // -------- 事件：命中（近战/直接来源） --------
  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance chestCavity,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof ServerPlayer player) || attacker.level().isClientSide()) {
      return damage;
    }
    // 仅对“自身·近战/直接来源”加成（飞剑/投掷物另走实体路径）
    if (source == null || source.getEntity() != attacker) {
      return damage;
    }
    double pOut =
        net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags.getSwordDomainPout(player);
    if (!(pOut > 0.0)) {
      return damage;
    }
    return (float) (damage * pOut);
  }

  // -------- 内部方法 --------
  private static void exitMeditation(ServerPlayer player, OrganState state) {
    state.setBoolean(K_MEDITATING, false);
    // 移除领域
    DomainHelper.removeJianXinDomain(player);
    // 标签同步清理：DomainHelper 在 tick 中会清
    // 恢复移动速度
    net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags
        .setJianxinVelocityDecreasement(player, 0.0);
    net.tigereye.chestcavity.compat.guzhenren.domain.DomainMovementOps
        .removeMeditationSlow(player);
  }

  // 领域等级计算已移入 Calc

  private static void spawnOrRefreshDomain(ServerPlayer player, int level) {
    // DomainHelper.createOrGetJianXinDomain 会处理已有域的更新与客户端同步
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    int daohen = (int) Math.round(handleOpt.map(h -> h.read("daohen_jiandao").orElse(0.0)).orElse(0.0));
    int school = (int) Math.round(handleOpt.map(h -> h.read("liupai_jiandao").orElse(0.0)).orElse(0.0));
    JianXinDomain domain = DomainHelper.createOrGetJianXinDomain(player, daohen, school);
    // 中心位置跟随
    if (player.level() instanceof ServerLevel sl) {
      domain.setCenter(new Vec3(player.getX(), player.getY(), player.getZ()));
    }
  }

  private static ItemStack findMatchingOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    // 优先查找明确的剑心蛊物品
    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (s.isEmpty()) continue;
      net.minecraft.resources.ResourceLocation id =
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
      if (id != null && id.equals(ORGAN_ID)) {
        return s;
      }
    }
    // 回退：返回第一个非空栈，维持旧行为
    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (!s.isEmpty()) return s;
    }
    return ItemStack.EMPTY;
  }

  private static void ensureClassLoaded(Object it) {
    // no-op
  }

  // UI 文本格式化如需更复杂，可迁至 Fx。
}
