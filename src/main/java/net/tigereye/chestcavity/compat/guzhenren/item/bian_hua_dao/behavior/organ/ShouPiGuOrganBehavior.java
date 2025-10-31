package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.shou_pi.ShouPiGuOps;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.runtime.shou_pi.ShouPiRuntime;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

public class ShouPiGuOrganBehavior
    implements OrganSlowTickListener, OrganOnHitListener, OrganIncomingDamageListener {

  public static final ShouPiGuOrganBehavior INSTANCE = new ShouPiGuOrganBehavior();

  public static final ResourceLocation ORGAN_ID = ShouPiGuTuning.ORGAN_ID;
  public static final ResourceLocation HUPI_GU_ID = ShouPiGuTuning.HUPI_GU_ID;
  public static final ResourceLocation TIE_GU_GU_ID = ShouPiGuTuning.TIE_GU_GU_ID;

  private ShouPiGuOrganBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity.level().isClientSide() || !(entity instanceof ServerPlayer player)) {
      return;
    }
    if (!ShouPiGuOps.isOrgan(organ, ORGAN_ID)) {
      return;
    }
    ShouPiRuntime.onSlowTick(player, cc, organ, entity.level().getGameTime());
  }

  @Override
  public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc,
      ItemStack organ, float damage) {
    if (victim.level().isClientSide() || !(victim instanceof ServerPlayer player)) {
      return damage;
    }
    if (!ShouPiGuOps.isOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    return ShouPiRuntime.onHurt(player, cc, organ, source, damage,
        victim.level().getGameTime());
  }

  @Override
  public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target,
      ChestCavityInstance cc, ItemStack organ, float damage) {
    if (attacker.level().isClientSide() || !(attacker instanceof ServerPlayer player)) {
      return damage;
    }
    if (!ShouPiGuOps.isOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    ShouPiRuntime.onHit(player, cc, organ, target, damage, attacker.level().getGameTime());
    return damage;
  }
}
