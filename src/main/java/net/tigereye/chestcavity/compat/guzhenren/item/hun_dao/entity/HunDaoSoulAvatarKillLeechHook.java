package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoRuntimeTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;

/**
 * 击杀后按受害者最大生命值百分比转换魂魄的 Hook。
 *
 * <p>倍率由 {@link HunDaoRuntimeTuning.SoulAvatarKillLeech#HP_FRACTION} 控制。</p>
 */
public final class HunDaoSoulAvatarKillLeechHook implements HunDaoSoulAvatarHook {

  private static final ResourceLocation ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/avatar_kill_leech_hook");
  private static final String FIELD_MAX_HUNPO = "zuida_hunpo";

  private HunDaoSoulAvatarKillLeechHook() {}

  public static void register() {
    HunDaoSoulAvatarHookRegistry.register(ID, new HunDaoSoulAvatarKillLeechHook());
  }

  @Override
  public void onKillEntity(HunDaoSoulAvatarEntity avatar, LivingEntity victim) {
    if (avatar == null || victim == null) {
      return;
    }
    double hunpoGain = computeHunpoGain(victim);
    if (!(hunpoGain > 0.0D)) {
      return;
    }
    LivingEntity recipient = resolveRecipient(avatar);
    ResourceOps.openHandle(recipient)
        .ifPresent(handle -> handle.adjustDouble(FIELD_MAX_HUNPO, hunpoGain, true));
  }

  private double computeHunpoGain(LivingEntity victim) {
    double fraction = HunDaoRuntimeTuning.SoulAvatarKillLeech.HP_FRACTION;
    if (!(fraction > 0.0D)) {
      return 0.0D;
    }
    double maxHealth = Math.max(0.0D, victim.getMaxHealth());
    return maxHealth * fraction;
  }

  private LivingEntity resolveRecipient(HunDaoSoulAvatarEntity avatar) {
    LivingEntity owner = avatar.getOwner();
    return owner == null ? avatar : owner;
  }
}
