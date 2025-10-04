package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunShouHuaConstants;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.soulbeast.api.SoulBeastAPI;

/**
 * Wraps {@link SoulBeastAPI#toSoulBeast} for GuScript usage.
 */
public final class SoulBeastTransformAction implements Action {

    public static final String ID = "action.soulbeast.transform";

    private final ResourceLocation source;

    public SoulBeastTransformAction() {
        this(HunShouHuaConstants.TRANSFORM_SOURCE);
    }

    public SoulBeastTransformAction(ResourceLocation source) {
        this.source = source == null ? HunShouHuaConstants.TRANSFORM_SOURCE : source;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Trigger Soul Beast transformation";
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
        SoulBeastAPI.toSoulBeast(performer, true, source);
    }
}
