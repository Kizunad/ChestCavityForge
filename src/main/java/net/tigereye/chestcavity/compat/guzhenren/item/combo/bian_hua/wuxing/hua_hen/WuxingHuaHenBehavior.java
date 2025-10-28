package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillContext;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillIds;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * 五行化痕 组合杀招入口。
 */
public final class WuxingHuaHenBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener {

  public static final WuxingHuaHenBehavior INSTANCE = new WuxingHuaHenBehavior();

  private static final net.minecraft.resources.ResourceLocation SKILL_TRANSMUTE =
      ComboSkillIds.skill("wuxing_hua_hen");
  private static final net.minecraft.resources.ResourceLocation SKILL_UNDO =
      ComboSkillIds.skill("wuxing_hua_hen_undo");
  private static final net.minecraft.resources.ResourceLocation SKILL_CHECK =
      ComboSkillIds.skill("wuxing_hua_hen_check");
  private static final net.minecraft.resources.ResourceLocation SKILL_CONFIG =
      ComboSkillIds.config("wuxing_hua_hen");

  static {
    OrganActivationListeners.register(
        SKILL_TRANSMUTE,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateTransmute(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_UNDO,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateUndo(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_CHECK,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateCheck(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_CONFIG,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateConfig(player, cc);
          }
        });
  }

  private WuxingHuaHenBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
      return;
    }
    WuxingHuaHenRuntime.handleSlowTick(player, entity.level().getGameTime());
  }

  private void activateTransmute(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }
    ComboSkillContext context = ComboSkillContext.capture(player, cc);
    WuxingHuaHenRuntime.activateTransmute(context, SKILL_TRANSMUTE);
  }

  private void activateUndo(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }
    ComboSkillContext context = ComboSkillContext.capture(player, cc);
    WuxingHuaHenRuntime.activateUndo(context);
  }

  private void activateCheck(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }
    ComboSkillContext context = ComboSkillContext.capture(player, cc);
    WuxingHuaHenRuntime.activateCheck(context);
  }

  private void activateConfig(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }
    ComboSkillContext context = ComboSkillContext.capture(player, cc);
    WuxingHuaHenRuntime.openConfig(context);
  }
}
