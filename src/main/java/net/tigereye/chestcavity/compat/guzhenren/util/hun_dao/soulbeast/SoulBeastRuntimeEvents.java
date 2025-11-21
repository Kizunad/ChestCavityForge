package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast;

import java.util.Optional;

import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.passive.DaHunGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.middleware.HunDaoMiddleware;
import net.tigereye.chestcavity.compat.guzhenren.registry.GRDamageTags;
import net.tigereye.chestcavity.compat.guzhenren.util.IntimidationHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageContext;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageHooks;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.event.SoulBeastStateChangedEvent;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.registration.CCStatusEffects;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Global runtime hooks for Soul Beast mechanics.
 *
 * <p>Performance: every handler returns early if the actor is not a soul beast.
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulBeastRuntimeEvents {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final double ON_HIT_HUNPO_COST = 18.0;
  private static final double SOUL_FLAME_PERCENT =
      0.01; // true damage per second = maxHunpo * percent
  private static final int SOUL_FLAME_SECONDS = 5;
  private static final double HUNPO_PER_DAMAGE = 1.0;
  private static final float DAMAGE_EPSILON = 1.0E-3f;
  private static final ResourceLocation HUN_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/hun_dao_increase_effect");
  private static final double INTIMIDATION_RADIUS = 12.0D;
  private static final int INTIMIDATION_DURATION = 100;

  private SoulBeastRuntimeEvents() {}

  /** Handles melee hit events to trigger soul flame DoT and consume hunpo. */
  @SubscribeEvent
  public static void onMeleeHit(AttackEntityEvent event) {
    if (!(event.getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide()) {
      return;
    }
    if (!SoulBeastStateManager.isActive(attacker)) {
      return;
    }
    Entity targetEntity = event.getTarget();
    if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
      return;
    }
    // Apply weiling (威灵) reduction if applicable
    double attackHunpoCost = ON_HIT_HUNPO_COST;
    if (attacker instanceof Player player) {
      ChestCavityInstance cc = CCAttachments.getExistingChestCavity(player).orElse(null);
      if (cc != null) {
        double reduction = DaHunGuBehavior.attackHunpoCostReduction(player, cc);
        if (reduction > 0.0) {
          attackHunpoCost = Math.max(0.0, attackHunpoCost - reduction);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "[soulbeast] weiling reduction applied: {} -> {} (reduction={})",
                ON_HIT_HUNPO_COST,
                attackHunpoCost,
                reduction);
          }
        }
      }
    }
    // Strictly consume hunpo first (no HP fallback)
    var res = ResourceOps.consumeHunpoStrict(attacker, attackHunpoCost);
    if (!res.succeeded()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "[soulbeast] blocked melee DoT: insufficient hunpo (cost={} reason={})",
            attackHunpoCost,
            res.failureReason());
      }
      return;
    }
    double dot = computeSoulFlameDps(attacker);
    if (dot > 0.0 && attacker instanceof Player player) {
      HunDaoMiddleware.INSTANCE.applySoulFlame(player, target, dot, SOUL_FLAME_SECONDS);
    }
  }

  /** Handles projectile impact events for soul beast projectiles. */

  // 远程投射命中魂兽玩家时同样按固定魂魄成本触发魂焰
  @SubscribeEvent
  public static void onProjectileImpact(ProjectileImpactEvent event) {
    if (!(event.getProjectile().getOwner() instanceof LivingEntity owner)
        || event.getProjectile().level().isClientSide()) {
      return;
    }
    if (!SoulBeastStateManager.isActive(owner)) {
      return;
    }
    if (!(event.getRayTraceResult() instanceof EntityHitResult ehr)) {
      return;
    }
    Entity hit = ehr.getEntity();
    if (!(hit instanceof LivingEntity target) || !target.isAlive()) {
      return;
    }
    // Apply weiling (威灵) reduction if applicable
    double attackHunpoCost = ON_HIT_HUNPO_COST;
    if (owner instanceof Player player) {
      ChestCavityInstance cc = CCAttachments.getExistingChestCavity(player).orElse(null);
      if (cc != null) {
        double reduction = DaHunGuBehavior.attackHunpoCostReduction(player, cc);
        if (reduction > 0.0) {
          attackHunpoCost = Math.max(0.0, attackHunpoCost - reduction);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "[soulbeast] weiling reduction applied (projectile): {} -> {} (reduction={})",
                ON_HIT_HUNPO_COST,
                attackHunpoCost,
                reduction);
          }
        }
      }
    }
    var res = ResourceOps.consumeHunpoStrict(owner, attackHunpoCost);
    if (!res.succeeded()) {
      return;
    }
    double dot = computeSoulFlameDps(owner);
    if (dot > 0.0 && owner instanceof Player player) {
      HunDaoMiddleware.INSTANCE.applySoulFlame(player, target, dot, SOUL_FLAME_SECONDS);
    }
  }

  /** Handles server tick events to process soul beast periodic logic. */

  // 服务器每秒维护魂兽的被动魂魄流失与饱和度补充
  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    long current = event.getServer().getTickCount();
    if ((current % 20L) != 0L) {
      return;
    }
    MinecraftServer server = event.getServer();
    tickPlayerSoulBeasts(server);
    tickNonPlayerSoulBeasts(server, current);
  }

  private static void tickPlayerSoulBeasts(MinecraftServer server) {
    for (Player player : server.getPlayerList().getPlayers()) {
      if (!SoulBeastStateManager.isActive(player)) {
        continue;
      }
      HunDaoMiddleware.INSTANCE.leakHunpoPerSecond(player, 3.0);
      HunDaoMiddleware.INSTANCE.handlerPlayer(player);
      maybeApplyIntimidation(player);
    }
  }

  private static void tickNonPlayerSoulBeasts(MinecraftServer server, long currentTick) {
    // 当前所有非玩家魂兽（如魂道分身）通过各自 Hook/实体 Tick 维持状态；预留入口便于未来扩展。
  }

  /** Handles soul beast state change events to update runtime state and effects. */

  // 在所有其他伤害修正完成后，把最终伤害转化为魂魄消耗；若伤害被取消则直接跳出
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void onIncomingDamage(LivingIncomingDamageEvent event) {
    LivingEntity victim = event.getEntity();
    if (victim == null || victim.level().isClientSide()) {
      return;
    }
    if (event.getSource().is(GRDamageTags.BYPASS_ORGAN_HOOKS)) {
      return;
    }
    if (!SoulBeastStateManager.isActive(victim)) {
      return;
    }
    if (event.isCanceled()) {
      return;
    }

    float incomingDamage = Math.max(0f, event.getAmount());
    if (!(incomingDamage > DAMAGE_EPSILON)) {
      return;
    }

    SoulBeastDamageContext context =
        new SoulBeastDamageContext(victim, event.getSource(), incomingDamage);
    double baseHunpoCost = incomingDamage * HUNPO_PER_DAMAGE;
    double adjustedHunpoCost =
        SoulBeastDamageHooks.applyHunpoCostModifiers(context, baseHunpoCost);
    if (!Double.isFinite(adjustedHunpoCost) || adjustedHunpoCost <= 0.0) {
      float adjustedDamage =
          SoulBeastDamageHooks.applyPostConversionDamageModifiers(context, incomingDamage);
      if (adjustedDamage != incomingDamage) {
        event.setAmount(Math.max(0f, adjustedDamage));
      }
      return;
    }

    double drainedHunpo = drainHunpo(victim, adjustedHunpoCost);
    double remainingHunpoCost = Math.max(0.0, adjustedHunpoCost - drainedHunpo);
    float remainingDamage = (float) (remainingHunpoCost / HUNPO_PER_DAMAGE);
    float adjustedDamage =
        SoulBeastDamageHooks.applyPostConversionDamageModifiers(context, remainingDamage);
    if (adjustedDamage < 0f) {
      adjustedDamage = 0f;
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[soulbeast] incoming damage converted: "
              + "dmg={} hunpoCost={} drained={} remainingDamage={}",
          incomingDamage,
          adjustedHunpoCost,
          drainedHunpo,
          adjustedDamage);
    }
    event.setAmount(adjustedDamage);
  }

  // 扣减魂魄资源（仅适用于有 Guzhenren 附件的玩家）
  private static double drainHunpo(LivingEntity victim, double requestedCost) {
    if (!(victim instanceof Player player)) {
      return 0.0;
    }
    if (!Double.isFinite(requestedCost) || requestedCost <= 0.0) {
      return 0.0;
    }
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return 0.0;
    }
    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    double available = handle.read("hunpo").orElse(0.0);
    if (!(available > 0.0)) {
      return 0.0;
    }
    double drain = Math.min(available, requestedCost);
    if (drain <= 0.0) {
      return 0.0;
    }
    if (handle.adjustDouble("hunpo", -drain, true, "zuida_hunpo").isEmpty()) {
      return 0.0;
    }
    return drain;
  }

  // 根据最大魂魄与 linkage 增益计算魂焰每秒真实伤害
  private static double computeSoulFlameDps(LivingEntity attacker) {
    double maxHunpo = 0.0;
    if (attacker instanceof Player player) {
      Optional<GuzhenrenResourceBridge.ResourceHandle> handle =
          GuzhenrenResourceBridge.open(player);
      if (handle.isPresent()) {
        maxHunpo = handle.get().read("zuida_hunpo").orElse(0.0);
      }
    }
    final double[] eff = {1.0};
    CCAttachments.getExistingChestCavity(attacker)
        .ifPresent(
            cc -> {
              ActiveLinkageContext ctx = LinkageManager.getContext(cc);
              if (ctx != null) {
                LinkageChannel ch = ctx.getOrCreateChannel(HUN_DAO_INCREASE_EFFECT);
                eff[0] += Math.max(0.0, ch.get());
              }
            });
    return Math.max(0.0, maxHunpo * SOUL_FLAME_PERCENT * eff[0]);
  }

  /**
   * Handles state changes to apply soul beast side effects such as intimidation and DoT cleanup.
   *
   * @param event the state change event
   */
  @SubscribeEvent
  public static void onSoulBeastStateChanged(SoulBeastStateChangedEvent event) {
    LivingEntity entity = event.entity();
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (event.current().isSoulBeast()
        && !event.previous().isSoulBeast()
        && entity instanceof Player player) {
      GuzhenrenResourceBridge.open(player)
          .ifPresent(
              handle -> {
                double max = handle.read("zuida_hunpo").orElse(0.0);
                if (Double.isFinite(max) && max > 0.0) {
                  handle.writeDouble("hunpo", max);
                }
              });
      maybeApplyIntimidation(player);
    }
    if (event.previous().isSoulBeast() && !event.current().isSoulBeast()) {
      DoTEngine.cancelAttacker(entity);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "[soulbeast] cleared pending DoT pulses after {} exited soul beast state",
            entity.getName().getString());
      }
    }
  }

  private static void maybeApplyIntimidation(Player performer) {
    if (performer == null || !IntimidationHelper.isSoulBeastIntimidationActive(performer)) {
      return;
    }
    GuzhenrenResourceBridge.open(performer)
        .ifPresent(
            handle -> {
              double hunpo = handle.read("hunpo").orElse(0.0);
              if (!(hunpo > 0.0)) {
                return;
              }
              IntimidationHelper.Settings settings =
                  new IntimidationHelper.Settings(
                      hunpo,
                      IntimidationHelper.AttitudeScope.HOSTILE,
                      CCStatusEffects.SOUL_BEAST_INTIMIDATED,
                      INTIMIDATION_DURATION,
                      0,
                      false,
                      true,
                      true,
                      false);
              IntimidationHelper.intimidateNearby(performer, INTIMIDATION_RADIUS, settings);
            });
  }
}
