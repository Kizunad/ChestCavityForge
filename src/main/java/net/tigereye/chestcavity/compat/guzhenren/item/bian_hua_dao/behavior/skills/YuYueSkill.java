package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.organ.yu.YuLinGuOps;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/** [主动B] 鱼跃破浪：水中高速冲刺并破浪而出，潮湿状态下提供低幅位移。 */
public final class YuYueSkill {

  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_yue");

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_lin_gu");

  private static final String COOLDOWN_KEY = "YuYueReadyAt";
  private static final int COOLDOWN_TICKS = 20 * 7;
  private static final double ZHENYUAN_COST = 400.0;
  private static final double JINGLI_COST = 8.0;

  private YuYueSkill() {}

  public static void bootstrap() {
    OrganActivationListeners.register(ABILITY_ID, YuYueSkill::activate);
  }

  public static void activate(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null) {
      return;
    }
    if (entity.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    boolean inWater = player.isInWaterOrBubble();
    OrganState state = OrganState.of(organ, "YuLinGu");
    boolean moist = inWater || YuLinGuOps.isPlayerMoist(player, state, player.level().getGameTime());
    if (!moist) {
      sendFailure(player, "需要潮湿或水中才能鱼跃破浪。");
      return;
    }

    Level level = player.level();
    long now = level.getGameTime();
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry(COOLDOWN_KEY).withDefault(0L);
    if (!ready.isReady(now)) {
      return;
    }

    Optional<ResourceHandle> handleOpt = net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    if (ResourceOps.tryConsumeScaledZhenyuan(handle, ZHENYUAN_COST).isEmpty()) {
      sendFailure(player, "真元不足，无法破浪。");
      return;
    }
    if (ResourceOps.tryAdjustJingli(handle, -JINGLI_COST, true).isEmpty()) {
      sendFailure(player, "精力不足，无法破浪。");
      return;
    }

    double baseRange = inWater ? 7.0 : 4.0;
    if (YuLinGuOps.hasTailSynergy(cc)) {
      baseRange += inWater ? 3.0 : 1.5;
    }
    boolean upgraded = YuLinGuOps.hasSharkArmor(organ);
    if (upgraded) {
      baseRange += 1.0;
    }

    Vec3 dashDir = player.getLookAngle().normalize();
    double horizontalScale = baseRange * 0.45;
    double vertical = inWater ? 0.25 : 0.12;
    player.setDeltaMovement(dashDir.x * horizontalScale, vertical, dashDir.z * horizontalScale);
    player.hurtMarked = true;
    player.hasImpulse = true;
    player.fallDistance = 0.0f;
    if (YuLinGuOps.hasTailSynergy(cc)) {
      player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false));
    }
    if (upgraded) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 0, false, false));
    }

    pushCollisions(player, dashDir, baseRange);
    YuLinGuOps.recordWetContact(player, organ);

    long readyAt = now + COOLDOWN_TICKS;
    ready.setReadyAt(readyAt);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
    level.playSound(
        null, player.blockPosition(), SoundEvents.DOLPHIN_JUMP, SoundSource.PLAYERS, 0.9f, 1.1f);
  }

  private static void pushCollisions(Player player, Vec3 direction, double range) {
    Level level = player.level();
    Vec3 start = player.position();
    AABB swept = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(1.0);
    List<LivingEntity> targets =
        level.getEntitiesOfClass(
            LivingEntity.class,
            swept,
            candidate ->
                candidate != player && candidate.isAlive() && !candidate.isAlliedTo(player));
    for (LivingEntity target : targets) {
      Vec3 toTarget = target.position().subtract(start);
      double proj = toTarget.dot(direction);
      if (proj < 0.0 || proj > range) {
        continue;
      }
      double strength = 0.35 + (player.isInWaterOrBubble() ? 0.2 : 0.0);
      target.push(direction.x * strength, 0.25, direction.z * strength);
      target.hurtMarked = true;
    }
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
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

  private static void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
  }
}
