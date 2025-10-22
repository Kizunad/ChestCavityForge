package net.tigereye.chestcavity.soulbeast.damage;

/**
 * @deprecated 迁移至 {@link
 *     net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageListener}
 */
@Deprecated(forRemoval = true)
public interface SoulBeastDamageListener
    extends net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage
        .SoulBeastDamageListener {

  /** 过渡签名，兼容旧包调用。 */
  default double modifyHunpoCost(SoulBeastDamageContext context, double currentHunpoCost) {
    return currentHunpoCost;
  }

  /** 过渡签名，兼容旧包调用。 */
  default float modifyPostConversionDamage(SoulBeastDamageContext context, float currentDamage) {
    return currentDamage;
  }

  @Override
  default double modifyHunpoCost(
      net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageContext
          context,
      double currentHunpoCost) {
    return modifyHunpoCost(
        new SoulBeastDamageContext(context.victim(), context.source(), context.incomingDamage()),
        currentHunpoCost);
  }

  @Override
  default float modifyPostConversionDamage(
      net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageContext
          context,
      float currentDamage) {
    return modifyPostConversionDamage(
        new SoulBeastDamageContext(context.victim(), context.source(), context.incomingDamage()),
        currentDamage);
  }
}
