package net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.gui_bian;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillContext;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillIds;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * 五行归变·逆转 组合杀招入口。
 *
 * <p>负责注册监听与调度核心逻辑至 {@link WuxingGuiBianRuntime}。
 */
public final class WuxingGuiBianBehavior implements OrganSlowTickListener {

  public static final WuxingGuiBianBehavior INSTANCE = new WuxingGuiBianBehavior();

  private static final net.minecraft.resources.ResourceLocation SKILL_ID =
      ComboSkillIds.skill("wuxing_gui_bian");
  private static final net.minecraft.resources.ResourceLocation SKILL_CONFIG_ID =
      ComboSkillIds.config("wuxing_gui_bian");

  static {
    OrganActivationListeners.register(
        SKILL_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateCombo(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_CONFIG_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateConfig(player, cc);
          }
        });
  }

  private WuxingGuiBianBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
      return;
    }
    WuxingGuiBianRuntime.handleSlowTick(player, entity.level().getGameTime());
  }

  private void activateCombo(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }
    ComboSkillContext context = ComboSkillContext.capture(player, cc);
    WuxingGuiBianRuntime.activate(context, SKILL_ID);
  }

  private void activateConfig(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }
    ComboSkillContext context = ComboSkillContext.capture(player, cc);
    WuxingGuiBianRuntime.openConfig(context);
  }
}

