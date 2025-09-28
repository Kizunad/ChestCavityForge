package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.guscript.fx.FxEventParameters;

/**
 * Bridge exposed to action execution for resource manipulation and effect dispatch.
 * Concrete implementations should wire into ChestCavity/Guzhenren systems.
 */

public interface GuScriptExecutionBridge {

    void consumeZhenyuan(int amount);

    void consumeHealth(int amount);

    void emitProjectile(String projectileId, double damage);

    void playFx(ResourceLocation fxId, FxEventParameters parameters);
}
