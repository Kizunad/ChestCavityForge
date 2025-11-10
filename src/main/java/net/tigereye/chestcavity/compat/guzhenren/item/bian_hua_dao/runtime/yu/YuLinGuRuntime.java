package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.runtime.yu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yu.YuLinGuOps;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.fx.yu.YuLinGuFx;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YuLinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.util.NetworkUtil;

/** 鱼鳞蛊运行时逻辑，供行为层复用。 */
public final class YuLinGuRuntime {

  private YuLinGuRuntime() {}

  public static void onTick(ServerPlayer player, ChestCavityInstance cc, long now) {
    if (player == null || cc == null) {
      return;
    }
    ServerLevel level = player.serverLevel();
    ItemStack organ = YuLinGuOps.findOrgan(player);
    if (organ.isEmpty()) {
      YuLinGuOps.tickSummons(level, player, now);
      return;
    }

    OrganState state = OrganState.of(organ, YuLinGuTuning.STATE_ROOT);
    boolean moistNow = player.isInWaterRainOrBubble();
    if (moistNow) {
      state.setLong(YuLinGuTuning.LAST_WET_TICK_KEY, now);
    }

    boolean hasFishArmor =
        state.getBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, false);
    int progress =
        Mth.clamp(
            state.getInt(YuLinGuTuning.PROGRESS_KEY, 0),
            0,
            YuLinGuTuning.FISH_ARMOR_MAX_PROGRESS);

    if (!hasFishArmor && progress >= YuLinGuTuning.FISH_ARMOR_MAX_PROGRESS) {
      hasFishArmor = true;
      state.setBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, true);
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
      YuLinGuFx.playArmorReady(level, player);
    }

    if (hasFishArmor) {
      double hungerDebt = Math.max(0.0, state.getDouble(YuLinGuTuning.HUNGER_PROGRESS_KEY, 0.0));
      hungerDebt += YuLinGuTuning.HUNGER_COST_PER_SECOND;
      int hungerToConsume = (int) hungerDebt;
      if (hungerToConsume > 0) {
        hungerDebt -= hungerToConsume;
        YuLinGuOps.drainHunger(player, hungerToConsume);
      }
      state.setDouble(YuLinGuTuning.HUNGER_PROGRESS_KEY, hungerDebt);

      boolean hasSharkArmor = state.getBoolean(YuLinGuTuning.HAS_SHARK_ARMOR_KEY, false);
      YuLinGuOps.applyArmorBuffs(player, hasSharkArmor);

      if (!YuLinGuOps.isPlayerMoist(player, state, now)) {
        hasFishArmor = false;
        state.setBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, false);
        progress = Math.max(0, progress - 2);
        state.setInt(YuLinGuTuning.PROGRESS_KEY, progress);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
        YuLinGuFx.playArmorLost(level, player);
      }
    }

    handleWaterHeal(player, cc, organ, state, now);
    YuLinGuOps.tickSummons(level, player, now);
  }

  public static void onMeleeHit(Player attacker, LivingEntity target, ChestCavityInstance cc) {
    if (attacker == null || cc == null) {
      return;
    }
    ItemStack organ = YuLinGuOps.findOrgan(attacker);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = OrganState.of(organ, YuLinGuTuning.STATE_ROOT);
    if (state.getBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, false)) {
      YuLinGuOps.addProgress(attacker, cc, organ, 1);
    }
    YuLinGuOps.recordWetContact(attacker, organ);
  }

  public static void onHurt(
      Player player, ChestCavityInstance cc, DamageSource source, float amount) {
    if (player == null || cc == null || amount <= 0.0f) {
      return;
    }
    ItemStack organ = YuLinGuOps.findOrgan(player);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = OrganState.of(organ, YuLinGuTuning.STATE_ROOT);
    if (state.getBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, false)) {
      int bonus = Mth.clamp((int) Math.floor(amount / 4.0f), 0, 2);
      if (bonus > 0) {
        YuLinGuOps.addProgress(player, cc, organ, bonus);
      }
    }
  }

  private static void handleWaterHeal(
      ServerPlayer player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    if (!player.isInWaterOrBubble()) {
      return;
    }
    float health = player.getHealth();
    float maxHealth = player.getMaxHealth();
    if (health >= maxHealth * 0.3f) {
      return;
    }

    boolean hasSharkArmor = state.getBoolean(YuLinGuTuning.HAS_SHARK_ARMOR_KEY, false);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry entry =
        cooldown
            .entry(YuLinGuTuning.WATER_HEAL_READY_AT_KEY)
            .withClamp(value -> Math.max(0L, value))
            .withDefault(0L);

    int cooldownTicks =
        hasSharkArmor
            ? YuLinGuTuning.WATER_HEAL_COOLDOWN_FINAL_TICKS
            : YuLinGuTuning.WATER_HEAL_COOLDOWN_TICKS;
    if (!entry.isReady(now)) {
      return;
    }

    float healAmount = hasSharkArmor ? 3.0f : 2.0f;
    player.heal(healAmount);
    entry.setReadyAt(now + cooldownTicks);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
    YuLinGuFx.playWaterHeal(player.serverLevel(), player);
  }
}
