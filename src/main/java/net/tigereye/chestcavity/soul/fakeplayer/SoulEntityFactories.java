package net.tigereye.chestcavity.soul.fakeplayer;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * 灵魂实体生成工厂注册表，允许按实体类型注入自定义的生成逻辑。
 */
public final class SoulEntityFactories {

    private static final Map<ResourceLocation, SoulEntityFactory> FACTORIES = new ConcurrentHashMap<>();

    private static final SoulEntityFactory DEFAULT_FACTORY = new SoulEntityFactory() {
        @Override
        public Entity instantiate(SoulEntitySpawnRequest request) {
            return SoulPlayer.create(request.owner(), request.soulId(), request.identity());
        }
    };

    static {
        register(EntityType.PLAYER, DEFAULT_FACTORY);
    }

    private SoulEntityFactories() {
    }

    public static void register(EntityType<?> type, SoulEntityFactory factory) {
        Objects.requireNonNull(type, "type");
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        register(id, factory);
    }

    public static void register(ResourceLocation id, SoulEntityFactory factory) {
        Objects.requireNonNull(id, "id");
        FACTORIES.put(id, Objects.requireNonNull(factory, "factory"));
    }

    public static SoulEntityFactory resolve(EntityType<?> type) {
        Objects.requireNonNull(type, "type");
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return FACTORIES.getOrDefault(id, DEFAULT_FACTORY);
    }

    public static SoulEntityFactory defaultFactory() {
        return DEFAULT_FACTORY;
    }

    @FunctionalInterface
    public interface SoulEntityFactory {
        Entity instantiate(SoulEntitySpawnRequest request);

        default void configureEntity(SoulEntitySpawnRequest request, Entity entity) {
        }

        default void onAfterEntitySpawned(SoulEntitySpawnRequest request, SoulEntitySpawnResult result) {
        }
    }
}
