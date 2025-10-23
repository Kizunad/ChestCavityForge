package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.DoTTypes;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import org.slf4j.Logger;
import org.joml.Vector3f;

/** 核心行为：血网蛊。负责维护领域状态、被动效果与主动技能共享逻辑。 */
public final class XieWangGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener, OrganRemovalListener {

  public static final XieWangGuOrganBehavior INSTANCE = new XieWangGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_wang_gu");

  public static final ResourceLocation CAST_SKILL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill/xie_wang_cast");
  public static final ResourceLocation PULL_SKILL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill/xie_wang_pull");
  public static final ResourceLocation ANCHOR_SKILL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill/xie_wang_anchor");
  public static final ResourceLocation BLIND_SKILL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill/xie_wang_blind");
  public static final ResourceLocation CONSTRICT_SKILL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill/xie_wang_constrict");
  private static final ResourceLocation SILK_SKILL_ICON =
      ResourceLocation.parse("chestcavity:silk");

  private static final ResourceLocation WEB_INTENSITY_CHANNEL =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_wang/web_intensity");
  private static final ResourceLocation WEB_DURATION_CHANNEL =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_wang/web_duration");

  private static final ClampPolicy ZERO_ONE = new ClampPolicy(0.0, 1.0);

  private static final ResourceLocation ATTACK_SPEED_DEBUFF_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_wang/web_attack_speed");

  private static final int BASE_DURATION_TICKS = 20 * 6;
  private static final int PASSIVE_EXTRA_DURATION_TICKS = 20 * 2;
  private static final int ANCHOR_EXTRA_DURATION_TICKS = 20;
  private static final double WEB_RADIUS = 10.0;
  private static final double ANCHOR_RADIUS = WEB_RADIUS * 0.5;
  private static final double PULL_MAX_DISTANCE = WEB_RADIUS * 1.3;
  private static final int SPIKE_DURATION_TICKS = 20 * 2;
  private static final int SPIKE_TICK_INTERVAL = 10;

  private static final double PROJECTILE_SLOW_FACTOR = 0.85;

  private static final String STATE_ROOT = "XieWangGu";
  private static final String STATE_SLOT_KEY = "Slot";

  // FX
  // 每隔几tick渲染一次，别太频繁避免卡顿
  private static final int FX_TICK_GAP = 2;
  final ParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.85f, 0.05f, 0.05f), 1.2f);

  private static final Map<UUID, BloodWebState> ACTIVE_WEBS = new ConcurrentHashMap<>();

  /** 记录当前已排队的血网 Tick，避免在 createWeb/tickWeb 重复调度时生成平行执行链导致伤害/减益叠加。 */
  private static final Set<UUID> SCHEDULED_TICKS = ConcurrentHashMap.newKeySet();

  private XieWangGuOrganBehavior() {}

  /**
   * 确保血网蛊在对应胸腔账本中具备所需的联动通道。若上下文缺失则静默跳过，以免在加载阶段引发 NullPointer。
   *
   * @param cc 当前持有血网蛊的胸腔实例。
   */
  public void ensureAttached(ChestCavityInstance cc) {
    if (cc == null) {
      return;
    }
    ActiveLinkageContext context = LedgerOps.context(cc);
    if (context == null) {
      return;
    }
    LedgerOps.ensureChannel(context, WEB_INTENSITY_CHANNEL, ZERO_ONE);
    LedgerOps.ensureChannel(context, WEB_DURATION_CHANNEL, ZERO_ONE);
  }

  /** 装备血网蛊时完成槽位记录与移除钩子注册。旧上下文会在 staleRemovalContexts 中清理，因此需在此 重新写入槽位索引。 */
  public void onEquip(
      ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
    if (cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, CCItems.GUZHENREN_XIE_WANG_GU, ORGAN_ID)) {
      return;
    }
    RemovalRegistration registration = registerRemovalHook(cc, organ, this, staleRemovalContexts);
    if (!registration.alreadyRegistered()) {
      OrganState state = organState(organ, STATE_ROOT);
      OrganStateOps.setInt(
          state, cc, organ, STATE_SLOT_KEY, registration.slotIndex(), value -> value, -1);
      ensureAttached(cc);
    }
  }

  @Override
  /** 每秒刷新血网状态：同步 HUD 通道，并在血网结束后清理强度与持续时间。 */
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player)
        || entity.level().isClientSide()
        || cc == null
        || organ == null
        || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, CCItems.GUZHENREN_XIE_WANG_GU, ORGAN_ID)) {
      return;
    }

    ensureAttached(cc);
    // 持续刷新领域剩余时间占比供 HUD/联动使用。
    BloodWebState state = ACTIVE_WEBS.get(player.getUUID());
    if (state == null) {
      updateLedger(cc, organ, 0.0, 0.0);
    } else {
      long now = player.level().getGameTime();
      double remaining =
          Mth.clamp((double) (state.expireTick - now) / state.totalDuration, 0.0, 1.0);
      updateLedger(cc, organ, state.isSpiking(now) ? 1.0 : 0.0, remaining);
    }
  }

  // 删除原来的：
  // @Override
  // public void onOrganRemoved(ChestCavityInstance cc, OrganRemovalContext context) { ... }

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player)) return;
    if (cc == null || organ == null || organ.isEmpty()) return;
    if (!matchesOrgan(organ, CCItems.GUZHENREN_XIE_WANG_GU, ORGAN_ID)) return;
    clearWeb(player);
  }


  /** 在胸腔背包中检索血网蛊实例，用于主动技能定位原件。 */
  public Optional<ItemStack> findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return Optional.empty();
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      if (matchesOrgan(stack, CCItems.GUZHENREN_XIE_WANG_GU, ORGAN_ID)) {
        return Optional.of(stack);
      }
    }
    return Optional.empty();
  }

  /**
   * 在指定位置生成血网领域并开始逐 tick 调度。
   *
   * @param player 血网的拥有者
   * @param cc 相关胸腔实例
   * @param organ 来源器官副本，用于 ledger 记录
   * @param center 血网中心位置
   * @param radius 血网半径
   * @param durationTicks 基础持续时间（未含被动加成）
   */
  public boolean createWeb(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      Vec3 center,
      double radius,
      int durationTicks) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(cc, "cc");
    Objects.requireNonNull(organ, "organ");
    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();
    int total = durationTicks + PASSIVE_EXTRA_DURATION_TICKS;
    BloodWebState state =
        new BloodWebState(
            player.getUUID(),
            level.dimension(),
            center,
            radius,
            now,
            now + total,
            total,
            organ.copy());
    ACTIVE_WEBS.put(player.getUUID(), state);
    scheduleTick(level, state.ownerId);
    updateLedger(cc, organ, 0.0, 1.0);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
    return true;
  }

  /** 对指定目标执行向心拉扯，受击退抗性影响。 */
  public boolean pullTarget(ServerPlayer player, LivingEntity target) {
    if (target == null || !target.isAlive()) {
      return false;
    }
    BloodWebState state = ACTIVE_WEBS.get(player.getUUID());
    if (state == null) {
      return false;
    }
    ServerLevel level = player.serverLevel();
    if (!state.isActive(level)) {
      return false;
    }
    if (target.level() != level) {
      return false;
    }
    if (target.isAlliedTo(player)) {
      return false;
    }
    Vec3 center = state.center;
    if (target.position().distanceToSqr(center) > (state.radius + 0.5) * (state.radius + 0.5)) {
      return false;
    }
    Vec3 delta = state.center.subtract(target.position());
    Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
    double distance = horizontal.length();
    if (distance <= 1e-3) return false;

    double pull = Math.min(PULL_MAX_DISTANCE, distance);

    // 牵引强度稍微提一点点（原来 *0.45 很保守），再乘以抗性系数
    double strength = pull * 0.6;
    double resistance = Optional.ofNullable(target.getAttribute(Attributes.KNOCKBACK_RESISTANCE))
        .map(AttributeInstance::getValue).orElse(0.0);
    double scale = 1.0 - Mth.clamp(resistance, 0.0, 1.0);
    if (scale <= 0.0) return false;

    Vec3 movement = horizontal.normalize().scale(strength * scale);

    // 给一点很小的 Y，避免被台阶/台阶边缘卡住；也可以直接用 0
    double yNudge = Math.min(0.1, delta.y * 0.05);
    target.setDeltaMovement(target.getDeltaMovement().add(movement.x, yNudge, movement.z));
    target.hasImpulse = true;
    target.hurtMarked = true;

    return true;
  }

  /** 在玩家脚下放置锚点，刷新禁跳范围并延长领域持续。 */
  public boolean placeAnchor(ServerPlayer player, ChestCavityInstance cc, ItemStack organ) {
    BloodWebState state = ACTIVE_WEBS.get(player.getUUID());
    if (state == null) {
      return false;
    }
    ServerLevel level = player.serverLevel();
    if (!state.isActive(level)) {
      return false;
    }
    long now = level.getGameTime();
    state.anchorPos = player.position();
    state.anchorExpireTick = now + 20 * 10;
    if (!state.extendedByAnchor) {
      state.expireTick += ANCHOR_EXTRA_DURATION_TICKS;
      state.totalDuration += ANCHOR_EXTRA_DURATION_TICKS;
      state.extendedByAnchor = true;
      updateLedger(cc, organ, state.isSpiking(now) ? 1.0 : 0.0, state.remainingFraction(now));
    }
    return true;
  }

  /** 释放血雾遮蔽，附带弱化与散射效果，同时刷新被血网标记的目标。 */
  public boolean applyBlindFog(ServerPlayer player, Vec3 center) {
    BloodWebState state = ACTIVE_WEBS.get(player.getUUID());
    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();
    double radius = WEB_RADIUS * 1.3;
    AABB area = new AABB(center, center).inflate(radius);
    List<LivingEntity> victims =
        level.getEntitiesOfClass(
            LivingEntity.class,
            area,
            candidate ->
                candidate != null
                    && candidate.isAlive()
                    && !candidate.isAlliedTo(player)
                    && candidate.distanceToSqr(center) <= radius * radius);
    for (LivingEntity victim : victims) {
      victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true, true));
      victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true, true));
      state = ACTIVE_WEBS.get(player.getUUID());
      if (state != null) {
        markWebbed(victim.getUUID(), now + 40);
      }
    }
    List<Projectile> projectiles =
        level.getEntitiesOfClass(
            Projectile.class, area, projectile -> projectile != null && projectile.isAlive());
    for (Projectile projectile : projectiles) {
      Vec3 current = projectile.getDeltaMovement();
      Vec3 jitter =
          new Vec3(level.random.nextGaussian(), 0.0, level.random.nextGaussian()).scale(0.15);
      projectile.setDeltaMovement(current.add(jitter));
      projectile.hurtMarked = true;
    }
    return true;
  }

  /** 将现有血网转换成倒刺态，在短时间内触发周期伤害。 */
  public boolean activateSpikes(ServerPlayer player) {
    BloodWebState state = ACTIVE_WEBS.get(player.getUUID());
    if (state == null) {
      return false;
    }
    ServerLevel level = player.serverLevel();
    if (!state.isActive(level)) {
      return false;
    }
    long now = level.getGameTime();
    state.spikeExpireTick = now + SPIKE_DURATION_TICKS;
    state.nextSpikeTick = now;
    return true;
  }

  /** 查询玩家当前的血网状态，供技能链路使用。 */
  public Optional<BloodWebState> getActiveState(Player player) {
    return Optional.ofNullable(ACTIVE_WEBS.get(player.getUUID()));
  }

  /** 主动清理血网缓存，用于下线或移除器官时复原状态。 */
  public void clearWeb(Player player) {
    UUID ownerId = player.getUUID();
    BloodWebState state = ACTIVE_WEBS.remove(ownerId);
    if (state != null && player.level() instanceof ServerLevel level) {
      cleanupWebState(level, state);
    }
    SCHEDULED_TICKS.remove(ownerId);
  }

  private static void updateLedger(
      ChestCavityInstance cc, ItemStack organ, double intensity, double durationFraction) {
    if (cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    LedgerOps.set(cc, organ, WEB_INTENSITY_CHANNEL, 1, intensity, ZERO_ONE, false);
    LedgerOps.set(cc, organ, WEB_DURATION_CHANNEL, 1, durationFraction, ZERO_ONE, false);
  }

  private void scheduleTick(ServerLevel level, UUID ownerId) {
    // 仅在调度集合中缺席时才排程下一次 Tick，以防止同一血网被重复 enqueue。
    if (!SCHEDULED_TICKS.add(ownerId)) {
      return;
    }
    TickOps.schedule(level, () -> tickWeb(level, ownerId), 1);
  }

  private void tickWeb(ServerLevel level, UUID ownerId) {
    BloodWebState state = ACTIVE_WEBS.get(ownerId);
    SCHEDULED_TICKS.remove(ownerId);
    if (state == null) {
      return;
    }
    if (!state.isActive(level)) {
      cleanupWebState(level, state);
      ACTIVE_WEBS.remove(ownerId);
      return;
    }
    Player owner = level.getPlayerByUUID(ownerId);
    long now = level.getGameTime();
    if (!(owner instanceof ServerPlayer spOwner) || !spOwner.isAlive()) {
      cleanupWebState(level, state);
      ACTIVE_WEBS.remove(ownerId);
      return;
    }
    applyWebEffects(level, spOwner, state, now);
    spawnWebFx(level, state, now);
    scheduleTick(level, ownerId);
  }

  private void applyWebEffects(
      ServerLevel level, ServerPlayer owner, BloodWebState state, long now) {
    Vec3 center = state.center;
    double radius = state.radius;
    AABB area = new AABB(center, center).inflate(radius + 0.5);

    Map<UUID, TargetInfo> updated = new HashMap<>();
    List<LivingEntity> victims =
        level.getEntitiesOfClass(
            LivingEntity.class,
            area,
            candidate ->
                candidate != null
                    && candidate.isAlive()
                    && candidate.distanceToSqr(center) <= (radius + 0.5) * (radius + 0.5));
    for (LivingEntity victim : victims) {
      if (victim == owner) {
        continue;
      }
      if (victim.isAlliedTo(owner)) {
        continue;
      }
      applyDebuffs(owner, victim, now, state);
      TargetInfo info = state.targets.computeIfAbsent(victim.getUUID(), id -> new TargetInfo());
      info.webbedUntil = now + 20;
      info.lastPos = victim.position();
      info.anchorZone =
          state.anchorPos != null
              && state.anchorExpireTick > now
              && victim.position().distanceToSqr(state.anchorPos) <= ANCHOR_RADIUS * ANCHOR_RADIUS;
      updated.put(victim.getUUID(), info);
    }

    // 移除离开领域的单位，撤销攻速削减。
    for (Map.Entry<UUID, TargetInfo> entry : state.targets.entrySet()) {
      UUID id = entry.getKey();
      TargetInfo info = entry.getValue();
      if (!updated.containsKey(id) || info.webbedUntil <= now) {
        LivingEntity entity = level.getEntity(id) instanceof LivingEntity living ? living : null;
        if (entity != null) {
          removeAttackSpeedDebuff(entity);
        }
      }
    }
    state.targets.keySet().retainAll(updated.keySet());

    List<Projectile> projectiles =
        level.getEntitiesOfClass(
            Projectile.class, area, projectile -> projectile != null && projectile.isAlive());
    for (Projectile projectile : projectiles) {
      if (!projectile.getPersistentData().getBoolean("XieWangSlowed")) {
        Vec3 pos = projectile.position();
        if (pos.distanceToSqr(center) <= radius * radius) {
          projectile.setDeltaMovement(projectile.getDeltaMovement().scale(PROJECTILE_SLOW_FACTOR));
          projectile.getPersistentData().putBoolean("XieWangSlowed", true);
        }
      }
    }

    if (state.isSpiking(now)) {
      if (state.nextSpikeTick <= now) {
        spikeVictims(level, owner, state, now, victims);
        state.nextSpikeTick = now + SPIKE_TICK_INTERVAL;
      }
    }

    if (state.anchorExpireTick > 0 && state.anchorExpireTick <= now) {
      state.anchorPos = null;
      state.anchorExpireTick = 0L;
    }

    if (now >= state.expireTick) {
      cleanupWebState(level, state);
      ACTIVE_WEBS.remove(owner.getUUID());
    }
  }

  private static void cleanupWebState(ServerLevel level, BloodWebState state) {
    if (level == null || state == null) {
      return;
    }
    for (UUID targetId : state.targets.keySet()) {
      if (level.getEntity(targetId) instanceof LivingEntity victim) {
        removeAttackSpeedDebuff(victim);
      }
    }
    state.targets.clear();
  }

  private void spikeVictims(
      ServerLevel level,
      ServerPlayer owner,
      BloodWebState state,
      long now,
      List<LivingEntity> victims) {
    for (LivingEntity victim : victims) {
      if (victim == owner || victim.isAlliedTo(owner)) {
        continue;
      }
      victim.hurt(level.damageSources().magic(), 1.0f);
      victim.addEffect(
          new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, true, true));
      DoTEngine.schedulePerSecond(
          owner,
          victim,
          1.0f,
          4,
          null,
          1.0f,
          1.0f,
          DoTTypes.XIE_WANG_BLEED,
          null,
          DoTEngine.FxAnchor.TARGET,
          Vec3.ZERO,
          1.0f);
      TargetInfo info = state.targets.computeIfAbsent(victim.getUUID(), id -> new TargetInfo());
      if (info.lastPos != null) {
        double moved = info.lastPos.distanceTo(victim.position());
        if (moved > 0.3) {
          victim.hurt(level.damageSources().magic(), 0.5f);
        }
      }
      info.lastPos = victim.position();
    }
  }

  private void applyDebuffs(
      ServerPlayer owner, LivingEntity victim, long now, BloodWebState state) {
    int slownessLevel = 1;
    victim.addEffect(
        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, slownessLevel, false, true, true));
    victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, false, true, true));
    applyAttackSpeedDebuff(victim);
    if (state.anchorPos != null
        && state.anchorExpireTick > now
        && victim.position().distanceToSqr(state.anchorPos) <= ANCHOR_RADIUS * ANCHOR_RADIUS) {
      victim.addEffect(
          new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
      clampJump(victim);
    } else if (!(victim instanceof Mob)) {
      clampJump(victim);
    }
    if (victim.getHealth() <= victim.getMaxHealth() * 0.4f) {
      victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true, true));
    }
    victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true));
  }

  private static void clampJump(LivingEntity victim) {
    Vec3 movement = victim.getDeltaMovement();
    if (movement.y > 0.0) {
      victim.setDeltaMovement(movement.x, Math.min(0.0, movement.y - 0.4), movement.z);
      victim.hasImpulse = true;
    }
  }

  private static void applyAttackSpeedDebuff(LivingEntity victim) {
    AttributeInstance attribute = victim.getAttribute(Attributes.ATTACK_SPEED);
    if (attribute == null) return;

    // 1.21 构造器： (ResourceLocation id, double amount, Operation op)
    AttributeModifier modifier = new AttributeModifier(
        ATTACK_SPEED_DEBUFF_MODIFIER_ID,          // 你上面定义的 ResourceLocation
        -0.20,                                    // -20% 总乘
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    // has/remove 也都用 ResourceLocation 作为键
    if (!attribute.hasModifier(ATTACK_SPEED_DEBUFF_MODIFIER_ID)) {
      attribute.addTransientModifier(modifier);
      // 若你项目提供了 addOrUpdateTransientModifier，可替换为：
      // attribute.addOrUpdateTransientModifier(modifier);
    }
    /*
     *  // 保险写法：先移除再加，等价于“有则更新、无则添加”
     *  attribute.removeModifier(ATTACK_SPEED_DEBUFF_MODIFIER_ID);
     *  attribute.addTransientModifier(modifier);  
     */
  }

  private static void removeAttackSpeedDebuff(LivingEntity victim) {
    AttributeInstance attribute = victim.getAttribute(Attributes.ATTACK_SPEED);
    if (attribute != null) {
      attribute.removeModifier(ATTACK_SPEED_DEBUFF_MODIFIER_ID);
    }
  }


  /** 打开古真人资源句柄，便于复用外部调用时的消耗结算。 */
  public Optional<ResourceHandle> openHandle(ServerPlayer player) {
    return GuzhenrenResourceBridge.open(player);
  }

  /** 扣除主动技能的资源消耗。该方法会在校验失败时提前返回，确保多种资源结算的一致性。 */
  public boolean consumeCost(ServerPlayer player, ResourceHandle handle, ResourceCost cost) {
    if (cost == null) {
      return true;
    }
    if (handle == null) {
      return false;
    }
    double scaledCost = 0.0;
    if (cost.zhenyuan() > 0.0) {
      OptionalDouble scaled = handle.estimateScaledZhenyuanCost(cost.zhenyuan());
      OptionalDouble current = handle.getZhenyuan();
      if (scaled.isEmpty() || current.isEmpty() || current.getAsDouble() < scaled.getAsDouble()) {
        return false;
      }
      scaledCost = scaled.getAsDouble();
    }
    if (cost.jingli() > 0.0) {
      double value = handle.getJingli().orElse(0.0);
      if (value + 1e-3 < cost.jingli()) {
        return false;
      }
    }
    if (cost.hunpo() > 0.0) {
      double value = handle.getHunpo().orElse(0.0);
      if (value + 1e-3 < cost.hunpo()) {
        return false;
      }
    }
    if (cost.niantou() > 0.0) {
      double value = handle.getNiantou().orElse(0.0);
      if (value + 1e-3 < cost.niantou()) {
        return false;
      }
    }
    if (cost.hunger() > 0.0) {
      int hungerCost = (int) Math.ceil(cost.hunger());
      if (player.getFoodData().getFoodLevel() < hungerCost) {
        return false;
      }
    }
    if (cost.health() > 0.0) {
      float available = player.getHealth() + player.getAbsorptionAmount();
      if (available <= cost.health()) {
        return false;
      }
    }

    if (cost.zhenyuan() > 0.0) {
      if (handle.consumeScaledZhenyuan(cost.zhenyuan()).isEmpty()) {
        return false;
      }
    }
    if (cost.jingli() > 0.0) {
      if (ResourceOps.tryAdjustJingli(handle, -cost.jingli(), true).isEmpty()) {
        return false;
      }
    }
    if (cost.hunpo() > 0.0) {
      if (handle.adjustHunpo(-cost.hunpo(), true).isEmpty()) {
        return false;
      }
    }
    if (cost.niantou() > 0.0) {
      if (handle.adjustNiantou(-cost.niantou(), true).isEmpty()) {
        return false;
      }
    }
    if (cost.hunger() > 0.0) {
      int hungerCost = (int) Math.ceil(cost.hunger());
      player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - hungerCost);
    }
    if (cost.health() > 0.0) {
      DamageSources sources = player.serverLevel().damageSources();
      if (!ResourceOps.drainHealth(player, (float) cost.health(), sources.generic())) {
        return false;
      }
    }
    return true;
  }


  private static void drawParticleLine(ServerLevel level, Vec3 from, Vec3 to, double step, ParticleOptions type) {
    Vec3 dir = to.subtract(from);
    double len = dir.length();
    if (len < 1e-3) return;
    Vec3 n = dir.scale(1.0 / len);
    for (double t = 0; t <= len; t += step) {
      Vec3 p = from.add(n.scale(t));
      level.sendParticles(type, p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }
  }

  private void spawnWebFx(ServerLevel level, BloodWebState state, long now) {
    if ((now % FX_TICK_GAP) != 0) return;

    // 领域边界：围一圈“血光”
    final ParticleOptions SPIKE = ParticleTypes.DAMAGE_INDICATOR;  // 倒刺时溅血

    Vec3 c = state.center;
    double y = c.y + 0.10;
    double r = state.radius;

    // 圆环密度跟半径走，最少 32 个点
    int points = Math.max(32, (int)(r * 24));
    for (int i = 0; i < points; i++) {
      double a = (Math.PI * 2 * i) / points;
      double x = c.x + Math.cos(a) * r;
      double z = c.z + Math.sin(a) * r;
      level.sendParticles(RED_DUST, x, y, z, 1, 0, 0.01, 0, 0);
    }

    // 从中心拉到每个受影响单位 —— “血丝”
    for (UUID id : state.targets.keySet()) {
      if (!(level.getEntity(id) instanceof LivingEntity victim)) continue;
      Vec3 head = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
      drawParticleLine(level, c.add(0, 0.15, 0), head, 0.35, RED_DUST);
    }

    // 锚点存在时画一个小圈提示范围
    if (state.anchorPos != null && state.anchorExpireTick > now) {
      double ar = 2.5; // 用你的 ANCHOR_RADIUS
      int apoints = Math.max(24, (int)(ar * 20));
      for (int i = 0; i < apoints; i++) {
        double a = (Math.PI * 2 * i) / apoints;
        double x = state.anchorPos.x + Math.cos(a) * ar;
        double z = state.anchorPos.z + Math.sin(a) * ar;
        level.sendParticles(RED_DUST, x, state.anchorPos.y + 0.05, z, 1, 0, 0.01, 0, 0);
      }
    }

    // 倒刺期：在圈内随机溅血
    if (state.isSpiking(now) && (now % 4 == 0)) {
      for (int i = 0; i < 12; i++) {
        double rx = (level.random.nextDouble() * 2 - 1) * r * 0.9;
        double rz = (level.random.nextDouble() * 2 - 1) * r * 0.9;
        Vec3 p = c.add(rx, 0.2, rz);
        if (p.distanceToSqr(c) <= r * r) {
          level.sendParticles(SPIKE, p.x, p.y, p.z, 1, 0, 0.02, 0, 0.1);
        }
      }
    }
  }


  /** 为外部联动提供的标记入口：当目标被判定为处于血网影响内时，延长其网缚持续时间。 */
  public void markWebbed(UUID entityId, long expireTick) {
    ACTIVE_WEBS
        .values()
        .forEach(
            state -> {
              TargetInfo info = state.targets.get(entityId);
              if (info != null) {
                info.webbedUntil = Math.max(info.webbedUntil, expireTick);
              }
            });
  }

  /** 触发“血涂丝”联动提示，通知玩家织网技能冷却完毕。 */
  public void notifySilkSynergy(ServerPlayer player) {
    ActiveSkillRegistry.pushToast(player, SILK_SKILL_ICON, "血涂丝就绪", null);
  }

  /** 当织网技能放置方块时调用，负责将其转化为血网效果并刷新领域状态。 */
  public void handleSilkPlacement(LivingEntity entity, ChestCavityInstance cc, BlockPos pos) {
    if (!(entity instanceof ServerPlayer player) || cc == null || pos == null) {
      return;
    }
    Optional<ItemStack> organOpt = findOrgan(cc);
    if (organOpt.isEmpty()) {
      return;
    }
    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();
    BloodWebState state = ACTIVE_WEBS.get(player.getUUID());
    if (state != null && state.isActive(level)) {
      state.expireTick += 20;
      state.totalDuration += 20;
    }
    Vec3 center = Vec3.atCenterOf(pos);
    AABB area = new AABB(center, center).inflate(1.5);
    List<LivingEntity> victims =
        level.getEntitiesOfClass(
            LivingEntity.class,
            area,
            candidate ->
                candidate != null
                    && candidate.isAlive()
                    && !candidate.isAlliedTo(player)
                    && candidate.position().distanceToSqr(center) <= 2.25 * 2.25);
    for (LivingEntity victim : victims) {
      victim.addEffect(
          new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, true, true));
      victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 0, false, true, true));
      clampJump(victim);
    }
    notifySilkSynergy(player);
  }

  /** 构建资源消耗记录，便于技能声明基础成本。 */
  public ResourceCost cost(
      double zhenyuan, double jingli, double hunpo, double niantou, double hunger, double health) {
    return new ResourceCost(zhenyuan, jingli, hunpo, niantou, hunger, health);
  }

  /** 血网技能的资源消耗描述。字段单位均为基础值，最终调用 ResourceBridge 进行缩放。 */
  public record ResourceCost(
      double zhenyuan, double jingli, double hunpo, double niantou, double hunger, double health) {}

  /** 返回血网蛊在 OrganState 中使用的根键。 */
  public String stateRoot() {
    return STATE_ROOT;
  }

  /** 获取默认的血网半径（不含额外加成）。 */
  public double defaultRadius() {
    return WEB_RADIUS;
  }

  /** 获取血网基础持续时间，用于主动技初始值。 */
  public int baseDurationTicks() {
    return BASE_DURATION_TICKS;
  }

  /** 记录当前血网的运行时状态，包括位置、目标列表以及锚定/倒刺信息。 */
  public static final class BloodWebState {
    private final UUID ownerId;
    private final ResourceKey<Level> levelKey;
    private final Vec3 center;
    private final double radius;
    private final long createdTick;
    private long expireTick;
    private long totalDuration;
    private final ItemStack sourceOrgan;

    private final Map<UUID, TargetInfo> targets = new ConcurrentHashMap<>();

    private Vec3 anchorPos;
    private long anchorExpireTick;
    private long spikeExpireTick;
    private long nextSpikeTick;
    private boolean extendedByAnchor;

    private BloodWebState(
        UUID ownerId,
        ResourceKey<Level> levelKey,
        Vec3 center,
        double radius,
        long createdTick,
        long expireTick,
        long totalDuration,
        ItemStack sourceOrgan) {
      this.ownerId = ownerId;
      this.levelKey = levelKey;
      this.center = center;
      this.radius = radius;
      this.createdTick = createdTick;
      this.expireTick = expireTick;
      this.totalDuration = totalDuration;
      this.sourceOrgan = sourceOrgan;
    }

    public double remainingFraction(long now) {
      if (totalDuration <= 0) {
        return 0.0;
      }
      double remaining = Math.max(0L, expireTick - now);
      return Mth.clamp(remaining / (double) totalDuration, 0.0, 1.0);
    }

    public boolean isActive(ServerLevel level) {
      return level != null
          && level.dimension().equals(levelKey)
          && level.getGameTime() < expireTick
          && level.getGameTime() >= createdTick;
    }

    public boolean isSpiking(long now) {
      return spikeExpireTick > now;
    }

    public Vec3 center() {
      return center;
    }

    public double radius() {
      return radius;
    }
  }

  /** 领域内个体的附加数据，用于追踪移动距离与锚覆盖状态。 */
  private static final class TargetInfo {
    private long webbedUntil;
    private Vec3 lastPos;
    private boolean anchorZone;
  }
}
