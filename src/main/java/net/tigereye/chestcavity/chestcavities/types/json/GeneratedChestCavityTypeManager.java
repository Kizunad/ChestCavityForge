package net.tigereye.chestcavity.chestcavities.types.json;

import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.types.GeneratedChestCavityType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class GeneratedChestCavityTypeManager implements ResourceManagerReloadListener {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(ChestCavity.MODID, "types");
    private final ChestCavityTypeSerializer SERIALIZER = new ChestCavityTypeSerializer();
    public static Map<ResourceLocation, GeneratedChestCavityType> GeneratedChestCavityTypes = new HashMap<>();



    public void onResourceManagerReload(ResourceManager manager) {
        GeneratedChestCavityTypes.clear();
        ChestCavity.LOGGER.info("Loading chest cavity types.");
        for(ResourceLocation id : manager.listResources(RESOURCE_LOCATION.getPath(), path -> path.endsWith(".json"))) {
            try(InputStream stream = manager.getResource(id).getInputStream()) {
                Reader reader = new InputStreamReader(stream);
                GeneratedChestCavityTypes.put(id,SERIALIZER.read(id,new Gson().fromJson(reader,ChestCavityTypeJsonFormat.class)));
            } catch(Exception e) {
                ChestCavity.LOGGER.error("Error occurred while loading resource json " + id.toString(), e);
            }
        }
        ChestCavity.LOGGER.info("Loaded "+GeneratedChestCavityTypes.size()+" chest cavity types.");
    }
}
