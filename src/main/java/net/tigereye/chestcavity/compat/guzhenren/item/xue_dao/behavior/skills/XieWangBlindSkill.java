package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieWangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/** 主动技：血雾·遮目。短暂制造失明雾域。 */
public final class XieWangBlindSkill {

  private static final XieWangGuOrganBehavior BEHAVIOR = XieWangGuOrganBehavior.INSTANCE;
  private static final int COOLDOWN_TICKS = 200;
  private static final double MAX_RANGE = 10.0;
  private static final XieWangGuOrganBehavior.ResourceCost COST =
      BEHAVIOR.cost(50.0, 6.0, 1.0, 1.0, 1.0, 0.0);

  private XieWangBlindSkill() {}

  public static void bootstrap() {
    OrganActivationListeners.register(BEHAVIOR.BLIND_SKILL_ID, XieWangBlindSkill::activate);
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
    MultiCooldown.Entry entry = cooldown.entry("BlindReadyAt").withDefault(0L);
    long now = player.level().getGameTime();
    if (!entry.isReady(now)) {
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    if (!BEHAVIOR.consumeCost(player, handle, COST)) {
      sendFailure(player, "真元不足，无法凝成血雾。");
      return;
    }

    Vec3 center = resolveCenter(player);
    if (center == null) {
      sendFailure(player, "视线内无有效落点散布血雾。");
      return;
    }

    BEHAVIOR.applyBlindFog(player, center);
    entry.setReadyAt(now + COOLDOWN_TICKS);
    ActiveSkillRegistry.scheduleReadyToast(
        player, BEHAVIOR.BLIND_SKILL_ID, entry.getReadyTick(), now);
  }

  private static Vec3 resolveCenter(ServerPlayer player) {
    Level level = player.level();
    Vec3 eye = player.getEyePosition();
    Vec3 look = player.getLookAngle();
    Vec3 end = eye.add(look.scale(MAX_RANGE));
    ClipContext context =
        new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
    HitResult result = level.clip(context);
    Vec3 pos =
        result.getType() == HitResult.Type.MISS ? eye.add(look.scale(4.0)) : result.getLocation();
    return pos == null ? null : pos;
  }

  private static void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }
}
