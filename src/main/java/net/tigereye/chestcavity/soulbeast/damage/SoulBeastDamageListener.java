package net.tigereye.chestcavity.soulbeast.damage;

/**
 * Listener used to adjust Soul Beast damage conversion.
 */
public interface SoulBeastDamageListener {

    /**
     * Allows listeners to scale the hunpo cost that will be deducted for this hit.
     *
     * @param context          immutable damage snapshot
     * @param currentHunpoCost current hunpo cost after previous listeners
     * @return new hunpo cost to apply
     */
    default double modifyHunpoCost(SoulBeastDamageContext context, double currentHunpoCost) {
        return currentHunpoCost;
    }

    /**
     * Allows listeners to adjust the remaining health damage after hunpo has been consumed.
     *
     * @param context        immutable damage snapshot
     * @param currentDamage  current remaining damage after previous listeners
     * @return new damage value to forward to vanilla processing
     */
    default float modifyPostConversionDamage(SoulBeastDamageContext context, float currentDamage) {
        return currentDamage;
    }
}
