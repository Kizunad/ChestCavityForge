package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

/**
 * Behaviour for 精铁骨蛊 – enhanced absorption and efficiency compared to the base iron bone.
 * <p>
 * The refined plating follows the same absorption contract: once the minute-long
 * interval elapses it spends steel-combo bone energy to raise absorption only when
 * the entity is below the per-stack target. It never “instantly refill” absorption
 * every tick, ensuring the shield drains meaningfully during combat while the combo
 * upkeep remains predictable. Direct healing is still delegated to
 * {@link net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.SteelBoneComboHealing}.
 */
public final class JingtieguguOrganBehavior extends AbstractMetalBoneSupportBehavior {

    public static final JingtieguguOrganBehavior INSTANCE = new JingtieguguOrganBehavior();

    public static final float ABSORPTION_PER_STACK = 40.0f;
    private static final double ENERGY_COST_PER_STACK = 40.0;
    private static final int ABSORPTION_INTERVAL_TICKS = 20 * 45; // 1 minute
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
