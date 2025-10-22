package net.tigereye.chestcavity.chestcavities.types.json;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.tigereye.chestcavity.ChestCavity;

public class GeneratedChestCavityAssignmentManager implements ResourceManagerReloadListener {
  private static final ResourceLocation RESOURCE_LOCATION = ChestCavity.id("entity_assignment");
  private final ChestCavityAssignmentSerializer SERIALIZER = new ChestCavityAssignmentSerializer();
  public static Map<ResourceLocation, ResourceLocation> GeneratedChestCavityAssignments =
      new HashMap<>();

  @Override
  public void onResourceManagerReload(ResourceManager manager) {
    GeneratedChestCavityAssignments.clear();
    ChestCavity.LOGGER.info("Loading chest cavity assignments.");
    manager
        .listResources(
            RESOURCE_LOCATION.getPath(), location -> location.getPath().endsWith(".json"))
        .forEach(
            (id, resource) -> {
              try (InputStream stream = resource.open()) {
                Reader reader = new InputStreamReader(stream);
                GeneratedChestCavityAssignments.putAll(
                    SERIALIZER.read(
                        id, new Gson().fromJson(reader, ChestCavityAssignmentJsonFormat.class)));
              } catch (Exception e) {
                ChestCavity.LOGGER.error(
                    "Error occurred while loading resource json " + id.toString(), e);
              }
            });
    ChestCavity.LOGGER.info(
        "Loaded " + GeneratedChestCavityAssignments.size() + " chest cavity assignments.");
  }
}
