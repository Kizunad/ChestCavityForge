package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunShouHuaConstants;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.fx.FxEventParameters;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/** Convenience action to emit the standard Hun Shou Hua failure FX bundle. */
public final class EmitFailFxAction implements Action {

  public static final String ID = "action.emit_fail_fx";

  private final ResourceLocation fxId;
  private final float intensity;

  public EmitFailFxAction() {
    this(HunShouHuaConstants.FX_FAIL, 1.0F);
  }

  public EmitFailFxAction(ResourceLocation fxId, float intensity) {
    this.fxId = fxId == null ? HunShouHuaConstants.FX_FAIL : fxId;
    this.intensity = intensity <= 0.0F ? 1.0F : intensity;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "Emit Hun Shou Hua failure FX";
  }

  @Override
  public void execute(GuScriptContext context) {
    if (context == null || context.bridge() == null) {
      return;
    }
    context
        .bridge()
        .playFx(
            fxId,
            new FxEventParameters(
                net.minecraft.world.phys.Vec3.ZERO, net.minecraft.world.phys.Vec3.ZERO, intensity));
  }
}
