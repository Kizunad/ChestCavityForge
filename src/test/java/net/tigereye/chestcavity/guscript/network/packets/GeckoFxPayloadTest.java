package net.tigereye.chestcavity.guscript.network.packets;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.fx.GeckoFxAnchor;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeckoFxPayloadTest {

    @Test
    void streamCodecRoundTripsPayload() {
        GeckoFxEventPayload original = new GeckoFxEventPayload(
                ChestCavity.id("ghost_dark"),
                GeckoFxAnchor.PERFORMER,
                42,
                UUID.randomUUID(),
                1.5,
                2.5,
                3.5,
                0.25,
                0.5,
                0.75,
                1.0,
                -2.0,
                3.0,
                90.0F,
                10.0F,
                0.0F,
                1.2F,
                0xFF112233,
                0.6F,
                true,
                80,
                ResourceLocation.parse("minecraft:geo/sample.geo.json"),
                ResourceLocation.parse("minecraft:textures/entity/sample.png"),
                ResourceLocation.parse("minecraft:animations/sample.animation.json"),
                UUID.randomUUID()
        );

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        GeckoFxEventPayload.STREAM_CODEC.encode(buffer, original);
        GeckoFxEventPayload decoded = GeckoFxEventPayload.STREAM_CODEC.decode(buffer);
        assertEquals(original, decoded);
    }
}
