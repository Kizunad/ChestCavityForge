package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.init;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordAttributes;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordModelTuning;

/**
 * 初始化定制读取与应用。
 *
 * <p>读取源物品的 CustomData 下的 "cc_flying_sword_init"，示例：
 * {
 *   cc_flying_sword_init: {
 *     attrs: { speedBase: 0.22, damageBase: 6.0 },
 *     model: { item_id: "minecraft:diamond_sword", gecko_model: "mymod:sword/gecko_key" },
 *     sound: { profile: "jade" }
 *   }
 * }
 */
public final class FlyingSwordInit {

  private static final String ROOT = "cc_flying_sword_init";
  private static final String KEY_ATTRS = "attrs";
  private static final String KEY_MODEL = "model";
  private static final String KEY_MODEL_ITEM = "item_id";
  private static final String KEY_MODEL_GECKO = "gecko_model";
  private static final String KEY_SOUND = "sound";
  private static final String KEY_SOUND_PROFILE = "profile";

  private FlyingSwordInit() {}

  public static FlyingSwordInitSpec fromItemStack(ItemStack stack) {
    FlyingSwordInitSpec spec = FlyingSwordInitSpec.empty();
    if (stack == null || stack.isEmpty()) return spec;

    CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
        CustomData.EMPTY).copyTag();
    if (tag == null || !tag.contains(ROOT)) return spec;

    CompoundTag root = tag.getCompound(ROOT);

    // attrs
    if (root.contains(KEY_ATTRS)) {
      CompoundTag attrs = root.getCompound(KEY_ATTRS).copy();
      if (!attrs.isEmpty()) spec.attributesOverride = attrs;
    }

    // model
    if (root.contains(KEY_MODEL)) {
      CompoundTag model = root.getCompound(KEY_MODEL);
      if (model.contains(KEY_MODEL_ITEM)) {
        try {
          ResourceLocation id = ResourceLocation.parse(model.getString(KEY_MODEL_ITEM));
          spec.displayItemId = id;
        } catch (Exception e) {
          ChestCavity.LOGGER.warn("[FlyingSword] Invalid model.item_id: {}", model.getString(KEY_MODEL_ITEM));
        }
      }
      if (model.contains(KEY_MODEL_GECKO)) {
        spec.geckoModelKey = model.getString(KEY_MODEL_GECKO);
      }
    }

    // sound
    if (root.contains(KEY_SOUND)) {
      CompoundTag sound = root.getCompound(KEY_SOUND);
      if (sound.contains(KEY_SOUND_PROFILE)) {
        spec.soundProfile = sound.getString(KEY_SOUND_PROFILE);
      }
    }

    return spec;
  }

  /** 应用初始化定制到实体（仅服务端调用）。 */
  public static void applyTo(Entity entity, FlyingSwordInitSpec spec) {
    if (!(entity instanceof FlyingSwordEntity sword) || spec == null || !spec.hasAny()) return;

    // 1) 属性覆盖（只覆盖存在的字段）
    if (spec.attributesOverride != null && !spec.attributesOverride.isEmpty()) {
      applyAttributeOverrides(sword.getSwordAttributes(), spec.attributesOverride);
    }

    // 2) 模型（物品 ID）
    ResourceLocation id = Optional.ofNullable(spec.displayItemId)
        .orElse(FlyingSwordModelTuning.defaultItemId());
    Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    if (item != null) {
      sword.setDisplayItemStack(new ItemStack(item));
    }

    // 3) Geckolib 模型键（预留）
    if (spec.geckoModelKey != null) {
      sword.setModelKey(spec.geckoModelKey);
    }

    // 4) 音效档（预留）
    if (spec.soundProfile != null) {
      sword.setSoundProfile(spec.soundProfile);
    }
  }

  private static void applyAttributeOverrides(FlyingSwordAttributes attrs, CompoundTag nbt) {
    if (nbt.contains("speedBase")) attrs.speedBase = nbt.getDouble("speedBase");
    if (nbt.contains("speedMax")) attrs.speedMax = nbt.getDouble("speedMax");
    if (nbt.contains("accel")) attrs.accel = nbt.getDouble("accel");
    if (nbt.contains("turnRate")) attrs.turnRate = nbt.getDouble("turnRate");

    if (nbt.contains("damageBase")) attrs.damageBase = nbt.getDouble("damageBase");
    if (nbt.contains("velDmgCoef")) attrs.velDmgCoef = nbt.getDouble("velDmgCoef");

    if (nbt.contains("maxDurability")) attrs.maxDurability = nbt.getDouble("maxDurability");
    if (nbt.contains("duraLossRatio")) attrs.duraLossRatio = nbt.getDouble("duraLossRatio");

    if (nbt.contains("upkeepRate")) attrs.upkeepRate = nbt.getDouble("upkeepRate");

    if (nbt.contains("toolTier")) attrs.toolTier = nbt.getInt("toolTier");
    if (nbt.contains("blockBreakEff")) attrs.blockBreakEff = nbt.getDouble("blockBreakEff");

    if (nbt.contains("hasGlowLayer")) attrs.hasGlowLayer = nbt.getBoolean("hasGlowLayer");
    if (nbt.contains("enableSweep")) attrs.enableSweep = nbt.getBoolean("enableSweep");
    if (nbt.contains("sweepPercent")) attrs.sweepPercent = nbt.getDouble("sweepPercent");
  }
}

