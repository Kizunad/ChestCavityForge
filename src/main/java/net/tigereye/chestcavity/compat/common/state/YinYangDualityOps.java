package net.tigereye.chestcavity.compat.common.state;

import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.common.tuning.YinYangZhuanShenGuTuning;
import net.tigereye.chestcavity.compat.common.state.YinYangDualityAttachment.Anchor;
import net.tigereye.chestcavity.compat.common.state.YinYangDualityAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TeleportOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.registration.CCAttachments;

/** Convenience helpers for accessing & mutating 阴阳转身蛊专用附件。 */
public final class YinYangDualityOps {

  private YinYangDualityOps() {}

  /** Returns the attachment if already initialised; empty when player is null or not created. */
  public static Optional<YinYangDualityAttachment> get(Player player) {
    if (player == null) {
      return Optional.empty();
    }
    return CCAttachments.getExistingYinYangDuality(player);
  }

  /** Ensures an attachment exists (creating one if needed). */
  public static YinYangDualityAttachment resolve(Player player) {
    if (player == null) {
      throw new IllegalArgumentException("player");
    }
    return CCAttachments.getYinYangDuality(player);
  }

  /** Returns a live resource handle if Guzhenren attachment is available. */
  public static Optional<ResourceHandle> openHandle(Player player) {
    return ResourceOps.openHandle(player);
  }

  /** Captures the player's当前维度坐标为 Anchor。 */
  public static Optional<Anchor> captureAnchor(ServerPlayer player) {
    if (player == null) {
      return Optional.empty();
    }
    ResourceLocation dimension = player.serverLevel().dimension().location();
    Anchor anchor =
        Anchor.create(
            dimension,
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot());
    return Optional.of(anchor);
  }

  /**
   * Attempt to teleport the player到指定锚点。支持跨维度（若目标维度已加载）。
   *
   * @return {@code true} when teleport succeeded.
   */
  public static boolean teleportToAnchor(ServerPlayer player, Anchor anchor) {
    if (player == null || anchor == null || !anchor.isValid()) {
      return false;
    }
    ResourceLocation dimension = anchor.dimension();
    if (dimension == null) {
      return false;
    }
    ServerLevel targetLevel = null;
    ServerLevel currentLevel = player.serverLevel();
    ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, dimension);
    if (currentLevel.dimension().equals(targetKey)) {
      targetLevel = currentLevel;
    } else {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server == null) {
        return false;
      }
      targetLevel = server.getLevel(targetKey);
    }
    if (targetLevel == null) {
      return false;
    }
    if (targetLevel == currentLevel) {
      return TeleportOps.blinkTo(
              player,
              new net.minecraft.world.phys.Vec3(anchor.x(), anchor.y(), anchor.z()),
              8,
              0.5D)
          .isPresent();
    }
    player.teleportTo(
        targetLevel,
        anchor.x(),
        anchor.y(),
        anchor.z(),
        anchor.yaw(),
        anchor.pitch());
    player.fallDistance = 0.0F;
    return true;
  }

  /**
   * Saves当前态资源并切换到另一态的离线资源池。若目标态尚未初始化，则复制当前态数据作为初始值。
   */
  public static boolean swapPools(
      ServerPlayer player, YinYangDualityAttachment attachment, Mode nextMode) {
    if (player == null || attachment == null || nextMode == null) {
      return false;
    }
    Optional<ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      return false;
    }
    ResourceHandle handle = handleOpt.get();
    Mode current = attachment.currentMode();
    attachment.pool(current).capture(player, handle);
    attachment.pool(nextMode).ensureInitializedFrom(attachment.pool(current));
    attachment.pool(nextMode).apply(player, handle);
    return true;
  }

  public static boolean hasOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    Item targetItem = BuiltInRegistries.ITEM.getOptional(YinYangZhuanShenGuTuning.ORGAN_ID).orElse(null);
    if (targetItem == null) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == targetItem) {
        return true;
      }
    }
    return false;
  }
}
