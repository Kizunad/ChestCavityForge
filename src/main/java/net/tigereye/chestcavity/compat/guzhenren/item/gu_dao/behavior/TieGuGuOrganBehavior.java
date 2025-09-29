package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

/**
 * Behaviour for 铁骨蛊 – provides modest absorption and boosts Gu/Jin Dao efficiency.
 */
public final class TieGuGuOrganBehavior extends AbstractMetalBoneSupportBehavior {

    public static final TieGuGuOrganBehavior INSTANCE = new TieGuGuOrganBehavior();

    private static final float ABSORPTION_PER_STACK = 20.0f;
    private static final double ENERGY_COST_PER_STACK = 20.0;
    private static final int ABSORPTION_INTERVAL_TICKS = 20 * 60; // 1 minute
    private static final double GU_DAO_EFFECT = 0.05;
    private static final double JIN_DAO_EFFECT = 0.05;

    private TieGuGuOrganBehavior() {
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
        return "TieGuGu";
    }
}

