package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common;

import net.minecraft.nbt.CompoundTag;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenUtil;

/**
 * Common logic for the Shou Pi skill combo, providing methods for buffing and cooldown calculations.
 */
public final class ShouPiComboLogic {
  private ShouPiComboLogic() {}

  /**
   * A snapshot of a player's Bian Hua Dao attributes at a specific moment.
   *
   * @param daoHen The player's Bian Hua Dao marks.
   * @param flowExperience The player's Bian Hua Dao flow experience.
   */
  public record BianHuaDaoSnapshot(double daoHen, double flowExperience) {
    public static final BianHuaDaoSnapshot EMPTY = new BianHuaDaoSnapshot(0, 0);

    /**
     * Serializes this snapshot to the given NBT tag.
     *
     * @param tag The NBT tag to write to.
     */
    public void writeToNBT(CompoundTag tag) {
      tag.putDouble("daoHen", daoHen);
      tag.putDouble("flowExperience", flowExperience);
    }

    /**
     * Deserializes a snapshot from the given NBT tag.
     *
     * @param tag The NBT tag to read from.
     * @return The deserialized snapshot.
     */
    public static BianHuaDaoSnapshot fromNBT(CompoundTag tag) {
      return new BianHuaDaoSnapshot(
          tag.getDouble("daoHen"), tag.getDouble("flowExperience"));
    }
  }

  /**
   * Applies a buff to a value based on the number of dao hen.
   *
   * @param value The base value to be buffed.
   * @param daoHen The amount of dao hen.
   * @return The buffed value.
   */
  public static double applyDaoHenBuff(double value, double daoHen) {
    return value * (1 + GuzhenrenUtil.getDaoHenBonus(daoHen));
  }

  /**
   * Calculates the cooldown, reduced by flow experience.
   *
   * @param baseCooldown The base cooldown in ticks.
   * @param flowExperience The amount of flow experience.
   * @return The calculated cooldown, with a minimum of 20 ticks.
   */
  public static long computeCooldown(long baseCooldown, double flowExperience) {
    double reduction =
        1 - (GuzhenrenUtil.getFlowExperienceTotalBonus(flowExperience));
    return Math.max(20, (long) (baseCooldown * reduction));
  }
}