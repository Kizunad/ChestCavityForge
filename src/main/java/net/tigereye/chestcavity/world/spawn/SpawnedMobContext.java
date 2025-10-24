package net.tigereye.chestcavity.world.spawn;

import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.registration.CCAttachments;

/** 生成中的生物上下文，提供快捷访问 ChestCavity / 鼓真刃等附件。 */
public final class SpawnedMobContext {

  private final CustomMobSpawnDefinition definition;
  private final MinecraftServer server;
  private final ServerLevel level;
  private final Mob mob;
  private final Vec3 spawnPosition;

  SpawnedMobContext(
      CustomMobSpawnDefinition definition,
      MinecraftServer server,
      ServerLevel level,
      Mob mob,
      Vec3 spawnPosition) {
    this.definition = definition;
    this.server = server;
    this.level = level;
    this.mob = mob;
    this.spawnPosition = spawnPosition;
  }

  public CustomMobSpawnDefinition definition() {
    return definition;
  }

  public MinecraftServer server() {
    return server;
  }

  public ServerLevel level() {
    return level;
  }

  public Mob mob() {
    return mob;
  }

  public Vec3 spawnPosition() {
    return spawnPosition;
  }

  /** 直接获取（或延迟创建）胸腔实例。 */
  public ChestCavityInstance chestCavity() {
    return CCAttachments.getChestCavity(mob);
  }

  /** 若已存在胸腔实例则返回，否则为空。 */
  public Optional<ChestCavityInstance> existingChestCavity() {
    return CCAttachments.getExistingChestCavity(mob);
  }

  public GuScriptAttachment guScript() {
    return CCAttachments.getGuScript(mob);
  }

  public Optional<GuScriptAttachment> existingGuScript() {
    return CCAttachments.getExistingGuScript(mob);
  }
}
