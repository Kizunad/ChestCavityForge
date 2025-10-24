package net.tigereye.chestcavity.compat.guzhenren.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.linkage.effect.GuzhenrenLinkageEffectRegistry;

/**
 * Centralised dispatcher that translates {@link OrganIntegrationSpec} data into runtime
 * registrations with {@link GuzhenrenLinkageEffectRegistry}.
 */
public final class GuzhenrenOrganIntegrationRegistry {

  private GuzhenrenOrganIntegrationRegistry() {}

  public static List<ResourceLocation> registerAll(Collection<OrganIntegrationSpec> specs) {
    Objects.requireNonNull(specs, "specs");
    List<ResourceLocation> registrationOrder = new ArrayList<>();
    int index = 0;
    for (OrganIntegrationSpec spec : specs) {
      if (spec == null) {
        continue;
      }
      ResourceLocation organId = Objects.requireNonNull(spec.organId(), "organId");
      final int ordinal = ++index;
      GuzhenrenLinkageEffectRegistry.registerSingle(
          organId,
          context -> {
            if (ChestCavity.LOGGER.isTraceEnabled()) {
              ChestCavity.LOGGER.trace(
                  "[compat/guzhenren] Applying integration #{} for {}", ordinal, organId);
            }
            spec.apply(context);
          });
      registrationOrder.add(organId);
      if (ChestCavity.LOGGER.isDebugEnabled()) {
        ChestCavity.LOGGER.debug(
            "[compat/guzhenren] Registered integration #{} for {}", ordinal, organId);
      }
    }
    return List.copyOf(registrationOrder);
  }
}
