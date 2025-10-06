package net.tigereye.chestcavity.soul.runtime;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.ai.SoulAIOrders;
import net.tigereye.chestcavity.soul.combat.FleeContext;
import net.tigereye.chestcavity.soul.combat.SoulAttackRegistry;
import net.tigereye.chestcavity.soul.combat.SoulFleeRegistry;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;
import net.tigereye.chestcavity.soul.registry.SoulHurtResult;
import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandler;
import net.tigereye.chestcavity.soul.util.SoulLook;

/**
 * Reactive hurt hook: if attacker is present, decide flee vs retaliate.
 * - myHP < 2x enemyHP => try flee away from attacker (anchor at owner).
 * - else => set goal to target and try immediate attack.
 * This returns PASS to preserve vanilla damage application by later handlers.
 */
public final class HurtRetaliateOrFleeHandler implements SoulRuntimeHandler {

    @Override
    public SoulHurtResult onHurt(SoulPlayer player, DamageSource source, float amount) {
        var attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return SoulHurtResult.pass();
        }
        if (!player.isAlive() || !attacker.isAlive()) {
            return SoulHurtResult.pass();
        }
        // Friendly-fire exclusions: skip retaliation/flee when the attacker is the owner body
        // or a soul that belongs to the same owner. This avoids fighting the owner or friendly souls.
        var myOwnerId = player.getOwnerId().orElse(null);
        if (myOwnerId != null) {
            if (attacker instanceof net.minecraft.server.level.ServerPlayer sp && sp.getUUID().equals(myOwnerId)) {
                return SoulHurtResult.pass();
            }
            if (attacker instanceof net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer spSoul
                    && spSoul.getOwnerId().map(myOwnerId::equals).orElse(false)) {
                return SoulHurtResult.pass();
            }
        }
        float myHp = player.getHealth();
        float enemyHp = attacker.getHealth();
        float ratio = net.tigereye.chestcavity.soul.util.SoulCombatTuning.guardHpRatio();
        var ownerId = player.getOwnerId().orElse(null);
        var owner = ownerId == null ? null : player.serverLevel().getServer().getPlayerList().getPlayer(ownerId);
        Vec3 anchor = owner != null ? owner.position() : player.position();

        // Face toward immediately for feedback
        SoulLook.faceTowards(player, attacker.position());

        if (myHp < ratio * enemyHp) {
            // flee
            SoulFleeRegistry.tryFlee(FleeContext.of(player, attacker, anchor));
            return SoulHurtResult.pass();
        }

        // retaliate: if in range attack now, else chase
        boolean attacked = SoulAttackRegistry.attackIfInRange(player, attacker);
        if (!attacked) {
            SoulNavigationMirror.setGoal(player, attacker.position(), 1.25, 2.0);
        } else {
            SoulNavigationMirror.clearGoal(player);
        }
        return SoulHurtResult.pass();
    }
}
