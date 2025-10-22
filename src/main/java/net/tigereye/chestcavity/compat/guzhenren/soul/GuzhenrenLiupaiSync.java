package net.tigereye.chestcavity.compat.guzhenren.soul;

import java.util.List;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.soul.profile.SoulProfile;
import net.tigereye.chestcavity.soul.profile.capability.guzhenren.GuzhenrenSnapshot;

/**
 * Utilities for keeping Guzhenren liupai experience values aligned between owner and soul profiles.
 */
public final class GuzhenrenLiupaiSync {

  private static final List<String> LIUPAI_FIELDS =
      List.of(
          "liupai_jindao",
          "liupai_shuidao",
          "liupai_mudao",
          "liupai_yandao",
          "liupai_tudao",
          "liupai_fengdao",
          "liupai_guangdao",
          "liupai_andao",
          "liupai_leidao",
          "liupai_dudao",
          "liupai_yudao",
          "liupai_zhoudao",
          "liupai_rendao",
          "liupai_tiandao",
          "liupai_bingxuedao",
          "liupai_qidao",
          "liupai_nudao",
          "liupai_zhidao",
          "liupai_xingdao",
          "liupai_zhendao",
          "liupai_yingdao",
          "liupai_lvdao",
          "liupai_liandao",
          "liupai_lidao",
          "liupai_shidao",
          "liupai_huadao",
          "liupai_toudao",
          "liupai_yundao",
          "liupai_yundao2",
          "liupai_xindao",
          "liupai_yindao",
          "Liupai_gudao",
          "liupai_xudao",
          "liupai_jindao2",
          "liupai_jiandao",
          "liupai_daodao",
          "liupai_hundao",
          "liupai_dandao",
          "liupai_xuedao",
          "liupai_huandao",
          "liupai_yuedao",
          "liupai_mengdao",
          "liupai_bingdao",
          "liupai_bianhuadao");

  private GuzhenrenLiupaiSync() {}

  public static boolean mergeMaxLiupai(SoulProfile left, SoulProfile right) {
    if (!GuzhenrenResourceBridge.isAvailable()) {
      return false;
    }
    var leftSnapshotOpt = left.capability(GuzhenrenSnapshot.ID, GuzhenrenSnapshot.class);
    var rightSnapshotOpt = right.capability(GuzhenrenSnapshot.ID, GuzhenrenSnapshot.class);
    if (leftSnapshotOpt.isEmpty() || rightSnapshotOpt.isEmpty()) {
      return false;
    }
    GuzhenrenSnapshot leftSnapshot = leftSnapshotOpt.get();
    GuzhenrenSnapshot rightSnapshot = rightSnapshotOpt.get();

    boolean changed = false;
    for (String field : LIUPAI_FIELDS) {
      double leftValue = leftSnapshot.getValueOrDefault(field, 0.0D);
      double rightValue = rightSnapshot.getValueOrDefault(field, 0.0D);
      double max = Math.max(leftValue, rightValue);
      if (!Double.isFinite(max)) {
        continue;
      }
      if (Double.doubleToLongBits(leftValue) != Double.doubleToLongBits(max)) {
        leftSnapshot.setValue(field, max);
        changed = true;
      }
      if (Double.doubleToLongBits(rightValue) != Double.doubleToLongBits(max)) {
        rightSnapshot.setValue(field, max);
        changed = true;
      }
    }
    return changed;
  }
}
