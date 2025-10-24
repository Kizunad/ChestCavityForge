package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XueYiGuEffects;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/**
 * 主动技2：血束收紧（Blood Bind）
 *
 * <p>发射视线束线，命中目标施加缓慢IV(2秒)与流血10/秒×5秒。
 *
 * <p>消耗：baseCost真元80、精力4、饱食1
 *
 * <p>冷却：16秒
 */
public final class XueShuShouJinSkill {

  private static final String STATE_ROOT = "XueYiGu";
  private static final String COOLDOWN_READY_AT_KEY = "XueShuShouJin_ReadyAt";

  private static final int COOLDOWN_TICKS = 320; // 16 seconds
  private static final double MAX_RANGE = 8.0;

  // Base costs (1转1阶段标准)
  private static final double BASE_ZHENYUAN = 80.0;
  private static final double BASE_JINGLI = 4.0;
  private static final double BASE_FOOD = 1.0;

  // Effects
  private static final int SLOWNESS_DURATION_TICKS = 40; // 2 seconds
  private static final int SLOWNESS_AMPLIFIER = 3; // Slowness IV (0-indexed)
  private static final float BASE_BLEED_DAMAGE = 10.0f; // 10 per second
  private static final int BLEED_DURATION_TICKS = 100; // 5 seconds

  private XueShuShouJinSkill() {}

  public static void bootstrap() {
    // Register activation listener
    OrganActivationListeners.register(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "guzhenren", "xue_yi_gu/xue_shu_shou_jin"),
        XueShuShouJinSkill::activate);
  }

  /**
   * Activates blood bind skill when player triggers it.
   */
  private static void activate(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null) {
      return;
    }
    if (entity.level().isClientSide()) {
      return;
    }

    Optional<ItemStack> organOpt = findOrgan(cc);
    if (organOpt.isEmpty()) {
      return;
    }
    ItemStack organ = organOpt.get();

    OrganState state = OrganState.of(organ, STATE_ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry entry = cooldown.entry(COOLDOWN_READY_AT_KEY).withDefault(0L);

    Level level = player.level();
    long now = level.getGameTime();

    // Check cooldown
    if (!entry.isReady(now)) {
      long remaining = entry.getReadyTick() - now;
      sendFailure(player, "血束收紧冷却中，还需 " + (remaining / 20) + " 秒。");
      return;
    }

    // Check resource cost
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      sendFailure(player, "无法获取真元，血束收紧施放失败。");
      return;
    }

    ResourceHandle handle = handleOpt.get();

    // Check food level
    if (player.getFoodData().getFoodLevel() < BASE_FOOD) {
      sendFailure(player, "资源不足，血束收紧施放失败。");
      return;
    }

    // Find target via raytrace
    Optional<LivingEntity> targetOpt = raytraceTarget(player, level);

    if (targetOpt.isEmpty()) {
      sendFailure(player, "视野内无可攻击目标。");
      return;
    }

    LivingEntity target = targetOpt.get();

    // Consume scaled resources
    if (handle.consumeScaledZhenyuan(BASE_ZHENYUAN).isEmpty()) {
      sendFailure(player, "资源不足，血束收紧施放失败。");
      return;
    }
    if (handle.consumeScaledJingli(BASE_JINGLI).isEmpty()) {
      sendFailure(player, "资源不足，血束收紧施放失败。");
      return;
    }
    player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - (int) BASE_FOOD);

    // Execute skill
    executeBloodBind(player, target, (ServerLevel) level);

    // Start cooldown
    entry.setReadyAt(now + COOLDOWN_TICKS);
    ActiveSkillRegistry.scheduleReadyToast(
        player,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "guzhenren", "xue_yi_gu/xue_shu_shou_jin"),
        entry.getReadyTick(),
        now);

    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  /**
   * Raytraces from player's eyes to find first living entity hit.
   */
  private static Optional<LivingEntity> raytraceTarget(ServerPlayer player, Level level) {
    Vec3 eyePos = player.getEyePosition();
    Vec3 lookVec = player.getLookAngle();
    Vec3 endPos = eyePos.add(lookVec.scale(MAX_RANGE));

    // First check block collision
    ClipContext blockContext =
        new ClipContext(
            eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
    HitResult blockHit = level.clip(blockContext);

    // Adjust end position if block hit
    if (blockHit.getType() != HitResult.Type.MISS) {
      endPos = blockHit.getLocation();
    }

    // Entity raytrace
    Vec3 finalEndPos = endPos;
    AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(MAX_RANGE)).inflate(1.0);

    EntityHitResult entityHit =
        ProjectileUtil.getEntityHitResult(
            level,
            player,
            eyePos,
            finalEndPos,
            searchBox,
            (entity) -> entity instanceof LivingEntity && !entity.isAlliedTo(player));

    if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
      return Optional.of(living);
    }

    return Optional.empty();
  }

  /**
   * Executes blood bind effect on target.
   */
  private static void executeBloodBind(ServerPlayer player, LivingEntity target, ServerLevel level) {
    Vec3 startPos = player.getEyePosition();
    Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);

    // Play charging effect
    XueYiGuEffects.playBeamCharging(level, player);

    // Play beam effect
    XueYiGuEffects.playBeamEffect(level, startPos, targetPos);

    // Apply slowness effect
    target.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION_TICKS, SLOWNESS_AMPLIFIER, false, true, true));

    // Apply bleed DoT
    applyBleedEffect(player, target);

    // Play hit effect
    XueYiGuEffects.playBeamHit(level, target);

    // Success feedback
    player.displayClientMessage(Component.literal("血束收紧命中！"), true);
  }

  /**
   * Applies bleeding effect to target.
   */
  private static void applyBleedEffect(ServerPlayer player, LivingEntity target) {
    // TODO: Integrate with proper bleed system
    // For now, apply damage over time via effect

    // Calculate tier-scaled bleed damage
    double tier = getTierLevel(player);
    float totalDamage = BASE_BLEED_DAMAGE * (float) (1.0 + tier * 0.2) * 5; // 5 seconds worth

    // Apply damage (simplified - should use bleed system)
    // Spread damage over duration
    int ticksPerDamage = BLEED_DURATION_TICKS / 5; // 5 hits over duration
    float damagePerHit = totalDamage / 5;

    // For now, apply instant damage (in real implementation, use tick-based bleed)
    target.hurt(player.damageSources().magic(), damagePerHit);
  }

  /**
   * Gets player's tier level (1-5).
   */
  private static double getTierLevel(ServerPlayer player) {
    // TODO: Implement tier detection
    // For now, return 2 (assume 2转)
    return 2.0;
  }

  /**
   * Finds xue yi gu organ in chest cavity.
   */
  private static Optional<ItemStack> findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return Optional.empty();
    }

    net.minecraft.resources.ResourceLocation organId =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("guzhenren", "xueyigu");

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty()) {
        net.minecraft.resources.ResourceLocation itemId =
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (organId.equals(itemId)) {
          return Optional.of(stack);
        }
      }
    }

    return Optional.empty();
  }

  private static void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }
}
