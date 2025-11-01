package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.fx;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning.JianDaoComboTuning;

/**
 * 剑道组合杀招 FX（占位）。
 */
public final class JianDaoComboFx {
  private JianDaoComboFx() {}

  public static void playActivate(ServerPlayer player) {
    if (!JianDaoComboTuning.FX_ENABLED || player == null) return;
    // 占位：后续接入粒子/音效
  }

  public static void playHit(ServerPlayer player) {
    if (!JianDaoComboTuning.FX_ENABLED || player == null) return;
    // 占位：后续接入粒子/音效
  }
}

