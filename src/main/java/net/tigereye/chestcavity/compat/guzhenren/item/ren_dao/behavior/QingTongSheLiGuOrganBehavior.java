package net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 青铜舍利蛊： - 被动：每10秒清除一次晕眩/失明等精神负面（CONFUSION/BLINDNESS） - 主动（ATTACKABILITY）：静心入定，3秒无法移动但获得抗性II，冷却25秒
 */
public enum QingTongSheLiGuOrganBehavior implements OrganSlowTickListener {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_tong_she_li_gu");
  public static final ResourceLocation ABILITY_ID = ORGAN_ID;

  private static final String STATE_ROOT = "QingTongSheLiGu";
  private static final String KEY_PURGE_READY_AT = "PurgeReadyAt"; // 定时净化readyAt
  private static final String KEY_ACTIVE_UNTIL = "ActiveUntil"; // 主动效果结束时间
  private static final String KEY_COOLDOWN_UNTIL = "CooldownUntil"; // 主动冷却结束时间

  private static final long PURGE_INTERVAL_TICKS = 200L; // 10s
  private static final int ACTIVE_DURATION_TICKS = 60; // 3s
  private static final int ACTIVE_COOLDOWN_TICKS = 500; // 25s
  private static final int RESIST_AMP = 1; // 抗性II（放大器=1）
  private static final int SLOW_AMP = 10; // 高等级缓慢，近似定身

  static {
    OrganActivationListeners.register(ABILITY_ID, QingTongSheLiGuOrganBehavior::activateAbility);
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null
        || entity.level().isClientSide()
        || cc == null
        || organ == null
        || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ)) {
      return;
    }
    ServerLevel server = entity.level() instanceof ServerLevel s ? s : null;
    if (server == null) {
      return;
    }
    MultiCooldown cd = createCooldown(cc, organ);
    MultiCooldown.Entry purge = cd.entry(KEY_PURGE_READY_AT);
    long now = server.getGameTime();
    if (purge.getReadyTick() <= 0L) {
      purge.setReadyAt(now + PURGE_INTERVAL_TICKS);
    }
    if (purge.isReady(now)) {
      purge.setReadyAt(now + PURGE_INTERVAL_TICKS);
      purgeMentalDebuffs(entity);
    }
  }

  public static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (entity == null || cc == null || entity.level().isClientSide()) {
      return;
    }
    ItemStack organ = findPrimaryOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    MultiCooldown cd = createCooldown(cc, organ);
    ServerLevel server = entity.level() instanceof ServerLevel s ? s : null;
    if (server == null) {
      return;
    }
    long now = server.getGameTime();
    long cooldownUntil = cd.entry(KEY_COOLDOWN_UNTIL).getReadyTick();
    if (now < Math.max(0L, cooldownUntil)) {
      return;
    }

    // 应用3秒定身与抗性II
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN, ACTIVE_DURATION_TICKS, SLOW_AMP, false, true, true));
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE, ACTIVE_DURATION_TICKS, RESIST_AMP, false, true, true));

    cd.entry(KEY_ACTIVE_UNTIL).setReadyAt(now + ACTIVE_DURATION_TICKS);
    long readyAt = now + ACTIVE_COOLDOWN_TICKS;
    cd.entry(KEY_COOLDOWN_UNTIL).setReadyAt(readyAt);
    if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
      ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, now);
    }
  }

  private static boolean matchesOrgan(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return ORGAN_ID.equals(id);
  }

  private static ItemStack findPrimaryOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack candidate = cc.inventory.getItem(i);
      if (matchesOrgan(candidate)) {
        return candidate;
      }
    }
    return ItemStack.EMPTY;
  }

  private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
    MultiCooldown.Builder b =
        MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
            .withLongClamp(value -> Math.max(0L, value), 0L);
    if (cc != null) {
      b.withSync(cc, organ);
    } else {
      b.withOrgan(organ);
    }
    return b.build();
  }

  private static void purgeMentalDebuffs(LivingEntity entity) {
    entity.removeEffect(MobEffects.CONFUSION);
    entity.removeEffect(MobEffects.BLINDNESS);
  }
}
