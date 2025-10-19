package net.tigereye.chestcavity.guscript.fx.gecko.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.fx.gecko.GeckoFxDefinition;
import net.tigereye.chestcavity.guscript.fx.gecko.GeckoFxDefinition.BlendMode;
import net.tigereye.chestcavity.guscript.fx.gecko.GeckoFxRegistry;
import net.tigereye.chestcavity.guscript.network.packets.GeckoFxEventPayload;
import net.tigereye.chestcavity.guscript.runtime.flow.fx.GeckoFxAnchor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeckoFxManagerTest {

    private GeckoFxClient.GeckoFxManager manager;

    @BeforeEach
    void setupManager() {
        manager = GeckoFxClient.managerForTests();
        manager.clear();
        GeckoFxRegistry.updateDefinitions(Map.of(
                ChestCavity.id("ghost_dark"), definition(ChestCavity.id("ghost_dark")),
                ChestCavity.id("ghost_loop"), definition(ChestCavity.id("ghost_loop"))
        ));
    }

    @AfterEach
    void teardown() {
        manager.clear();
    }

    @Test
    void nonLoopingEffectExpiresAfterDuration() {
        Level level = Mockito.mock(Level.class);
        GeckoFxEventPayload payload = new GeckoFxEventPayload(
                ChestCavity.id("ghost_dark"),
                GeckoFxAnchor.PERFORMER,
                1,
                UUID.randomUUID(),
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                1.0F,
                0xFFFFFFFF,
                1.0F,
                false,
                20,
                null,
                null,
                null,
                UUID.randomUUID()
        );

        manager.enqueue(level, 100L, payload);
        assertEquals(1, manager.activeCount());

        manager.tick(level, 110L);
        assertEquals(1, manager.activeCount(), "Effect should remain active before duration elapses");

        manager.tick(level, 121L);
        assertEquals(0, manager.activeCount(), "Effect should expire after duration");
    }

    @Test
    void loopingEffectPersists() {
        Level level = Mockito.mock(Level.class);
        GeckoFxEventPayload payload = new GeckoFxEventPayload(
                ChestCavity.id("ghost_loop"),
                GeckoFxAnchor.WORLD,
                -1,
                null,
                1.0D,
                2.0D,
                3.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                1.0F,
                0xFFFFFFFF,
                1.0F,
                true,
                10,
                null,
                null,
                null,
                UUID.randomUUID()
        );

        manager.enqueue(level, 200L, payload);
        manager.tick(level, 208L);
        assertEquals(1, manager.activeCount(), "Looping effect should remain active before refresh window closes");

        // simulate the server sending another payload with the same eventId to keep the loop alive
        manager.enqueue(level, 209L, payload);
        manager.tick(level, 218L);
        assertEquals(1, manager.activeCount(), "Looping effect should stay alive after refresh");
    }

    private static GeckoFxDefinition definition(ResourceLocation id) {
        ResourceLocation model = ChestCavity.id("geo/ghost_tiger.geo.json");
        ResourceLocation texture = ResourceLocation.parse("minecraft:textures/entity/phantom.png");
        ResourceLocation animation = ChestCavity.id("animations/ghost_tiger.animation.json");
        return new GeckoFxDefinition(id, model, texture, animation, "animation.chestcavity.ghost_tiger.idle", 1.0F, 0xFFFFFF, 1.0F, BlendMode.TRANSLUCENT);
    }

}
