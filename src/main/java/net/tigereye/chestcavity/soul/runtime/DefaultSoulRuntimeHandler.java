package net.tigereye.chestcavity.soul.runtime;

import net.minecraft.world.damagesource.DamageSource;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulRuntimeUtil;
import net.tigereye.chestcavity.soul.registry.SoulHurtResult;
import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandler;

public final class DefaultSoulRuntimeHandler implements SoulRuntimeHandler {

  @Override
  public void onTickEnd(SoulPlayer player) {
    SoulRuntimeUtil.ensureSurvivalTick(player);
  }

  @Override
  public SoulHurtResult onHurt(SoulPlayer player, DamageSource source, float amount) {
    return SoulHurtResult.applied(SoulRuntimeUtil.applyVanillaHurt(player, source, amount));
  }
}
