package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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

/** [主动A] 鱼群：短延时齐射水灵体，对前方敌人造成击飞与潮湿减速。 */
public final class YuQunSkill {

  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_qun");

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_lin_gu");
  private static final ResourceLocation BLEED_EFFECT_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "lliuxue");

  private static final String COOLDOWN_KEY = "YuQunReadyAt";
  private static final int COOLDOWN_TICKS = 20 * 12;
  private static final double ZHENYUAN_COST = 120.0;
  private static final double JINGLI_COST = 12.0;
  private static final int HUNGER_COST = 2;
  private static final double RANGE = 10.0;
  private static final double WIDTH = 1.75;

  private YuQunSkill() {}

  public static void bootstrap() {
    OrganActivationListeners.register(ABILITY_ID, YuQunSkill::activate);
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
    OrganState state = OrganState.of(organ, "YuLinGu");
    if (!YuLinGuOps.hasFishArmor(organ)
        && !YuLinGuOps.isPlayerMoist(
            player, state, player.level().getGameTime())) {
      sendFailure(player, "需要水中或潮湿状态才能施展鱼群。");
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
      sendFailure(player, "真元不足，鱼群溃散。");
      return;
    }
    if (ResourceOps.tryAdjustJingli(handle, -JINGLI_COST, true).isEmpty()) {
      sendFailure(player, "精力不足，鱼群溃散。");
      return;
    }
    drainHunger(player, HUNGER_COST, YuLinGuOps.isPlayerMoist(player, state, now));

    boolean upgraded = YuLinGuOps.hasSharkArmor(organ);
    performVolley(player, upgraded);
    YuLinGuOps.addProgress(player, cc, organ, upgraded ? 2 : 1);

    long readyAt = now + COOLDOWN_TICKS;
    ready.setReadyAt(readyAt);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private static void performVolley(ServerPlayer player, boolean upgraded) {
    Level level = player.level();
    Vec3 origin = player.getEyePosition();
    Vec3 direction = player.getLookAngle().normalize();
    double maxRange = upgraded ? RANGE + 2.0 : RANGE;
    AABB searchBox = player.getBoundingBox().inflate(maxRange, 2.0, maxRange);
    List<LivingEntity> candidates =
        level.getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            target ->
                target != player
                    && target.isAlive()
                    && !target.isAlliedTo(player)
                    && !target.isInvulnerable());
    Optional<Holder.Reference<net.minecraft.world.effect.MobEffect>> bleedEffect =
        BuiltInRegistries.MOB_EFFECT.getHolder(BLEED_EFFECT_ID);
    for (LivingEntity target : candidates) {
      Vec3 toTarget = target.getEyePosition().subtract(origin);
      double forward = toTarget.dot(direction);
      if (forward <= 0.0 || forward > maxRange) {
        continue;
      }
      Vec3 lateral = toTarget.subtract(direction.scale(forward));
      if (lateral.lengthSqr() > WIDTH * WIDTH) {
        continue;
      }
      double pushStrength = upgraded ? 0.6 : 0.45;
      target.push(
          direction.x * pushStrength, 0.45 + (upgraded ? 0.25 : 0.0), direction.z * pushStrength);
      target.hurtMarked = true;
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, upgraded ? 1 : 0));
      bleedEffect.ifPresent(
          holder ->
              target.addEffect(
                  new MobEffectInstance(holder, upgraded ? 120 : 80, upgraded ? 1 : 0)));
    }
    level.playSound(
        null, player.blockPosition(), SoundEvents.SALMON_FLOP, SoundSource.PLAYERS, 0.8f, 1.0f);
  }

  private static void drainHunger(ServerPlayer player, int amount, boolean moist) {
    if (amount <= 0) {
      return;
    }
    int actual = moist ? Math.max(0, amount - 1) : amount;
    net.minecraft.world.food.FoodData foodData = player.getFoodData();
    foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - actual));
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
