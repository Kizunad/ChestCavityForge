package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.tigereye.chestcavity.util.NetworkUtil;

/** 主动技：投网·血丝。负责创建血网域并初始化其持续时间/中心点。 */
public final class XieWangCastSkill {

  private static final XieWangGuOrganBehavior BEHAVIOR = XieWangGuOrganBehavior.INSTANCE;
  private static final int COOLDOWN_TICKS = 140;
  private static final double MAX_RANGE = 12.0;
  private static final XieWangGuOrganBehavior.ResourceCost COST =
      BEHAVIOR.cost(60.0, 8.0, 2.0, 2.0, 2.0, 1.0);

  private XieWangCastSkill() {}

  public static void bootstrap() {
    OrganActivationListeners.register(BEHAVIOR.CAST_SKILL_ID, XieWangCastSkill::activate);
  }

  private static void activate(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof net.minecraft.server.level.ServerPlayer player) || cc == null) {
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
    MultiCooldown.Entry entry = cooldown.entry("CastReadyAt").withDefault(0L);

    Level level = player.level();
    long now = level.getGameTime();
    if (!entry.isReady(now)) {
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    if (!BEHAVIOR.consumeCost(player, handle, COST)) {
      sendFailure(player, "真元或体能不足，血丝囊尚未酝酿完成。");
      return;
    }

    Vec3 center = resolveCenter(player);
    if (center == null) {
      sendFailure(player, "视野内无合适位置编织血网。");
      return;
    }

    BEHAVIOR.createWeb(
        player, cc, organ, center, BEHAVIOR.defaultRadius(), BEHAVIOR.baseDurationTicks());
    entry.setReadyAt(now + COOLDOWN_TICKS);
    ActiveSkillRegistry.scheduleReadyToast(
        player, BEHAVIOR.CAST_SKILL_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private static Vec3 resolveCenter(Player player) {
    Vec3 eye = player.getEyePosition();
    Vec3 look = player.getLookAngle();
    Vec3 end = eye.add(look.scale(MAX_RANGE));
    ClipContext context =
        new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
    HitResult hit = player.level().clip(context);
    Vec3 target =
        hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(4.0)) : hit.getLocation();
    if (target == null) {
      return null;
    }
    return new Vec3(target.x, target.y, target.z);
  }

  private static void sendFailure(Player player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }
}
