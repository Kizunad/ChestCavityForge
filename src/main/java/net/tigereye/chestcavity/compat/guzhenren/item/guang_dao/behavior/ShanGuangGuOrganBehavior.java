package net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.fx.ShanGuangFx;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TargetingOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TeleportOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NBTWriter;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

/** Behaviour for 闪光蛊（光道·肾脏）。 */
public final class ShanGuangGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener, OrganIncomingDamageListener {

  public static final ShanGuangGuOrganBehavior INSTANCE = new ShanGuangGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String LOG_PREFIX = "[compat/guzhenren][guang_dao][shan_guang_gu]";

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shan_guang_gu");
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shan_guang_gu_flash");

  private static final String STATE_ROOT = "ShanGuangGu";
  private static final String KEY_LAST_SAMPLE_TICK = "SampleTick";
  private static final String KEY_SAMPLE_X = "SampleX";
  private static final String KEY_SAMPLE_Y = "SampleY";
  private static final String KEY_SAMPLE_Z = "SampleZ";
  private static final String KEY_PREV_SAMPLE_TICK = "PrevSampleTick";
  private static final String KEY_PREV_SAMPLE_X = "PrevSampleX";
  private static final String KEY_PREV_SAMPLE_Y = "PrevSampleY";
  private static final String KEY_PREV_SAMPLE_Z = "PrevSampleZ";
  private static final String KEY_LAST_TRAIL_TICK = "LastTrailTick";
  private static final String KEY_TRAILS = "Trails";
  private static final String KEY_DODGE_READY_AT = "DodgeReadyAt";
  private static final String KEY_ABILITY_READY_AT = "AbilityReadyAt";

  private static final double PASSIVE_AURA_RADIUS =
      BehaviorConfigAccess.getFloat(ShanGuangGuOrganBehavior.class, "PASSIVE_AURA_RADIUS", 2.0F);
  private static final int PASSIVE_AURA_DURATION_TICKS =
      BehaviorConfigAccess.getInt(
          ShanGuangGuOrganBehavior.class, "PASSIVE_AURA_DURATION_TICKS", 40);
  private static final int PASSIVE_GLOW_DURATION_TICKS =
      BehaviorConfigAccess.getInt(
          ShanGuangGuOrganBehavior.class, "PASSIVE_GLOW_DURATION_TICKS", 60);

  private static final double DODGE_DISTANCE =
      BehaviorConfigAccess.getFloat(ShanGuangGuOrganBehavior.class, "DODGE_DISTANCE", 3.0F);
  private static final double DODGE_CHANCE =
      BehaviorConfigAccess.getFloat(ShanGuangGuOrganBehavior.class, "DODGE_CHANCE", 0.10F);
  private static final long DODGE_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(ShanGuangGuOrganBehavior.class, "DODGE_COOLDOWN_TICKS", 200);

  private static final long TRAIL_SAMPLE_INTERVAL_TICKS =
      BehaviorConfigAccess.getInt(
          ShanGuangGuOrganBehavior.class, "TRAIL_SAMPLE_INTERVAL_TICKS", 100);
  private static final long TRAIL_PLACE_INTERVAL_TICKS =
      BehaviorConfigAccess.getInt(
          ShanGuangGuOrganBehavior.class, "TRAIL_PLACE_INTERVAL_TICKS", 200);
  private static final long TRAIL_LIFETIME_TICKS =
      BehaviorConfigAccess.getInt(ShanGuangGuOrganBehavior.class, "TRAIL_LIFETIME_TICKS", 600);
  private static final double TRAIL_TRIGGER_RADIUS =
      BehaviorConfigAccess.getFloat(ShanGuangGuOrganBehavior.class, "TRAIL_TRIGGER_RADIUS", 1.0F);
  private static final int TRAIL_MAX_COUNT =
      BehaviorConfigAccess.getInt(ShanGuangGuOrganBehavior.class, "TRAIL_MAX_COUNT", 2);

  private static final double ACTIVE_RADIUS =
      BehaviorConfigAccess.getFloat(ShanGuangGuOrganBehavior.class, "ACTIVE_RADIUS", 10.0F);
  private static final float ACTIVE_DAMAGE =
      BehaviorConfigAccess.getFloat(ShanGuangGuOrganBehavior.class, "ACTIVE_DAMAGE", 8.0F);
  private static final int ACTIVE_SLOW_DURATION_TICKS =
      BehaviorConfigAccess.getInt(
          ShanGuangGuOrganBehavior.class, "ACTIVE_SLOW_DURATION_TICKS", 120);
  private static final int ACTIVE_BLIND_DURATION_TICKS =
      BehaviorConfigAccess.getInt(
          ShanGuangGuOrganBehavior.class, "ACTIVE_BLIND_DURATION_TICKS", 120);
  private static final int ACTIVE_SPEED_DURATION_TICKS =
      BehaviorConfigAccess.getInt(
          ShanGuangGuOrganBehavior.class, "ACTIVE_SPEED_DURATION_TICKS", 100);
  private static final int ACTIVE_SPEED_AMPLIFIER =
      BehaviorConfigAccess.getInt(ShanGuangGuOrganBehavior.class, "ACTIVE_SPEED_AMPLIFIER", 2);
  private static final long ACTIVE_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(ShanGuangGuOrganBehavior.class, "ACTIVE_COOLDOWN_TICKS", 400);
  private static final double ACTIVE_ZHENYUAN_COST =
      BehaviorConfigAccess.getFloat(ShanGuangGuOrganBehavior.class, "ACTIVE_ZHENYUAN_COST", 200.0F);

  private ShanGuangGuOrganBehavior() {}

  static {
    OrganActivationListeners.register(ABILITY_ID, ShanGuangGuOrganBehavior::activateAbility);
  }

  public static long getActiveCooldownTicks() {
    return ACTIVE_COOLDOWN_TICKS;
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }
    Level level = entity.level();
    long gameTime = level.getGameTime();
    OrganState state = organState(organ, STATE_ROOT);
    OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

    applyPassiveAura(entity, level);

    updateSamples(entity, state, collector, gameTime);

    boolean trailsChanged = updateTrails(entity, organ, state, gameTime);
    collector.record(trailsChanged);

    collector.commit();
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (victim == null || victim.level().isClientSide() || damage <= 0.0F) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    Level level = victim.level();
    long gameTime = level.getGameTime();
    MultiCooldown cooldown = createCooldown(cc, organ);
    MultiCooldown.Entry dodgeReady = cooldown.entry(KEY_DODGE_READY_AT).withDefault(0L);
    if (gameTime < dodgeReady.getReadyTick()) {
      return damage;
    }
    if (victim.getRandom().nextDouble() > DODGE_CHANCE) {
      return damage;
    }

    Entity attackerEntity = source.getEntity();
    Vec3 anchor = attackerEntity != null ? attackerEntity.position() : null;
    Optional<Vec3> result = TeleportOps.blinkAwayFrom(victim, anchor, DODGE_DISTANCE);
    if (result.isEmpty()) {
      return damage;
    }

    dodgeReady.setReadyAt(gameTime + DODGE_COOLDOWN_TICKS);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);

    if (level instanceof ServerLevel server) {
      ShanGuangFx.playDodge(server, victim);
    }
    playWhooshSound(victim);
    return 0.0F;
  }

  private void applyPassiveAura(LivingEntity entity, Level level) {
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.GLOWING, PASSIVE_GLOW_DURATION_TICKS, 0, false, false, true));
    if (!(level instanceof ServerLevel server)) {
      return;
    }
    Vec3 center = entity.position();
    double radius = PASSIVE_AURA_RADIUS;
    AABB box = entity.getBoundingBox().inflate(radius);
    double radiusSq = radius * radius;
    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
      if (target == null || target == entity || !target.isAlive()) {
        continue;
      }
      if (target.distanceToSqr(center) > radiusSq) {
        continue;
      }
      if (!CombatEntityUtil.areEnemies(entity, target)) {
        continue;
      }
      target.addEffect(
          new MobEffectInstance(
              MobEffects.BLINDNESS, PASSIVE_AURA_DURATION_TICKS, 0, false, false, false));
      target.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN, PASSIVE_AURA_DURATION_TICKS, 1, false, false, false));
      ReactionTagOps.add(target, ReactionTagKeys.LIGHT_DAZE, PASSIVE_AURA_DURATION_TICKS);
    }
  }

  private void updateSamples(
      LivingEntity entity, OrganState state, OrganStateOps.Collector collector, long gameTime) {
    long lastSampleTick = state.getLong(KEY_LAST_SAMPLE_TICK, 0L);
    if (gameTime - lastSampleTick < TRAIL_SAMPLE_INTERVAL_TICKS) {
      return;
    }
    double currentX = state.getDouble(KEY_SAMPLE_X, entity.getX());
    double currentY = state.getDouble(KEY_SAMPLE_Y, entity.getY());
    double currentZ = state.getDouble(KEY_SAMPLE_Z, entity.getZ());

    collector.recordAll(
        state.setLong(KEY_PREV_SAMPLE_TICK, lastSampleTick),
        state.setDouble(KEY_PREV_SAMPLE_X, currentX),
        state.setDouble(KEY_PREV_SAMPLE_Y, currentY),
        state.setDouble(KEY_PREV_SAMPLE_Z, currentZ));
    collector.recordAll(
        state.setDouble(KEY_SAMPLE_X, entity.getX()),
        state.setDouble(KEY_SAMPLE_Y, entity.getY()),
        state.setDouble(KEY_SAMPLE_Z, entity.getZ()),
        state.setLong(KEY_LAST_SAMPLE_TICK, gameTime));
  }

  private boolean updateTrails(
      LivingEntity entity, ItemStack organ, OrganState state, long gameTime) {
    if (!(entity.level() instanceof ServerLevel serverLevel)) {
      return false;
    }
    List<LightTrail> trails = new ArrayList<>(readTrails(organ));

    boolean changed = trails.removeIf(trail -> gameTime >= trail.expireTick());

    Iterator<LightTrail> iterator = trails.iterator();
    while (iterator.hasNext()) {
      LightTrail trail = iterator.next();
      if (entity.position().distanceToSqr(trail.start())
          <= TRAIL_TRIGGER_RADIUS * TRAIL_TRIGGER_RADIUS) {
        Optional<Vec3> result = TeleportOps.blinkTo(entity, trail.end(), 4, 0.5D);
        if (result.isPresent()) {
          iterator.remove();
          changed = true;
          ShanGuangFx.playTrailJump(
              serverLevel, result.get(), trail.end().subtract(trail.start()), entity);
          playWhooshSound(entity);
          break;
        }
      }
    }

    long prevSampleTick = state.getLong(KEY_PREV_SAMPLE_TICK, 0L);
    Vec3 prevSamplePos =
        new Vec3(
            state.getDouble(KEY_PREV_SAMPLE_X, entity.getX()),
            state.getDouble(KEY_PREV_SAMPLE_Y, entity.getY()),
            state.getDouble(KEY_PREV_SAMPLE_Z, entity.getZ()));
    long lastTrailTick = state.getLong(KEY_LAST_TRAIL_TICK, 0L);
    if (prevSampleTick > 0
        && gameTime - lastTrailTick >= TRAIL_PLACE_INTERVAL_TICKS
        && trails.size() < Math.max(1, TRAIL_MAX_COUNT)) {
      Vec3 currentPos = entity.position();
      if (currentPos.distanceToSqr(prevSamplePos) >= 1.0E-3) {
        trails.add(
            new LightTrail(prevSamplePos, currentPos, gameTime, gameTime + TRAIL_LIFETIME_TICKS));
        state.setLong(KEY_LAST_TRAIL_TICK, gameTime);
        ShanGuangFx.playTrailPlacement(
            serverLevel, prevSamplePos.add(0.0D, entity.getBbHeight() * 0.5D, 0.0D));
        playWhooshSound(entity);
        changed = true;
      }
    }

    if (changed) {
      writeTrails(organ, trails);
    }
    return changed;
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof Player player) || cc == null || entity.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    Level level = entity.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }
    MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
    MultiCooldown.Entry readyEntry = cooldown.entry(KEY_ABILITY_READY_AT).withDefault(0L);
    long gameTime = level.getGameTime();
    if (gameTime < readyEntry.getReadyTick()) {
      return;
    }

    var payment = ResourceOps.consumeStrict(player, ACTIVE_ZHENYUAN_COST, 0.0D);
    if (!payment.succeeded()) {
      return;
    }

    long readyAt = gameTime + ACTIVE_COOLDOWN_TICKS;
    readyEntry.setReadyAt(readyAt);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);

    DamageSource source = resolveDamageSource(entity);
    List<LivingEntity> targets =
        TargetingOps.hostilesWithinRadius(entity, serverLevel, ACTIVE_RADIUS);
    for (LivingEntity target : targets) {
      if (target == null) {
        continue;
      }
      target.hurt(source, ACTIVE_DAMAGE);
      target.addEffect(
          new MobEffectInstance(
              MobEffects.BLINDNESS, ACTIVE_BLIND_DURATION_TICKS, 0, false, false, false));
      target.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN, ACTIVE_SLOW_DURATION_TICKS, 4, false, false, false));
      int tagDuration = Math.max(ACTIVE_SLOW_DURATION_TICKS, ACTIVE_BLIND_DURATION_TICKS);
      ReactionTagOps.add(target, ReactionTagKeys.LIGHT_DAZE, tagDuration);
    }

    entity.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SPEED,
            ACTIVE_SPEED_DURATION_TICKS,
            Math.max(0, ACTIVE_SPEED_AMPLIFIER - 1),
            false,
            false,
            true));

    ShanGuangFx.playBurst(entity);
    if (entity instanceof ServerPlayer serverPlayer) {
      ActiveSkillRegistry.scheduleReadyToast(serverPlayer, ABILITY_ID, readyAt, gameTime);
      serverLevel.playSound(
          null,
          serverPlayer.blockPosition(),
          SoundEvents.BEACON_ACTIVATE,
          SoundSource.PLAYERS,
          1.0F,
          1.2F);
    } else {
      level.playSound(
          null,
          entity.blockPosition(),
          SoundEvents.BEACON_ACTIVATE,
          SoundSource.PLAYERS,
          1.0F,
          1.2F);
    }
  }

  private static DamageSource resolveDamageSource(LivingEntity attacker) {
    if (attacker instanceof Player player) {
      return player.damageSources().playerAttack(player);
    }
    return attacker.damageSources().mobAttack(attacker);
  }

  private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
    MultiCooldown.Builder builder =
        MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
            .withLongClamp(value -> Math.max(0L, value), 0L);
    if (cc != null) {
      builder.withSync(cc, organ);
    } else {
      builder.withOrgan(organ);
    }
    return builder.build();
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack == null || stack.isEmpty()) {
        continue;
      }
      ResourceLocation id =
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (Objects.equals(id, ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  private static void playWhooshSound(LivingEntity entity) {
    entity
        .level()
        .playSound(
            null,
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            SoundEvents.ALLAY_AMBIENT_WITH_ITEM,
            SoundSource.PLAYERS,
            0.4F,
            1.5F);
  }

  private static List<LightTrail> readTrails(ItemStack organ) {
    if (organ == null || organ.isEmpty()) {
      return List.of();
    }
    CustomData data = organ.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      return List.of();
    }
    CompoundTag root = data.copyTag();
    if (!root.contains(STATE_ROOT, Tag.TAG_COMPOUND)) {
      return List.of();
    }
    CompoundTag state = root.getCompound(STATE_ROOT);
    if (!state.contains(KEY_TRAILS, Tag.TAG_LIST)) {
      return List.of();
    }
    ListTag list = state.getList(KEY_TRAILS, Tag.TAG_COMPOUND);
    List<LightTrail> result = new ArrayList<>(list.size());
    for (Tag raw : list) {
      if (!(raw instanceof CompoundTag compound)) {
        continue;
      }
      Vec3 start =
          new Vec3(
              compound.getDouble("StartX"),
              compound.getDouble("StartY"),
              compound.getDouble("StartZ"));
      Vec3 end =
          new Vec3(
              compound.getDouble("EndX"), compound.getDouble("EndY"), compound.getDouble("EndZ"));
      long created = compound.getLong("Created");
      long expire = compound.getLong("Expire");
      result.add(new LightTrail(start, end, created, expire));
    }
    return result;
  }

  private static void writeTrails(ItemStack organ, List<LightTrail> trails) {
    if (organ == null || organ.isEmpty()) {
      return;
    }
    ListTag updated = new ListTag();
    for (LightTrail trail : trails) {
      CompoundTag tag = new CompoundTag();
      tag.putDouble("StartX", trail.start().x);
      tag.putDouble("StartY", trail.start().y);
      tag.putDouble("StartZ", trail.start().z);
      tag.putDouble("EndX", trail.end().x);
      tag.putDouble("EndY", trail.end().y);
      tag.putDouble("EndZ", trail.end().z);
      tag.putLong("Created", trail.createdTick());
      tag.putLong("Expire", trail.expireTick());
      updated.add(tag);
    }
    NBTWriter.updateCustomData(
        organ,
        tag -> {
          CompoundTag state =
              tag.contains(STATE_ROOT, Tag.TAG_COMPOUND)
                  ? tag.getCompound(STATE_ROOT)
                  : new CompoundTag();
          if (updated.isEmpty()) {
            state.remove(KEY_TRAILS);
          } else {
            state.put(KEY_TRAILS, updated);
          }
          tag.put(STATE_ROOT, state);
        });
  }

  private record LightTrail(Vec3 start, Vec3 end, long createdTick, long expireTick) {}
}
