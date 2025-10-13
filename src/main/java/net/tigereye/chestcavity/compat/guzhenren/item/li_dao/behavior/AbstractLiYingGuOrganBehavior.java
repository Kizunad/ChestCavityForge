package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.JianYingGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.LiDaoConstants;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.registration.CCSoundEvents;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Shared behaviour for 力影类（黑/白豕蛊）肌肉器官。
 */
abstract class AbstractLiYingGuOrganBehavior extends AbstractLiDaoOrganBehavior
        implements OrganSlowTickListener, OrganOnHitListener {

    private static final double TRIGGER_CHANCE = 1;
    private static final float BASE_CLONE_DAMAGE = BehaviorConfigAccess.getFloat(AbstractLiYingGuOrganBehavior.class, "BASE_CLONE_DAMAGE", 10.0f);
    private static final double DAMAGE_RATIO = 0.10;
    private static final long COOLDOWN_TICKS = 20L * 20L; // 20 seconds
    private static final long REGEN_INTERVAL_TICKS = 3L * 20L; // 3 seconds
    private static final double JINGLI_PER_TICK = 1.0;
    private static final String LAST_REGEN_TICK_KEY = "LastRegenTick";
    private static final String NEXT_READY_TICK_KEY = "NextReadyTick";

    private final ResourceLocation organId;
    private final String stateRoot;

    protected AbstractLiYingGuOrganBehavior(
            ResourceLocation organId,
            String stateRoot
    ) {
        this.organId = Objects.requireNonNull(organId, "organId");
        this.stateRoot = Objects.requireNonNull(stateRoot, "stateRoot");
    }

    public ResourceLocation organId() {
        return organId;
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ActiveLinkageContext context = linkageContext(cc);
        ensureChannel(context, LiDaoConstants.LI_DAO_INCREASE_EFFECT);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, organId)) {
            return;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return;
        }

        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.Entry lastRegenEntry = cooldown.entry(LAST_REGEN_TICK_KEY);
        long gameTime = entity.level().getGameTime();
        long lastRegenTick = Math.max(0L, lastRegenEntry.getReadyTick());
        if (gameTime < lastRegenTick) {
            lastRegenTick = 0L;
        }
        if (gameTime - lastRegenTick < REGEN_INTERVAL_TICKS) {
            return;
        }

        OptionalDouble result = ResourceOps.tryAdjustJingli(player, JINGLI_PER_TICK, true);
        if (result.isEmpty()) {
            return;
        }

        lastRegenEntry.setReadyAt(gameTime);
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
        if (!matchesOrgan(organ, organId)) {
            return damage;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return damage;
        }
        if (target == null || !target.isAlive() || target == attacker || target.isAlliedTo(attacker)) {
            return damage;
        }
        if (source == null || source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() != attacker) {
            return damage;
        }
        if (!(attacker.level() instanceof ServerLevel server)) {
            return damage;
        }

        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.Entry nextReadyEntry = cooldown.entry(NEXT_READY_TICK_KEY);
        long gameTime = server.getGameTime();
        long nextReadyTick = Math.max(0L, nextReadyEntry.getReadyTick());
        if (gameTime < nextReadyTick) {
            return damage;
        }

        RandomSource random = player.getRandom();
        if (random.nextDouble() >= TRIGGER_CHANCE) {
            return damage;
        }

        double liDaoIncrease = Math.max(0.0, liDaoIncrease(cc));
        float baseDamage = Math.max(0.0f, damage);
        float cloneDamage = (float) ((BASE_CLONE_DAMAGE + baseDamage * DAMAGE_RATIO) * (1.0 + liDaoIncrease));
        if (cloneDamage <= 0.0f) {
            return damage;
        }

        JianYingGuOrganBehavior.applyTrueDamage(player, target, cloneDamage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, true, true));

        server.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                punchSound(),
                SoundSource.PLAYERS,
                0.9f,
                1.0f + (random.nextFloat() - 0.5f) * 0.25f
        );

        nextReadyEntry.setReadyAt(gameTime + COOLDOWN_TICKS);

        return damage;
    }

    protected MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
        MultiCooldown.Builder builder = MultiCooldown.builder(OrganState.of(organ, stateRoot))
                .withLongClamp(value -> Math.max(0L, value), 0L);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else {
            builder.withOrgan(organ);
        }
        return builder.build();
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
            if (!matchesOrgan(slotStack, organId)) {
                continue;
            }
            return slotStack == organ;
        }
        return false;
    }

    private static SoundEvent punchSound() {
        return CCSoundEvents.CUSTOM_FIGHT_PUNCH.get();
    }
}
