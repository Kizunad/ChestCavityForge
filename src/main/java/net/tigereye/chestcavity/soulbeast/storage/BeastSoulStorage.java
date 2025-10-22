package net.tigereye.chestcavity.soulbeast.storage;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * @deprecated 迁移至 {@link
 *     net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage.BeastSoulStorage}
 */
@Deprecated(forRemoval = true)
public interface BeastSoulStorage {

  /** 判断承载物是否已有存储的兽魂。 */
  boolean hasStoredSoul(ItemStack organ);

  default boolean canStore(ItemStack organ, LivingEntity entity) {
    return organ != null
        && !organ.isEmpty()
        && entity != null
        && !(entity instanceof net.minecraft.world.entity.player.Player)
        && !hasStoredSoul(organ);
  }

  Optional<BeastSoulRecord> store(ItemStack organ, LivingEntity entity, long storedGameTime);

  Optional<BeastSoulRecord> peek(ItemStack organ);

  Optional<BeastSoulRecord> consume(ItemStack organ);

  void clear(ItemStack organ);
}
