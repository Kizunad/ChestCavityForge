package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.runtime.YuQunRuntime;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

/** 鱼群组合杀招：行为层仅负责注册与转发到运行时。 */
public final class YuQunComboBehavior {
  public static final ResourceLocation ABILITY_ID = YuQunRuntime.ABILITY_ID;

  static {
    OrganActivationListeners.register(
        ABILITY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            YuQunRuntime.activate(player, cc);
          }
        });
  }

  private YuQunComboBehavior() {}
}
