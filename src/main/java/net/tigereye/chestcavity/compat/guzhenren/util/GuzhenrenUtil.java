package net.tigereye.chestcavity.compat.guzhenren.util;

/**
 * General utility methods for the Guzhenren compatibility module.
 */
public final class GuzhenrenUtil {
    private GuzhenrenUtil() {}

    /**
     * Calculates the bonus from dao hen.
     *
     * @param daoHen The amount of dao hen.
     * @return The calculated bonus.
     */
    public static double getDaoHenBonus(double daoHen) {
        return daoHen / 100.0;
    }

    /**
     * Calculates the total bonus from flow experience.
     *
     * @param flowExperience The amount of flow experience.
     * @return The calculated bonus.
     */
    public static double getFlowExperienceTotalBonus(double flowExperience) {
        if (flowExperience <= 0) {
            return 0;
        }
        if (flowExperience >= 10001) {
            return 0.5;
        }
        return (flowExperience / 10001) * 0.5;
    }
}
