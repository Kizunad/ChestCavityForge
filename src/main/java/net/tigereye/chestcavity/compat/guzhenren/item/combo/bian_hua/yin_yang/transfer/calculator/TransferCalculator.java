package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.calculator;

import net.tigereye.chestcavity.compat.common.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.common.state.YinYangDualityAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.tuning.TransferTuning;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 阴阳互渡 纯逻辑计算
 */
public final class TransferCalculator {
    private TransferCalculator() {}

    public static double performTransfer(YinYangDualityAttachment attachment, ResourceHandle handle, double transferRatio) {
        Mode current = attachment.currentMode();
        Mode other = current.opposite();
        attachment.pool(other).ensureInitializedFrom(attachment.pool(current));
        double moved = 0.0D;

        double currentZhenyuan = handle.getZhenyuan().orElse(0.0D);
        double sendZhenyuan = currentZhenyuan * transferRatio;
        if (sendZhenyuan > 0.0D) {
            double accepted = attachment.pool(other).receiveZhenyuan(sendZhenyuan);
            if (accepted > 0.0D) {
                handle.adjustZhenyuan(-accepted, true);
                moved += accepted;
            }
        }

        double currentJingli = handle.getJingli().orElse(0.0D);
        double sendJingli = currentJingli * transferRatio;
        if (sendJingli > 0.0D) {
            double accepted = attachment.pool(other).receiveJingli(sendJingli);
            if (accepted > 0.0D) {
                handle.adjustJingli(-accepted, true);
                moved += accepted;
            }
        }

        double currentHunpo = handle.getHunpo().orElse(0.0D);
        double sendHunpo = currentHunpo * transferRatio;
        if (sendHunpo > 0.0D) {
            double accepted = attachment.pool(other).receiveSoul(sendHunpo);
            if (accepted > 0.0D) {
                handle.adjustHunpo(-accepted, true);
                moved += accepted;
            }
        }

        double currentNian = handle.getNiantou().orElse(0.0D);
        double sendNian = currentNian * transferRatio;
        if (sendNian > 0.0D) {
            double accepted = attachment.pool(other).receiveNiantou(sendNian);
            if (accepted > 0.0D) {
                handle.adjustNiantou(-accepted, true);
                moved += accepted;
            }
        }
        return moved;
    }
}
