package net.tigereye.chestcavity.compat.guzhenren.combat;

import net.minecraft.world.damagesource.DamageSource;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Utility helpers for marking soul-dao melee attacks.
 */
public final class HunDaoDamageUtil {

    private static final Set<DamageSource> MARKED_SOURCES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private HunDaoDamageUtil() {
    }

    public static void markHunDaoAttack(@Nullable DamageSource source) {
        if (source != null) {
            MARKED_SOURCES.add(source);
        }
    }

    public static boolean isHunDao(@Nullable DamageSource source) {
        return source != null && MARKED_SOURCES.contains(source);
    }
}
