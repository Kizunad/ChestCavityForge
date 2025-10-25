package net.tigereye.chestcavity.compat.guzhenren.item.jin_dao.client;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.tigereye.chestcavity.compat.guzhenren.network.packets.TiePiProgressPayload;

/** Lightweight client侧缓存，供 HUD/ModernUI 读取最近一次铁皮蛊同步数据。 */
public final class TiePiClientState {

  private static final AtomicReference<TiePiProgressPayload> SNAPSHOT =
      new AtomicReference<>(null);

  private TiePiClientState() {}

  public static void mirror(TiePiProgressPayload payload) {
    SNAPSHOT.set(payload);
  }

  public static Optional<TiePiProgressPayload> snapshot() {
    return Optional.ofNullable(SNAPSHOT.get());
  }
}
