package net.tigereye.chestcavity.chestcavities.organs;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.util.Pair;

public class OrganManager implements ResourceManagerReloadListener {
  private static final ResourceLocation RESOURCE_LOCATION = ChestCavity.id("organs");
  private final OrganSerializer SERIALIZER = new OrganSerializer();
  public static Map<ResourceLocation, OrganData> GeneratedOrganData = new HashMap<>();

  public void onResourceManagerReload(ResourceManager manager) {
    GeneratedOrganData.clear();
    ChestCavity.LOGGER.info("Loading organs.");
    manager
        .listResources(RESOURCE_LOCATION.getPath(), path -> path.getPath().endsWith(".json"))
        .forEach(
            (id, resource) -> {
              try (Reader reader = new InputStreamReader(resource.open())) {
                Pair<ResourceLocation, OrganData> organDataPair =
                    SERIALIZER.read(id, new Gson().fromJson(reader, OrganJsonFormat.class));
                GeneratedOrganData.put(organDataPair.getLeft(), organDataPair.getRight());
              } catch (Exception e) {
                ChestCavity.LOGGER.error("Error occurred while loading resource json " + id, e);
              }
            });
    ChestCavity.LOGGER.info("Loaded " + GeneratedOrganData.size() + " organs.");
  }

  public static boolean hasEntry(Item item) {
    return GeneratedOrganData.containsKey(BuiltInRegistries.ITEM.getKey(item));
  }

  public static OrganData getEntry(Item item) {
    return GeneratedOrganData.get(BuiltInRegistries.ITEM.getKey(item));
  }

  public static boolean isTrueOrgan(Item item) {
    if (hasEntry(item)) {
      return !getEntry(item).pseudoOrgan;
    }
    return false;
  }
}
