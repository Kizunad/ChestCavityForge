package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import org.joml.Vector3f;

/** Tracks blood trail states applied by 血眼蛊. */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class XieyanguTrailHandler {

  private static final DustParticleOptions BLOOD_DROP =
      new DustParticleOptions(new Vector3f(0.55f, 0.03f, 0.06f), 1.25f);
  private static final DustParticleOptions BLOOD_BURST =
      new DustParticleOptions(new Vector3f(0.62f, 0.04f, 0.09f), 1.5f);

  private static final Map<Integer, TrailState> ACTIVE = new HashMap<>();

  private static final int PARTICLE_INTERVAL = 4;
  private static final int BURST_PARTICLES = 40;

  private XieyanguTrailHandler() {}

  public static void applyTrail(LivingEntity target, int durationTicks) {
    if (target == null || durationTicks <= 0) {
      return;
    }
    ACTIVE.put(target.getId(), new TrailState(durationTicks));
  }

  @SubscribeEvent
  public static void onEntityTick(EntityTickEvent.Post event) {
    if (!(event.getEntity() instanceof LivingEntity living)) {
      return;
    }
    TrailState state = ACTIVE.get(living.getId());
    if (state == null) {
      return;
    }

    Level level = living.level();
    if (level.isClientSide()) {
      return;
    }
    if (!(level instanceof ServerLevel server)) {
      ACTIVE.remove(living.getId());
      return;
    }

    if (!living.isAlive()) {
      spawnBurst(server, living);
      ACTIVE.remove(living.getId());
      return;
    }

    if (state.ticksRemaining <= 0) {
      ACTIVE.remove(living.getId());
      return;
    }

    state.ticksRemaining--;
    state.particleCooldown--;
    if (state.particleCooldown <= 0) {
      state.particleCooldown = PARTICLE_INTERVAL;
      spawnTrailParticle(server, living);
    }
  }

  private static void spawnTrailParticle(ServerLevel server, LivingEntity target) {
    Vec3 base = target.position();
    double y = target.getY(0.05);
    RandomSource random = server.getRandom();
    double offsetRadius = 0.25 + random.nextDouble() * 0.25;
    double angle = random.nextDouble() * Math.PI * 2.0;
    double x = base.x + Math.cos(angle) * offsetRadius;
    double z = base.z + Math.sin(angle) * offsetRadius;

    server.sendParticles(BLOOD_DROP, x, y, z, 1, 0.02, 0.0, 0.02, 0.0);
  }

  private static void spawnBurst(ServerLevel server, LivingEntity target) {
    Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.35, 0.0);
    server.sendParticles(
        BLOOD_BURST, center.x, center.y, center.z, BURST_PARTICLES, 0.4, 0.3, 0.4, 0.05);
  }

  private static final class TrailState {
    private int ticksRemaining;
    private int particleCooldown = PARTICLE_INTERVAL;

    private TrailState(int durationTicks) {
      this.ticksRemaining = durationTicks;
    }
  }
}
