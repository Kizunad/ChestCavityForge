package net.tigereye.chestcavity.soul.combat.handlers;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.tigereye.chestcavity.soul.combat.AttackContext;
import net.tigereye.chestcavity.soul.combat.SoulAttackHandler;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.util.SoulLook;

/** Default close-range melee swing based on attack damage attribute. */
public final class MeleeAttackHandler implements SoulAttackHandler {
  private final double range;
  private final int cooldownTicks;
  private final java.util.Map<java.util.UUID, Long> lastAttackTick =
      new java.util.concurrent.ConcurrentHashMap<>();

  public MeleeAttackHandler() {
    this(3.0, 10); // ~melee reach, ~0.5s cooldown at 20tps
  }

  public MeleeAttackHandler(double range, int cooldownTicks) {
    this.range = range;
    this.cooldownTicks = Math.max(1, cooldownTicks);
  }

  @Override
  public double getRange(SoulPlayer self, LivingEntity target) {
    // include half of target width to make melee feel a bit more forgiving
    return range + (target.getBbWidth() * 0.5);
  }

  @Override
  public boolean tryAttack(AttackContext ctx) {
    var self = ctx.self();
    var target = ctx.target();
    if (self.isUsingItem()) return false; // don't interrupt eating/drinking
    if (!target.isAlive()) return false;

    // face the target before attack
    SoulLook.faceTowards(self, target.position());

    long now = self.level().getGameTime();
    Long last = lastAttackTick.get(self.getSoulId());
    if (last != null) {
      long diff = now - last;
      if (diff < 0) {
        // Level time moved backwards (dimension swap, load, etc.) — reset cooldown state safely.
        lastAttackTick.remove(self.getSoulId());
      } else if (diff < cooldownTicks) {
        long remain = cooldownTicks - diff;
        // clamp to [0, cooldown]
        if (remain < 0) remain = 0;
        if (remain > cooldownTicks) remain = cooldownTicks;
        net.tigereye.chestcavity.soul.util.SoulLog.info(
            "[soul][attack] melee-cd soul={} remain={}t", self.getSoulId(), remain);
        return false; // on cooldown
      }
    }

    double base = 2.0;
    var inst = self.getAttribute(Attributes.ATTACK_DAMAGE);
    if (inst != null) base = inst.getValue();

    float damage = (float) Math.max(0.5, base);
    self.swing(InteractionHand.MAIN_HAND, true);
    // Prefer vanilla attack pipeline for proper events/enchants/KB
    self.attack(target);
    boolean hit = true; // assume consumed; damage resolution is internal
    if (hit) {
      lastAttackTick.put(self.getSoulId(), now);
      // audio feedback server-side
      self.level()
          .playSound(
              null,
              self.blockPosition(),
              SoundEvents.PLAYER_ATTACK_STRONG,
              SoundSource.PLAYERS,
              0.8f,
              1.0f);
      net.tigereye.chestcavity.soul.util.SoulLog.info(
          "[soul][attack] melee soul={} target={} estDmg={}",
          self.getSoulId(),
          target.getUUID(),
          damage);
    }
    return hit;
  }
}
