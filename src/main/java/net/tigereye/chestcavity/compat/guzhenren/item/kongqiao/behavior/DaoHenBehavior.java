package net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.behavior;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import org.slf4j.Logger;

/** Shared DaoHen utilities. Client-specific listeners live in {@link DaoHenClientBehavior}. */
public final class DaoHenBehavior {
  static final Logger LOGGER = LogUtils.getLogger();
  static final String LOG_PREFIX = "[compat/guzhenren][kongqiao]";
  private static final String MOD_ID = "guzhenren";
  private static final double EPSILON = 1.0e-4;
  private static final double DAO_HEN_TO_INCREASE_RATIO = 1.0 / 1000.0;
  private static final Map<String, ResourceLocation> DAO_HEN_CHANNELS =
      Map.ofEntries(
          Map.entry(
              "jindao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jin_dao_increase_effect")),
          Map.entry(
              "shuidao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/shui_dao_increase_effect")),
          Map.entry(
              "mudao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/mu_dao_increase_effect")),
          Map.entry(
              "yandao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yan_dao_increase_effect")),
          Map.entry(
              "tudao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/tu_dao_increase_effect")),
          Map.entry(
              "fengdao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/feng_dao_increase_effect")),
          Map.entry(
              "guangdao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/guang_dao_increase_effect")),
          Map.entry(
              "andao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/an_dao_increase_effect")),
          Map.entry(
              "leidao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/lei_dao_increase_effect")),
          Map.entry(
              "dudao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/du_dao_increase_effect")),
          Map.entry(
              "yudao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yu_dao_increase_effect")),
          Map.entry(
              "zhoudao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/zhou_dao_increase_effect")),
          Map.entry(
              "rendao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/ren_dao_increase_effect")),
          Map.entry(
              "tiandao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/tian_dao_increase_effect")),
          Map.entry(
              "bingxuedao",
              ResourceLocation.fromNamespaceAndPath(
                  MOD_ID, "linkage/bing_xue_dao_increase_effect")),
          Map.entry(
              "qidao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/qi_dao_increase_effect")),
          Map.entry(
              "nudao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/nu_dao_increase_effect")),
          Map.entry(
              "zhidao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/zhi_dao_increase_effect")),
          Map.entry(
              "xingdao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xing_dao_increase_effect")),
          Map.entry(
              "zhendao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/zhen_dao_increase_effect")),
          Map.entry(
              "yingdao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/ying_dao_increase_effect")),
          Map.entry(
              "lvdao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/lv_dao_increase_effect")),
          Map.entry(
              "liandao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/lian_dao_increase_effect")),
          Map.entry(
              "lidao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/li_dao_increase_effect")),
          Map.entry(
              "shidao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/shi_dao_increase_effect")),
          Map.entry(
              "huadao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/hua_dao_increase_effect")),
          Map.entry(
              "toudao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/tou_dao_increase_effect")),
          Map.entry(
              "yundao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yun_dao_increase_effect")),
          Map.entry(
              "xindao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xin_dao_increase_effect")),
          Map.entry(
              "yindao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yin_dao_increase_effect")),
          Map.entry(
              "gudao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/gu_dao_increase_effect")),
          Map.entry(
              "xudao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xu_dao_increase_effect")),
          Map.entry(
              "jiandao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jian_dao_increase_effect")),
          Map.entry(
              "daodao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/dao_dao_increase_effect")),
          Map.entry(
              "hundao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/hun_dao_increase_effect")),
          Map.entry(
              "dandao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/dan_dao_increase_effect")),
          Map.entry(
              "xuedao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xue_dao_increase_effect")),
          Map.entry(
              "huandao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/huan_dao_increase_effect")),
          Map.entry(
              "yuedao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yue_dao_increase_effect")),
          Map.entry(
              "mengdao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/meng_dao_increase_effect")),
          Map.entry(
              "bingdao",
              ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bing_dao_increase_effect")),
          Map.entry(
              "bianhuadao",
              ResourceLocation.fromNamespaceAndPath(
                  MOD_ID, "linkage/bian_hua_dao_increase_effect")));

  private DaoHenBehavior() {}

  public static void bootstrap() {
    var dist = FMLEnvironment.dist;
    LOGGER.debug("{} DaoHenBehavior bootstrap request (dist={})", LOG_PREFIX, dist);
    if (!dist.isClient()) {
      LOGGER.debug("{} DaoHenBehavior bootstrap skipped: non-client dist", LOG_PREFIX);
      return;
    }
    DaoHenClientBehavior.bootstrap();
  }

  static Map<String, Double> filterDaoHen(Map<String, Double> snapshot) {
    Map<String, Double> filtered = new HashMap<>();
    for (Map.Entry<String, Double> entry : snapshot.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("daohen_") || key.startsWith("dahen_")) {
        filtered.put(key, entry.getValue());
      }
    }
    return filtered;
  }

  static void processSnapshot(
      Player player,
      Map<String, Double> tracked,
      Map<UUID, Map<String, Double>> cache,
      boolean logChanges) {
    UUID playerId = player.getUUID();
    Map<String, Double> previous = cache.get(playerId);
    if (logChanges) {
      if (previous == null) {
        logInitialSnapshot(player, tracked);
      } else {
        logDiff(player, previous, tracked);
      }
    }
    seedIncreaseEffects(player, tracked);
    cache.put(playerId, Collections.unmodifiableMap(new HashMap<>(tracked)));
  }

  static void seedIncreaseEffects(Player player, Map<String, Double> daoHenValues) {
    if (player == null || daoHenValues == null || daoHenValues.isEmpty()) {
      return;
    }
    Map<ResourceLocation, Double> seeds = new LinkedHashMap<>();
    daoHenValues.forEach(
        (rawKey, rawValue) -> {
          if (rawValue == null || Math.abs(rawValue) < EPSILON) {
            return;
          }
          ResourceLocation channelId = resolveIncreaseChannel(rawKey);
          if (channelId == null) {
            LOGGER.trace(
                "{} skipping DaoHen key {} (no mapped increase effect)", LOG_PREFIX, rawKey);
            return;
          }
          double converted = rawValue * DAO_HEN_TO_INCREASE_RATIO;
          seeds.put(channelId, converted);
        });
    if (seeds.isEmpty()) {
      return;
    }
    DaoHenSeedHandler.applyChannelSeeds(player, seeds);
  }

  private static ResourceLocation resolveIncreaseChannel(String rawKey) {
    if (rawKey == null || rawKey.isBlank()) {
      return null;
    }
    String normalised = rawKey.toLowerCase(Locale.ROOT);
    if (normalised.startsWith("daohen_")) {
      normalised = normalised.substring("daohen_".length());
    } else if (normalised.startsWith("dahen_")) {
      normalised = normalised.substring("dahen_".length());
    }
    if (normalised.isEmpty()) {
      return null;
    }
    normalised = normalised.replace("_", "");
    while (!normalised.isEmpty() && Character.isDigit(normalised.charAt(normalised.length() - 1))) {
      normalised = normalised.substring(0, normalised.length() - 1);
    }
    if (normalised.isEmpty()) {
      return null;
    }
    return DAO_HEN_CHANNELS.get(normalised);
  }

  private static void logInitialSnapshot(Player player, Map<String, Double> values) {
    for (Map.Entry<String, Double> entry : values.entrySet()) {
      var label = GuzhenrenResourceBridge.documentationLabel(entry.getKey()).orElse(entry.getKey());
      LOGGER.info(
          "{} {} {} = {}", LOG_PREFIX, player.getScoreboardName(), label, format(entry.getValue()));
    }
  }

  private static void logDiff(
      Player player, Map<String, Double> previous, Map<String, Double> current) {
    boolean any = false;
    for (Map.Entry<String, Double> entry : current.entrySet()) {
      String key = entry.getKey();
      double newValue = entry.getValue();
      double oldValue = previous.getOrDefault(key, Double.NaN);
      if (Double.isNaN(oldValue) || Math.abs(newValue - oldValue) > EPSILON) {
        double delta = Double.isNaN(oldValue) ? newValue : newValue - oldValue;
        var label = GuzhenrenResourceBridge.documentationLabel(key).orElse(key);
        LOGGER.info(
            "{} {} {} -> {} (Δ {})",
            LOG_PREFIX,
            player.getScoreboardName(),
            label,
            format(newValue),
            format(delta));
        any = true;
      }
    }
    if (!any) {
      LOGGER.debug(
          "{} DaoHen diff computed but no changes exceeded epsilon={}", LOG_PREFIX, EPSILON);
    }
  }

  static String format(double value) {
    return String.format(Locale.ROOT, "%.3f", value);
  }
}
