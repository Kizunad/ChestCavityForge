package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** Maintains the runtime soul avatar template registry loaded from JSON. */
public final class HunDaoSoulAvatarTemplates {

  public static final String DATA_FOLDER = "hun_dao/soul_avatars";
  public static final ResourceLocation DEFAULT_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/default_avatar");

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<ResourceLocation, HunDaoSoulAvatarTemplate> TEMPLATES =
      new ConcurrentHashMap<>();

  static {
    TEMPLATES.put(DEFAULT_ID, HunDaoSoulAvatarTemplate.defaults(DEFAULT_ID));
  }

  private HunDaoSoulAvatarTemplates() {}

  public static void update(Map<ResourceLocation, HunDaoSoulAvatarTemplate> newTemplates) {
    TEMPLATES.clear();
    TEMPLATES.put(DEFAULT_ID, HunDaoSoulAvatarTemplate.defaults(DEFAULT_ID));
    if (newTemplates != null && !newTemplates.isEmpty()) {
      TEMPLATES.putAll(newTemplates);
    }
    LOGGER.info(
        "[hun_dao][soul_avatar] Loaded {} templates (default included)", TEMPLATES.size());
  }

  public static HunDaoSoulAvatarTemplate get(ResourceLocation id) {
    if (id == null) {
      return TEMPLATES.get(DEFAULT_ID);
    }
    return TEMPLATES.getOrDefault(id, TEMPLATES.get(DEFAULT_ID));
  }

  public static Map<ResourceLocation, HunDaoSoulAvatarTemplate> current() {
    return Collections.unmodifiableMap(TEMPLATES);
  }
}
