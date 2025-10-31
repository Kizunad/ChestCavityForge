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
import net.tigereye.chestcavity.compat.common.tuning.YuYueTuning;
import net.tigereye.chestcavity.compat.common.organ.yu.YuYueCalculator;

/** [主动B] 鱼跃破浪：水中高速冲刺并破浪而出，潮湿状态下提供低幅位移。 */
public final class YuYueSkill {

  public static final ResourceLocation ABILITY_ID = YuYueTuning.ABILITY_ID;

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_lin_gu");

  private static final String COOLDOWN_KEY = YuYueTuning.COOLDOWN_KEY;
  private static final int COOLDOWN_TICKS = YuYueTuning.COOLDOWN_TICKS;
  private static final double ZHENYUAN_COST = YuYueTuning.ZHENYUAN_COST;
  private static final double JINGLI_COST = YuYueTuning.JINGLI_COST;

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

    boolean tail = YuLinGuOps.hasTailSynergy(cc);
    boolean upgraded = YuLinGuOps.hasSharkArmor(organ);
    double baseRange = YuYueCalculator.computeBaseRange(inWater, tail, upgraded);

    Vec3 dashDir = player.getLookAngle().normalize();
    double horizontalScale = YuYueCalculator.computeHorizontalScale(baseRange);
    double vertical = YuYueCalculator.computeVertical(inWater);
    player.setDeltaMovement(dashDir.x * horizontalScale, vertical, dashDir.z * horizontalScale);
    player.hurtMarked = true;
    player.hasImpulse = true;
    player.fallDistance = 0.0f;
    YuYueCalculator.grantAuxiliaryBuffs(player, tail, upgraded);

    YuYueCalculator.pushCollisions(player, dashDir, baseRange);
    YuLinGuOps.recordWetContact(player, organ);

    long readyAt = now + COOLDOWN_TICKS;
    ready.setReadyAt(readyAt);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
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
