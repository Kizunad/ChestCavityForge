package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArrowItem;

import net.minecraft.world.item.ProjectileWeaponItem;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Behaviour for 螺旋骨枪蛊. Handles slow tick recharging and active projectile firing.
 */
public enum LuoXuanGuQiangguOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, IncreaseEffectContributor, OrganIncomingDamageListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "luo_xuan_gu_qiang_gu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final ResourceLocation GU_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/gu_dao_increase_effect");
    private static final ResourceLocation LI_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/li_dao_increase_effect");

    private static final ResourceLocation BONE_SPEAR_ITEM_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gu_qiang");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = "LuoXuanGuCharge";
    private static final int MAX_CHARGE = 3;
    private static final double BASE_ZHENYUAN_COST = 50.0;
    private static final double BASE_JINGLI_COST = 50.0;

    private static final double BASE_PROJECTILE_DAMAGE = 7.0;
    private static final double BASE_PROJECTILE_SPEED = 2.6;

    private static final double EPSILON = 1.0E-4;

    static {
        OrganActivationListeners.register(ABILITY_ID, LuoXuanGuQiangguOrganBehavior::activateAbility);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        int currentCharge = Math.max(0, Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY)));
        if (currentCharge >= MAX_CHARGE) {
            return;
        }

        boolean paid;
        if (entity instanceof Player player) {
            paid = tryConsumeRechargeResources(player);
        } else {
            // 非玩家：使用 Helper 购买 1 点充能（允许生命值替代）
            paid = GuzhenrenResourceCostHelper.consumeWithFallback(entity, BASE_ZHENYUAN_COST, BASE_JINGLI_COST).succeeded();
        }
        if (!paid) {
            return;
        }

        int updated = Math.min(MAX_CHARGE, currentCharge + 1);
        if (updated != currentCharge) {
            NBTCharge.setCharge(organ, STATE_KEY, updated);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
            if (entity instanceof Player) {
                ChestCavity.LOGGER.info("[LuoXuanGuQiangGu] recharge -> {}/{}", updated, MAX_CHARGE);
            }
        }
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannel(cc, GU_DAO_INCREASE_EFFECT);
        ensureChannel(cc, LI_DAO_INCREASE_EFFECT);
    }

    private static LinkageChannel ensureChannel(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
    }


    private static boolean tryConsumeRechargeResources(Player player) {
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
        double jingliAfter = jingliAfterOpt.getAsDouble();
        if ((jingliBefore - jingliAfter) + EPSILON < BASE_JINGLI_COST) {
            handle.setJingli(jingliBefore);
            return false;
        }

        OptionalDouble zhenyuanResult = handle.consumeScaledZhenyuan(BASE_ZHENYUAN_COST);
        if (zhenyuanResult.isPresent()) {
            return true;
        }

        handle.setJingli(jingliBefore);
        return false;
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        ItemStack organ = findChargedOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        int currentCharge = Math.max(0, Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY)));
        if (currentCharge <= 0) {
            return;
        }

        double guDaoEfficiency = 1.0 + ensureChannel(cc, GU_DAO_INCREASE_EFFECT).get();
        double liDaoEfficiency = 1.0 + ensureChannel(cc, LI_DAO_INCREASE_EFFECT).get();
        double multiplier = Math.max(0.0, guDaoEfficiency * liDaoEfficiency);

        if (!fireProjectile(player, multiplier)) {
            return;
        }

        int updated = Math.max(0, currentCharge - 1);
        if (updated != currentCharge) {
            NBTCharge.setCharge(organ, STATE_KEY, updated);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    private static ItemStack findChargedOrgan(ChestCavityInstance cc) {
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && ORGAN_ID.equals(id)) {
                if (NBTCharge.getCharge(stack, STATE_KEY) > 0) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean fireProjectile(Player player, double multiplier) {
        // 转发到通用实现
        return fireProjectile(player, player.getEyePosition().add(player.getLookAngle().scale(0.4)), player.getLookAngle().normalize(), multiplier);
    }

    private static boolean fireProjectile(LivingEntity user, Vec3 origin, Vec3 direction, double multiplier) {
        Level level = user.level();
        if (!(level instanceof ServerLevel server)) {
            return false;
        }

        double speed = BASE_PROJECTILE_SPEED * multiplier;
        double damage = BASE_PROJECTILE_DAMAGE * multiplier;

        AbstractArrow projectile;
        if (user instanceof Player p) {
            projectile = ((ArrowItem) Items.ARROW).createArrow(level, new ItemStack(Items.ARROW), p, findCompatibleWeapon(p));
        } else {
            projectile = ((ArrowItem) Items.ARROW).createArrow(level, new ItemStack(Items.ARROW), user, Items.BOW.getDefaultInstance());
        }

        projectile.setSoundEvent(SoundEvents.ARROW_HIT);
        projectile.setBaseDamage(damage);
        projectile.pickup = AbstractArrow.Pickup.DISALLOWED;
        projectile.setPos(origin.x, origin.y, origin.z);
        projectile.shoot(direction.x, direction.y, direction.z, (float) speed, 0.0F);
        projectile.setCritArrow(true);

        Optional<Item> spearItem = BuiltInRegistries.ITEM.getOptional(BONE_SPEAR_ITEM_ID);
        spearItem.ifPresent(item -> applyCustomPickupItem(projectile, new ItemStack(item)));

        server.addFreshEntity(projectile);

        playLuoXuanActivationEffects(server, user, origin, direction, multiplier);
        return true;
    }

    private static void playLuoXuanActivationEffects(ServerLevel server, LivingEntity player, Vec3 origin, Vec3 direction, double multiplier) {
        Level level = player.level();
        float ambientPitch = 0.95f + (float)Math.max(-0.2, Math.min(0.3, (multiplier - 1.0) * 0.2));
        float shootPitch = 1.1f + (float)Math.max(-0.3, Math.min(0.3, (multiplier - 1.0) * 0.15));

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SKELETON_AMBIENT, SoundSource.PLAYERS, 0.7f, ambientPitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, shootPitch);

        server.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z, 20, 0.15, 0.15, 0.15, 0.05);
        spawnLuoXuanSpiralTrail(server, origin, direction, multiplier);
    }

    private static void spawnLuoXuanSpiralTrail(ServerLevel server, Vec3 origin, Vec3 direction, double multiplier) {
        Vec3 forward = direction.normalize();
        if (forward.lengthSqr() < 1.0E-4) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }

        Vec3 upReference = Math.abs(forward.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(upReference);
        if (right.lengthSqr() < 1.0E-4) {
            right = forward.cross(new Vec3(0.0, 0.0, 1.0));
        }
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        RandomSource random = server.getRandom();
        int segments = 18;
        double distanceStep = 0.35;
        double baseRadius = 0.2;

        for (int i = 0; i <= segments; i++) {
            double distance = 0.4 + i * distanceStep;
            double progress = i / (double)Math.max(1, segments);
            double radius = baseRadius + progress * 0.35 * Math.max(0.8, multiplier);
            double swirlAngle = progress * Math.PI * 4.0;

            Vec3 radial = right.scale(Math.cos(swirlAngle) * radius).add(up.scale(Math.sin(swirlAngle) * radius));
            Vec3 point = origin.add(forward.scale(distance)).add(radial);

            double jitter = 0.01 + random.nextDouble() * 0.015;
            server.sendParticles(
                ParticleTypes.CRIT,
                point.x,
                point.y,
                point.z,
                3,
                forward.x * jitter,
                forward.y * jitter,
                forward.z * jitter,
                0.01
            );

            if (random.nextFloat() < 0.2f) {
                server.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
                );
            }
        }
    }

    private static ItemStack findCompatibleWeapon(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (isProjectileWeapon(mainHand)) {
            return mainHand.copy();
        }

        ItemStack offHand = player.getOffhandItem();
        if (isProjectileWeapon(offHand)) {
            return offHand.copy();
        }

        return Items.BOW.getDefaultInstance();
    }

    private static boolean isProjectileWeapon(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ProjectileWeaponItem;
    }

    private static void applyCustomPickupItem(AbstractArrow projectile, ItemStack pickup) {
        try {
            java.lang.reflect.Field field = AbstractArrow.class.getDeclaredField("pickupItem");
            field.setAccessible(true);
            field.set(projectile, pickup);
        } catch (NoSuchFieldException ignored) {
            try {
                java.lang.reflect.Field altField = AbstractArrow.class.getDeclaredField("pickupItemStack");
                altField.setAccessible(true);
                altField.set(projectile, pickup);
            } catch (ReflectiveOperationException ignoredToo) {
                // Fall back silently when the field cannot be customised on this version.
            }
        } catch (IllegalAccessException ignored) {
            // ignore
        }
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        return damage;
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide()) {
            return damage;
        }
        if (cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        // 玩家受伤不触发自动发射，仅限非玩家
        if (victim instanceof Player) {
            return damage;
        }
        int currentCharge = Math.max(0, Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY)));
        if (currentCharge <= 0) {
            return damage;
        }
        RandomSource random = victim.getRandom();
        if (random.nextInt(10) != 0) {
            return damage;
        }

        // 方向：受击者 -> 攻击者
        Vec3 origin = victim.getEyePosition().add(0.0, -0.2, 0.0);
        Vec3 forward;
        LivingEntity attacker = null;
        if (source != null) {
            if (source.getEntity() instanceof LivingEntity le) attacker = le;
            else if (source.getDirectEntity() instanceof LivingEntity le2) attacker = le2;
        }
        if (attacker != null) {
            forward = attacker.position().add(0.0, attacker.getBbHeight() * 0.5, 0.0).subtract(origin).normalize();
        } else {
            forward = victim.getLookAngle().normalize();
        }
        if (forward.lengthSqr() < EPSILON) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }

        double guDaoEfficiency = 1.0 + ensureChannel(cc, GU_DAO_INCREASE_EFFECT).get();
        double liDaoEfficiency = 1.0 + ensureChannel(cc, LI_DAO_INCREASE_EFFECT).get();
        double multiplier = Math.max(0.0, guDaoEfficiency * liDaoEfficiency);

        if (!fireProjectile(victim, origin, forward, multiplier)) {
            return damage;
        }

        int updated = Math.max(0, currentCharge - 1);
        if (updated != currentCharge) {
            NBTCharge.setCharge(organ, STATE_KEY, updated);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
        return damage;
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        // LuoXuanGuQianggu does not contribute to INCREASE effects.
    }
}
