package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

import net.minecraft.util.Mth;

public class CooldownOps {

    public static final int MAX_EXP = 10001;
    public static final int MIN_COOLDOWN_TICKS = 20;

    /**
     * Calculates the reduced cooldown for 'bing_xue_dao' abilities based on school experience.
     * The cooldown is reduced linearly from the base value down to a minimum floor.
     *
     * @param baseTicks The base cooldown in ticks.
     * @param exp       The 'liupai_bingxuedao' experience, clamped between [0, 10001].
     * @param floor     The minimum cooldown in ticks, defaulting to 20.
     * @return The calculated cooldown in ticks, never falling below the floor.
     */
    public static long withBingXueExp(long baseTicks, int exp, long floor) {
        if (baseTicks <= floor) {
            return baseTicks;
        }
        int clampedExp = Mth.clamp(exp, 0, MAX_EXP);
        double progress = clampedExp / (double) MAX_EXP;
        long reduction = Math.round((baseTicks - floor) * progress);
        return Math.max(floor, baseTicks - reduction);
    }

    /**
     * Overloaded method that uses the default minimum cooldown of 20 ticks.
     *
     * @param baseTicks The base cooldown in ticks.
     * @param exp       The 'liupai_bingxuedao' experience.
     * @return The calculated cooldown in ticks.
     */
    public static long withBingXueExp(long baseTicks, int exp) {
        return withBingXueExp(baseTicks, exp, MIN_COOLDOWN_TICKS);
    }
}
