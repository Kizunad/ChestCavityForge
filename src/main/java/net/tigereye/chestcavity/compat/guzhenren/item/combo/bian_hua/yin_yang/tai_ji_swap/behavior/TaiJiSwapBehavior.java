package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.tai_ji_swap.behavior;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.Anchor;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.util.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.common.YinYangComboUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.tai_ji_swap.fx.TaiJiSwapFx;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.tai_ji_swap.messages.TaiJiSwapMessages;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.tai_ji_swap.tuning.TaiJiSwapTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;

/**
 * Behavior logic for the Taiji Swap combo skill.
 */
public final class TaiJiSwapBehavior {

    public static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_tai_ji_swap");
    private static final String COOLDOWN_KEY = "TaiJiSwapReadyAt";

    /**
     * Registers the activation listener for this combo skill.
     */
    public static void initialize() {
        OrganActivationListeners.register(SKILL_ID, (entity, cc) -> {
            if (entity instanceof ServerPlayer player) {
                activate(player, cc);
            }
        });
    }

    private static void activate(ServerPlayer player, ChestCavityInstance cc) {
        if (player.level().isClientSide() || cc == null) {
            return;
        }

        // Find the host organ and its state
        var organ = YinYangComboUtil.findYinYangGu(cc).orElse(null);
        if (organ == null) {
            return; // Should be caught by ComboSkillRegistry check, but as a safeguard
        }
        OrganState organState = YinYangComboUtil.resolveState(organ);

        YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
        long now = player.level().getGameTime();

        MultiCooldown cooldown = YinYangComboUtil.getCooldown(cc, organ, organState);
        if (!cooldown.entry(COOLDOWN_KEY).isReady(now)) {
            return;
        }

        if (!ComboSkillUtil.tryPayCost(player, TaiJiSwapTuning.COST, p -> TaiJiSwapMessages.sendFailure(p, TaiJiSwapMessages.INSUFFICIENT_RESOURCES))) {
            return;
        }

        Mode currentMode = attachment.currentMode();
        Mode otherMode = currentMode.opposite();
        Anchor destination = attachment.anchor(otherMode);
        if (destination == null || !destination.isValid()) {
            TaiJiSwapMessages.sendFailure(player, TaiJiSwapMessages.MISSING_ANCHOR);
            return;
        }

        Optional<Anchor> originOpt = YinYangDualityOps.captureAnchor(player);
        if (originOpt.isEmpty()) {
            return;
        }
        Vec3 originPos = player.position();

        if (!YinYangDualityOps.teleportToAnchor(player, destination)) {
            TaiJiSwapMessages.sendFailure(player, TaiJiSwapMessages.UNREACHABLE_ANCHOR);
            return;
        }

        // Swap anchors
        attachment.setAnchor(otherMode, originOpt.get());
        YinYangDualityOps.captureAnchor(player).ifPresent(anchor -> attachment.setAnchor(currentMode, anchor));

        boolean withinWindow = now <= attachment.swapWindowEndTick();
        if (!withinWindow) {
            attachment.setSwapWindowEndTick(now + TaiJiSwapTuning.CONSECUTIVE_SWAP_WINDOW_TICKS);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 4, false, false));
        }
        attachment.setFallGuardEndTick(now + TaiJiSwapTuning.FALL_GUARD_TICKS);

        long cooldownDuration =
            withinWindow ? Math.max(10L, TaiJiSwapTuning.COOLDOWN_TICKS / 2) : TaiJiSwapTuning.COOLDOWN_TICKS;
        long readyTick = now + cooldownDuration;
        cooldown.entry(COOLDOWN_KEY).setReadyAt(readyTick);

        ComboSkillRegistry.scheduleReadyToast(player, SKILL_ID, readyTick, now);
        TaiJiSwapFx.play(player, originPos, player.position(), withinWindow);
    }
}
