package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.jianxin.fx;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 剑心蛊行为侧的轻量特效/提示接口。
 *
 * <p>集中管理 UI 提示、轻粒子/音效入口，便于后续统一调参与开关。
 */
public final class JianXinGuFx {

  private JianXinGuFx() {}

  /** 冷却提示（客户端 toast）。*/
  public static void scheduleCooldownToast(
      ServerPlayer player, ResourceLocation abilityId, long readyAtTick, long nowTick) {
    ActiveSkillRegistry.scheduleReadyToast(player, abilityId, readyAtTick, nowTick);
  }
}

