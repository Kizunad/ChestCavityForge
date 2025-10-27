package net.tigereye.chestcavity.compat.guzhenren.item.combo.framework;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 组合杀招激活上下文。
 *
 * <p>集中封装激活时常用的数据，便于在单元测试中构造假上下文。
 */
public final class ComboSkillContext {

  private final ServerPlayer player;
  private final ChestCavityInstance chestCavity;
  private final long gameTime;

  private ComboSkillContext(ServerPlayer player, ChestCavityInstance chestCavity, long gameTime) {
    this.player = player;
    this.chestCavity = chestCavity;
    this.gameTime = gameTime;
  }

  public static ComboSkillContext capture(ServerPlayer player, ChestCavityInstance chestCavity) {
    return new ComboSkillContext(player, chestCavity, player.level().getGameTime());
  }

  public static ComboSkillContext of(ServerPlayer player, ChestCavityInstance chestCavity, long now) {
    return new ComboSkillContext(player, chestCavity, now);
  }

  public ServerPlayer player() {
    return player;
  }

  public ChestCavityInstance chestCavity() {
    return chestCavity;
  }

  public long gameTime() {
    return gameTime;
  }

  /**
   * 默认以 GuzhenrenResourceBridge 读取句柄。
   *
   * <p>保持 Optional 形式以便上层自定义错误处理。
   */
  public Optional<ResourceHandle> openResources() {
    return ResourceOps.openHandle(player);
  }
}
