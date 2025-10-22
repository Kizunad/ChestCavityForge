package net.tigereye.chestcavity.guscript.fx.gecko;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/** Client-side registry holding Gecko FX definitions. */
public final class GeckoFxRegistry {

  private static final Map<ResourceLocation, GeckoFxDefinition> DEFINITIONS = new HashMap<>();

  private GeckoFxRegistry() {}

  public static synchronized void updateDefinitions(
      Map<ResourceLocation, GeckoFxDefinition> definitions) {
    DEFINITIONS.clear();
    DEFINITIONS.putAll(definitions);
    ChestCavity.LOGGER.info("[GuScript] Loaded {} Gecko FX definitions", DEFINITIONS.size());
  }

  public static synchronized Optional<GeckoFxDefinition> definition(ResourceLocation id) {
    return Optional.ofNullable(DEFINITIONS.get(id));
  }

  public static synchronized Map<ResourceLocation, GeckoFxDefinition> snapshot() {
    return Collections.unmodifiableMap(new HashMap<>(DEFINITIONS));
  }
}
