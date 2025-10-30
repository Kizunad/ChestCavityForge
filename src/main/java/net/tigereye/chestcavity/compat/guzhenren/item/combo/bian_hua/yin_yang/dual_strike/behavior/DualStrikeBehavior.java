package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.DualStrikeWindow;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.util.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.common.YinYangComboUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.calculator.DualStrikeCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.calculator.DualStrikeLogic;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.calculator.DualStrikeParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.fx.DualStrikeFx;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.messages.DualStrikeMessages;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.tuning.DualStrikeTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;

/**
 * Behavior logic for the Dual Strike combo skill.
 */
public final class DualStrikeBehavior {

    public static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_dual_strike");
    private static final String COOLDOWN_KEY = "DualStrikeReadyAt";

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

        var organ = YinYangComboUtil.findYinYangGu(cc).orElse(null);
        if (organ == null) {
            return;
        }
        OrganState organState = YinYangComboUtil.resolveState(organ);

        YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
        long now = player.level().getGameTime();

        MultiCooldown cooldown = YinYangComboUtil.getCooldown(cc, organ, organState);
        if (!cooldown.entry(COOLDOWN_KEY).isReady(now)) {
            return;
        }

        if (!ComboSkillUtil.tryPayCost(player, DualStrikeTuning.COST, p -> DualStrikeMessages.sendFailure(p, DualStrikeMessages.INSUFFICIENT_RESOURCES))) {
            return;
        }

        double changeDaoHen = SkillEffectBus.consumeMetadata(player, SKILL_ID, "yin_yang:daohen_bianhuadao", 0.0D);
        double changeFlowExp = SkillEffectBus.consumeMetadata(player, SKILL_ID, "yin_yang:liupai_bianhuadao", 0.0D);

        DualStrikeParameters params = DualStrikeLogic.computeParameters(changeDaoHen, changeFlowExp);

        DualStrikeWindow window = attachment.dualStrike();
        window.clear();
        window.start(null, now + DualStrikeTuning.STRIKE_WINDOW_TICKS);
        window.setBaseAttacks(
            attachment.pool(Mode.YIN).attackSnapshot(), attachment.pool(Mode.YANG).attackSnapshot());
        window.setDamageFactor(params.damageFactor());
        
        long ready = now + params.cooldownTicks();
        cooldown.entry(COOLDOWN_KEY).setReadyAt(ready);

        ComboSkillRegistry.scheduleReadyToast(player, SKILL_ID, ready, now);
        DualStrikeMessages.sendAction(player, DualStrikeMessages.WINDOW_OPEN);
        DualStrikeFx.playActivate(player);
    }

    /**
     * Handles the logic when a player hits a target while the Dual Strike window is active.
     * This is called from the OnHit listener of the host organ.
     * @param player The attacking player.
     * @param target The entity that was hit.
     * @param attachment The player's YinYangDualityAttachment.
     */
    public static void handleHit(ServerPlayer player, LivingEntity target, YinYangDualityAttachment attachment) {
        long now = player.level().getGameTime();
        DualStrikeWindow window = attachment.dualStrike();
        if (!window.isActive(now)) {
            return;
        }
        if (!window.matchOrSetTarget(target.getUUID())) {
            return;
        }
        Mode mode = attachment.currentMode();
        if (mode == Mode.YIN) {
            if (window.yinHit()) {
                return;
            }
            window.markYinHit();
        } else {
            if (window.yangHit()) {
                return;
            }
            window.markYangHit();
        }
        
        boolean bothHit = window.yinHit() && window.yangHit();
        DualStrikeFx.playHit(player, target, bothHit);

        if (bothHit) {
            double damage = DualStrikeCalculator.calculateBonusDamage(window.baseAttackYin(), window.baseAttackYang(), window.damageFactor());
            if (damage > 0.0D) {
                target.hurt(player.damageSources().magic(), (float) damage);
            }
            if (mode == Mode.YANG) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
            }
            window.clear();
        }
    }
}
