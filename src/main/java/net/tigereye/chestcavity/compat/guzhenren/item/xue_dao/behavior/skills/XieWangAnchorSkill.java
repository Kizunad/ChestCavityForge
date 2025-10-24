package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieWangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/** 主动技：定锚·凝血桩。强化血网并刷新禁跳区域。 */
public final class XieWangAnchorSkill {

  private static final XieWangGuOrganBehavior BEHAVIOR = XieWangGuOrganBehavior.INSTANCE;
  private static final int COOLDOWN_TICKS = 240;
  private static final XieWangGuOrganBehavior.ResourceCost COST =
      BEHAVIOR.cost(80.0, 10.0, 2.0, 3.0, 2.0, 1.0);

  private XieWangAnchorSkill() {}

  public static void bootstrap() {
    OrganActivationListeners.register(BEHAVIOR.ANCHOR_SKILL_ID, XieWangAnchorSkill::activate);
  }

  private static void activate(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null) {
      return;
    }
    if (entity.level().isClientSide()) {
      return;
    }
    Optional<ItemStack> organOpt = BEHAVIOR.findOrgan(cc);
    if (organOpt.isEmpty()) {
      return;
    }
    ItemStack organ = organOpt.get();

    OrganState state = OrganState.of(organ, BEHAVIOR.stateRoot());
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry entry = cooldown.entry("AnchorReadyAt").withDefault(0L);
    long now = player.level().getGameTime();
    if (!entry.isReady(now)) {
      return;
    }

    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    if (!BEHAVIOR.consumeCost(player, handle, COST)) {
      sendFailure(player, "真元不足，无法凝血成锚。");
      return;
    }

    if (!BEHAVIOR.placeAnchor(player, cc, organ)) {
      sendFailure(player, "需要现有血网才能定锚。");
      return;
    }

    entry.setReadyAt(now + COOLDOWN_TICKS);
    ActiveSkillRegistry.scheduleReadyToast(
        player, BEHAVIOR.ANCHOR_SKILL_ID, entry.getReadyTick(), now);
  }

  private static void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }
}
