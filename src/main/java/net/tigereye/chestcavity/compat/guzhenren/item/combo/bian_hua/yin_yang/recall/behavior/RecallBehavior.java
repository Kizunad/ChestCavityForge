package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.Anchor;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.common.YinYangComboUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.calculator.RecallLogic;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.calculator.RecallParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.fx.RecallFx;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.messages.RecallMessages;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.tuning.RecallTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;

/**
 * Behavior logic for the Recall combo skill.
 */
public final class RecallBehavior {

    public static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_recall");
    private static final String COOLDOWN_KEY = "RecallReadyAt";

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

        if (!ComboSkillUtil.tryPayCost(player, RecallTuning.COST, p -> RecallMessages.sendFailure(p, RecallMessages.INSUFFICIENT_RESOURCES))) {
            return;
        }

        double changeDaoHen = SkillEffectBus.consumeMetadata(player, SKILL_ID, "yin_yang:daohen_bianhuadao", 0.0D);
        double changeFlowExp = SkillEffectBus.consumeMetadata(player, SKILL_ID, "yin_yang:liupai_bianhuadao", 0.0D);

        RecallParameters params = RecallLogic.computeParameters(changeDaoHen, changeFlowExp);

        Mode other = attachment.currentMode().opposite();
        Anchor anchor = attachment.anchor(other);
        if (anchor == null || !anchor.isValid()) {
            RecallMessages.sendFailure(player, RecallMessages.MISSING_ANCHOR);
            return;
        }

        double currentHealthRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        if (!YinYangDualityOps.teleportToAnchor(player, anchor)) {
            RecallMessages.sendFailure(player, RecallMessages.UNREACHABLE_ANCHOR);
            return;
        }

        clearNearbyAggro(player);
        if (currentHealthRatio < 0.2D) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
        }

        long ready = now + params.cooldownTicks();
        cooldown.entry(COOLDOWN_KEY).setReadyAt(ready);

        ComboSkillRegistry.scheduleReadyToast(player, SKILL_ID, ready, now);
        RecallFx.play(player);
    }

    private static void clearNearbyAggro(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(8.0D);
        player
            .level()
            .getEntitiesOfClass(
                Mob.class, box, mob -> mob.getTarget() == player && mob.isAlive() && !mob.isRemoved())
            .forEach(mob -> mob.setTarget(null));
    }
}
