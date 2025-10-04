package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunShouHuaConstants;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * Marks the Hun Shou Hua ability as permanently consumed on the performer.
 */
public final class HunShouHuaMarkUsedAction implements Action {

    public static final String ID = "action.hun_shou_hua.mark_used";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Mark Hun Shou Hua as used";
    }

    @Override
    public void execute(GuScriptContext context) {
        if (context == null) {
            return;
        }
        Player performer = context.performer();
        if (performer == null) {
            return;
        }
        CCAttachments.getGuScript(performer).markAbilityFlag(HunShouHuaConstants.ABILITY_FLAG);
    }
}
