package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieWangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieWangGuOrganBehavior.BloodWebState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/** 主动技：牵引·缚脉。对领域内锁定目标施加向心拉扯。 */
public final class XieWangPullSkill {

  private static final XieWangGuOrganBehavior BEHAVIOR = XieWangGuOrganBehavior.INSTANCE;
  private static final int COOLDOWN_TICKS = 120;
  private static final XieWangGuOrganBehavior.ResourceCost COST =
      BEHAVIOR.cost(40.0, 6.0, 1.0, 2.0, 1.0, 1.0);

  private XieWangPullSkill() {}

  public static void bootstrap() {
    OrganActivationListeners.register(BEHAVIOR.PULL_SKILL_ID, XieWangPullSkill::activate);
  }

  private static void activate(
      net.minecraft.world.entity.LivingEntity entity, ChestCavityInstance cc) {
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
    MultiCooldown.Entry entry = cooldown.entry("PullReadyAt").withDefault(0L);
    long now = player.level().getGameTime();
    if (!entry.isReady(now)) {
      return;
    }

    Optional<BloodWebState> activeOpt = BEHAVIOR.getActiveState(player);
    if (activeOpt.isEmpty()) {
      sendFailure(player, "需先布下血网，才能缚脉牵引。");
      return;
    }
    BloodWebState active = activeOpt.get();

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    if (!BEHAVIOR.consumeCost(player, handle, COST)) {
      sendFailure(player, "真元或魂魄不足，牵丝无力。");
      return;
    }

    LivingEntity target = selectTarget(player, active);
    if (target == null) {
      sendFailure(player, "血网内无可牵引的敌手。");
      return;
    }

    if (!BEHAVIOR.pullTarget(player, target)) {
      sendFailure(player, "目标抗性过强，牵引失败。");
      return;
    }

    entry.setReadyAt(now + COOLDOWN_TICKS);
    ActiveSkillRegistry.scheduleReadyToast(
        player, BEHAVIOR.PULL_SKILL_ID, entry.getReadyTick(), now);
  }

  private static LivingEntity selectTarget(ServerPlayer player, BloodWebState state) {
    Vec3 center = player.position();
    Vec3 look = player.getLookAngle().normalize();
    double radius = state.radius();
    AABB area = new AABB(state.center(), state.center()).inflate(radius + 0.5);
    List<LivingEntity> candidates =
        player
            .level()
            .getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate ->
                    candidate != null
                        && candidate.isAlive()
                        && !candidate.isAlliedTo(player)
                        && candidate.position().distanceToSqr(state.center())
                            <= (radius + 0.5) * (radius + 0.5));
    return candidates.stream()
        .max(
            Comparator.comparingDouble(
                target -> alignmentScore(look, target.position().subtract(center))))
        .orElse(null);
  }

  private static double alignmentScore(Vec3 look, Vec3 offset) {
    double distance = Math.max(0.01, offset.length());
    Vec3 direction = offset.normalize();
    double facing = Math.max(0.0, look.dot(direction));
    double distanceFactor = 1.0 / (1.0 + distance);
    return facing * distanceFactor;
  }

  private static void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }
}
