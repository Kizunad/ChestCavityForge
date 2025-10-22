package net.tigereye.chestcavity.mob_effect;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.tigereye.chestcavity.compat.guzhenren.util.SoulBeastIntimidationGoalManager;
import org.slf4j.Logger;

/**
 * Mob effect applied to entities that should temporarily flee from the soul beast intimidator.
 * Stores the intimidator's UUID on the affected entity and injects a flee goal while active.
 */
public class SoulBeastIntimidatedEffect extends CCStatusEffect {

  private static final String INTIMIDATOR_TAG = "ChestCavitySoulBeastIntimidator";
  private static final Logger LOGGER = LogUtils.getLogger();

  public SoulBeastIntimidatedEffect() {
    super(MobEffectCategory.HARMFUL, 0x4b2f70);
  }

  public static void assignIntimidator(LivingEntity entity, UUID intimidatorId) {
    if (entity == null || intimidatorId == null) {
      return;
    }
    CompoundTag data = entity.getPersistentData();
    data.putUUID(INTIMIDATOR_TAG, intimidatorId);
  }

  public static void handleEffectAdded(LivingEntity entity) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (entity instanceof Mob mob) {
      SoulBeastIntimidationGoalManager.ensureFleeGoal(mob);
    }
    LOGGER.info("[Intimidation] effect added on {}", entity.getName().getString());
  }

  public static void handleEffectRemoved(LivingEntity entity) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (entity instanceof Mob mob) {
      SoulBeastIntimidationGoalManager.clearFleeGoal(mob);
    }
    clearIntimidator(entity);
    LOGGER.info("[Intimidation] effect removed from {}", entity.getName().getString());
  }

  public static void clearIntimidator(LivingEntity entity) {
    if (entity == null) {
      return;
    }
    CompoundTag data = entity.getPersistentData();
    if (data.contains(INTIMIDATOR_TAG)) {
      data.remove(INTIMIDATOR_TAG);
    }
  }

  public static Optional<UUID> getIntimidator(LivingEntity entity) {
    if (entity == null) {
      return Optional.empty();
    }
    CompoundTag data = entity.getPersistentData();
    if (!data.hasUUID(INTIMIDATOR_TAG)) {
      return Optional.empty();
    }
    try {
      return Optional.of(data.getUUID(INTIMIDATOR_TAG));
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }
}
