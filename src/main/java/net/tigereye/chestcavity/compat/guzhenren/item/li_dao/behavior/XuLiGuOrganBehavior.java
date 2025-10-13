package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.LiDaoConstants;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.LiDaoHelper;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;

/**
 * Behaviour for 蓄力蛊（三转力道，肋骨）。
 */
public final class XuLiGuOrganBehavior extends AbstractLiDaoOrganBehavior implements OrganOnHitListener {

    public static final XuLiGuOrganBehavior INSTANCE = new XuLiGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xu_li_gu");

    private static final double BASE_TRIGGER_CHANCE = 0.12;
    private static final double BONUS_PER_MUSCLE_GROUP = 0.005;
    private static final int MUSCLES_PER_GROUP = BehaviorConfigAccess.getInt(XuLiGuOrganBehavior.class, "MUSCLES_PER_GROUP", 16);
    private static final double MAX_TRIGGER_CHANCE = 0.23;
    private static final double DAMAGE_MULTIPLIER_BASE = 1.4;

    private XuLiGuOrganBehavior() {
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ActiveLinkageContext context = linkageContext(cc);
        ensureChannel(context, LiDaoConstants.LI_DAO_INCREASE_EFFECT);
    }

    @Override
    public float onHit(
            DamageSource source,
            LivingEntity attacker,
            LivingEntity target,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return damage;
        }
        if (damage <= 0.0f) {
            return damage;
        }
        if (target == null || !target.isAlive() || target == attacker || target.isAlliedTo(attacker)) {
            return damage;
        }
        if (source == null || source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() != attacker) {
            return damage;
        }

        double triggerChance = computeTriggerChance(cc);
        if (triggerChance <= 0.0) {
            return damage;
        }

        RandomSource random = player.getRandom();
        if (random.nextDouble() >= triggerChance) {
            return damage;
        }

        double liDaoIncrease = Math.max(0.0, liDaoIncrease(cc));
        double multiplier = DAMAGE_MULTIPLIER_BASE * (1.0 + liDaoIncrease);
        if (multiplier <= 0.0) {
            return damage;
        }

        return (float) (damage * multiplier);
    }

    private double computeTriggerChance(ChestCavityInstance cc) {
        int muscles = countMuscleStacks(cc);
        int groups = muscles / MUSCLES_PER_GROUP;
        double bonus = groups * BONUS_PER_MUSCLE_GROUP;
        double chance = BASE_TRIGGER_CHANCE + bonus;
        return Mth.clamp(chance, 0.0, MAX_TRIGGER_CHANCE);
    }

    private int countMuscleStacks(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return 0;
        }
        int total = 0;
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (LiDaoHelper.isMuscle(stack)) {
                total += 1;
            }
        }
        return total;
    }

    private boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack slotStack = cc.inventory.getItem(i);
            if (slotStack == null || slotStack.isEmpty()) {
                continue;
            }
            if (!matchesOrgan(slotStack, ORGAN_ID)) {
                continue;
            }
            return slotStack == organ;
        }
        return false;
    }
}
