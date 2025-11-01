package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/** 剑域蛊 行为侧轻量提示/FX。 */
public final class JianYuGuFx {
  private JianYuGuFx() {}

  public static void scheduleCooldownToast(
      ServerPlayer player, ResourceLocation abilityId, long readyAtTick, long nowTick) {
    ActiveSkillRegistry.scheduleReadyToast(player, abilityId, readyAtTick, nowTick);
  }
}

