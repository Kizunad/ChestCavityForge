package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.reflect.Method;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.registry.GuScriptFlowLoader;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guscript.runtime.flow.actions.FlowActions.GeckoFxParameters;
import net.tigereye.chestcavity.guscript.runtime.flow.fx.GeckoFxAnchor;
import org.junit.jupiter.api.Test;

class GeckoFxParameterParseTest {

  @Test
  void parseGeckoParametersFromJson() throws Exception {
    JsonObject json = new JsonObject();
    json.addProperty("type", "emit_gecko");
    json.addProperty("fx", "chestcavity:ghost_dark");
    json.addProperty("anchor", "world");
    json.addProperty("yaw", 15.0D);
    json.addProperty("pitch", -10.0D);
    json.addProperty("roll", 5.0D);
    json.addProperty("scale", 0.75D);
    json.addProperty("tint", "#123456");
    json.addProperty("alpha", 0.4D);
    json.addProperty("loop", true);
    json.addProperty("duration", 80);
    json.addProperty("entity_id_variable", "guardian");

    JsonArray offset = new JsonArray();
    offset.add(0.0D);
    offset.add(1.5D);
    offset.add(-0.5D);
    json.add("offset", offset);

    JsonArray relativeOffset = new JsonArray();
    relativeOffset.add(0.25D);
    relativeOffset.add(-0.75D);
    relativeOffset.add(2.5D);
    json.add("relative_offset", relativeOffset);

    JsonArray worldPos = new JsonArray();
    worldPos.add(4.0D);
    worldPos.add(5.0D);
    worldPos.add(6.0D);
    json.add("world_position", worldPos);

    Method parseAction =
        GuScriptFlowLoader.class.getDeclaredMethod("parseAction", JsonObject.class);
    parseAction.setAccessible(true);
    Object action = parseAction.invoke(null, json);
    assertTrue(action instanceof FlowEdgeAction);

    Method parseParams =
        GuScriptFlowLoader.class.getDeclaredMethod("parseGeckoFxParameters", JsonObject.class);
    parseParams.setAccessible(true);
    GeckoFxParameters params = (GeckoFxParameters) parseParams.invoke(null, json);

    assertEquals(ChestCavity.id("ghost_dark"), params.fxId());
    assertEquals(GeckoFxAnchor.WORLD, params.anchor());
    assertEquals(new Vec3(0.0D, 1.5D, -0.5D), params.offset());
    assertEquals(new Vec3(0.25D, -0.75D, 2.5D), params.relativeOffset());
    assertEquals(new Vec3(4.0D, 5.0D, 6.0D), params.worldPosition());
    assertEquals(15.0F, params.yaw());
    assertEquals(-10.0F, params.pitch());
    assertEquals(5.0F, params.roll());
    assertEquals(0.75F, params.scale());
    assertEquals(0x00123456, params.tint());
    assertEquals(0.4F, params.alpha(), 1.0E-5F);
    assertTrue(params.loop());
    assertEquals(80, params.durationTicks());
    assertEquals("guardian", params.entityIdVariable());

    FlowEdgeAction edgeAction = (FlowEdgeAction) action;
    assertTrue(edgeAction.describe().contains("ghost_dark"));
  }

  @Test
  void parseEmitGeckoOnAllies() throws Exception {
    JsonObject json = new JsonObject();
    json.addProperty("type", "emit_gecko_on_allies");
    json.addProperty("fx", "chestcavity:ghost_tiger");
    json.addProperty("ally_radius", 6.0D);
    json.addProperty("scale", 0.9D);
    json.addProperty("tint", "#202020");
    json.addProperty("alpha", 0.5D);
    json.addProperty("loop", true);
    json.addProperty("duration", 30);

    Method parseAction =
        GuScriptFlowLoader.class.getDeclaredMethod("parseAction", JsonObject.class);
    parseAction.setAccessible(true);
    FlowEdgeAction action = (FlowEdgeAction) parseAction.invoke(null, json);
    assertNotNull(action);
    assertTrue(action.describe().contains("ghost_tiger"));
  }
}
