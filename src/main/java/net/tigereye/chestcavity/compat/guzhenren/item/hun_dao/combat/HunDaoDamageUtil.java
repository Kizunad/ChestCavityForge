package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.combat;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;

/** Utility helpers for marking soul-dao melee attacks. */
public final class HunDaoDamageUtil {

  private static final Set<DamageSource> MARKED_SOURCES =
      Collections.newSetFromMap(new WeakHashMap<>());

  private HunDaoDamageUtil() {}

  /**
   * Marks a damage source as originating from a Hun Dao attack.
   *
   * @param source the damage source to mark; may be null
   */
  public static void markHunDaoAttack(@Nullable DamageSource source) {
    if (source != null) {
      MARKED_SOURCES.add(source);
    }
  }

  /**
   * Returns whether the given damage source was marked as Hun Dao damage.
   *
   * @param source the damage source to check; may be null
   * @return true if the source is marked as Hun Dao damage
   */
  public static boolean isHunDao(@Nullable DamageSource source) {
    return source != null && MARKED_SOURCES.contains(source);
  }
}
