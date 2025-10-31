package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.behavior;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.util.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.calculator.YinYangZhuanShenGuCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.tuning.YinYangZhuanShenGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganActivation;
import net.tigereye.chestcavity.compat.guzhenren.item.common.cost.ResourceCost;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

public enum YinYangZhuanShenGuActive implements OrganActivation {
  INSTANCE;

  static {
    OrganActivationListeners.register(
        net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.YinYangZhuanShenGuIds.SKILL_BODY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateAbility(player, cc);
          }
        });
  }

  @Override
  public void activateAbility(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null || !YinYangDualityOps.hasOrgan(cc)) {
      return;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    long now = player.level().getGameTime();
    if (attachment.sealEndTick() > now) {
      YinYangZhuanShenGuCalculator.sendFailure(player, "封印尚未解除，无法切换阴阳身。");
      return;
    }
    long readyAt = attachment.getCooldown(YinYangZhuanShenGuTuning.SKILL_BODY_ID);
    if (readyAt > now) {
      return;
    }
    if (!ResourceOps.payCost(player, YinYangZhuanShenGuTuning.COST_BODY, "资源不足，无法切换阴阳身。")) {
      return;
    }
    YinYangDualityOps.captureAnchor(player)
        .ifPresent(anchor -> attachment.setAnchor(attachment.currentMode(), anchor));
    Mode next = attachment.currentMode().opposite();
    if (!YinYangDualityOps.swapPools(player, attachment, next)) {
      YinYangZhuanShenGuCalculator.sendFailure(player, "无法同步阴阳资源，切态终止。");
      return;
    }
    attachment.setCurrentMode(next);
    YinYangZhuanShenGuCalculator.applyModeAttributes(player, attachment);
    YinYangZhuanShenGuCalculator.clampHealth(player);
    YinYangDualityOps.captureAnchor(player).ifPresent(anchor -> attachment.setAnchor(next, anchor));
    attachment.setFallGuardEndTick(now + YinYangZhuanShenGuTuning.FALL_GUARD_TICKS);
    long nextReady = now + YinYangZhuanShenGuTuning.BODY_COOLDOWN_TICKS;
    attachment.setCooldown(YinYangZhuanShenGuTuning.SKILL_BODY_ID, nextReady);
    ActiveSkillRegistry.scheduleReadyToast(player, YinYangZhuanShenGuTuning.SKILL_BODY_ID, nextReady, now);
    YinYangZhuanShenGuCalculator.sendAction(player, next == Mode.YANG ? "阳身护体" : "阴身出鞘");
    YinYangZhuanShenGuCalculator.playBodySwitchFx(player, next);
  }
}
