package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;

/** Block manipulation helpers used by flow actions. */
final class BlockFlowActions {

  private static final int MAX_SNOW_LAYERS = 8;

  private BlockFlowActions() {}

  static FlowEdgeAction replaceBlocksSphere(
      double baseRadius,
      String radiusParam,
      String radiusVariable,
      double maxHardness,
      boolean includeFluids,
      boolean dropBlocks,
      List<ResourceLocation> replacementIds,
      List<Integer> replacementWeights,
      List<ResourceLocation> forbiddenIds,
      boolean placeSnowLayers,
      int snowLayersMin,
      int snowLayersMax,
      String originKey) {
    double sanitizedRadius = Math.max(0.0D, baseRadius);
    double hardnessCap = maxHardness <= 0.0D ? Double.POSITIVE_INFINITY : maxHardness;
    ReplacementTable replacements = ReplacementTable.from(replacementIds, replacementWeights);
    Set<ResourceLocation> forbidden = buildForbiddenSet(forbiddenIds);
    boolean allowSnowLayers = placeSnowLayers && snowLayersMax > 0;
    int minSnowLayers = allowSnowLayers ? Mth.clamp(snowLayersMin, 1, MAX_SNOW_LAYERS) : 0;
    int maxSnowLayers =
        allowSnowLayers ? Mth.clamp(snowLayersMax, minSnowLayers, MAX_SNOW_LAYERS) : 0;

    if (replacements.isEmpty() && !allowSnowLayers && dropBlocks) {
      ChestCavity.LOGGER.warn(
          "[Flow] replace_blocks_sphere has no replacements defined; action will only destroy"
              + " blocks.");
    }

    return new FlowEdgeAction() {
      @Override
      public void apply(
          Player performer, LivingEntity target, FlowController controller, long gameTime) {
        Level level =
            performer != null ? performer.level() : (target != null ? target.level() : null);
        if (!(level instanceof ServerLevel server)) {
          return;
        }
        double resolvedRadius = sanitizedRadius;
        if (controller != null) {
          if (radiusParam != null && !radiusParam.isBlank()) {
            resolvedRadius =
                Math.max(0.0D, controller.resolveFlowParamAsDouble(radiusParam, resolvedRadius));
          }
          if (radiusVariable != null && !radiusVariable.isBlank()) {
            resolvedRadius = Math.max(0.0D, controller.getDouble(radiusVariable, resolvedRadius));
          }
        }
        if (resolvedRadius <= 0.01D) {
          return;
        }
        Vec3 origin = resolveOrigin(originKey, performer, target);
        if (origin == null) {
          origin =
              performer != null
                  ? performer.position()
                  : (target != null ? target.position() : null);
        }
        if (origin == null) {
          return;
        }
        runBlockReplacement(
            server,
            performer,
            origin,
            resolvedRadius,
            hardnessCap,
            includeFluids,
            dropBlocks,
            replacements,
            forbidden,
            allowSnowLayers,
            minSnowLayers,
            maxSnowLayers);
      }

      @Override
      public String describe() {
        return "replace_blocks_sphere(radius="
            + sanitizedRadius
            + ", replacements="
            + replacements.describe()
            + ")";
      }
    };
  }

  private static void runBlockReplacement(
      ServerLevel server,
      Player performer,
      Vec3 origin,
      double radius,
      double hardnessCap,
      boolean includeFluids,
      boolean dropBlocks,
      ReplacementTable replacements,
      Set<ResourceLocation> forbidden,
      boolean placeSnowLayers,
      int snowLayersMin,
      int snowLayersMax) {
    if (server == null) {
      return;
    }
    // 玩家偏好：若执行者禁用破块，则仅允许非破坏性的替换/铺雪；否则直接返回
    boolean allow =
        performer == null
            ? net.tigereye.chestcavity.playerprefs.PlayerPreferenceOps
                .defaultSwordSlashBlockBreak()
            : net.tigereye.chestcavity.playerprefs.PlayerPreferenceOps.resolve(
                performer,
                net.tigereye.chestcavity.playerprefs.PlayerPreferenceOps.SWORD_SLASH_BLOCK_BREAK,
                net.tigereye.chestcavity.playerprefs.PlayerPreferenceOps
                    ::defaultSwordSlashBlockBreak);
    if (!allow && (dropBlocks || replacements.isEmpty()) && !placeSnowLayers) {
      return;
    }
    if (replacements.isEmpty() && !placeSnowLayers && !dropBlocks) {
      return;
    }
    RandomSource random = server.random;
    double radiusSq = radius * radius;
    BlockPos center = BlockPos.containing(origin);
    int range = Mth.ceil(radius);
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    for (int dx = -range; dx <= range; dx++) {
      for (int dy = -range; dy <= range; dy++) {
        for (int dz = -range; dz <= range; dz++) {
          cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
          if (!server.isLoaded(cursor)) {
            continue;
          }
          if (cursor.getY() < server.getMinBuildHeight()
              || cursor.getY() > server.getMaxBuildHeight()) {
            continue;
          }
          double blockCenterX = cursor.getX() + 0.5D;
          double blockCenterY = cursor.getY() + 0.5D;
          double blockCenterZ = cursor.getZ() + 0.5D;
          double distanceSq = origin.distanceToSqr(blockCenterX, blockCenterY, blockCenterZ);
          if (distanceSq > radiusSq) {
            continue;
          }
          BlockState currentState = server.getBlockState(cursor);
          if (!includeFluids) {
            FluidState fluid = currentState.getFluidState();
            if (fluid != null && !fluid.isEmpty()) {
              continue;
            }
          }
          if (!forbidden.isEmpty()) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(currentState.getBlock());
            if (blockId != null && forbidden.contains(blockId)) {
              continue;
            }
          }
          float hardness = currentState.getDestroySpeed(server, cursor);
          if (hardness < 0.0F || hardness > hardnessCap) {
            continue;
          }
          boolean changed = false;
          if (!replacements.isEmpty()) {
            BlockState replacement = replacements.choose(server, random);
            if (replacement != null) {
              if (dropBlocks && !currentState.isAir() && !currentState.is(replacement.getBlock())) {
                server.destroyBlock(cursor, true, performer);
                currentState = server.getBlockState(cursor);
              }
              if (!replacement.isAir() && !currentState.equals(replacement)) {
                changed = server.setBlock(cursor, replacement, Block.UPDATE_ALL);
              }
            }
          } else if (dropBlocks && !currentState.isAir()) {
            changed = server.destroyBlock(cursor, true, performer);
          }
          if (placeSnowLayers && changed) {
            BlockPos above = cursor.above();
            placeSnowLayer(server, above, snowLayersMin, snowLayersMax, random);
          }
        }
      }
    }
  }

  private static Vec3 resolveOrigin(String originKey, Player performer, LivingEntity target) {
    if (originKey == null || originKey.isBlank()) {
      return performer != null ? performer.position() : (target != null ? target.position() : null);
    }
    String key = originKey.trim().toLowerCase(Locale.ROOT);
    return switch (key) {
      case "performer", "self", "caster" -> performer != null ? performer.position() : null;
      case "performer_eye", "performer_eyes", "performer_head" -> performer != null
          ? performer.getEyePosition()
          : null;
      case "target", "victim", "enemy" -> target != null
          ? target.position()
          : (performer != null ? performer.position() : null);
      case "target_eye", "target_eyes", "target_head" -> target != null
          ? target.getEyePosition()
          : null;
      case "target_feet" -> target != null
          ? new Vec3(target.getX(), target.getY(), target.getZ())
          : null;
      case "performer_feet" -> performer != null
          ? new Vec3(performer.getX(), performer.getY(), performer.getZ())
          : null;
      default -> performer != null
          ? performer.position()
          : (target != null ? target.position() : null);
    };
  }

  private static void placeSnowLayer(
      ServerLevel server, BlockPos pos, int minLayers, int maxLayers, RandomSource random) {
    if (minLayers <= 0 || maxLayers <= 0) {
      return;
    }
    if (!server.isLoaded(pos)) {
      return;
    }
    BlockState current = server.getBlockState(pos);
    if (!current.isAir() && !current.is(Blocks.SNOW)) {
      return;
    }
    int layers = minLayers;
    if (maxLayers > minLayers) {
      layers += random.nextInt(maxLayers - minLayers + 1);
    }
    layers = Mth.clamp(layers, 1, MAX_SNOW_LAYERS);
    BlockState snowState = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
    if (!snowState.canSurvive(server, pos)) {
      return;
    }
    server.setBlock(pos, snowState, Block.UPDATE_ALL);
  }

  private static Set<ResourceLocation> buildForbiddenSet(List<ResourceLocation> forbidden) {
    if (forbidden == null || forbidden.isEmpty()) {
      return Set.of();
    }
    Set<ResourceLocation> sanitized = new HashSet<>();
    for (ResourceLocation id : forbidden) {
      if (id != null) {
        sanitized.add(id);
      }
    }
    return sanitized;
  }

  private static final class ReplacementTable {
    private final List<WeightedReplacement> entries;
    private final int totalWeight;

    private ReplacementTable(List<WeightedReplacement> entries, int totalWeight) {
      this.entries = entries;
      this.totalWeight = totalWeight;
    }

    static ReplacementTable from(List<ResourceLocation> ids, List<Integer> weights) {
      if (ids == null || ids.isEmpty()) {
        return new ReplacementTable(List.of(), 0);
      }
      List<WeightedReplacement> entries = new ArrayList<>();
      int total = 0;
      for (int i = 0; i < ids.size(); i++) {
        ResourceLocation id = ids.get(i);
        if (id == null) {
          continue;
        }
        int weight = 1;
        if (weights != null && i < weights.size()) {
          weight = Math.max(0, weights.get(i));
        }
        if (weight <= 0) {
          continue;
        }
        entries.add(new WeightedReplacement(id, weight));
        total += weight;
      }
      return new ReplacementTable(List.copyOf(entries), Math.max(0, total));
    }

    boolean isEmpty() {
      return entries.isEmpty() || totalWeight <= 0;
    }

    BlockState choose(ServerLevel server, RandomSource random) {
      if (isEmpty()) {
        return null;
      }
      int value = random.nextInt(totalWeight);
      WeightedReplacement selected = null;
      for (WeightedReplacement entry : entries) {
        value -= entry.weight();
        if (value < 0) {
          selected = entry;
          break;
        }
      }
      if (selected == null) {
        selected = entries.get(entries.size() - 1);
      }
      return selected.resolve(server);
    }

    String describe() {
      if (isEmpty()) {
        return "none";
      }
      return entries.stream()
          .map(
              entry -> {
                ResourceLocation id = entry.id();
                String name = id != null ? id.toString() : "unknown";
                return name + "x" + entry.weight();
              })
          .collect(Collectors.joining(","));
    }
  }

  private static final class WeightedReplacement {
    private final ResourceLocation id;
    private final int weight;
    private BlockState cachedState;
    private boolean missingLogged;

    private WeightedReplacement(ResourceLocation id, int weight) {
      this.id = id;
      this.weight = weight;
    }

    ResourceLocation id() {
      return id;
    }

    int weight() {
      return weight;
    }

    BlockState resolve(ServerLevel server) {
      if (cachedState != null) {
        return cachedState;
      }
      if (server == null || id == null) {
        return null;
      }
      Registry<Block> registry = server.registryAccess().registryOrThrow(Registries.BLOCK);
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
      var optional = registry.getOptional(key);
      if (optional.isEmpty()) {
        if (!missingLogged) {
          ChestCavity.LOGGER.warn("[Flow] Unknown replacement block id {}", id);
          missingLogged = true;
        }
        return null;
      }
      Block block = optional.get();
      cachedState = block.defaultBlockState();
      return cachedState;
    }
  }
}
