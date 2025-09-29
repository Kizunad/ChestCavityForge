package net.tigereye.chestcavity.guscript.actions;

import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/**
 * Exports a flat time-scale adjustment into the current execution session.
 */
public record ExportTimeScaleFlatAction(double amount) implements Action {

    public static final String ID = "export.time_scale_flat";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "导出时间倍率加成 +" + amount;
    }

    @Override
    public void execute(GuScriptContext context) {
        if (context == null) {
            return;
        }
        context.exportTimeScaleFlat(amount);
    }
}

