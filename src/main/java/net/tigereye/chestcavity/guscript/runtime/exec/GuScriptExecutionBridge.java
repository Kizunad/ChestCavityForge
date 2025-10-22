package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.guscript.fx.FxEventParameters;
import org.jetbrains.annotations.Nullable;

/**
 * Bridge exposed to action execution for resource manipulation and effect dispatch. Concrete
 * implementations should wire into ChestCavity/Guzhenren systems.
 */
public interface GuScriptExecutionBridge {

  void consumeZhenyuan(int amount);

  void consumeHealth(int amount);

  void emitProjectile(String projectileId, double damage, @Nullable CompoundTag parameters);

  void playFx(ResourceLocation fxId, FxEventParameters parameters);
}
