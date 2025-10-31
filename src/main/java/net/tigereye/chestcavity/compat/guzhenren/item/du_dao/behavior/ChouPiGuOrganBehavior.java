package net.tigereye.chestcavity.compat.guzhenren.item.du_dao.behavior;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.tuning.ChouPiTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.calculator.ChouPiGuCalculator;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
// ReactionEngine 相关调用已不在此类使用，移除旧导入。
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.fx.ChouPiFx;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.messages.ChouPiMessages;

/** Behaviour implementation for 臭屁蛊 (Chou Pi Gu). */
public enum ChouPiGuOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener {
  INSTANCE;

  // 数值常量已迁出至 ChouPiTuning，行为内仅保留 ID 等。

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation DU_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/du_dao_increase_effect");
  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private static final String STATE_ROOT = "ChouPiGu";
  private static final String INTERVAL_KEY = "NextIntervalTicks";
  private static final ResourceLocation READY_AT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "ready_at/chou_pi_gu_interval");
  // 视图效果/行为数值均从 ChouPiTuning 读取。
  private static final ResourceLocation[] ATTRACTABLE_ENTITIES =
      new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "choupifeichonggu"),
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "aibieli")
      };

  private enum TriggerCause {
    RANDOM,
    DAMAGE,
    FOOD
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity.level().isClientSide()) {
      return;
    }
    MultiCooldown cooldown = createCooldown(cc, organ);
    MultiCooldown.Entry ready = cooldown.entry(READY_AT_ID.toString());
    if (!(entity instanceof Player player)) {
      handleNonPlayerSlowTick(entity, cc, organ, ready);
      return;
    }
    if (!player.isAlive()) {
      return;
    }

    RandomSource random = player.getRandom();
    ServerLevel server = player.level() instanceof ServerLevel s ? s : null;
    if (server != null) {
      long now = server.getGameTime();
      if (ready.getReadyTick() <= 0L || now >= ready.getReadyTick()) {
        int interval = randomInterval(random);
        ready.setReadyAt(now + interval);
      }
      ready.onReady(
          server,
          server.getGameTime(),
          () -> {
            // 无论是否成功释放，下一次都按随机间隔排队
            int intervalNext = randomInterval(random);
            MultiCooldown.Entry e = createCooldown(cc, organ).entry(READY_AT_ID.toString());
            e.setReadyAt(server.getGameTime() + intervalNext);
            e.onReady(server, server.getGameTime(), () -> {});
          });
    }
  }

  private void handleNonPlayerSlowTick(
      LivingEntity entity, ChestCavityInstance cc, ItemStack organ, MultiCooldown.Entry ready) {
    if (entity == null || organ == null || organ.isEmpty() || !entity.isAlive()) {
      return;
    }
    if (!(entity.level() instanceof ServerLevel server)) {
      return;
    }
    RandomSource random = entity.getRandom();
    long now = server.getGameTime();
    if (ready.getReadyTick() <= 0L || now >= ready.getReadyTick()) {
      ready.setReadyAt(now + randomInterval(random));
    }
    ready.onReady(
        server,
        now,
        () -> {
          if (!releaseGas(entity, cc, organ, random, TriggerCause.RANDOM)) {
            MultiCooldown.Entry e = createCooldown(cc, organ).entry(READY_AT_ID.toString());
            e.setReadyAt(server.getGameTime() + randomInterval(random));
            e.onReady(server, server.getGameTime(), () -> {});
          } else {
            MultiCooldown.Entry e = createCooldown(cc, organ).entry(READY_AT_ID.toString());
            e.setReadyAt(server.getGameTime() + randomInterval(random));
            e.onReady(server, server.getGameTime(), () -> {});
          }
        });
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (victim.level().isClientSide()) {
      return damage;
    }
    MultiCooldown cooldown = createCooldown(cc, organ);
    MultiCooldown.EntryInt intervalEntry = cooldown.entryInt(INTERVAL_KEY);
    RandomSource random = victim.getRandom();
    if (victim instanceof Player player) {
      double increase = Math.max(0.0, getPoisonIncrease(cc));
      double chance =
          ChouPiGuCalculator.triggerChanceWithIncrease(
              ChouPiTuning.DAMAGE_TRIGGER_BASE_CHANCE, increase);
      if (ChouPiGuCalculator.shouldTrigger(chance, random.nextDouble())) {
        releaseGas(player, cc, organ, random, TriggerCause.DAMAGE);
        if (player.level() instanceof ServerLevel server) {
          int interval = randomInterval(random);
          MultiCooldown.Entry e = createCooldown(cc, organ).entry(READY_AT_ID.toString());
          e.setReadyAt(server.getGameTime() + interval);
          e.onReady(server, server.getGameTime(), () -> {});
        }
      }
      return damage;
    }
    handleNonPlayerDamage(victim, cc, organ, intervalEntry, random);
    return damage;
  }

  private void handleNonPlayerDamage(
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      MultiCooldown.EntryInt intervalEntry,
      RandomSource random) {
    if (victim == null || organ == null || organ.isEmpty()) {
      return;
    }
    if (!victim.isAlive()) {
      return;
    }

    double increase = Math.max(0.0, getPoisonIncrease(cc));
    double chance =
        ChouPiGuCalculator.triggerChanceWithIncrease(
            ChouPiTuning.DAMAGE_TRIGGER_BASE_CHANCE, increase);
    if (ChouPiGuCalculator.shouldTrigger(chance, random.nextDouble())) {
      releaseGas(victim, cc, organ, random, TriggerCause.DAMAGE);
      if (victim.level() instanceof ServerLevel server) {
        int interval = randomInterval(random);
        MultiCooldown.Entry e = createCooldown(cc, organ).entry(READY_AT_ID.toString());
        e.setReadyAt(server.getGameTime() + interval);
        e.onReady(server, server.getGameTime(), () -> {});
      }
    }
  }

  public void onFoodConsumed(
      Player player, ChestCavityInstance cc, ItemStack food, double baseChance) {
    if (player.level().isClientSide() || cc == null || cc.inventory == null) {
      return;
    }
    RandomSource random = player.getRandom();
    double chance = Math.min(1.0, Math.max(0.0, baseChance));
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack organ = cc.inventory.getItem(i);
      if (organ == null || organ.isEmpty() || !organ.is(CCItems.GUZHENREN_CHOU_PI_GU)) {
        continue;
      }
      if (random.nextDouble() < chance) {
        releaseGas(player, cc, organ, random, TriggerCause.FOOD);
        if (player.level() instanceof ServerLevel server) {
          int interval = randomInterval(random);
          MultiCooldown.Entry e = createCooldown(cc, organ).entry(READY_AT_ID.toString());
          e.setReadyAt(server.getGameTime() + interval);
          e.onReady(server, server.getGameTime(), () -> {});
        }
        break;
      }
    }
  }

  public void ensureAttached(ChestCavityInstance cc) {
    if (cc == null) {
      return;
    }
    ensurePoisonChannel(cc);
  }

  private boolean releaseGas(
      LivingEntity entity,
      ChestCavityInstance cc,
      ItemStack organ,
      RandomSource random,
      TriggerCause cause) {
    if (entity == null) {
      return false;
    }
    Level level = entity.level();
    if (!(level instanceof ServerLevel server)) {
      return false;
    }
    performEffects(server, entity, cc, organ, random, cause);
    return true;
  }

  private void performEffects(
      ServerLevel level,
      LivingEntity entity,
      ChestCavityInstance cc,
      ItemStack organ,
      RandomSource random,
      TriggerCause cause) {
    if (ChouPiTuning.FX_SOUND_ENABLED) {
      ChouPiFx.playSounds(level, entity, random);
    }
    if (ChouPiTuning.FX_PARTICLE_ENABLED) {
      ChouPiFx.spawnParticles(level, entity, random);
    }
    ChouPiMessages.notify(level, entity, random);
    applyDebuffs(level, entity, cc, organ, random);
    maybeDebuffSelf(entity, cc, organ, random);
    panicNearbyCreatures(level, entity, random);
    maybeSummonAttractedCreature(level, entity, random);
  }

  

  private void applyDebuffs(
      ServerLevel level,
      LivingEntity entity,
      ChestCavityInstance cc,
      ItemStack organ,
      RandomSource random) {
    int stackCount = Math.max(1, organ.getCount());
    int duration = ChouPiGuCalculator.effectDurationTicks(stackCount);
    int poisonAmplifier = ChouPiGuCalculator.poisonAmplifier(getPoisonIncrease(cc));

    AABB area = entity.getBoundingBox().inflate(ChouPiTuning.EFFECT_RADIUS);
    List<LivingEntity> victims =
        level.getEntitiesOfClass(
            LivingEntity.class,
            area,
            candidate -> candidate != null && candidate.isAlive() && candidate != entity);
    // 在脚下投放一小团“毒雾残留”（先复用腐蚀残留域实现）
    net.tigereye.chestcavity.engine.reaction.ResidueManager.spawnOrRefreshCorrosion(
        level,
        entity.getX(),
        entity.getY(),
        entity.getZ(),
        ChouPiGuCalculator.residueRadius(ChouPiTuning.EFFECT_RADIUS),
        ChouPiGuCalculator.residueDurationTicks(duration));
    for (LivingEntity victim : victims) {
      victim.addEffect(
          new MobEffectInstance(MobEffects.POISON, duration, poisonAmplifier, false, true, true));
      victim.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, 0, false, true, true));
      victim.addEffect(
          new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0, false, true, true));
      victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
      ReactionTagOps.add(victim, ReactionTagKeys.STENCH_CLOUD, duration);
      ReactionTagOps.add(victim, ReactionTagKeys.TOXIC_MARK, Math.max(20, duration / 2));
    }
  }

  private void maybeDebuffSelf(
      LivingEntity entity, ChestCavityInstance cc, ItemStack organ, RandomSource random) {
    if (!ChouPiGuCalculator.shouldTrigger(ChouPiTuning.SELF_DEBUFF_CHANCE, random.nextDouble())) {
      return;
    }
    int stackCount = Math.max(1, organ.getCount());
    int duration = ChouPiGuCalculator.effectDurationTicks(stackCount);
    int poisonAmplifier = ChouPiGuCalculator.poisonAmplifier(getPoisonIncrease(cc));
    entity.addEffect(
        new MobEffectInstance(MobEffects.POISON, duration, poisonAmplifier, false, true, true));
    entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
    ReactionTagOps.add(entity, ReactionTagKeys.TOXIC_IMMUNE, Math.max(40, duration / 2));
  }

  private void panicNearbyCreatures(ServerLevel level, LivingEntity entity, RandomSource random) {
    AABB area = entity.getBoundingBox().inflate(ChouPiTuning.PANIC_DISTANCE);
    List<PathfinderMob> mobs =
        level.getEntitiesOfClass(
            PathfinderMob.class,
            area,
            mob ->
                mob != null
                    && mob.isAlive()
                    && mob.distanceToSqr(entity)
                        <= (double) (ChouPiTuning.PANIC_DISTANCE * ChouPiTuning.PANIC_DISTANCE)
                    && (mob instanceof Animal || mob instanceof AbstractVillager));
    Vec3 entityPos = entity.position();
    for (PathfinderMob mob : mobs) {
      Vec3 away = mob.position().subtract(entityPos);
      if (away.lengthSqr() < 1.0E-4) {
        away = new Vec3(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5);
      }
      Vec3 target = mob.position().add(away.normalize().scale(ChouPiTuning.PANIC_DISTANCE));
      mob.getNavigation().moveTo(target.x, target.y, target.z, 1.4);
    }
  }

  private void maybeSummonAttractedCreature(
      ServerLevel level, LivingEntity entity, RandomSource random) {
    if (!ChouPiGuCalculator.shouldTrigger(ChouPiTuning.ATTRACT_CHANCE, random.nextDouble())
        || ATTRACTABLE_ENTITIES.length == 0) {
      return;
    }
    ResourceLocation id = ATTRACTABLE_ENTITIES[random.nextInt(ATTRACTABLE_ENTITIES.length)];
    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
    if (type == null) {
      return;
    }

    Vec3 spawnPos = findSpawnPosition(level, entity, random);
    if (spawnPos == null) {
      return;
    }

    Entity spawned = type.create(level);
    if (spawned == null) {
      return;
    }
    spawned.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, random.nextFloat() * 360.0f, 0.0f);
    level.addFreshEntity(spawned);
  }

  private static Vec3 findSpawnPosition(
      ServerLevel level, LivingEntity entity, RandomSource random) {
    Vec3 origin = entity.position();
    for (int attempt = 0; attempt < 5; attempt++) {
      double distance = 2.5 + random.nextDouble() * 2.5;
      double angle = random.nextDouble() * Math.PI * 2.0;
      double x = origin.x + Math.cos(angle) * distance;
      double z = origin.z + Math.sin(angle) * distance;
      BlockPos sample = BlockPos.containing(x, origin.y, z);
      if (!level.isLoaded(sample)) {
        continue;
      }
      BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sample);
      if (!level.getWorldBorder().isWithinBounds(surface)) {
        continue;
      }
      return new Vec3(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
    }
    return null;
  }

  private static int randomInterval(RandomSource random) {
    return ChouPiGuCalculator.randomIntervalTicks(
        ChouPiTuning.RANDOM_INTERVAL_MIN_TICKS,
        ChouPiTuning.RANDOM_INTERVAL_MAX_TICKS,
        random.nextDouble());
  }

  private static double getPoisonIncrease(ChestCavityInstance cc) {
    LinkageChannel channel = ensurePoisonChannel(cc);
    return channel == null ? 0.0 : channel.get();
  }

  private static LinkageChannel ensurePoisonChannel(ChestCavityInstance cc) {
    if (cc == null) {
      return null;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    return LedgerOps.ensureChannel(context, DU_DAO_INCREASE_EFFECT, NON_NEGATIVE);
  }

  private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
    MultiCooldown.Builder builder =
        MultiCooldown.builder(organ, STATE_ROOT).withIntClamp(value -> Math.max(0, value), 0);
    if (cc != null && organ != null && !organ.isEmpty()) {
      builder.withSync(cc, organ);
    } else if (organ != null) {
      builder.withOrgan(organ);
    }
    return builder.build();
  }
}
