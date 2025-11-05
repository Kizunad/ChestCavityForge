package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.rift.RiftEntity;
import net.tigereye.chestcavity.compat.guzhenren.rift.RiftManager;
import net.tigereye.chestcavity.compat.guzhenren.rift.RiftType;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.Mob;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AIIntrospection;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ActiveSkillOps;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/**
 * 裂剑蛊器官行为（枚举单例）
 *
 * <p>五转剑道器官：
 *
 * <ul>
 *   <li>主动：裂刃空隙（放置裂隙）
 *   <li>被动：剑击生成微型裂隙（20%概率）
 * </ul>
 */
public enum LieJianGuOrganBehavior implements OrganSlowTickListener, OrganOnHitListener {

  INSTANCE;

  private static final String MOD_ID = "guzhenren";

  /** 器官物品ID */
  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "liejiangu");

  /** 主动技能ID */
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "lie_jian_gu_activate");

  // ========== OrganState字段键 ==========
  private static final String STATE_ROOT = "LieJian";
  private static final String ACTIVE_READY_KEY = "ActiveReadyAt"; // long - 技能就绪时间戳
  private static final String K_LAST_PASSIVE_RIFT = "LastPassiveRiftAt"; // long - 上次被动生成裂隙的时间
  private static final String K_LAST_ATTACK_GOALS = "LastAttackGoals"; // ListTag(StringTag)
  private static final String K_DISENGAGED_AT = "DisengagedAt"; // long - 脱战时间戳

  /** 脱战后延迟停止（tick） */
  private static final int DISENGAGE_DELAY_TICKS = 100; // 5秒

  /** 技能基础冷却（tick） */
  private static final int ABILITY_COOLDOWN = 8 * 20; // 8秒（基础）
  /** 技能最短冷却（tick） */
  private static final int MIN_COOLDOWN_TICKS = 1 * 20; // 1秒（流派经验满值）

  /** 被动生成微型裂隙的概率 */
  private static final double PASSIVE_RIFT_CHANCE = 0.2; // 20%

  /** 被动生成微型裂隙的冷却（防止同一次攻击多次触发） */
  private static final int PASSIVE_RIFT_COOLDOWN = 5; // 0.25秒

  // ========== 主动技能注册 ==========
  static {
    OrganActivationListeners.register(ABILITY_ID, LieJianGuOrganBehavior::activateAbility);
  }

  // ========== 主动技能：裂刃空隙 ==========

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

    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = level.getGameTime();

    // 使用 MultiCooldown 检查与记录冷却
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry(ACTIVE_READY_KEY);
    if (!ready.isReady(now)) {
      if (entity instanceof ServerPlayer player) {
        long remaining = ready.remaining(now);
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable(
                "guzhenren.ability.cooldown", remaining / 20.0),
            true);
      }
      return;
    }

    // 消耗真元：五转，阶段1，Burst级别
    OptionalDouble consumed = ResourceOps.tryConsumeTieredZhenyuan(
        entity, 5, 1, net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier.BURST);
    if (consumed.isEmpty()) {
      if (entity instanceof ServerPlayer player) {
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("guzhenren.ability.insufficient_resource"),
            true);
      }
      return;
    }

    // 计算额外持续时间（每100剑道道痕+6秒）
    double swordPath = ResourceOps.openHandle(entity)
        .map(h -> net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps.get(h, "daohen_jiandao"))
        .orElse(0.0);
    int extraDuration = (int) ((swordPath / 100.0) * 6 * 20); // 转换为tick

    // 伤害倍率：仅使用初始倍率（道痕增幅在穿刺伤害阶段单独计算）
    float damageMultiplier = (float) RiftType.MAJOR.initialDamageMultiplier;

    // 放置裂隙
    placeRift(level, entity, RiftType.MAJOR, extraDuration, damageMultiplier);

    // 计算冷却：剑道流派经验 [0, 50000] 线性映射到 [8s, 1s]
    double swordSchoolExp = ResourceOps.openHandle(entity)
        .map(h -> h.read("liupai_jiandao").orElse(0.0))
        .orElse(0.0);
    int scaledCooldown = computeCooldownTicks(swordSchoolExp);

    // 启动冷却并安排就绪提示
    long readyAt = now + scaledCooldown;
    ready.setReadyAt(readyAt);
    if (entity instanceof ServerPlayer sp) {
      ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, now);
    }

    // 反馈消息
    if (entity instanceof ServerPlayer player) {
      int totalSeconds = (RiftType.MAJOR.baseDuration + extraDuration) / 20;
      player.displayClientMessage(
          net.minecraft.network.chat.Component.translatable(
              "guzhenren.ability.lie_jian.activated", totalSeconds),
          true);
    }
  }

  /**
   * 将剑道流派经验线性映射为冷却时长。
   * 区间: exp∈[0,50000] -> cooldown∈[8s,1s]
   */
  private static int computeCooldownTicks(double swordSchoolExp) {
    final double MAX = 50000.0;
    double ratio = Math.max(0.0, Math.min(1.0, swordSchoolExp / MAX));
    double ticks = ABILITY_COOLDOWN - (ABILITY_COOLDOWN - MIN_COOLDOWN_TICKS) * ratio;
    return (int) Math.round(ticks);
  }

  /**
   * 放置裂隙
   *
   * @param level 世界
   * @param caster 施放者
   * @param type 裂隙类型
   * @param extraDuration 额外持续时间（tick）
   * @param damageMultiplier 伤害倍率（基于道痕加成）
   */
  private static void placeRift(
      ServerLevel level, LivingEntity caster, RiftType type, int extraDuration, float damageMultiplier) {

    // 获取放置位置（玩家前方地面）
    Vec3 pos = caster.position();
    Vec3 lookVec = caster.getLookAngle();
    Vec3 targetPos = pos.add(lookVec.x * 2.0, 0, lookVec.z * 2.0);

    // 贴地（寻找最近的地面）
    BlockPos blockPos = BlockPos.containing(targetPos);
    BlockPos groundPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos);
    Vec3 finalPos = new Vec3(targetPos.x, groundPos.getY() + 0.1, targetPos.z);

    // 创建裂隙实体
    RiftEntity rift = RiftEntity.create(level, finalPos, caster, type, extraDuration);

    // 应用伤害倍率（基于道痕）
    rift.setDamageMultiplier(damageMultiplier);

    // 添加到世界
    level.addFreshEntity(rift);

    // 注册到RiftManager
    RiftManager.getInstance().registerRift(rift);
  }

  /**
   * 查找匹配的器官
   *
   * @param cc 胸腔实例
   * @return 器官物品（如果没有则返回空）
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
      if (id == null) {
        continue;
      }

      if (id.equals(ORGAN_ID)) {
        return stack;
      }
    }

    return ItemStack.EMPTY;
  }

  // ========== 慢速Tick ==========
  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    // 玩家分支：无逻辑
    if (entity instanceof ServerPlayer) {
      return;
    }
    // 非 Mob 或客户端：无逻辑
    if (!(entity instanceof Mob mob) || entity.level().isClientSide) {
      return;
    }

    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = mob.level().getGameTime();
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();

    // 1. 战斗状态判定
    List<String> currentGoals = AIIntrospection.getRunningAttackGoalNames(mob);
    boolean inCombat = !currentGoals.isEmpty() && mob.getTarget() != null;

    ListTag lastGoalsList = state.getList(K_LAST_ATTACK_GOALS, 8);
    List<String> lastGoals = lastGoalsList.stream().map(tag -> tag.getAsString()).collect(Collectors.toList());
    boolean wasInCombat = !lastGoals.isEmpty();

    // 2. 状态变化处理
    if (inCombat && !wasInCombat) {
      // 进入战斗：清除脱战计时
      state.remove(K_DISENGAGED_AT);
    } else if (!inCombat && wasInCombat) {
      // 脱离战斗：记录脱战时间
      state.setLong(K_DISENGAGED_AT, now);
    }

    // 3. 施放逻辑
    long disengagedAt = state.getLong(K_DISENGAGED_AT, 0);
    if (inCombat || (disengagedAt > 0 && (now - disengagedAt) < DISENGAGE_DELAY_TICKS)) {
      if (cooldown.entry(ACTIVE_READY_KEY).isReady(now)) {
        ActiveSkillOps.activateFor(mob, ABILITY_ID);
      }
    }

    // 4. 更新状态快照
    ListTag goalTags = new ListTag();
    currentGoals.forEach(g -> goalTags.add(StringTag.valueOf(g)));
    state.setList(K_LAST_ATTACK_GOALS, goalTags);
  }

  // ========== 被动效果：剑击生成微型裂隙 ==========

  /**
   * 当实体造成近战伤害时调用（需要挂载到攻击事件）
   *
   * <p>注意：此方法需要在LivingDamageEvent或类似事件中调用
   *
   * @param attacker 攻击者
   * @param target 目标
   * @param cc 胸腔实例
   */
  public static void onMeleeAttack(
      LivingEntity attacker, LivingEntity target, ChestCavityInstance cc) {

    if (attacker.level().isClientSide) {
      return;
    }

    if (!(attacker.level() instanceof ServerLevel level)) {
      return;
    }

    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = level.getGameTime();

    // 检查冷却
    long lastPassive = state.getLong(K_LAST_PASSIVE_RIFT, 0L);
    if (now - lastPassive < PASSIVE_RIFT_COOLDOWN) {
      return;
    }

    // 概率触发
    if (attacker.getRandom().nextDouble() >= PASSIVE_RIFT_CHANCE) {
      return;
    }

    // 在目标位置生成微型裂隙
    Vec3 targetPos = target.position();
    BlockPos blockPos = BlockPos.containing(targetPos);
    BlockPos groundPos = level.getHeightmapPos(
        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos);
    Vec3 finalPos = new Vec3(targetPos.x, groundPos.getY() + 0.1, targetPos.z);

    RiftEntity rift = RiftEntity.create(level, finalPos, attacker, RiftType.MINOR, 0);
    level.addFreshEntity(rift);
    RiftManager.getInstance().registerRift(rift);

    // 记录时间
    state.setLong(K_LAST_PASSIVE_RIFT, now);
  }

  // ========== OnHit 接入（集成到 Organ 系统） ==========
  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (attacker == null || target == null) return damage;
    if (attacker.level().isClientSide()) return damage;
    // 仅近战：过滤投射物等
    if (source != null && source.is(DamageTypeTags.IS_PROJECTILE)) {
      return damage;
    }
    onMeleeAttack(attacker, target, cc);
    return damage;
  }
}
