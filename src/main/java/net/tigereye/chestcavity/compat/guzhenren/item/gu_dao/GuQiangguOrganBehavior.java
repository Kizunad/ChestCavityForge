package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;

import java.util.Optional;

/**
 * Gu Qiang Gu (骨枪蛊) consumes bone_growth energy, charging a spear release.
 */
public enum GuQiangguOrganBehavior implements OrganSlowTickListener, OrganOnHitListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bone_growth");
    private static final ResourceLocation DAMAGE_CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bone_damage_increase");
    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = GuQiangguRenderUtil.CHARGE_TAG;
    private static final double ENERGY_PER_CHARGE = 60.0;
    private static final int MAX_CHARGE = 10;

    private static final SoundEvent SOUND_BONE_CRACK = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MOD_ID, "bone_crack"));
    private static final SoundEvent SOUND_ENERGY_HUM = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MOD_ID, "energy_hum"));
    private static final ResourceLocation BLEED_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "lliuxue");

    private static final double BASE_PHYSICAL_DAMAGE = 10.0;
    private static final double MAX_PHYSICAL_DAMAGE = 30.0;
    private static final double DAMAGE_CURVE = 4.0;
    private static final double EFFECT_CURVE = 3.0;
    private static final int MAX_EFFECT_LEVEL = 10;
    private static final int EFFECT_DURATION_TICKS = 30 * 20;

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        LinkageChannel energyChannel = ensureEnergyChannel(cc);
        double available = energyChannel.get();
        LinkageChannel damageChannel = ensureDamageChannel(cc);
        int currentCharge = NBTCharge.getCharge(organ, STATE_KEY);
        if (currentCharge >= MAX_CHARGE) {
            return;
        }

        double energySpent = 0.0;
        int charge = currentCharge;
        while (available - energySpent >= ENERGY_PER_CHARGE && charge < MAX_CHARGE) {
            energySpent += ENERGY_PER_CHARGE;
            charge++;
        }
        if (charge == currentCharge) {
            return;
        }

        double remaining = energyChannel.adjust(-energySpent);
        NBTCharge.setCharge(organ, STATE_KEY, charge);
        damageChannel.set(charge);
        playChargeSounds(player.level(), player);
        sendDebug(player, String.format("charge+=%d (total=%d, energySpent=%.1f, pool=%.1f)", charge - currentCharge, charge, energySpent, remaining));
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ensureEnergyChannel(cc);
        ensureDamageChannel(cc);
    }

    private static LinkageChannel ensureEnergyChannel(ChestCavityInstance cc) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.configureChannel(CHANNEL_ID, channel -> channel.addPolicy(NON_NEGATIVE));
    }

    private static LinkageChannel ensureDamageChannel(ChestCavityInstance cc) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.configureChannel(DAMAGE_CHANNEL_ID, channel -> channel.addPolicy(NON_NEGATIVE));
    }

    private static void playChargeSounds(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SOUND_BONE_CRACK, SoundSource.PLAYERS, 0.8f, 1.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SOUND_ENERGY_HUM, SoundSource.PLAYERS, 0.5f, 0.9f);
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance chestCavity, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        int charge = NBTCharge.getCharge(organ, STATE_KEY);
        if (charge <= 0) {
            return damage;
        }

        int stacks = Math.max(1, organ.getCount());
        LinkageChannel damageChannel = ensureDamageChannel(chestCavity);
        double bonusChannel = damageChannel.get();
        double boneIncrease = charge * stacks + bonusChannel;
        double physicalBonus = computePhysicalBonus(boneIncrease);
        damage += physicalBonus;
        applyBleedEffect(target, boneIncrease);
        NBTCharge.setCharge(organ, STATE_KEY, 0);
        damageChannel.set(0);
        sendDebug(player, String.format("release charge=%d stacks=%d bonusChannel=%.2f -> bonus=%.2f final=%.2f", charge, stacks, bonusChannel, physicalBonus, damage));
        return damage;
    }

    private static double computePhysicalBonus(double boneIncrease) {
        double raw = BASE_PHYSICAL_DAMAGE * boneIncrease;
        double curve = MAX_PHYSICAL_DAMAGE * (1 - Math.exp(-raw / (DAMAGE_CURVE * MAX_PHYSICAL_DAMAGE)));
        return Math.min(MAX_PHYSICAL_DAMAGE, curve);
    }

    private static void applyBleedEffect(LivingEntity target, double boneIncrease) {
        Optional<Holder.Reference<MobEffect>> effectOpt = BuiltInRegistries.MOB_EFFECT.getHolder(BLEED_EFFECT_ID);
        effectOpt.ifPresent(holder -> {
            double scaled = MAX_EFFECT_LEVEL * (1 - Math.exp(-boneIncrease / EFFECT_CURVE));
            int amplifier = Math.max(0, Math.min(MAX_EFFECT_LEVEL - 1, (int)Math.round(scaled) - 1));
            target.addEffect(new MobEffectInstance(holder, EFFECT_DURATION_TICKS, amplifier, false, true, true));
        });
    }

    private static void sendDebug(Player player, String message) {
        player.displayClientMessage(Component.literal("[GuQiang] " + message), true);
    }
}
