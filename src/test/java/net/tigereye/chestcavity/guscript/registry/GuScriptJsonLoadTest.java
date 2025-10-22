package net.tigereye.chestcavity.guscript.registry;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;
import org.junit.jupiter.api.Test;

/**
 * Loads and parses all GuScript JSON resources (leaves, rules, flows). Helps catch malformed files
 * during tests rather than at runtime /reload.
 */
class GuScriptJsonLoadTest {

  private static final Gson GSON = new GsonBuilder().setLenient().create();

  @Test
  void loadAllGuScriptJson() throws IOException {
    // Load leaves
    Map<ResourceLocation, JsonElement> leaves =
        readJsonDir("src/main/resources/data/chestcavity/guscript/leaves");
    assertFalse(leaves.isEmpty(), "No leaves found under guscript/leaves");
    GuScriptLeafLoader leafLoader = new GuScriptLeafLoader();
    leafLoader.apply(leaves, null, (ProfilerFiller) null);
    assertFalse(
        GuScriptRegistry.leafIds().isEmpty(), "Leaf registry should not be empty after load");

    // Load rules
    Map<ResourceLocation, JsonElement> rules =
        readJsonDir("src/main/resources/data/chestcavity/guscript/rules");
    assertFalse(rules.isEmpty(), "No rules found under guscript/rules");
    GuScriptRuleLoader ruleLoader = new GuScriptRuleLoader();
    ruleLoader.apply(rules, null, (ProfilerFiller) null);
    assertFalse(
        GuScriptRegistry.reactionRules().isEmpty(),
        "Reaction rules should not be empty after load");

    // Load flows
    Map<ResourceLocation, JsonElement> flows =
        readJsonDir("src/main/resources/data/chestcavity/guscript/flows");
    assertFalse(flows.isEmpty(), "No flows found under guscript/flows");
    GuScriptFlowLoader flowLoader = new GuScriptFlowLoader();
    flowLoader.apply(flows, null, (ProfilerFiller) null);

    // Validate representative flows exist
    assertTrue(
        FlowProgramRegistry.get(ResourceLocation.parse("chestcavity:demo_charge_release"))
            .isPresent(),
        "demo_charge_release flow missing");
    assertTrue(
        FlowProgramRegistry.get(ResourceLocation.parse("chestcavity:time_acceleration"))
            .isPresent(),
        "time_acceleration flow missing");
    assertTrue(
        FlowProgramRegistry.get(ResourceLocation.parse("chestcavity:thoughts_cycle")).isPresent(),
        "thoughts_cycle flow missing");
    assertTrue(
        FlowProgramRegistry.get(ResourceLocation.parse("chestcavity:thoughts_remote_burst"))
            .isPresent(),
        "thoughts_remote_burst flow missing");
  }

  private static Map<ResourceLocation, JsonElement> readJsonDir(String dir) throws IOException {
    Path root = Path.of(dir);
    if (!Files.isDirectory(root)) {
      return Map.of();
    }
    try (var stream = Files.walk(root)) {
      return stream
          .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json"))
          .collect(
              Collectors.toMap(
                  GuScriptJsonLoadTest::toId,
                  p -> {
                    JsonElement element = readJson(p);
                    if (element == null) {
                      throw new IllegalStateException("Parsed null for " + p);
                    }
                    return element;
                  }));
    }
  }

  private static ResourceLocation toId(Path path) {
    String file = path.getFileName().toString();
    String name = file.substring(0, file.length() - ".json".length());
    return ResourceLocation.fromNamespaceAndPath("chestcavity", name);
  }

  private static JsonElement readJson(Path path) {
    try {
      String content = Files.readString(path);
      return GSON.fromJson(content, JsonElement.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
