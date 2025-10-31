package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.passive;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.passive.PassiveHook;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.runtime.yin_yang.YinYangRuntime;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityOps;
import org.jetbrains.annotations.NotNull;

public enum YinYangZhuanShenGuPassive implements PassiveHook {
  INSTANCE;

  @Override
  public void onTick(@NotNull LivingEntity self, @NotNull ChestCavityInstance cc, long time) {
    if (!(self instanceof ServerPlayer player)) {
      return;
    }
    if (!YinYangDualityOps.hasOrgan(cc)) {
      return;
    }
    YinYangRuntime.onTick(player, cc, time);
  }

  @Override
  public void onHurt(
      @NotNull LivingEntity self,
      @NotNull DamageSource source,
      float amount,
      @NotNull ChestCavityInstance cc,
      long now) {}

  @Override
  public void onHitMelee(
      @NotNull LivingEntity attacker,
      @NotNull LivingEntity target,
      @NotNull ChestCavityInstance cc,
      long now) {
    if (!(attacker instanceof ServerPlayer player)) {
      return;
    }
    if (!YinYangDualityOps.hasOrgan(cc)) {
      return;
    }
    YinYangRuntime.onMeleeHit(player, target, now);
  }
}
