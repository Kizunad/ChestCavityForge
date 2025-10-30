package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.behavior;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.util.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.common.YinYangComboUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.calculator.TransferCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.fx.TransferFx;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.messages.TransferMessages;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.calculator.TransferLogic;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.calculator.TransferParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.tuning.TransferTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;

/**
 * Behavior logic for the Transfer combo skill.
 */
public final class TransferBehavior {

    public static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_transfer");
    private static final String COOLDOWN_KEY = "TransferReadyAt";

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

        if (!ComboSkillUtil.tryPayCost(player, TransferTuning.COST, p -> TransferMessages.sendFailure(p, TransferMessages.INSUFFICIENT_RESOURCES))) {
            return;
        }

        double changeDaoHen = SkillEffectBus.consumeMetadata(player, SKILL_ID, "yin_yang:daohen_bianhuadao", 0.0D);
        double changeFlowExp = SkillEffectBus.consumeMetadata(player, SKILL_ID, "yin_yang:liupai_bianhuadao", 0.0D);

        TransferParameters params = TransferLogic.computeParameters(changeDaoHen, changeFlowExp);

        Optional<ResourceHandle> handleOpt = YinYangDualityOps.openHandle(player);
        if (handleOpt.isEmpty()) {
            TransferMessages.sendFailure(player, TransferMessages.CANNOT_READ_ATTACHMENT);
            return;
        }

        double moved = TransferCalculator.performTransfer(attachment, handleOpt.get(), params.transferRatio());
        if (moved <= 0.0D) {
            TransferMessages.sendFailure(player, TransferMessages.INSUFFICIENT_TRANSFER_AMOUNT);
            // Refund cost if nothing was transferred
            ComboSkillUtil.tryPayCost(player, new ComboSkillUtil.ResourceCost(-TransferTuning.COST.zhenyuan(), 0, 0, -TransferTuning.COST.niantou(), 0, 0), p -> {});
            return;
        }

        long ready = now + params.cooldownTicks();
        cooldown.entry(COOLDOWN_KEY).setReadyAt(ready);

        ComboSkillRegistry.scheduleReadyToast(player, SKILL_ID, ready, now);
        TransferMessages.sendAction(player, TransferMessages.TRANSFER_COMPLETE);
        TransferFx.play(player);
    }
}
