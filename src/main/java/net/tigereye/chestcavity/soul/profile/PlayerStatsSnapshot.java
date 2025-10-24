package net.tigereye.chestcavity.soul.profile;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

/**
 * 原版玩家状态快照（经验/饥饿/生命/吸收/属性）
 *
 * <p>作用 - 为灵魂存档记录可恢复的基础状态，包含经验、饥饿三件套、生命/吸收、以及可同步属性的基值。 -
 * 仅设置属性的“BaseValue”，不处理修饰符（Modifiers），以避免跨模组状态不一致。
 */
public record PlayerStatsSnapshot(
    int experienceLevel,
    float experienceProgress,
    int experienceTotal,
    float health,
    float absorption,
    int foodLevel,
    float saturation,
    float exhaustion,
    Map<ResourceLocation, Double> attributeBaseValues) {

  private static final String ATTRIBUTES_KEY = "attributes";

  public static PlayerStatsSnapshot capture(Player player) {
    Map<ResourceLocation, Double> attributes = new HashMap<>();
    for (AttributeInstance instance : player.getAttributes().getSyncableAttributes()) {
      var holder = instance.getAttribute();
      holder.unwrapKey().ifPresent(key -> attributes.put(key.location(), instance.getBaseValue()));
    }
    return new PlayerStatsSnapshot(
        player.experienceLevel,
        player.experienceProgress,
        player.totalExperience,
        player.getHealth(),
        player.getAbsorptionAmount(),
        player.getFoodData().getFoodLevel(),
        player.getFoodData().getSaturationLevel(),
        player.getFoodData().getExhaustionLevel(),
        attributes);
  }

  public void restore(Player player, HolderLookup.Provider provider) {
    // 回写经验
    player.experienceLevel = experienceLevel;
    player.experienceProgress = experienceProgress;
    player.totalExperience = experienceTotal;

    // 回写饥饿
    player.getFoodData().setFoodLevel(foodLevel);
    player.getFoodData().setSaturation(saturation);
    player.getFoodData().setExhaustion(exhaustion);

    // 回写生命/吸收（生命值夹取 [0, max health]）
    float maxHp = player.getMaxHealth();
    float safeHp = health;
    if (!Float.isFinite(safeHp)) safeHp = maxHp;
    safeHp = Math.max(0f, Math.min(maxHp, safeHp));
    float safeAbs = absorption;
    if (!Float.isFinite(safeAbs)) safeAbs = 0f;
    player.setAbsorptionAmount(safeAbs);
    player.setHealth(safeHp);

    // 回写可同步属性的基值
    var lookup = provider.lookupOrThrow(Registries.ATTRIBUTE);
    attributeBaseValues.forEach(
        (id, value) -> {
          ResourceKey<Attribute> key = ResourceKey.create(Registries.ATTRIBUTE, id);
          lookup
              .get(key)
              .ifPresent(
                  holder -> {
                    AttributeInstance instance = player.getAttribute(holder);
                    if (instance != null) {
                      instance.setBaseValue(value);
                    }
                  });
        });
  }

  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putInt("xpLevel", experienceLevel);
    tag.putFloat("xpProgress", experienceProgress);
    tag.putInt("xpTotal", experienceTotal);
    tag.putFloat("health", health);
    tag.putFloat("absorption", absorption);
    tag.putInt("food", foodLevel);
    tag.putFloat("saturation", saturation);
    tag.putFloat("exhaustion", exhaustion);

    CompoundTag attributesTag = new CompoundTag();
    attributeBaseValues.forEach((id, value) -> attributesTag.putDouble(id.toString(), value));
    tag.put(ATTRIBUTES_KEY, attributesTag);
    return tag;
  }

  public static PlayerStatsSnapshot load(CompoundTag tag, HolderLookup.Provider provider) {
    int level = tag.getInt("xpLevel");
    float progress = tag.getFloat("xpProgress");
    int total = tag.getInt("xpTotal");
    float health = tag.getFloat("health");
    float absorption = tag.getFloat("absorption");
    int food = tag.getInt("food");
    float saturation = tag.getFloat("saturation");
    float exhaustion = tag.getFloat("exhaustion");

    Map<ResourceLocation, Double> attributes = new HashMap<>();
    CompoundTag attributesTag = tag.getCompound(ATTRIBUTES_KEY);
    for (String key : attributesTag.getAllKeys()) {
      ResourceLocation id = ResourceLocation.tryParse(key);
      if (id != null) {
        attributes.put(id, attributesTag.getDouble(key));
      }
    }

    return new PlayerStatsSnapshot(
        level, progress, total, health, absorption, food, saturation, exhaustion, attributes);
  }

  public static PlayerStatsSnapshot empty() {
    return new PlayerStatsSnapshot(0, 0f, 0, 20f, 0f, 20, 5f, 0f, new HashMap<>());
  }
}
