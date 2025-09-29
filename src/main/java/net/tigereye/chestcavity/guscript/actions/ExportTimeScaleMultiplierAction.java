package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/**
 * Exports a multiplicative time-scale modifier into the current execution session.
 */
public record ExportTimeScaleMultiplierAction(double amount) implements Action {

    public static final String ID = "export.time_scale_mult";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "导出时间倍率 ×" + amount;
    }

    @Override
    public void execute(GuScriptContext context) {
        if (context == null) {
            return;
        }
        context.exportTimeScaleMultiplier(amount);
    }
}

