package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.runtime.yin_yang;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.organ.yin_yang.YinYangOps;
import net.tigereye.chestcavity.compat.common.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.common.state.YinYangDualityAttachment.Mode;
import net.tigereye.chestcavity.compat.common.tuning.YinYangZhuanShenGuTuning;
import net.tigereye.chestcavity.compat.common.state.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.behavior.DualStrikeBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/** 阴阳转身蛊运行时逻辑。 */
public final class YinYangRuntime {

  private YinYangRuntime() {}

  /** 每 tick 被动逻辑。 */
  public static void onTick(ServerPlayer player, ChestCavityInstance cc, long now) {
    if (player == null || cc == null || !YinYangDualityOps.hasOrgan(cc)) {
      return;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    ensureAnchorPresent(player, attachment);
    YinYangOps.applyModeAttributes(player, attachment);
    YinYangOps.clampHealth(player);
    YinYangDualityOps.openHandle(player)
        .ifPresent(
            handle -> {
              YinYangOps.runPassives(player, attachment, handle, now);
              attachment.pool(attachment.currentMode()).capture(player, handle);
            });
    if (!attachment.dualStrike().isActive(now)) {
      attachment.dualStrike().clear();
    }
  }

  /** 处理主动技能切换流程。 */
  public static void activateBodySwitch(ServerPlayer player, ChestCavityInstance cc, long now) {
    if (player == null || cc == null || player.level().isClientSide()) {
      return;
    }
    if (!YinYangDualityOps.hasOrgan(cc)) {
      return;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    if (attachment.sealEndTick() > now) {
      YinYangOps.sendFailure(player, "封印尚未解除，无法切换阴阳身。");
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
      YinYangOps.sendFailure(player, "无法同步阴阳资源，切态终止。");
      return;
    }
    attachment.setCurrentMode(next);
    YinYangOps.applyModeAttributes(player, attachment);
    YinYangOps.clampHealth(player);
    YinYangDualityOps.captureAnchor(player).ifPresent(anchor -> attachment.setAnchor(next, anchor));
    attachment.setFallGuardEndTick(now + YinYangZhuanShenGuTuning.FALL_GUARD_TICKS);
    long nextReady = now + YinYangZhuanShenGuTuning.BODY_COOLDOWN_TICKS;
    attachment.setCooldown(YinYangZhuanShenGuTuning.SKILL_BODY_ID, nextReady);
    ActiveSkillRegistry.scheduleReadyToast(
        player, YinYangZhuanShenGuTuning.SKILL_BODY_ID, nextReady, now);
    YinYangOps.sendAction(player, next == Mode.YANG ? "阳身护体" : "阴身出鞘");
    YinYangOps.playBodySwitchFx(player, next);
  }

  /** 处理近战命中逻辑（双击窗口等）。 */
  public static void onMeleeHit(ServerPlayer player, LivingEntity target, long now) {
    if (player == null || target == null || !target.isAlive()) {
      return;
    }
    YinYangDualityOps.get(player)
        .ifPresent(attachment -> DualStrikeBehavior.handleHit(player, target, attachment));
  }

  private static void runPassives(
      ServerPlayer player, YinYangDualityAttachment attachment, ResourceHandle handle, long now) {
    // 逻辑下沉 Calculator，保留转发以避免破坏调用方
    YinYangOps.runPassives(player, attachment, handle, now);
  }

  private static void ensureAnchorPresent(
      ServerPlayer player, YinYangDualityAttachment attachment) {
    if (attachment.anchor(attachment.currentMode()).isValid()) {
      return;
    }
    YinYangDualityOps.captureAnchor(player)
        .ifPresent(anchor -> attachment.setAnchor(attachment.currentMode(), anchor));
  }
}
