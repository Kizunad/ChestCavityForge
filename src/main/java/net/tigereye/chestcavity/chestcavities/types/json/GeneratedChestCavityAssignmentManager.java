package net.tigereye.chestcavity.chestcavities.types.json;

import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.tigereye.chestcavity.ChestCavity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class GeneratedChestCavityAssignmentManager implements ResourceManagerReloadListener {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(ChestCavity.MODID, "entity_assignment");
    private final ChestCavityAssignmentSerializer SERIALIZER = new ChestCavityAssignmentSerializer();
    public static Map<ResourceLocation, ResourceLocation> GeneratedChestCavityAssignments = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        GeneratedChestCavityAssignments.clear();
        ChestCavity.LOGGER.info("Loading chest cavity assignments.");
        for(ResourceLocation id : manager.listResources(RESOURCE_LOCATION.getPath(), path -> path.endsWith(".json"))) {
            try(InputStream stream = manager.getResource(id).getInputStream()) {
                Reader reader = new InputStreamReader(stream);
                GeneratedChestCavityAssignments.putAll(SERIALIZER.read(id,new Gson().fromJson(reader,ChestCavityAssignmentJsonFormat.class)));
            } catch(Exception e) {
                ChestCavity.LOGGER.error("Error occurred while loading resource json " + id.toString(), e);
            }
        }
        ChestCavity.LOGGER.info("Loaded "+GeneratedChestCavityAssignments.size()+" chest cavity assignments.");
    }
}
