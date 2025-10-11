package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.util.CombatUtil;

/**
 * Behaviour for 龙丸蛐蛐蛊（三转力道，肋骨）。
 */
public final class LongWanQuQuGuOrganBehavior extends AbstractLiDaoOrganBehavior implements OrganIncomingDamageListener {

    public static final LongWanQuQuGuOrganBehavior INSTANCE = new LongWanQuQuGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "long_wan_qu_qu_gu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final String STATE_ROOT = "LongWanQuQuGu";
    private static final String ACTIVE_KEY = "Active";
    private static final String CHARGES_KEY = "Charges";
    private static final String NEXT_READY_TICK_KEY = "NextReadyTick";
    private static final String INVULN_EXPIRE_TICK_KEY = "InvulnExpireTick";

    private static final int MAX_CHARGES = 3;
    private static final long COOLDOWN_TICKS = 30L * 20L; // 30 seconds
    private static final long INVULN_WINDOW_TICKS = 10L; // half a second
    private static final float MIN_DODGE_DISTANCE = 0.9f;
    private static final float MAX_DODGE_DISTANCE = 1.4f;
    private static final float DODGE_YAW_RANGE = 100.0f;

    static {
        OrganActivationListeners.register(ABILITY_ID, LongWanQuQuGuOrganBehavior::activateAbility);
    }

    private LongWanQuQuGuOrganBehavior() {
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player) || victim.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return damage;
        }

        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.Entry invulnExpireEntry = cooldown.entry(INVULN_EXPIRE_TICK_KEY);
        long gameTime = victim.level().getGameTime();

        long invulnExpire = invulnExpireEntry.getReadyTick();
        if (invulnExpire > gameTime) {
            return 0.0f;
        }

        if (!state.getBoolean(ACTIVE_KEY, false)) {
            return damage;
        }

        int charges = Math.max(0, state.getInt(CHARGES_KEY, 0));
        if (charges <= 0) {
            boolean dirty = OrganStateOps.setBoolean(state, cc, organ, ACTIVE_KEY, false, false).changed();
            if (dirty) {
                sendSlotUpdate(cc, organ);
            }
            return damage;
        }

        LivingEntity attacker = source != null && source.getEntity() instanceof LivingEntity living ? living : null;
        boolean dodged = CombatUtil.performShortDodge(
                victim,
                attacker,
                MIN_DODGE_DISTANCE,
                MAX_DODGE_DISTANCE,
                DODGE_YAW_RANGE,
                SoundEvents.SHULKER_TELEPORT,
                ParticleTypes.CLOUD
        );
        if (!dodged) {
            return damage;
        }

        boolean dirty = false;
        int remaining = Math.max(0, charges - 1);
        dirty |= OrganStateOps.setInt(state, cc, organ, CHARGES_KEY, remaining, value -> Math.max(0, Math.min(value, MAX_CHARGES)), 0).changed();
        invulnExpireEntry.setReadyAt(gameTime + INVULN_WINDOW_TICKS);
        if (remaining <= 0) {
            dirty |= OrganStateOps.setBoolean(state, cc, organ, ACTIVE_KEY, false, false).changed();
        }
        if (dirty) {
            sendSlotUpdate(cc, organ);
        }
        victim.invulnerableTime = Math.max(victim.invulnerableTime, (int) INVULN_WINDOW_TICKS);
        return 0.0f;
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.Entry nextReadyEntry = cooldown.entry(NEXT_READY_TICK_KEY);
        MultiCooldown.Entry invulnExpireEntry = cooldown.entry(INVULN_EXPIRE_TICK_KEY);
        long gameTime = player.level().getGameTime();
        long nextReady = Math.max(0L, nextReadyEntry.getReadyTick());
        if (nextReady > gameTime) {
            return;
        }
        if (state.getBoolean(ACTIVE_KEY, false) && state.getInt(CHARGES_KEY, 0) > 0) {
            return;
        }

        boolean dirty = false;
        dirty |= OrganStateOps.setBoolean(state, cc, organ, ACTIVE_KEY, true, false).changed();
        dirty |= OrganStateOps.setInt(state, cc, organ, CHARGES_KEY, MAX_CHARGES, value -> Math.max(0, Math.min(value, MAX_CHARGES)), 0).changed();
        nextReadyEntry.setReadyAt(gameTime + COOLDOWN_TICKS);
        invulnExpireEntry.setReadyAt(0L);
        if (dirty) {
            INSTANCE.sendSlotUpdate(cc, organ);
        }

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SHIELD_BLOCK,
                player.getSoundSource(),
                0.8f,
                1.1f
        );

        // Cooldown toast at end of recharge window
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            long now = gameTime;
            nextReadyEntry.onReady(sp.serverLevel(), now, () -> {
                try {
                    var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(organ.getItem());
                    var payload = new net.tigereye.chestcavity.network.packets.CooldownReadyToastPayload(
                            true,
                            itemId,
                            "技能就绪",
                            organ.getHoverName().getString()
                    );
                    net.tigereye.chestcavity.network.NetworkHandler.sendCooldownToast(sp, payload);
                } catch (Throwable ignored) { }
            });
        }
    }

    private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
        MultiCooldown.Builder builder = MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
                .withLongClamp(value -> Math.max(0L, value), 0L);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else {
            builder.withOrgan(organ);
        }
        return builder.build();
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
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
