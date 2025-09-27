package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.data.ListenerType;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * Hooks into entity events to dispatch listener-based GuScript triggers.
 */
public final class GuScriptListenerHooks {

    private static final int FIRE_COOLDOWN_TICKS = 20;
    private static final int GROUND_COOLDOWN_TICKS = 20;

    private GuScriptListenerHooks() {
    }

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        GuScriptAttachment attachment = CCAttachments.getExistingGuScript(attacker).orElse(null);
        if (attachment == null) {
            return;
        }
        if (attachment.getBindingTarget() != BindingTarget.LISTENER || attachment.getListenerType() != ListenerType.ON_HIT) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.world.entity.LivingEntity target)) {
            return;
        }
        GuScriptExecutor.trigger(attacker, target, attachment);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        GuScriptAttachment attachment = CCAttachments.getExistingGuScript(player).orElse(null);
        if (attachment == null || attachment.getBindingTarget() != BindingTarget.LISTENER) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (attachment.getListenerType() == ListenerType.ON_FIRE) {
            if (player.isOnFire() && gameTime - attachment.getLastListenerTrigger(ListenerType.ON_FIRE) >= FIRE_COOLDOWN_TICKS) {
                attachment.setLastListenerTrigger(ListenerType.ON_FIRE, gameTime);
                GuScriptExecutor.trigger(player, player, attachment);
            }
        } else if (attachment.getListenerType() == ListenerType.ON_GROUND) {
            if (player.onGround() && gameTime - attachment.getLastListenerTrigger(ListenerType.ON_GROUND) >= GROUND_COOLDOWN_TICKS) {
                attachment.setLastListenerTrigger(ListenerType.ON_GROUND, gameTime);
                GuScriptExecutor.trigger(player, player, attachment);
            }
        }
    }
}
