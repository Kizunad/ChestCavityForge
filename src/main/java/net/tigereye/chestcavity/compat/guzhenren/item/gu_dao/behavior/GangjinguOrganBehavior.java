package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.SteelBoneComboHelper;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * Behaviour for 钢筋蛊：hunger-gated absorption, jingli restoration and combo-driven buffs.
 */
public final class GangjinguOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener, OrganOnHitListener {

    public static final GangjinguOrganBehavior INSTANCE = new GangjinguOrganBehavior();

    private static final String STATE_ROOT = "Gangjingu";
    private static final String ABSORPTION_KEY = "LastAbsorptionTick";
    private static final int ABSORPTION_INTERVAL_TICKS = 20 * 120; // 2 minutes
    private static final float ABSORPTION_PER_STACK = 60.0f;
    private static final double JINGLI_PER_SECOND = 1.0;
    private static final double BONUS_DAMAGE_CHANCE = 0.15;
    private static final double BONUS_DAMAGE_RATIO = 0.08;
    private static final int EFFECT_DURATION_TICKS = 60;
    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final ResourceLocation GU_DAO_CHANNEL =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/gu_dao_increase_effect");
    private static final ResourceLocation JIN_DAO_CHANNEL =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/jin_dao_increase_effect");
    private static final ResourceLocation BONE_GROWTH_CHANNEL =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/bone_growth");

    private GangjinguOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (!SteelBoneComboHelper.isPrimarySteelOrgan(cc, organ)) {
            return;
        }

        SteelBoneComboHelper.ComboState comboState = SteelBoneComboHelper.analyse(cc);
        if (!SteelBoneComboHelper.hasSteel(comboState)) {
            return;
        }

        if (!SteelBoneComboHelper.consumeMaintenanceHunger(player)) {
            ChestCavity.LOGGER.debug("[compat/guzhenren] Gangjingu skipped: insufficient hunger for {}", describeStack(organ));
            return;
        }

        int steelStacks = Math.max(1, comboState.steel());
        applyAbsorption(player, organ, steelStacks);

        SteelBoneComboHelper.restoreJingli(player, JINGLI_PER_SECOND * steelStacks);

        if (SteelBoneComboHelper.hasActiveCombo(comboState)) {
            applyResistance(player, cc);
        }
        if (SteelBoneComboHelper.hasRefinedCombo(comboState)) {
            applyHaste(player, cc);
        }
    }

    private void applyAbsorption(Player player, ItemStack organ, int steelStacks) {
        OrganState state = organState(organ, STATE_ROOT);
        long gameTime = player.level().getGameTime();
        long lastTick = state.getLong(ABSORPTION_KEY, Long.MIN_VALUE);
        if (gameTime - lastTick < ABSORPTION_INTERVAL_TICKS) {
            return;
        }
        float required = Math.max(0.0f, ABSORPTION_PER_STACK * steelStacks);
        if (required <= 0.0f) {
            state.setLong(ABSORPTION_KEY, gameTime);
            return;
        }
        if (player.getAbsorptionAmount() + 1.0E-3f < required) {
            player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), required));
            ChestCavity.LOGGER.debug(
                    "[compat/guzhenren] Gangjingu applied absorption {} (stacks={})",
                    String.format(java.util.Locale.ROOT, "%.1f", required),
                    steelStacks
            );
        }
        state.setLong(ABSORPTION_KEY, gameTime);
    }

    private void applyResistance(Player player, ChestCavityInstance cc) {
        double increase = Math.max(0.0, SteelBoneComboHelper.guDaoIncrease(cc));
        int amplifier = Math.max(0, (int) Math.round(increase));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, amplifier, true, true));
    }

    private void applyHaste(Player player, ChestCavityInstance cc) {
        double jinIncrease = Math.max(0.0, SteelBoneComboHelper.jinDaoIncrease(cc));
        int amplifier = Math.max(0, (int) Math.round(jinIncrease));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, EFFECT_DURATION_TICKS, amplifier, true, true));
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (!SteelBoneComboHelper.isPrimarySteelOrgan(cc, organ)) {
            return damage;
        }
        if (target == null || source.getDirectEntity() != attacker) {
            return damage;
        }
        if (attacker.distanceToSqr(target) > 100.0) {
            return damage;
        }

        RandomSource random = attacker.getRandom();
        if (random.nextDouble() >= BONUS_DAMAGE_CHANCE) {
            return damage;
        }

        double jinIncrease = Math.max(0.0, SteelBoneComboHelper.jinDaoIncrease(cc));
        double bonus = damage * BONUS_DAMAGE_RATIO * (1.0 + jinIncrease);
        if (bonus <= 0.0) {
            return damage;
        }

        float result = (float) (damage + bonus);
        ChestCavity.LOGGER.debug(
                "[compat/guzhenren] Gangjingu bonus damage +{}/{} (jinIncrease={})",
                String.format(java.util.Locale.ROOT, "%.2f", bonus),
                String.format(java.util.Locale.ROOT, "%.2f", result),
                String.format(java.util.Locale.ROOT, "%.3f", jinIncrease)
        );
        if (attacker.level() instanceof ServerLevel server) {
            Vec3 impact = target.position().add(0.0, target.getBbHeight() * 0.4, 0.0);
            SteelBoneComboHelper.spawnImpactFx(server, target, impact);
        }
        return result;
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        ensureChannel(context, GU_DAO_CHANNEL).addPolicy(NON_NEGATIVE);
        ensureChannel(context, JIN_DAO_CHANNEL).addPolicy(NON_NEGATIVE);
        ensureChannel(context, BONE_GROWTH_CHANNEL).addPolicy(NON_NEGATIVE);
    }
}

