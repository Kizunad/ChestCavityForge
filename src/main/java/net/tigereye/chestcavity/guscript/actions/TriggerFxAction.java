package net.tigereye.chestcavity.guscript.actions;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.fx.FxEventParameters;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/** Action that dispatches a configured FX bundle through the execution bridge. */
public final class TriggerFxAction implements Action {

  public static final String ID = "emit.fx";

  private final ResourceLocation fxId;
  private final FxEventParameters parameters;

  public TriggerFxAction(ResourceLocation fxId, FxEventParameters parameters) {
    this.fxId = Objects.requireNonNull(fxId, "FX id");
    this.parameters = parameters == null ? FxEventParameters.DEFAULT : parameters;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "Trigger FX " + fxId;
  }

  @Override
  public void execute(GuScriptContext context) {
    if (context == null || context.bridge() == null) {
      return;
    }
    context.bridge().playFx(fxId, parameters);
  }

  public static TriggerFxAction from(
      ResourceLocation fxId, Vec3 originOffset, Vec3 targetOffset, float intensity) {
    FxEventParameters parameters = new FxEventParameters(originOffset, targetOffset, intensity);
    return new TriggerFxAction(fxId, parameters);
  }
}
