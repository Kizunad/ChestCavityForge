package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Collection;
import java.util.Comparator;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYinGuTuning;

/**
 * 剑引蛊指挥中心：维护玩家的目标标记、战术选择以及飞剑指令。
 */
public final class SwordCommandCenter {

  private static final Map<UUID, CommandSession> SESSIONS = new HashMap<>();
  private static final TagKey<EntityType<?>> COMMAND_TARGET_TAG =
      TagKey.create(
          Registries.ENTITY_TYPE,
          ResourceLocation.fromNamespaceAndPath("chestcavity", "jianyin_command_targets"));
  private static final String COMMAND_TARGET_SCOREBOARD_TAG = "cc_command_target";

  private SwordCommandCenter() {}

  public static Optional<CommandSession> session(ServerPlayer player) {
    return Optional.ofNullable(SESSIONS.get(player.getUUID()));
  }

  public static boolean hasCommandOverride(Entity entity) {
    if (!(entity instanceof LivingEntity living)) {
      return false;
    }
    if (living.getTags().contains(COMMAND_TARGET_SCOREBOARD_TAG)) {
      return true;
    }
    return living.getType().is(COMMAND_TARGET_TAG);
  }

  public static CommandSession sessionOrCreate(ServerPlayer player) {
    return SESSIONS.computeIfAbsent(player.getUUID(), id -> new CommandSession(id));
  }

  public static void clear(ServerPlayer player) {
    SESSIONS.remove(player.getUUID());
  }

  public static void clearMarks(ServerPlayer player) {
    session(player).ifPresent(CommandSession::clearCurrentMarks);
  }

  public static boolean isSelectionActive(ServerPlayer player) {
    return session(player)
        .map(session -> session.selectionActive)
        .orElse(false);
  }

  public static int startSelection(ServerPlayer player, long nowTick) {
    CommandSession session = sessionOrCreate(player);
    session.resetSelection(nowTick);
    int count =
        markTargetsAlongRay(
            player,
            JianYinGuTuning.COMMAND_SCAN_DISTANCE,
            JianYinGuTuning.COMMAND_SCAN_RADIUS,
            nowTick,
            session);
    session.updateSelectionCount(count);
    return count;
  }

  public static void cancelSelection(ServerPlayer player) {
    session(player)
        .ifPresent(
            session -> {
              session.clearSelection();
            });
  }

  public static boolean setTactic(ServerPlayer player, CommandTactic tactic) {
    if (tactic == null) {
      return false;
    }
    CommandSession session = sessionOrCreate(player);
    session.tactic = tactic;
    return true;
  }

  public static Optional<CommandTactic> currentTactic(ServerPlayer player) {
    return session(player).map(session -> session.tactic);
  }

  public static int currentGroup(ServerPlayer player) {
    return session(player).map(session -> session.groupId).orElse(0);
  }

  public static void setCommandGroup(ServerPlayer player, int groupId) {
    CommandSession session = sessionOrCreate(player);
    session.setGroupId(groupId);
  }

  public static void openTui(ServerPlayer player) {
    CommandSession session = sessionOrCreate(player);
    SwordCommandTUI.open(player, session);
    session.lastTuiSentAt = player.level().getGameTime();
  }

  public static boolean execute(ServerPlayer player, long nowTick) {
    CommandSession session = session(player).orElse(null);
    if (session == null) {
      return false;
    }
    pruneMarks(player, session, nowTick);
    if (!session.selectionActive || !session.hasSelectionMarks()) {
      return false;
    }
    GroupState state = session.currentState(true);
    long executingUntil = nowTick + JianYinGuTuning.COMMAND_EXECUTE_DURATION_T;
    state.replaceMarksFromSelection(session.selectionMarks().values(), executingUntil);
    session.clearSelection();
    return true;
  }

  public static void tick(ServerPlayer player, long nowTick) {
    CommandSession session = session(player).orElse(null);
    if (session == null) {
      return;
    }
    pruneMarks(player, session, nowTick);
    if (session.isCompletelyIdle()) {
      SESSIONS.remove(player.getUUID());
    }
  }

  public static Optional<IntentResult> buildIntent(AIContext ctx) {
    if (!(ctx.owner() instanceof ServerPlayer player)) {
      return Optional.empty();
    }
    CommandSession session = session(player).orElse(null);
    if (session == null) {
      return Optional.empty();
    }
    pruneMarks(player, session, ctx.level().getGameTime());
    GroupState state = resolveGroupStateForIntent(session, ctx.sword().getGroupId());
    if (state == null || !state.executing || !state.hasMarks()) {
      return Optional.empty();
    }

    CommandTactic tactic = session.tactic;
    List<LivingEntity> candidates = collectMarkedEntities(ctx.level(), state);
    if (candidates.isEmpty()) {
      return Optional.empty();
    }

    LivingEntity target;
    if (tactic == CommandTactic.FOCUS_FIRE) {
      target = chooseTargetForTactic(tactic, ctx, candidates);
      if (target == null) {
        target = pickBestTarget(ctx.sword(), ctx.level(), state.marks.keySet());
      }
    } else {
      target = pickBestTarget(ctx.sword(), ctx.level(), state.marks.keySet());
    }
    if (target == null) {
      return Optional.empty();
    }

    IntentResult.Builder builder =
        IntentResult.builder()
            .target(target)
            .priority(computePriority(tactic, target, ctx))
            .trajectory(tactic.trajectory());
    applyTacticParameters(tactic, builder, target, candidates, ctx);
    return Optional.of(builder.build());
  }

  public static void removeTarget(ServerPlayer player, UUID targetId) {
    session(player)
        .ifPresent(
            session -> {
              session.selectionMarks().remove(targetId);
              for (GroupState state : session.groups.values()) {
                state.marks.remove(targetId);
              }
            });
  }

  private static GroupState resolveGroupStateForIntent(CommandSession session, int swordGroupId) {
    GroupState state = session.stateForGroup(swordGroupId, false);
    if (state != null && state.executing && state.hasMarks()) {
      return state;
    }
    if (swordGroupId != 0) {
      GroupState fallback = session.stateForGroup(0, false);
      if (fallback != null && fallback.executing && fallback.hasMarks()) {
        return fallback;
      }
    }
    return null;
  }

  private static void pruneMarks(ServerPlayer player, CommandSession session, long nowTick) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    if (session.selectionActive) {
      Iterator<Map.Entry<UUID, MarkedTarget>> selectionIt = session.selectionMarks().entrySet().iterator();
      while (selectionIt.hasNext()) {
        Map.Entry<UUID, MarkedTarget> entry = selectionIt.next();
        MarkedTarget mark = entry.getValue();
        if (nowTick > mark.expiresAt()) {
          selectionIt.remove();
          continue;
        }
        Entity entity = level.getEntity(entry.getKey());
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
          selectionIt.remove();
        }
      }
      if (session.selectionMarks().isEmpty()) {
        session.clearSelection();
      }
    }
    Iterator<Map.Entry<Integer, GroupState>> groupIt = session.groups.entrySet().iterator();
    while (groupIt.hasNext()) {
      Map.Entry<Integer, GroupState> groupEntry = groupIt.next();
      GroupState state = groupEntry.getValue();
      Iterator<Map.Entry<UUID, MarkedTarget>> it = state.marks.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<UUID, MarkedTarget> entry = it.next();
        MarkedTarget mark = entry.getValue();
        if (nowTick > mark.expiresAt()) {
          it.remove();
          continue;
        }
        Entity entity = level.getEntity(entry.getKey());
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
          it.remove();
        }
      }
      state.concludeIfExpired(nowTick);
      if (state.isIdle()) {
        groupIt.remove();
      }
    }
  }

  private static List<LivingEntity> collectMarkedEntities(ServerLevel level, GroupState state) {
    List<LivingEntity> result = new ArrayList<>();
    for (UUID id : state.marks.keySet()) {
      Entity entity = level.getEntity(id);
      if (entity instanceof LivingEntity living && living.isAlive()) {
        result.add(living);
      }
    }
    return result;
  }

  private static LivingEntity pickBestTarget(
      FlyingSwordEntity sword, ServerLevel level, Set<UUID> candidates) {
    Vec3 origin = sword.position();
    LivingEntity best = null;
    double bestScore = Double.POSITIVE_INFINITY;
    for (UUID id : candidates) {
      Entity entity = level.getEntity(id);
      if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
        continue;
      }
      double distSq = living.position().distanceToSqr(origin);
      if (distSq < bestScore) {
        bestScore = distSq;
        best = living;
      }
    }
    return best;
  }

  private static LivingEntity chooseTargetForTactic(
      CommandTactic tactic, AIContext ctx, List<LivingEntity> candidates) {
    return switch (tactic) {
      case FOCUS_FIRE -> pickLowestHealth(candidates);
      case INTERCEPT -> pickFastest(candidates);
      case SUPPRESS -> pickHighestCluster(candidates, ctx.level());
      case SHEPHERD -> pickClosestToOwner(candidates, ctx.owner());
      case DUEL -> pickHighestHealthPool(candidates);
    };
  }

  private static LivingEntity pickLowestHealth(List<LivingEntity> candidates) {
    LivingEntity best = null;
    double bestRatio = Double.POSITIVE_INFINITY;
    for (LivingEntity living : candidates) {
      double ratio = living.getHealth() / Math.max(1.0f, living.getMaxHealth());
      if (ratio < bestRatio) {
        bestRatio = ratio;
        best = living;
      }
    }
    return best;
  }

  private static LivingEntity pickFastest(List<LivingEntity> candidates) {
    LivingEntity best = null;
    double bestSpeed = Double.NEGATIVE_INFINITY;
    for (LivingEntity living : candidates) {
      double speed = living.getDeltaMovement().lengthSqr();
      if (speed > bestSpeed) {
        bestSpeed = speed;
        best = living;
      }
    }
    return best;
  }

  private static LivingEntity pickHighestCluster(
      List<LivingEntity> candidates, ServerLevel level) {
    LivingEntity best = null;
    double bestScore = Double.NEGATIVE_INFINITY;
    for (LivingEntity living : candidates) {
      double radius = 6.0;
      AABB area = new AABB(living.position(), living.position()).inflate(radius);
      long count =
          level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != living)
              .stream()
              .filter(e -> e.getType().getCategory() == MobCategory.MONSTER)
              .count();
      double score = count * 2.0;
      if (score > bestScore) {
        bestScore = score;
        best = living;
      }
    }
    return best != null ? best : pickLowestHealth(candidates);
  }

  private static LivingEntity pickClosestToOwner(
      List<LivingEntity> candidates, LivingEntity owner) {
    if (owner == null) {
      return pickLowestHealth(candidates);
    }
    LivingEntity best = null;
    double bestDist = Double.POSITIVE_INFINITY;
    for (LivingEntity living : candidates) {
      double dist = living.distanceToSqr(owner);
      if (dist < bestDist) {
        bestDist = dist;
        best = living;
      }
    }
    return best;
  }

  private static LivingEntity pickHighestHealthPool(List<LivingEntity> candidates) {
    LivingEntity best = null;
    double bestHp = Double.NEGATIVE_INFINITY;
    for (LivingEntity living : candidates) {
      double pool = Math.max(living.getHealth(), living.getMaxHealth());
      if (pool > bestHp) {
        bestHp = pool;
        best = living;
      }
    }
    return best;
  }

  private static double computePriority(
      CommandTactic tactic, LivingEntity target, AIContext ctx) {
    double base = JianYinGuTuning.COMMAND_INTENT_PRIORITY;
    return switch (tactic) {
      case FOCUS_FIRE -> base + (1.0 - target.getHealth() / Math.max(1.0f, target.getMaxHealth())) * 25.0;
      case INTERCEPT -> base + target.getDeltaMovement().length() * 18.0;
      case SUPPRESS -> base + 12.0;
      case SHEPHERD -> base + Math.max(0.0, 8.0 - target.distanceTo(ctx.owner())) * 3.0;
      case DUEL -> base + Math.max(target.getMaxHealth(), target.getHealth()) * 0.8;
    };
  }

  private static void applyTacticParameters(
      CommandTactic tactic,
      IntentResult.Builder builder,
      LivingEntity target,
      List<LivingEntity> candidates,
      AIContext ctx) {
    switch (tactic) {
      case FOCUS_FIRE ->
          builder.param("speed_scale", 1.1).param("lead_time", 0.45);
      case INTERCEPT ->
          builder.param("speed_scale", 1.2).param("curvature_scale", 1.3).param("lead_time", 0.55);
      case SUPPRESS -> {
        double clusterRadius = 8.0 + countNearby(target, candidates, 6.0) * 0.6;
        builder.param("kiting_safe_radius", clusterRadius)
            .param("serpentine_amplitude", 0.8)
            .param("serpentine_frequency", 0.42)
            .param("speed_scale", 0.95);
      }
      case SHEPHERD ->
          builder.param("orbit_radius", 1.8)
              .param("orbit_shrink", 0.8)
              .param("speed_scale", 1.15);
      case DUEL ->
          builder.param("corkscrew_radius", 0.9)
              .param("corkscrew_frequency", 0.36)
              .param("speed_scale", 1.05);
    }
  }

  private static double countNearby(
      LivingEntity center, List<LivingEntity> candidates, double radius) {
    double rSq = radius * radius;
    int total = 0;
    for (LivingEntity other : candidates) {
      if (other == center) {
        continue;
      }
      if (other.position().distanceToSqr(center.position()) <= rSq) {
        total++;
      }
    }
    return total;
  }

  private static int markTargetsAlongRay(
      ServerPlayer player,
      double maxDistance,
      double radius,
      long nowTick,
      CommandSession session) {
    if (!(player.level() instanceof ServerLevel level)) {
      return 0;
    }

    Vec3 eye = player.getEyePosition();
    Vec3 direction = player.getLookAngle().normalize();
    Vec3 end = eye.add(direction.scale(maxDistance));

    BlockHitResult blockHit =
        level.clip(
            new net.minecraft.world.level.ClipContext(
                eye,
                end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));
    if (blockHit.getType() != BlockHitResult.Type.MISS) {
      end = blockHit.getLocation();
    }

    AABB searchBox = new AABB(eye, end).inflate(radius);
    List<LivingEntity> candidates =
        level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> isCommandTarget(player, entity));

    List<LivingEntity> marked = new ArrayList<>();
    for (LivingEntity target : candidates) {
      Vec3 closest = closestPointOnSegment(target.position(), eye, end);
      double dist = closest.distanceTo(target.position());
      if (dist > radius + target.getBbWidth() * 0.5) {
        continue;
      }
      UUID id = target.getUUID();
      long expires = nowTick + JianYinGuTuning.COMMAND_MARK_DURATION_T;
      session.selectionMarks().put(id, new MarkedTarget(id, expires));
      target.addEffect(
          new MobEffectInstance(
              MobEffects.GLOWING, JianYinGuTuning.COMMAND_MARK_DURATION_T, 0, false, false));
      marked.add(target);
    }
    session.selectionExpiresAt = nowTick + JianYinGuTuning.COMMAND_MARK_DURATION_T;
    return marked.size();
  }

  private static boolean isCommandTarget(ServerPlayer player, LivingEntity target) {
    if (!target.isAlive()) {
      return false;
    }
    if (target == player) {
      return false;
    }
    if (target instanceof ServerPlayer other) {
      return player.isAlliedTo(other);
    }
    if (target instanceof FlyingSwordEntity sword) {
      LivingEntity owner = sword.getOwner();
      if (owner != null && Objects.equals(owner.getUUID(), player.getUUID())) {
        return false;
      }
    }
    if (target instanceof OwnableEntity ownable) {
      UUID ownerId = ownable.getOwnerUUID();
      if (ownerId != null && ownerId.equals(player.getUUID())) {
        return false;
      }
    }
    if (hasCommandOverride(target)) {
      return true;
    }
    if (target instanceof Mob mob) {
      if (mob.getTarget() == player) {
        return true;
      }
    }
    // 允许所有通过前面过滤的生物实体
    return true;
  }

  private static Vec3 closestPointOnSegment(Vec3 point, Vec3 start, Vec3 end) {
    Vec3 seg = end.subtract(start);
    double lenSq = seg.lengthSqr();
    if (lenSq < 1.0e-6) {
      return start;
    }
    double t = point.subtract(start).dot(seg) / lenSq;
    t = Math.max(0.0, Math.min(1.0, t));
    return start.add(seg.scale(t));
  }

  /**
   * 打印调试信息（仅供命令使用）。
   */
  public static List<String> dumpDebug(ServerPlayer player) {
    CommandSession session = session(player).orElse(null);
    if (session == null) {
      return List.of("no-session");
    }
    List<String> lines = new ArrayList<>();
    lines.add("currentGroup=" + session.groupId);
    lines.add("tactic=" + session.tactic.id());
    if (session.groups.isEmpty()) {
      lines.add("groups=none");
    } else {
      for (Map.Entry<Integer, GroupState> entry : session.groups.entrySet()) {
        GroupState state = entry.getValue();
        lines.add(
            String.format(
                Locale.ROOT,
                "g%d exec=%s marks=%d",
                entry.getKey(),
                state.executing,
                state.marks.size()));
      }
    }
    return lines;
  }

  static final class CommandSession {
    private final UUID ownerId;
    private final Map<Integer, GroupState> groups = new HashMap<>();
    private final LinkedHashMap<UUID, MarkedTarget> selectionMarks = new LinkedHashMap<>();
    CommandTactic tactic;
    long lastTuiSentAt;
    int groupId;
    boolean selectionActive;
    long selectionExpiresAt;

    private CommandSession(UUID ownerId) {
      this.ownerId = ownerId;
      this.tactic =
          CommandTactic.byId(JianYinGuTuning.COMMAND_DEFAULT_TACTIC)
              .orElse(CommandTactic.FOCUS_FIRE);
      this.groupId = 0;
    }

    GroupState stateForGroup(int groupId) {
      return groups.computeIfAbsent(groupId, id -> new GroupState());
    }

    GroupState stateForGroup(int groupId, boolean create) {
      if (create) {
        return stateForGroup(groupId);
      }
      return groups.get(groupId);
    }

    GroupState currentState() {
      return stateForGroup(groupId);
    }

    GroupState currentState(boolean create) {
      return stateForGroup(groupId, create);
    }

    void clearCurrentMarks() {
      if (selectionActive) {
        clearSelection();
        return;
      }
      GroupState state = currentState(false);
      if (state != null) {
        state.clearMarks();
        if (state.isIdle()) {
          groups.remove(groupId);
        }
      }
    }

    boolean isCompletelyIdle() {
      return !selectionActive && selectionMarks.isEmpty() && groups.values().stream().allMatch(GroupState::isIdle);
    }

    int markedCount(int groupId) {
      if (selectionActive) {
        return selectionMarks.size();
      }
      GroupState state = stateForGroup(groupId, false);
      return state == null ? 0 : state.markCount();
    }

    boolean hasExecutingGroup(int groupId) {
      GroupState state = stateForGroup(groupId, false);
      return state != null && state.executing;
    }

    boolean hasSelectionActive() {
      return selectionActive;
    }

    long executingUntil(int groupId) {
      GroupState state = stateForGroup(groupId, false);
      return state == null ? 0L : state.executingUntil;
    }

    long selectionExpiresAt() {
      return selectionExpiresAt;
    }

    void removeEmptyGroups() {
      groups.entrySet().removeIf(entry -> entry.getValue().isIdle());
    }

    Set<Integer> activeGroups() {
      return groups.keySet();
    }

    CommandTactic tactic() {
      return tactic;
    }

    int groupId() {
      return groupId;
    }

    void setGroupId(int groupId) {
      this.groupId = Math.max(0, groupId);
    }

    List<GroupSummary> groupSummaries(long nowTick) {
      List<GroupSummary> list = new ArrayList<>();
      for (Map.Entry<Integer, GroupState> entry : groups.entrySet()) {
        GroupState state = entry.getValue();
        long execRemain =
            state.executing ? Math.max(0L, state.executingUntil - nowTick) : 0L;
        list.add(
            new GroupSummary(
                entry.getKey(), state.markCount(), state.executing, execRemain));
      }
      list.sort(Comparator.comparingInt(GroupSummary::groupId));
      return list;
    }

    void resetSelection(long nowTick) {
      selectionMarks.clear();
      selectionActive = true;
      selectionExpiresAt = nowTick + JianYinGuTuning.COMMAND_MARK_DURATION_T;
    }

    void updateSelectionCount(int count) {
      if (count == 0) {
        selectionActive = false;
      }
    }

    void clearSelection() {
      selectionMarks.clear();
      selectionActive = false;
      selectionExpiresAt = 0L;
    }

    LinkedHashMap<UUID, MarkedTarget> selectionMarks() {
      return selectionMarks;
    }

    boolean hasSelectionMarks() {
      return !selectionMarks.isEmpty();
    }

    static final class GroupSummary {
      private final int groupId;
      private final int marks;
      private final boolean executing;
      private final long executingRemainingTicks;

      private GroupSummary(
          int groupId,
          int marks,
          boolean executing,
          long executingRemainingTicks) {
        this.groupId = groupId;
        this.marks = marks;
        this.executing = executing;
        this.executingRemainingTicks = executingRemainingTicks;
      }

      int groupId() {
        return groupId;
      }

      int marks() {
        return marks;
      }

      boolean executing() {
        return executing;
      }

      double executingSeconds() {
        return executingRemainingTicks / 20.0;
      }

      boolean hasActivity() {
        return executing && marks > 0;
      }
    }
  }

  private static final class GroupState {
    private final LinkedHashMap<UUID, MarkedTarget> marks = new LinkedHashMap<>();
    private boolean executing;
    private long executingUntil;

    boolean isIdle() {
      return !executing && marks.isEmpty();
    }

    void concludeIfExpired(long nowTick) {
      if (executing && nowTick > executingUntil) {
        executing = false;
      }
    }

    boolean hasMarks() {
      return !marks.isEmpty();
    }

    int markCount() {
      return marks.size();
    }

    void replaceMarksFromSelection(Collection<MarkedTarget> selection, long executingUntil) {
      marks.clear();
      for (MarkedTarget source : selection) {
        marks.put(source.id, new MarkedTarget(source.id, executingUntil));
      }
      this.executing = true;
      this.executingUntil = executingUntil;
    }

    void clearMarks() {
      marks.clear();
      executing = false;
      executingUntil = 0L;
    }
  }

  private static final class MarkedTarget {
    private final UUID id;
    private long expiresAt;

    private MarkedTarget(UUID id, long expiresAt) {
      this.id = id;
      this.expiresAt = expiresAt;
    }

    long expiresAt() {
      return expiresAt;
    }

    void extend(long tick) {
      this.expiresAt = Math.max(this.expiresAt, tick);
    }
  }
}
