package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.FxOps;
import org.joml.Vector3f;

/**
 * Visual and audio effects for 血衣蛊 (Xue Yi Gu). All effects use vanilla particles and sounds
 * only. Provides quality scaling support for performance optimization.
 */
public final class XueYiGuEffects {

  // Blood-themed particle palette
  private static final DustParticleOptions BLOOD_DARK =
      new DustParticleOptions(new Vector3f(0.5f, 0.0f, 0.0f), 1.2f);

  private static final DustParticleOptions BLOOD_BRIGHT =
      new DustParticleOptions(new Vector3f(0.9f, 0.1f, 0.1f), 1.0f);

  private static final DustParticleOptions BLOOD_CRIMSON =
      new DustParticleOptions(new Vector3f(0.7f, 0.05f, 0.05f), 0.8f);

  private static final DustParticleOptions BLOOD_GLOW =
      new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.2f), 1.4f);

  private XueYiGuEffects() {}

  // ============================================================
  // 1. 血涌披身 (Blood Aura) Effects
  // ============================================================

  /** Plays activation burst when blood aura is enabled. */
  public static void playAuraActivation(ServerLevel level, Player player) {
    Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);

    // Expanding blood mist rings
    for (int ring = 0; ring < 3; ring++) {
      double radius = 0.5 + ring * 0.7;
      int particleCount = 12 + ring * 4;

      for (int i = 0; i < particleCount; i++) {
        double angle = (i / (double) particleCount) * Math.PI * 2;
        double x = center.x + Math.cos(angle) * radius;
        double z = center.z + Math.sin(angle) * radius;

        FxOps.particles(
            level, BLOOD_BRIGHT, new Vec3(x, center.y, z), 1, 0.05, 0.15, 0.05, 0.02);
      }
    }

    // Ground blood pool
    FxOps.particles(
        level,
        ParticleTypes.LANDING_LAVA,
        new Vec3(center.x, player.getY() + 0.1, center.z),
        16,
        1.0,
        0.0,
        1.0,
        0.0);

    // Rising blood mist
    FxOps.particles(
        level,
        ParticleTypes.CRIMSON_SPORE,
        new Vec3(center.x, center.y - 0.5, center.z),
        12,
        0.4,
        0.0,
        0.4,
        0.08);

    // Sound: activation
    Vec3 pos = player.position();
    FxOps.playSound(
        level,
        pos,
        SoundEvents.ARMOR_EQUIP_LEATHER.value(),
        SoundSource.PLAYERS,
        1.0f,
        0.9f);

    FxOps.playSound(
        level,
        pos,
        SoundEvents.WARDEN_HEARTBEAT,
        SoundSource.PLAYERS,
        0.3f,
        0.7f);
  }

  /** Plays continuous aura effect while blood aura is active. */
  public static void playAuraMaintain(ServerLevel level, Player player, int tickCount) {
    Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
    double time = tickCount * 0.1;

    // Spiral blood bands around body
    int spiralArms = 2;
    for (int arm = 0; arm < spiralArms; arm++) {
      double armOffset = (arm / (double) spiralArms) * Math.PI * 2;

      for (int point = 0; point < 8; point++) {
        double height = (point / 8.0) * player.getBbHeight() * 1.2;
        double angle = time * 0.5 + armOffset + (height * 2);
        double radius = 0.6 + Math.sin(time + point) * 0.15;

        double x = center.x + Math.cos(angle) * radius;
        double z = center.z + Math.sin(angle) * radius;
        double y = player.getY() + height;

        FxOps.particles(level, BLOOD_DARK, new Vec3(x, y, z), 1, 0.02, 0.02, 0.02, 0.0);
      }
    }

    // Range boundary pulse every 2 seconds
    if (tickCount % 40 == 0) {
      playRangePulse(level, center, 2.0, BLOOD_CRIMSON, 24);
    }

    // Random blood drips
    if (level.random.nextFloat() < 0.3f) {
      double rx = center.x + (level.random.nextDouble() - 0.5) * 1.5;
      double rz = center.z + (level.random.nextDouble() - 0.5) * 1.5;

      FxOps.particles(
          level,
          ParticleTypes.DRIPPING_DRIPSTONE_LAVA,
          new Vec3(rx, center.y + 0.8, rz),
          1,
          0.0,
          0.0,
          0.0,
          0.0);
    }
  }

  /** Plays effect when aura damages an enemy. */
  public static void playAuraHitTarget(ServerLevel level, LivingEntity target) {
    Vec3 pos = target.position().add(0, target.getBbHeight() * 0.5, 0);

    // Damage indicators
    FxOps.particles(level, ParticleTypes.DAMAGE_INDICATOR, pos, 3, 0.3, 0.4, 0.3, 0.1);

    // Rising blood mist
    FxOps.particles(level, BLOOD_CRIMSON, pos, 2, 0.2, 0.3, 0.2, 0.02);

    // Ground blood stain
    FxOps.particles(
        level,
        ParticleTypes.LANDING_LAVA,
        new Vec3(pos.x, target.getY() + 0.05, pos.z),
        4,
        0.3,
        0.0,
        0.3,
        0.0);

    // Sound: hit
    FxOps.playSound(
        level, target.blockPosition().getCenter(),         SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS, 0.4f, 1.2f);
  }

  /** Plays deactivation effect when aura is turned off. */
  public static void playAuraDeactivation(ServerLevel level, Player player) {
    Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);

    // Inward contraction
    FxOps.particles(level, ParticleTypes.POOF, center, 20, 0.8, 0.6, 0.8, 0.05);

    // Sinking blood mist
    FxOps.particles(level, BLOOD_DARK, center, 12, 0.5, 0.3, 0.5, -0.02);

    // Sound: extinguish
    FxOps.playSound(
        level,
        player.blockPosition().getCenter(),
        SoundEvents.FIRE_EXTINGUISH,
        SoundSource.PLAYERS,
        0.8f,
        1.0f);
  }

  // ============================================================
  // 2. 血束收紧 (Blood束) Effects
  // ============================================================

  /** Plays charging effect before launching blood束. */
  public static void playBeamCharging(ServerLevel level, Player player) {
    Vec3 eyePos = player.getEyePosition();

    // Blood energy gathering
    FxOps.particles(level, BLOOD_GLOW, eyePos, 8, 0.2, 0.2, 0.2, -0.05);

    // Hand position flash
    Vec3 handPos =
        player
            .position()
            .add(player.getLookAngle().scale(0.5))
            .add(0, player.getBbHeight() * 0.6, 0);

    FxOps.particles(level, ParticleTypes.ENCHANT, handPos, 5, 0.1, 0.1, 0.1, 0.02);

    // Sound: charging
    FxOps.playSound(
        level,
        player.blockPosition().getCenter(),
        SoundEvents.ENCHANTMENT_TABLE_USE,
        SoundSource.PLAYERS,
        0.7f,
        1.1f);
  }

  /** Plays beam trail from start to end position. */
  public static void playBeamEffect(ServerLevel level, Vec3 start, Vec3 end) {
    Vec3 direction = end.subtract(start).normalize();
    double distance = start.distanceTo(end);
    int sampleCount = (int) (distance * 5);

    // Main beam line
    for (int i = 0; i < sampleCount; i++) {
      double t = i / (double) sampleCount;
      Vec3 point = start.add(direction.scale(distance * t));

      // Inner bright layer
      FxOps.particles(level, BLOOD_BRIGHT, point, 1, 0.02, 0.02, 0.02, 0.0);

      // Outer dark layer (offset for thickness)
      if (i % 2 == 0) {
        Vec3 offset = getPerpendicularOffset(direction, level, 0.08);
        FxOps.particles(
            level,
            BLOOD_DARK,
            new Vec3(point.x + offset.x, point.y + offset.y, point.z + offset.z),
            1,
            0.03,
            0.03,
            0.03,
            0.0);
      }
    }

    // Blood splatter along trail
    for (int i = 0; i < (int) distance; i++) {
      Vec3 point = start.add(direction.scale(i + 0.5));

      if (level.random.nextFloat() < 0.4f) {
        FxOps.particles(
            level, ParticleTypes.CRIMSON_SPORE, point, 2, 0.15, 0.15, 0.15, 0.03);
      }
    }

    // Sound: projectile
    FxOps.playSound(
        level,
        start,
        SoundEvents.TRIDENT_THROW.value(),
        SoundSource.PLAYERS,
        0.9f,
        1.3f);
  }

  /** Plays impact effect when beam hits target. */
  public static void playBeamHit(ServerLevel level, LivingEntity target) {
    Vec3 pos = target.position().add(0, target.getBbHeight() * 0.5, 0);

    // Explosive blood mist
    FxOps.particles(level, BLOOD_GLOW, pos, 20, 0.4, 0.5, 0.4, 0.15);

    // Sweep attack
    FxOps.particles(level, ParticleTypes.SWEEP_ATTACK, pos, 2, 0.5, 0.3, 0.5, 0.0);

    // Damage indicators
    FxOps.particles(level, ParticleTypes.DAMAGE_INDICATOR, pos, 8, 0.4, 0.5, 0.4, 0.2);

    // Blood threads binding effect
    for (int i = 0; i < 3; i++) {
      double radius = 0.6;
      double heightOffset = i * 0.3;

      for (int angle = 0; angle < 360; angle += 30) {
        double rad = Math.toRadians(angle);
        double x = pos.x + Math.cos(rad) * radius;
        double z = pos.z + Math.sin(rad) * radius;

        FxOps.particles(level, BLOOD_DARK, new Vec3(x, pos.y + heightOffset, z), 1, 0.0, 0.0, 0.0, 0.0);
      }
    }

    // Sound: impact
    Vec3 targetPos = target.position();
    FxOps.playSound(
        level, targetPos, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.0f, 0.9f);

    FxOps.playSound(
        level, targetPos, SoundEvents.GENERIC_HURT, SoundSource.PLAYERS, 0.5f, 1.1f);
  }

  // ============================================================
  // 3. 血缝急闭 (Blood Seal) Effects
  // ============================================================

  /** Plays scanning effect to detect bleeding targets. */
  public static void playSealScan(ServerLevel level, Player player, List<LivingEntity> targets) {
    Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);

    // Detection wave expanding
    for (int wave = 0; wave < 2; wave++) {
      double radius = 4.0;
      int points = 32;

      for (int i = 0; i < points; i++) {
        double angle = (i / (double) points) * Math.PI * 2;
        double x = center.x + Math.cos(angle) * radius;
        double z = center.z + Math.sin(angle) * radius;

        // Use crimson spore instead of entity effect for simplicity
        FxOps.particles(level, ParticleTypes.CRIMSON_SPORE, new Vec3(x, center.y, z), 1, 0.0, 0.1, 0.0, 0.02);
      }
    }

    // Mark each target
    for (LivingEntity target : targets) {
      Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
      FxOps.particles(level, BLOOD_BRIGHT, targetPos, 5, 0.3, 0.3, 0.3, 0.0);
    }

    // Sound: scan
    FxOps.playSound(
        level,
        player.blockPosition().getCenter(),
        SoundEvents.ENCHANTMENT_TABLE_USE,
        SoundSource.PLAYERS,
        0.8f,
        1.3f);
  }

  /** Plays absorption effect as blood flows from targets to player. */
  public static void playSealAbsorption(
      ServerLevel level, Player player, List<LivingEntity> targets, int delayTicks) {
    Vec3 playerCenter = player.position().add(0, player.getBbHeight() * 0.5, 0);

    for (LivingEntity target : targets) {
      Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
      Vec3 direction = playerCenter.subtract(targetPos).normalize();
      double distance = targetPos.distanceTo(playerCenter);

      // Stream of blood particles flowing to player
      for (int i = 0; i < 20; i++) {
        int particleIndex = i;
        int delay = delayTicks + (i * 2);

        // Schedule particle spawn
        spawnParticleDelayed(
            level,
            delay,
            () -> {
              double progress = particleIndex / 20.0;
              Vec3 currentPos = targetPos.add(direction.scale(distance * progress));

              FxOps.particles(
                  level, BLOOD_GLOW, currentPos, 1, 0.05, 0.05, 0.05, 0.1);

              // Heart particles every 5 steps
              if (particleIndex % 5 == 0) {
                FxOps.particles(
                    level, ParticleTypes.HEART, currentPos, 1, 0.0, 0.0, 0.0, 0.0);
              }
            });
      }

      // Target loses blood
      Vec3 pos = target.position().add(0, target.getBbHeight() * 0.5, 0);
      FxOps.particles(level, ParticleTypes.DAMAGE_INDICATOR, pos, 6, 0.3, 0.4, 0.3, 0.1);
      FxOps.particles(level, ParticleTypes.EFFECT, pos, 8, 0.3, 0.5, 0.3, 0.02);
    }
  }

  /** Plays effect when player gains absorption. */
  public static void playSealAbsorptionGain(ServerLevel level, Player player, float amount) {
    Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);

    // Blood shield forming (outward burst)
    FxOps.particles(level, BLOOD_GLOW, center, 30, 0.0, 0.0, 0.0, 0.2);

    // Shield membrane around body (2 seconds)
    for (int frame = 0; frame < 40; frame++) {
      int finalFrame = frame;
      spawnParticleDelayed(
          level,
          frame,
          () -> {
            double radius = 0.8;
            int points = 12;

            for (int i = 0; i < points; i++) {
              double angle = (i / (double) points) * Math.PI * 2 + (finalFrame * 0.1);
              double x = center.x + Math.cos(angle) * radius;
              double z = center.z + Math.sin(angle) * radius;
              double y = center.y + Math.sin(finalFrame * 0.2) * 0.3;

              FxOps.particles(level, BLOOD_BRIGHT, new Vec3(x, y, z), 1, 0.0, 0.0, 0.0, 0.0);
            }
          });
    }

    // Large absorption - totem effect
    if (amount >= 20) {
      FxOps.particles(
          level, ParticleTypes.TOTEM_OF_UNDYING, center, 8, 0.5, 0.6, 0.5, 0.1);
    }

    // Debuff clear flash
    FxOps.particles(level, ParticleTypes.GLOW, center, 12, 0.4, 0.5, 0.4, 0.05);

    // Sound: shield block
    FxOps.playSound(
        level,
        player.blockPosition().getCenter(),
        SoundEvents.SHIELD_BLOCK,
        SoundSource.PLAYERS,
        1.0f,
        0.9f);

    if (amount >= 30) {
      FxOps.playSound(
          level,
          player.blockPosition().getCenter(),
          SoundEvents.TOTEM_USE,
          SoundSource.PLAYERS,
          0.3f,
          1.2f);
    }

    // Sound: level up (debuff clear)
    FxOps.playSound(
        level,
        player.blockPosition().getCenter(),
        SoundEvents.PLAYER_LEVELUP,
        SoundSource.PLAYERS,
        0.4f,
        1.5f);
  }

  // ============================================================
  // 4. 溢血反刺 (Blood Reflect) Effects
  // ============================================================

  /** Plays effect when reflect window starts. */
  public static void playReflectWindowStart(ServerLevel level, Player player) {
    Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);

    // Ground sweep attack
    FxOps.particles(
        level,
        ParticleTypes.SWEEP_ATTACK,
        new Vec3(center.x, player.getY() + 0.1, center.z),
        3,
        0.0,
        0.0,
        0.0,
        0.0);

    // Upward blood spikes
    for (int spike = 0; spike < 8; spike++) {
      double angle = (spike / 8.0) * Math.PI * 2;
      double radius = 0.5;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;

      FxOps.particles(level, BLOOD_DARK, new Vec3(x, center.y - 0.5, z), 3, 0.05, 0.0, 0.05, 0.15);
    }

    // Sound: activation
    Vec3 pos = player.position();
    FxOps.playSound(
        level,
        pos,
        SoundEvents.NOTE_BLOCK_BASS.value(),
        SoundSource.PLAYERS,
        1.0f,
        0.7f);

    FxOps.playSound(
        level,
        player.blockPosition().getCenter(),
        SoundEvents.PLAYER_ATTACK_SWEEP,
        SoundSource.PLAYERS,
        0.7f,
        0.9f);
  }

  /** Plays continuous effect during reflect window. */
  public static void playReflectWindowMaintain(ServerLevel level, Player player, int tickCount) {
    Vec3 center = player.position();

    // Sweep ring every second
    if (tickCount % 20 == 0) {
      FxOps.particles(
          level,
          ParticleTypes.SWEEP_ATTACK,
          new Vec3(center.x, player.getY() + 0.05, center.z),
          1,
          0.0,
          0.0,
          0.0,
          0.0);

      // Ring pulse
      double radius = 1.2;
      int points = 16;
      for (int i = 0; i < points; i++) {
        double angle = (i / (double) points) * Math.PI * 2;
        double x = center.x + Math.cos(angle) * radius;
        double z = center.z + Math.sin(angle) * radius;

        FxOps.particles(level, BLOOD_CRIMSON, new Vec3(x, center.y + 0.3, z), 1, 0.0, 0.1, 0.0, 0.0);
      }
    }

    // Floating blood spikes
    if (tickCount % 5 == 0) {
      for (int i = 0; i < 6; i++) {
        double angle = (tickCount * 0.05) + (i / 6.0) * Math.PI * 2;
        double radius = 0.7;
        double height = player.getBbHeight() * 0.5 + Math.sin(angle * 2) * 0.3;

        double x = center.x + Math.cos(angle) * radius;
        double z = center.z + Math.sin(angle) * radius;

        FxOps.particles(level, BLOOD_DARK, new Vec3(x, center.y + height, z), 1, 0.0, 0.0, 0.0, 0.0);
      }
    }
  }

  /** Plays effect when damage is reflected. */
  public static void playReflectTrigger(
      ServerLevel level, Player player, LivingEntity attacker, float reflectedAmount) {
    Vec3 playerPos = player.position().add(0, player.getBbHeight() * 0.5, 0);
    Vec3 attackerPos = attacker.position().add(0, attacker.getBbHeight() * 0.5, 0);

    Vec3 direction = attackerPos.subtract(playerPos).normalize();

    // Shield counter-burst from player
            FxOps.particles(
                level,
                BLOOD_GLOW,
                playerPos,
                8,
                direction.x * 0.5,
                direction.y * 0.5,
                direction.z * 0.5,
                0.3);
    // Blood spike projectile to attacker
    double distance = playerPos.distanceTo(attackerPos);
    int spikeCount = (int) (distance * 3);

    for (int i = 0; i < spikeCount; i++) {
      int finalI = i;
      spawnParticleDelayed(
          level,
          i,
          () -> {
            double progress = finalI / (double) spikeCount;
            Vec3 point = playerPos.add(direction.scale(distance * progress));

            FxOps.particles(level, BLOOD_BRIGHT, point, 2, 0.1, 0.1, 0.1, 0.0);
          });
    }

    // Impact on attacker
    int damageParticles = 5 + (int) (reflectedAmount / 10);
    FxOps.particles(
        level,
        ParticleTypes.DAMAGE_INDICATOR,
        attackerPos,
        damageParticles,
        0.3,
        0.5,
        0.3,
        0.15);

    FxOps.particles(level, BLOOD_DARK, attackerPos, 8, 0.4, 0.5, 0.4, 0.1);

    // Blood spikes erupting from attacker
    for (int spike = 0; spike < 4; spike++) {
      int finalSpike = spike;
      spawnParticleDelayed(
          level,
          spike * 3,
          () -> {
            double rx = attackerPos.x + (level.random.nextDouble() - 0.5) * 0.6;
            double ry = attackerPos.y + level.random.nextDouble() * attacker.getBbHeight();
            double rz = attackerPos.z + (level.random.nextDouble() - 0.5) * 0.6;

            FxOps.particles(level, BLOOD_CRIMSON, new Vec3(rx, ry, rz), 2, 0.0, 0.0, 0.0, 0.05);
          });
    }

    // Sound: reflect trigger
    FxOps.playSound(
        level, player.blockPosition().getCenter(), SoundEvents.GENERIC_HURT, SoundSource.PLAYERS, 0.8f, 1.1f);

    FxOps.playSound(
        level,
        attacker.blockPosition().getCenter(),
        SoundEvents.PLAYER_ATTACK_CRIT,
        SoundSource.PLAYERS,
        0.6f,
        0.8f);
  }

  /** Plays effect when reflect window ends. */
  public static void playReflectWindowEnd(ServerLevel level, Player player) {
    Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);

    // Blood spikes retract inward
    FxOps.particles(level, BLOOD_DARK, center, 16, 0.6, 0.5, 0.6, -0.08);

    // Ground pattern fades
    FxOps.particles(
        level,
        ParticleTypes.SMOKE,
        new Vec3(center.x, player.getY() + 0.1, center.z),
        8,
        0.5,
        0.0,
        0.5,
        0.02);

    // Sound: deactivate
    FxOps.playSound(
        level,
        player.blockPosition().getCenter(),
        SoundEvents.FIRE_EXTINGUISH,
        SoundSource.PLAYERS,
        0.5f,
        0.9f);
  }

  // ============================================================
  // Passive Skill Effects (Subtle)
  // ============================================================

  /** Blood armor stack gained (passive: 血衣). */
  public static void playArmorStackGain(ServerLevel level, Player player, int currentStacks) {
    Vec3 chest = player.position().add(0, player.getBbHeight() * 0.6, 0);

    // Use blood dark particles instead of entity effect
    FxOps.particles(
        level,
        BLOOD_DARK,
        chest,
        3 + currentStacks,
        0.2,
        0.2,
        0.2,
        0.0);

    // Every 5 stacks - enhanced visual
    if (currentStacks % 5 == 0) {
      FxOps.particles(level, BLOOD_BRIGHT, chest, 12, 0.4, 0.3, 0.4, 0.05);

      Vec3 playerPos = player.position();
      FxOps.playSound(
          level,
          playerPos,
          SoundEvents.ARMOR_EQUIP_IRON.value(),
          SoundSource.PLAYERS,
          0.3f,
          1.0f + (currentStacks / 20.0f));
    }
  }

  /** Penetration bleed applied (passive: 渗透). */
  public static void playPenetrateEffect(ServerLevel level, LivingEntity target) {
    Vec3 pos = target.position().add(0, target.getBbHeight() * 0.6, 0);

    FxOps.particles(level, ParticleTypes.DAMAGE_INDICATOR, pos, 2, 0.2, 0.2, 0.2, 0.1);

    // Subtle blood seepage
    for (int i = 0; i < 3; i++) {
      double rx = pos.x + (level.random.nextDouble() - 0.5) * 0.4;
      double ry = pos.y + (level.random.nextDouble() - 0.5) * 0.4;
      double rz = pos.z + (level.random.nextDouble() - 0.5) * 0.4;

      FxOps.particles(level, BLOOD_CRIMSON, new Vec3(rx, ry, rz), 1, 0.0, 0.0, 0.0, 0.0);
    }

    FxOps.playSound(
        level, target.blockPosition().getCenter(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.2f, 1.3f);
  }

  /** Enrage activation (passive: 越染越坚). */
  public static void playEnrageActivation(ServerLevel level, Player player) {
    Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);

    // Blood rage burst
    FxOps.particles(level, BLOOD_GLOW, center, 24, 0.0, 0.0, 0.0, 0.25);

    FxOps.particles(level, ParticleTypes.LAVA, center, 8, 0.5, 0.6, 0.5, 0.1);

    FxOps.particles(
        level,
        ParticleTypes.LANDING_LAVA,
        new Vec3(center.x, player.getY() + 0.05, center.z),
        20,
        1.2,
        0.0,
        1.2,
        0.0);

    FxOps.playSound(
        level,
        player.blockPosition().getCenter(),
        SoundEvents.ENDER_DRAGON_GROWL,
        SoundSource.PLAYERS,
        0.4f,
        1.5f);
  }

  /** Blood reward from kill (passive: 血偿). */
  public static void playBloodReward(ServerLevel level, Player player, Vec3 corpsePos) {
    Vec3 playerPos = player.position().add(0, player.getBbHeight() * 0.5, 0);

    // Blood released from corpse
    FxOps.particles(level, BLOOD_BRIGHT, corpsePos, 12, 0.4, 0.5, 0.4, 0.1);

    // Blood flows to player
    Vec3 direction = playerPos.subtract(corpsePos).normalize();
    double distance = corpsePos.distanceTo(playerPos);

    for (int i = 0; i < 8; i++) {
      int finalI = i;
      spawnParticleDelayed(
          level,
          i * 3,
          () -> {
            double progress = finalI / 8.0;
            Vec3 point = corpsePos.add(direction.scale(distance * progress));

            FxOps.particles(level, BLOOD_GLOW, point, 1, 0.0, 0.0, 0.0, 0.15);
          });
    }

    // Player receives energy
    spawnParticleDelayed(
        level,
        24,
        () -> {
          FxOps.particles(level, ParticleTypes.GLOW, playerPos, 6, 0.3, 0.3, 0.3, 0.0);

          FxOps.playSound(
              level,
              player.blockPosition().getCenter(),
              SoundEvents.EXPERIENCE_ORB_PICKUP,
              SoundSource.PLAYERS,
              0.5f,
              0.8f);
        });
  }

  /** Hardened blood shield (passive: 凝血止创). */
  public static void playHardenedBloodShield(ServerLevel level, Player player, float absorbAmount) {
    Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);

    // Blood congealing inward
    FxOps.particles(level, BLOOD_DARK, center, 20, 0.8, 0.6, 0.8, -0.1);

    // Shield forming (delayed)
    spawnParticleDelayed(
        level,
        10,
        () -> {
          FxOps.particles(level, BLOOD_BRIGHT, center, 16, 0.0, 0.0, 0.0, 0.15);

          FxOps.playSound(
              level,
              player.blockPosition().getCenter(),
              SoundEvents.SHIELD_BLOCK,
              SoundSource.PLAYERS,
              0.7f,
              1.1f);
        });

    // Shield membrane (3 seconds)
    for (int frame = 0; frame < 60; frame++) {
      int finalFrame = frame;
      spawnParticleDelayed(
          level,
          frame,
          () -> {
            double radius = 0.9;
            int points = 10;

            for (int i = 0; i < points; i++) {
              double angle = (i / (double) points) * Math.PI * 2 + (finalFrame * 0.08);
              double x = center.x + Math.cos(angle) * radius;
              double z = center.z + Math.sin(angle) * radius;

              FxOps.particles(level, BLOOD_CRIMSON, new Vec3(x, center.y, z), 1, 0.0, 0.0, 0.0, 0.0);
            }
          });
    }
  }

  /** Life cost paid (passive: 代价). */
  public static void playLifeCost(ServerLevel level, Player player) {
    Vec3 chest = player.position().add(0, player.getBbHeight() * 0.6, 0);

    FxOps.particles(level, BLOOD_DARK, chest, 6, 0.3, 0.3, 0.3, 0.08);

    FxOps.particles(
        level, ParticleTypes.DAMAGE_INDICATOR, chest, 2, 0.2, 0.2, 0.2, 0.05);

    FxOps.playSound(
        level, player.blockPosition().getCenter(),        SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.2f, 1.4f);
  }

  // ============================================================
  // Utility Methods
  // ============================================================

  /** Creates a pulse ring at given radius. */
  private static void playRangePulse(
      ServerLevel level, Vec3 center, double radius, DustParticleOptions particle, int points) {
    for (int i = 0; i < points; i++) {
      double angle = (i / (double) points) * Math.PI * 2;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;

      FxOps.particles(level, particle, new Vec3(x, center.y, z), 1, 0.0, 0.1, 0.0, 0.0);
    }
  }

  /** Gets perpendicular offset for beam thickness. */
  private static Vec3 getPerpendicularOffset(Vec3 direction, ServerLevel level, double magnitude) {
    // Create perpendicular vector
    Vec3 perp;
    if (Math.abs(direction.y) < 0.9) {
      perp = new Vec3(0, 1, 0).cross(direction).normalize();
    } else {
      perp = new Vec3(1, 0, 0).cross(direction).normalize();
    }

    double angle = level.random.nextDouble() * Math.PI * 2;
    return perp.scale(Math.cos(angle) * magnitude)
        .add(direction.cross(perp).normalize().scale(Math.sin(angle) * magnitude));
  }

  /**
   * Spawns particle with delay. Note: This is a simplified version. In production, use a proper
   * scheduler.
   */
  private static void spawnParticleDelayed(ServerLevel level, int delayTicks, Runnable action) {
    // TODO: Implement proper delayed spawning via server scheduler
    // For now, effects will be instantaneous
    if (delayTicks == 0) {
      action.run();
    }
    // In actual implementation, schedule this via level's tick scheduler or behavior tick counter
  }
}
