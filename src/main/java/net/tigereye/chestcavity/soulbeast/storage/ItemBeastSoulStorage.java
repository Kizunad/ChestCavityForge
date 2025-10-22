package net.tigereye.chestcavity.soulbeast.storage;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * @deprecated 迁移至 {@link
 *     net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage.ItemBeastSoulStorage}
 */
@Deprecated(forRemoval = true)
public final class ItemBeastSoulStorage implements BeastSoulStorage {

  private final net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage
          .ItemBeastSoulStorage
      delegate;

  public ItemBeastSoulStorage() {
    this.delegate =
        new net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage
            .ItemBeastSoulStorage();
  }

  public ItemBeastSoulStorage(String rootKey) {
    this.delegate =
        new net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage
            .ItemBeastSoulStorage(rootKey);
  }

  @Override
  public boolean hasStoredSoul(ItemStack organ) {
    return delegate.hasStoredSoul(organ);
  }

  @Override
  public boolean canStore(ItemStack organ, LivingEntity entity) {
    return delegate.canStore(organ, entity);
  }

  @Override
  public Optional<BeastSoulRecord> store(
      ItemStack organ, LivingEntity entity, long storedGameTime) {
    return delegate.store(organ, entity, storedGameTime).map(BeastSoulRecord::fromCompat);
  }

  @Override
  public Optional<BeastSoulRecord> peek(ItemStack organ) {
    return delegate.peek(organ).map(BeastSoulRecord::fromCompat);
  }

  @Override
  public Optional<BeastSoulRecord> consume(ItemStack organ) {
    return delegate.consume(organ).map(BeastSoulRecord::fromCompat);
  }

  @Override
  public void clear(ItemStack organ) {
    delegate.clear(organ);
  }
}
