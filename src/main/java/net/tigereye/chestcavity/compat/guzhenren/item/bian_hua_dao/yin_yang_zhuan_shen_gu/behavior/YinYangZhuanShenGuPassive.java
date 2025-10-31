package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.behavior;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.passive.PassiveHook;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.util.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.calculator.YinYangZhuanShenGuCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.tuning.YinYangZhuanShenGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.behavior.DualStrikeBehavior;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import org.jetbrains.annotations.NotNull;

public enum YinYangZhuanShenGuPassive implements PassiveHook {
  INSTANCE;

  @Override
  public void onTick(@NotNull LivingEntity self, @NotNull ChestCavityInstance cc, long time) {
    if (!(self instanceof ServerPlayer player)) {
      return;
    }
    if (!YinYangDualityOps.hasOrgan(cc)) {
      return;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    ensureAnchorPresent(player, attachment);
    YinYangZhuanShenGuCalculator.applyModeAttributes(player, attachment);
    YinYangZhuanShenGuCalculator.clampHealth(player);
    YinYangDualityOps.openHandle(player)
        .ifPresent(
            handle -> {
              runPassives(player, attachment, handle);
              attachment.pool(attachment.currentMode()).capture(player, handle);
            });
    long now = player.level().getGameTime();
    if (!attachment.dualStrike().isActive(now)) {
      attachment.dualStrike().clear();
    }
  }

  @Override
  public float onHurt(
      @NotNull LivingEntity self,
      @NotNull ChestCavityInstance cc,
      @NotNull DamageSource source,
      float amount) {
    if (!(self instanceof ServerPlayer player)) {
      return amount;
    }
    if (!YinYangDualityOps.hasOrgan(cc) || source == null || amount <= 0.0f) {
      return amount;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    long now = player.level().getGameTime();
    if (now > attachment.fallGuardEndTick()) {
      return amount;
    }
    boolean fallRelated =
        source.is(DamageTypeTags.IS_FALL)
            || source == player.damageSources().flyIntoWall()
            || source == player.damageSources().cramming();
    if (!fallRelated) {
      return amount;
    }
    return amount * YinYangZhuanShenGuTuning.FALL_REDUCTION;
  }

  @Override
  public float onHitMelee(
      @NotNull LivingEntity attacker,
      @NotNull LivingEntity target,
      @NotNull ChestCavityInstance cc,
      float damage) {
    if (!(attacker instanceof ServerPlayer player) || target == null || !target.isAlive()) {
      return damage;
    }
    if (!YinYangDualityOps.hasOrgan(cc)) {
      return damage;
    }
    YinYangDualityOps.get(player).ifPresent(attachment -> DualStrikeBehavior.handleHit(player, target, attachment));
    return damage;
  }

  private void ensureAnchorPresent(ServerPlayer player, YinYangDualityAttachment attachment) {
    if (attachment.anchor(attachment.currentMode()).isValid()) {
      return;
    }
    YinYangDualityOps.captureAnchor(player)
        .ifPresent(anchor -> attachment.setAnchor(attachment.currentMode(), anchor));
  }

  private void runPassives(
      ServerPlayer player, YinYangDualityAttachment attachment, ResourceHandle handle) {
    Mode mode = attachment.currentMode();
    FoodData foodData = player.getFoodData();
    if (mode == Mode.YANG) {
      handle.adjustJingli(10.0D, true);
      player.heal(20.0F);
      foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - 5));
      handle.adjustHunpo(-1.0D, true);
      handle.adjustNiantou(-1.0D, true);
      if (player.level().getGameTime() % 40L == 0) {
        YinYangZhuanShenGuCalculator.playPassiveYangFx(player);
      }
    } else {
      handle.adjustHunpo(20.0D, true);
      handle.adjustNiantou(2.0D, true);
      handle.adjustZhenyuan(10.0D, true);
      handle.adjustJingli(-1.0D, true);
      foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - 5));
      if (player.level().getGameTime() % 40L == 0) {
        YinYangZhuanShenGuCalculator.playPassiveYinFx(player);
      }
    }
  }
}
