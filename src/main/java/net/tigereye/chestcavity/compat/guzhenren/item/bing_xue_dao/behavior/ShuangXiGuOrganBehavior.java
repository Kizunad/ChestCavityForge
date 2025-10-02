package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.DamageOverTimeHelper;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Behaviour for 霜息蛊 (Shuang Xi Gu).
 */
public final class ShuangXiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganOnGroundListener, OrganRemovalListener, IncreaseEffectContributor {

    public static final ShuangXiGuOrganBehavior INSTANCE = new ShuangXiGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuang_xi_gu");
    private static final ResourceLocation BING_JI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu");
    private static final ResourceLocation BING_XUE_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bing_xue_dao_increase_effect");
    private static final ResourceLocation ICE_COLD_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "hhanleng");

    public static final ResourceLocation ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuang_xi_gu_frost_breath");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final double INCREASE_PER_STACK = 0.025;
    private static final double FROSTBITE_CHANCE = 0.15;
    private static final double FROSTBITE_DAMAGE_PERCENT = 0.05;
    private static final int FROSTBITE_DURATION_SECONDS = 4;
    private static final int COLD_DURATION_TICKS = 3 * 20;
    private static final int FREEZE_REDUCTION_TICKS = 40;
    private static final double ABILITY_RANGE = 6.0;
    private static final double CONE_DOT_THRESHOLD = 0.45; // roughly 63 degrees cone
    private static final int BREATH_PARTICLE_STEPS = 12;
    private static final double BREATH_PARTICLE_SPACING = 0.45;

    private ShuangXiGuOrganBehavior() {
    }

    static {
        OrganActivationListeners.register(ABILITY_ID, ShuangXiGuOrganBehavior::activateAbility);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, CCItems.GUZHENREN_SHUANG_XI_GU, ORGAN_ID)) {
            return;
        }

        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return;
        }
        context.getOrCreateChannel(BING_XUE_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.registerContributor(organ, this, BING_XUE_INCREASE_EFFECT);

        registerRemovalHook(cc, organ, this, staleRemovalContexts);
        refreshIncreaseContribution(cc, organ);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, CCItems.GUZHENREN_SHUANG_XI_GU, ORGAN_ID)) {
            return;
        }
        refreshIncreaseContribution(cc, organ);

        if (!hasBingJiGu(cc)) {
            return;
        }

        clearColdEffects(entity);
        reduceFreezing(entity);
        maintainSnowStride(entity);
    }

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, CCItems.GUZHENREN_SHUANG_XI_GU, ORGAN_ID)) {
            return;
        }
        if (!hasBingJiGu(cc)) {
            return;
        }
        maintainSnowStride(entity);
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, CCItems.GUZHENREN_SHUANG_XI_GU, ORGAN_ID)) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return;
        }
        IncreaseEffectLedger ledger = context.increaseEffects();
        double removed = ledger.remove(organ, BING_XUE_INCREASE_EFFECT);
        ledger.unregisterContributor(organ);
        context.lookupChannel(BING_XUE_INCREASE_EFFECT)
                .ifPresent(channel -> channel.adjust(-removed));
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return;
        }
        context.getOrCreateChannel(BING_XUE_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        if (organ == null || organ.isEmpty()) {
            return;
        }
        int count = Math.max(1, organ.getCount());
        registrar.record(BING_XUE_INCREASE_EFFECT, count, count * INCREASE_PER_STACK);
    }

    private void refreshIncreaseContribution(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return;
        }
        LinkageChannel channel = context.getOrCreateChannel(BING_XUE_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
        IncreaseEffectLedger ledger = context.increaseEffects();
        double previous = ledger.adjust(organ, BING_XUE_INCREASE_EFFECT, 0.0);
        double target = Math.max(1, organ.getCount()) * INCREASE_PER_STACK;
        double delta = target - previous;
        if (delta != 0.0) {
            channel.adjust(delta);
            ledger.adjust(organ, BING_XUE_INCREASE_EFFECT, delta);
        }
    }

    private static void clearColdEffects(LivingEntity entity) {
        Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(ICE_COLD_EFFECT_ID);
        holder.ifPresent(entity::removeEffect);
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.DIG_SLOWDOWN);
    }

    private static void reduceFreezing(LivingEntity entity) {
        int frozen = entity.getTicksFrozen();
        if (frozen > 0) {
            entity.setTicksFrozen(Math.max(0, frozen - FREEZE_REDUCTION_TICKS));
        }
    }

    private static void maintainSnowStride(LivingEntity entity) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().minY, entity.getZ());
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.POWDER_SNOW)) {
            return;
        }
        double targetY = pos.getY() + 1.0D;
        if (entity.getY() < targetY) {
            entity.setPos(entity.getX(), targetY, entity.getZ());
        }
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y < 0.0D) {
            entity.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
        entity.fallDistance = 0.0F;
        server.sendParticles(
                ParticleTypes.SNOWFLAKE,
                entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.5,
                entity.getZ(),
                4,
                0.1,
                0.1,
                0.1,
                0.01
        );
    }

    private static boolean hasBingJiGu(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(CCItems.GUZHENREN_BING_JI_GU)) {
                return true;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(id, BING_JI_GU_ID)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(CCItems.GUZHENREN_SHUANG_XI_GU)) {
                return stack;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(id, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (entity == null || cc == null || entity.level().isClientSide()) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 origin = entity.getEyePosition();
        Vec3 look = entity.getLookAngle().normalize();
        AABB search = entity.getBoundingBox().expandTowards(look.scale(ABILITY_RANGE)).inflate(1.5);
        List<LivingEntity> candidates = server.getEntitiesOfClass(LivingEntity.class, search, target ->
                target != entity && target.isAlive() && !target.isAlliedTo(entity));
        List<LivingEntity> affected = new ArrayList<>();
        for (LivingEntity target : candidates) {
            Vec3 toTarget = target.getEyePosition().subtract(origin);
            double distance = toTarget.length();
            if (distance <= 0.0001D || distance > ABILITY_RANGE) {
                continue;
            }
            Vec3 direction = toTarget.normalize();
            double dot = direction.dot(look);
            if (dot < CONE_DOT_THRESHOLD) {
                continue;
            }
            affected.add(target);
            applyColdEffect(target);
            if (entity.getRandom().nextDouble() < FROSTBITE_CHANCE) {
                double increase = 0.0;
                ActiveLinkageContext ctx = LinkageManager.getContext(cc);
                if (ctx != null) {
                    increase = ctx.lookupChannel(BING_XUE_INCREASE_EFFECT)
                            .map(LinkageChannel::get)
                            .orElse(0.0);
                }
                double percent = Math.max(0.0, FROSTBITE_DAMAGE_PERCENT + increase);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("[compat/guzhenren][shuang_xi] apply frostbite DoT: base={} increase={} final={} target={}",
                            FROSTBITE_DAMAGE_PERCENT,
                            increase,
                            percent,
                            target.getName().getString());
                }
                DamageOverTimeHelper.applyBaseAttackPercentDoT(
                        entity,
                        target,
                        percent,
                        FROSTBITE_DURATION_SECONDS,
                        SoundEvents.GLASS_BREAK,
                        0.55f,
                        1.25f
                );
            }
        }
        spawnBreathParticles(server, origin, look, affected.isEmpty() ? entity : affected.get(0));
        playBreathSound(level, entity, !affected.isEmpty());
    }

    private static void applyColdEffect(LivingEntity target) {
        Level level = target.level();
        if (level.isClientSide()) {
            return;
        }
        Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(ICE_COLD_EFFECT_ID);
        holder.ifPresent(effect -> target.addEffect(new MobEffectInstance(effect, COLD_DURATION_TICKS, 0, false, true, true)));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, COLD_DURATION_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, COLD_DURATION_TICKS, 0, false, true, true));
    }

    private static void spawnBreathParticles(ServerLevel server, Vec3 origin, Vec3 look, Entity focus) {
        Vec3 direction = look.normalize();
        for (int i = 0; i < BREATH_PARTICLE_STEPS; i++) {
            double scale = (i + 1) * BREATH_PARTICLE_SPACING;
            Vec3 point = origin.add(direction.scale(scale));
            server.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    point.x,
                    point.y,
                    point.z,
                    6,
                    0.1,
                    0.1,
                    0.1,
                    0.02
            );
        }
        if (focus != null) {
            server.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    focus.getX(),
                    focus.getY() + focus.getBbHeight() * 0.5,
                    focus.getZ(),
                    12,
                    0.25,
                    0.25,
                    0.25,
                    0.04
            );
        }
    }

    private static void playBreathSound(Level level, LivingEntity entity, boolean hit) {
        float volume = hit ? 0.9f : 0.6f;
        float pitch = hit ? 0.8f : 1.1f;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, volume, pitch);
        if (hit) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 0.5f, 1.3f);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[compat/guzhenren][shuang_xi] frost breath triggered (hit={})", hit);
        }
    }
}
