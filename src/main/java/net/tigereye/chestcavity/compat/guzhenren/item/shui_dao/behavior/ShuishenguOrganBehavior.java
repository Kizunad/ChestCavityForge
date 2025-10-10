package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;


import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.listeners.damage.IncomingDamageShield;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.util.NBTCharge;

/**
 * Water Path organ: Shuishengu (水肾蛊) — handles shield charging and FX.
 */
public enum ShuishenguOrganBehavior implements OrganOnGroundListener, OrganSlowTickListener, OrganIncomingDamageListener, IncreaseEffectContributor {
    INSTANCE;

    private static final double BASE_ZHENYUAN_COST = 80.0;
    private static final Component READY_MESSAGE = Component.translatable("message.chestcavity.shuishengu.ready");

    private static final IncomingDamageShield SHIELD = ShuishenguShield.INSTANCE;

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // slow-tick only behaviour
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity.level().isClientSide() || !entity.isInWater()) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        int effectiveMaxCharge = ShuishenguShield.getEffectiveMaxCharge(stackCount);
        int current = Math.min(NBTCharge.getCharge(organ, ShuishenguShield.STATE_KEY), effectiveMaxCharge);
        if (current >= effectiveMaxCharge) {
            return;
        }

        ConsumptionResult payment;
        if (entity instanceof Player) {
            payment = ResourceOps.consumeStrict(entity, BASE_ZHENYUAN_COST * stackCount, 0.0);
        } else {
            payment = ResourceOps.consumeWithFallback(entity, BASE_ZHENYUAN_COST * stackCount, 0.0);
        }
        if (!payment.succeeded()) {
            return;
        }

        int updated = Math.min(effectiveMaxCharge, current + stackCount);
        ShuishenguShield.setCharge(organ, updated, stackCount);
        ShuishenguShield.broadcastChargeRatio(cc, effectiveMaxCharge, updated);

        if (updated > current) {
            playSound(entity, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 0.35f, 1.0f);
            spawnChargingParticles(entity, stackCount);
            if (entity instanceof Player player) {
                applyChargingEffect(player);
            }
        }

        if (updated == effectiveMaxCharge) {
            if (entity instanceof Player player) {
                player.displayClientMessage(READY_MESSAGE, true);
            }
            playSound(entity, SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.2f);
            spawnFullChargeBurst(entity, stackCount);
        }
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        IncomingDamageShield.ShieldResult result = SHIELD.absorb(
                new IncomingDamageShield.ShieldContext(source, victim, cc, organ, damage, victim.level().getGameTime())
        );
        return result.remainingDamage();
    }

    private static void applyChargingEffect(Player player) {
        MobEffectInstance existing = player.getEffect(MobEffects.CONDUIT_POWER);
        int amplifier = 0;
        int duration = 60;
        if (existing == null || existing.getDuration() <= 30) {
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, duration, amplifier, false, false, true));
        }
    }

    private static void playSound(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void spawnChargingParticles(LivingEntity entity, int stackCount) {
        if (!(entity.level() instanceof ServerLevel server)) {
            return;
        }
        double radius = 0.4 + 0.2 * (stackCount - 1);
        for (int i = 0; i < 3 * stackCount; i++) {
            double angle = entity.getRandom().nextDouble() * Math.PI * 2;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double baseX = entity.getX() + offsetX;
            double baseY = entity.getY() + 0.1 + entity.getRandom().nextDouble() * 0.6;
            double baseZ = entity.getZ() + offsetZ;
            server.sendParticles(ParticleTypes.BUBBLE, baseX, baseY, baseZ, 1, 0.0, 0.05, 0.0, 0.0);
            server.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, baseX, baseY, baseZ, 1, 0.0, 0.04, 0.0, 0.01);
            if (entity.getRandom().nextBoolean()) {
                server.sendParticles(ParticleTypes.BUBBLE_POP, baseX, baseY + 0.2, baseZ, 1, 0.0, 0.05, 0.0, 0.0);
            }
        }
    }

    private static void spawnFullChargeBurst(LivingEntity entity, int stackCount) {
        if (!(entity.level() instanceof ServerLevel server)) {
            return;
        }
        double radius = 0.6 + 0.3 * (stackCount - 1);
        int segments = 16 + stackCount * 4;
        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            double x = entity.getX() + Math.cos(angle) * radius;
            double y = entity.getY() + entity.getBbHeight() * 0.5;
            double z = entity.getZ() + Math.sin(angle) * radius;
            server.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.03, 0.0, 0.0);
            if (i % 2 == 0) {
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.1, z, 1, 0.0, 0.02, 0.0, 0.0);
            }
        }
    }

    @Override
    public void rebuildIncreaseEffects(ChestCavityInstance cc, ActiveLinkageContext context, ItemStack organ, IncreaseEffectLedger.Registrar registrar) {
        // No INCREASE effect contribution.
    }
}
