package net.tigereye.chestcavity.guscript.actions;

import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunShouHuaConstants;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * Branches execution based on whether Hun Shou Hua has already been used.
 */
public final class HunShouHuaPredicateAction implements Action {

    public static final String ID = "predicate.hun_shou_hua.is_used";

    private final List<Action> ifUsed;
    private final List<Action> ifUnused;

    public HunShouHuaPredicateAction(List<Action> ifUsed, List<Action> ifUnused) {
        this.ifUsed = ifUsed == null ? List.of() : List.copyOf(ifUsed);
        this.ifUnused = ifUnused == null ? List.of() : List.copyOf(ifUnused);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Branch on Hun Shou Hua usage";
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
        boolean used = CCAttachments.getExistingGuScript(performer)
                .map(attachment -> attachment.hasAbilityFlag(HunShouHuaConstants.ABILITY_FLAG))
                .orElse(false);
        List<Action> branch = used ? ifUsed : ifUnused;
        if (branch.isEmpty()) {
            return;
        }
        for (Action action : branch) {
            try {
                action.execute(context);
            } catch (Exception ignored) {
                // Let subsequent actions continue even if one branch action throws.
            }
        }
    }
}
