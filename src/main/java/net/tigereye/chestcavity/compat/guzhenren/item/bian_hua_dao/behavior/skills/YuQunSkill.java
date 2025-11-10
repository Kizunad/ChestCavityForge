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
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yu.YuLinGuOps;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YuQunTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yu.YuQunCalculator;

/** [主动A] 鱼群：短延时齐射水灵体，对前方敌人造成击飞与潮湿减速。 */
public final class YuQunSkill {

  public static final ResourceLocation ABILITY_ID = YuQunTuning.ABILITY_ID;

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_lin_gu");
  // BLEED 效果 ID 统一由 YuQunTuning 提供

  private static final String COOLDOWN_KEY = YuQunTuning.COOLDOWN_KEY;
  private static final int COOLDOWN_TICKS = YuQunTuning.COOLDOWN_TICKS;
  private static final double ZHENYUAN_COST = YuQunTuning.ZHENYUAN_COST;
  private static final double JINGLI_COST = YuQunTuning.JINGLI_COST;
  private static final int HUNGER_COST = YuQunTuning.HUNGER_COST;

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

    Optional<ResourceHandle> handleOpt =
        net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps.openHandle(player);
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
    Vec3 origin = player.getEyePosition();
    Vec3 direction = player.getLookAngle().normalize();
    YuQunCalculator.performVolley(level, player, upgraded, origin, direction);
    YuLinGuOps.addProgress(player, cc, organ, upgraded ? 2 : 1);

    long readyAt = now + COOLDOWN_TICKS;
    ready.setReadyAt(readyAt);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
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
