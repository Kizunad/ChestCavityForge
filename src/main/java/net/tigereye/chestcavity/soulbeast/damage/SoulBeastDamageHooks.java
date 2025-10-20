package net.tigereye.chestcavity.soulbeast.damage;

/**
 * @deprecated 迁移至 {@link net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageHooks}
 */
@Deprecated(forRemoval = true)
public final class SoulBeastDamageHooks {

    private SoulBeastDamageHooks() {}

    public static void register(SoulBeastDamageListener listener) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageHooks.register(listener);
    }

    public static void unregister(SoulBeastDamageListener listener) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageHooks.unregister(listener);
    }

    public static double applyHunpoCostModifiers(SoulBeastDamageContext context, double baseHunpoCost) {
        var delegate = new net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageContext(
                context.victim(), context.source(), context.incomingDamage());
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageHooks.applyHunpoCostModifiers(delegate, baseHunpoCost);
    }

    public static float applyPostConversionDamageModifiers(SoulBeastDamageContext context, float baseDamage) {
        var delegate = new net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageContext(
                context.victim(), context.source(), context.incomingDamage());
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageHooks.applyPostConversionDamageModifiers(delegate, baseDamage);
    }
}
