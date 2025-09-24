package net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.util.NBTWriter;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Active behaviour for 镰刀蛊. Handles resource consumption, cooldown management
 * and the delayed blade wave attack sequence.
 */
public enum LiandaoGuOrganBehavior {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "liandaogu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final ResourceLocation JIAN_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jian_dao_increase_effect");
    private static final ResourceLocation JIN_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jin_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String COOLDOWN_KEY = "LiandaoGuCooldown";

    private static final double BASE_DAMAGE = 30.0;
    private static final double BASE_ZHENYUAN_COST = 120.0;
    private static final double BASE_JINGLI_COST = 80.0;

    private static final int CHARGE_DURATION_TICKS = 3;
    private static final int RELEASE_TELEGRAPH_TICKS = 5;
    private static final int MIN_COOLDOWN_TICKS = 160; // 8 seconds
    private static final int COOLDOWN_VARIANCE_TICKS = 80; // +0-80 ticks (8-12s window)

    private static final double WAVE_LENGTH = 8.0;
    private static final double WAVE_HALF_WIDTH = 1.2;
    private static final double WAVE_HALF_HEIGHT = 1.5;
    private static final double KNOCKBACK_FORCE = 1.2;

    private static final int EFFECT_DURATION_TICKS = 20; // 1 second

    private static final double EPSILON = 1.0E-4;

    static {
        OrganActivationListeners.register(ABILITY_ID, LiandaoGuOrganBehavior::activateAbility);
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT);
        ensureChannel(cc, JIN_DAO_INCREASE_EFFECT);
    }

    private static LinkageChannel ensureChannel(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
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

        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long gameTime = level.getGameTime();
        long nextAllowed = readCooldown(organ);
        if (nextAllowed > gameTime) {
            return;
        }

        if (!tryConsumeResources(player)) {
            return;
        }

        int cooldown = MIN_COOLDOWN_TICKS;
        RandomSource random = player.getRandom();
        if (COOLDOWN_VARIANCE_TICKS > 0) {
            cooldown += random.nextInt(COOLDOWN_VARIANCE_TICKS + 1);
        }
        writeCooldown(organ, gameTime + cooldown);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);

        double swordMultiplier = 1.0 + ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT).get();
        double metalMultiplier = 1.0 + ensureChannel(cc, JIN_DAO_INCREASE_EFFECT).get();
        double totalMultiplier = Math.max(0.0, swordMultiplier * metalMultiplier);
        double damageAmount = BASE_DAMAGE * totalMultiplier;

        Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        Vec3 look = player.getLookAngle();
        Vec3 fallback = player.getForward();
        Vec3 baseForward = look.lengthSqr() < EPSILON ? fallback : look;
        Vec3 forward = baseForward.lengthSqr() < EPSILON
                ? new Vec3(0.0, 0.0, 1.0)
                : baseForward.normalize();
        playChargeStartEffects(serverLevel, player);
        spawnChargeParticles(serverLevel, player, 0);
        schedule(serverLevel, () -> spawnChargeParticles(serverLevel, player, 1), 1);
        schedule(serverLevel, () -> spawnChargeParticles(serverLevel, player, 2), 2);

        Vec3 telegraphOrigin = origin.add(forward.scale(0.6));
        schedule(serverLevel, () -> playTelegraph(serverLevel, telegraphOrigin, forward), CHARGE_DURATION_TICKS);

        Vec3 impactCenter = origin.add(forward.scale(WAVE_LENGTH * 0.75));
        schedule(serverLevel, () -> applyBladeWave(
                serverLevel,
                player,
                cc,
                origin,
                forward,
                damageAmount,
                impactCenter
        ), RELEASE_TELEGRAPH_TICKS);
    }

    private static void playChargeStartEffects(ServerLevel serverLevel, Player player) {
        double x = player.getX();
        double y = player.getY(0.5);
        double z = player.getZ();
        serverLevel.playSound(null, x, y, z, SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.7f, 0.7f);
        serverLevel.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private static void spawnChargeParticles(ServerLevel serverLevel, Player player, int stage) {
        if (!player.isAlive()) {
            return;
        }
        double centerX = player.getX();
        double centerY = player.getY(0.7 + 0.1 * stage);
        double centerZ = player.getZ();
        RandomSource random = player.getRandom();
        double radius = 0.6 + stage * 0.1;
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * Mth.TWO_PI + random.nextDouble() * 0.2;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    centerX + offsetX,
                    centerY,
                    centerZ + offsetZ,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
        serverLevel.sendParticles(ParticleTypes.ENCHANT, centerX, centerY, centerZ, 6, 0.2, 0.25, 0.2, 0.01);
    }

    private static void playTelegraph(ServerLevel serverLevel, Vec3 origin, Vec3 forward) {
        if (forward.lengthSqr() < EPSILON) {
            return;
        }
        serverLevel.playSound(null, origin.x, origin.y, origin.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
        serverLevel.playSound(null, origin.x, origin.y, origin.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f, 1.4f);
        serverLevel.playSound(null, origin.x, origin.y, origin.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.3f, 1.6f);

        int segments = 18;
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 point = origin.add(forward.scale(t * WAVE_LENGTH));
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 3, 0.1, 0.05, 0.1, 0.0);
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 2, 0.2, 0.2, 0.2, 0.01);
            }
        }
        serverLevel.sendParticles(ParticleTypes.FLASH, origin.x + forward.x * 0.5, origin.y, origin.z + forward.z * 0.5, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static void applyBladeWave(
            ServerLevel serverLevel,
            Player player,
            ChestCavityInstance cc,
            Vec3 origin,
            Vec3 forward,
            double damageAmount,
            Vec3 impactCenter
    ) {
        if (!player.isAlive()) {
            return;
        }
        AABB hitbox = new AABB(origin, origin.add(forward.scale(WAVE_LENGTH))).inflate(WAVE_HALF_WIDTH, WAVE_HALF_HEIGHT, WAVE_HALF_WIDTH);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitbox, entity -> entity != player && entity.isAlive());
        for (LivingEntity target : targets) {
            float appliedDamage = (float) damageAmount;
            if (appliedDamage > 0.0f) {
                target.hurt(player.damageSources().playerAttack(player), appliedDamage);
            }
            target.knockback(KNOCKBACK_FORCE, -forward.x, -forward.z);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION_TICKS, 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION_TICKS, 0, false, true, true));
            serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        }

        serverLevel.playSound(null, impactCenter.x, impactCenter.y, impactCenter.z, SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 0.7f, 1.1f);
        serverLevel.playSound(null, impactCenter.x, impactCenter.y, impactCenter.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.2f);

        spawnResidualParticles(serverLevel, origin, forward);
        affectBlocks(serverLevel, player);
    }

    private static void spawnResidualParticles(ServerLevel serverLevel, Vec3 origin, Vec3 forward) {
        Vec3 end = origin.add(forward.scale(WAVE_LENGTH));
        for (int i = 0; i < 24; i++) {
            double t = serverLevel.getRandom().nextDouble();
            Vec3 point = origin.add(forward.scale(t * WAVE_LENGTH));
            serverLevel.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 1, 0.1, 0.1, 0.1, 0.01);
            serverLevel.sendParticles(ParticleTypes.POOF, point.x, point.y, point.z, 1, 0.15, 0.05, 0.15, 0.02);
            if (serverLevel.getRandom().nextInt(5) == 0) {
                serverLevel.sendParticles(ParticleTypes.FLASH, point.x, point.y + 0.1, point.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        serverLevel.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 12, 0.4, 0.4, 0.4, 0.05);
    }

    private static void affectBlocks(ServerLevel serverLevel, Player player) {
        Direction facing = player.getDirection();
        BlockPos basePos = player.blockPosition().relative(facing, 2);
        for (int dy = 0; dy < 3; dy++) {
            for (int offset = -1; offset <= 1; offset++) {
                BlockPos targetPos;
                if (facing.getAxis() == Direction.Axis.X) {
                    targetPos = basePos.offset(0, dy, offset);
                } else {
                    targetPos = basePos.offset(offset, dy, 0);
                }
                processBlock(serverLevel, player, targetPos);
            }
        }
    }

    private static void processBlock(ServerLevel serverLevel, Player player, BlockPos pos) {
        BlockState state = serverLevel.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        if (isSoftBlock(state)) {
            serverLevel.destroyBlock(pos, true, player);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.25, 0.25, 0.25, 0.1);
        } else {
            serverLevel.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.PLAYERS, 0.5f, 1.1f);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
        }
    }

    private static boolean isSoftBlock(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.WOOL);
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && ORGAN_ID.equals(id)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryConsumeResources(Player player) {
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return false;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        OptionalDouble jingliBeforeOpt = handle.getJingli();
        if (jingliBeforeOpt.isEmpty()) {
            return false;
        }
        double jingliBefore = jingliBeforeOpt.getAsDouble();
        if (jingliBefore + EPSILON < BASE_JINGLI_COST) {
            return false;
        }
        OptionalDouble jingliAfterOpt = handle.adjustJingli(-BASE_JINGLI_COST, true);
        if (jingliAfterOpt.isEmpty()) {
            return false;
        }
        OptionalDouble zhenyuanResult = handle.consumeScaledZhenyuan(BASE_ZHENYUAN_COST);
        if (zhenyuanResult.isPresent()) {
            return true;
        }
        handle.adjustJingli(BASE_JINGLI_COST, true);
        return false;
    }

    private static void schedule(ServerLevel level, Runnable runnable, int delayTicks) {
        if (delayTicks <= 0) {
            runnable.run();
            return;
        }
        level.getServer().execute(() -> schedule(level, runnable, delayTicks - 1));
    }

    private static long readCooldown(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0L;
        }
        CompoundTag tag = data.copyTag();
        return tag.getLong(COOLDOWN_KEY);
    }

    private static void writeCooldown(ItemStack stack, long value) {
        NBTWriter.updateCustomData(stack, tag -> tag.putLong(COOLDOWN_KEY, value));
    }
}
