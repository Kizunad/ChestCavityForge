package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.command.SwordCommandCenter;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning;

/**
 * TUI会话管理器：负责生成、刷新和校验TUI会话ID（sid）。
 *
 * <p>核心功能：
 * <ul>
 *   <li>生成短随机会话ID（6位字母数字）</li>
 *   <li>管理会话过期时间（TTL）</li>
 *   <li>控制TUI刷新频率（限流）</li>
 *   <li>校验会话有效性</li>
 * </ul>
 */
public final class TUISessionManager {

  private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
  private static final int SID_LENGTH = 6;

  private TUISessionManager() {}

  /**
   * 生成新的会话ID。
   *
   * @return 6位随机字母数字组合
   */
  public static String generateSid() {
    Random rng = ThreadLocalRandom.current();
    StringBuilder sb = new StringBuilder(SID_LENGTH);
    for (int i = 0; i < SID_LENGTH; i++) {
      sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
    }
    return sb.toString();
  }

  /**
   * 获取当前玩家的会话ID（如果有效）。
   *
   * @param player 玩家
   * @param nowTick 当前游戏时间
   * @return 会话ID，如果会话已过期或不存在则返回null
   */
  public static String currentSid(ServerPlayer player, long nowTick) {
    return SwordCommandCenter.session(player)
        .map(session -> {
          if (session.tuiSessionExpiresAt() > nowTick && session.tuiSessionId() != null) {
            return session.tuiSessionId();
          }
          return null;
        })
        .orElse(null);
  }

  /**
   * 确保玩家有一个新鲜的会话。
   *
   * <p>如果当前会话有效则刷新TTL，否则生成新会话。
   *
   * @param player 玩家
   * @param nowTick 当前游戏时间
   * @return 新的或刷新后的会话ID
   */
  public static String ensureFreshSession(ServerPlayer player, long nowTick) {
    var session = SwordCommandCenter.sessionOrCreate(player);

    String sid = session.tuiSessionId();
    long expiresAt = session.tuiSessionExpiresAt();

    // 如果会话已过期或不存在，生成新会话
    if (sid == null || nowTick >= expiresAt) {
      sid = generateSid();
      session.setTuiSessionId(sid);
    }

    // 刷新过期时间（TTL）
    long ttlTicks = FlyingSwordTuning.TUI_SESSION_TTL_SECONDS * 20L;
    session.setTuiSessionExpiresAt(nowTick + ttlTicks);

    return sid;
  }

  /**
   * 检查是否允许发送TUI（限流）。
   *
   * @param player 玩家
   * @param nowTick 当前游戏时间
   * @return true如果允许发送，false如果触发限流
   */
  public static boolean canSendTui(ServerPlayer player, long nowTick) {
    return SwordCommandCenter.session(player)
        .map(session -> {
          long lastSent = session.lastTuiSentAt();
          long minIntervalTicks = FlyingSwordTuning.TUI_MIN_REFRESH_MILLIS / 50L;
          return (nowTick - lastSent) >= minIntervalTicks;
        })
        .orElse(true); // 如果没有会话，允许发送
  }

  /**
   * 更新最后发送TUI的时间。
   *
   * @param player 玩家
   * @param nowTick 当前游戏时间
   */
  public static void markTuiSent(ServerPlayer player, long nowTick) {
    SwordCommandCenter.session(player).ifPresent(session -> {
      session.setLastTuiSentAt(nowTick);
    });
  }

  /**
   * 校验会话ID是否有效。
   *
   * @param player 玩家
   * @param providedSid 命令提供的会话ID
   * @param nowTick 当前游戏时间
   * @return true如果会话有效，false如果已过期或不匹配
   */
  public static boolean isValidSession(ServerPlayer player, String providedSid, long nowTick) {
    if (providedSid == null || providedSid.isEmpty()) {
      return false;
    }

    return SwordCommandCenter.session(player)
        .map(session -> {
          String currentSid = session.tuiSessionId();
          long expiresAt = session.tuiSessionExpiresAt();

          // 检查会话是否匹配且未过期
          return providedSid.equals(currentSid) && nowTick < expiresAt;
        })
        .orElse(false);
  }

  /**
   * 获取会话剩余时间（秒）。
   *
   * @param player 玩家
   * @param nowTick 当前游戏时间
   * @return 剩余秒数，如果会话无效则返回0
   */
  public static double getRemainingSeconds(ServerPlayer player, long nowTick) {
    return SwordCommandCenter.session(player)
        .map(session -> {
          long expiresAt = session.tuiSessionExpiresAt();
          long remainingTicks = Math.max(0L, expiresAt - nowTick);
          return remainingTicks / 20.0;
        })
        .orElse(0.0);
  }
}
