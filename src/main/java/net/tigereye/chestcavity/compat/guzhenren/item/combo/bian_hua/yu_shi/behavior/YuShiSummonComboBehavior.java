package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.runtime.YuShiRuntime;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

/** 饵祭召鲨（组合）行为壳：仅负责注册与转发。 */
public final class YuShiSummonComboBehavior {
  public static final ResourceLocation ABILITY_ID = YuShiRuntime.ABILITY_ID;

  static {
    OrganActivationListeners.register(
        ABILITY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            YuShiRuntime.activate(player, cc);
          }
        });
  }

  private YuShiSummonComboBehavior() {}
}
