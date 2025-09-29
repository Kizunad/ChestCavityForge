package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

/**
 * Behaviour for 精铁骨蛊 – enhanced absorption and efficiency compared to the base iron bone.
 */
public final class JingtieguguOrganBehavior extends AbstractMetalBoneSupportBehavior {

    public static final JingtieguguOrganBehavior INSTANCE = new JingtieguguOrganBehavior();

    private static final float ABSORPTION_PER_STACK = 40.0f;
    private static final double ENERGY_COST_PER_STACK = 40.0;
    private static final int ABSORPTION_INTERVAL_TICKS = 20 * 60; // 1 minute
    private static final double GU_DAO_EFFECT = 0.10;
    private static final double JIN_DAO_EFFECT = 0.10;

    private JingtieguguOrganBehavior() {
    }

    @Override
    protected float absorptionPerStack() {
        return ABSORPTION_PER_STACK;
    }

    @Override
    protected double boneEnergyCostPerStack() {
        return ENERGY_COST_PER_STACK;
    }

    @Override
    protected int absorptionIntervalTicks() {
        return ABSORPTION_INTERVAL_TICKS;
    }

    @Override
    protected double guDaoEffectPerStack() {
        return GU_DAO_EFFECT;
    }

    @Override
    protected double jinDaoEffectPerStack() {
        return JIN_DAO_EFFECT;
    }

    @Override
    protected String stateRootKey() {
        return "Jingtiegugu";
    }
}

