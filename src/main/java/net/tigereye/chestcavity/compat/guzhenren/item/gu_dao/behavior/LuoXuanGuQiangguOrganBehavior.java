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
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Behaviour for 螺旋骨枪蛊. Handles slow tick recharging and active projectile firing.
 */
public enum LuoXuanGuQiangguOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, IncreaseEffectContributor {
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
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        int currentCharge = Math.max(0, Math.min(MAX_CHARGE, NBTCharge.getCharge(organ, STATE_KEY)));
        if (currentCharge >= MAX_CHARGE) {
            return;
        }


        if (!tryConsumeRechargeResources(player)) {

            return;
        }

        int updated = Math.min(MAX_CHARGE, currentCharge + 1);
        if (updated != currentCharge) {
            NBTCharge.setCharge(organ, STATE_KEY, updated);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
            ChestCavity.LOGGER.info("[LuoXuanGuQiangGu] recharge -> {}/{}", updated, MAX_CHARGE);
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
        Level level = player.level();
        if (!(level instanceof ServerLevel server)) {
            return false;
        }

        double speed = BASE_PROJECTILE_SPEED * multiplier;
        double damage = BASE_PROJECTILE_DAMAGE * multiplier;

        Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.4));
        Vec3 direction = player.getLookAngle().normalize();


        AbstractArrow projectile = ((ArrowItem) Items.ARROW).createArrow(level, new ItemStack(Items.ARROW), player, findCompatibleWeapon(player));

        projectile.setSoundEvent(SoundEvents.ARROW_HIT);
        projectile.setBaseDamage(damage);
        projectile.pickup = AbstractArrow.Pickup.DISALLOWED;
        projectile.setPos(origin.x, origin.y, origin.z);
        projectile.shoot(direction.x, direction.y, direction.z, (float) speed, 0.0F);
        projectile.setCritArrow(true);

        Optional<Item> spearItem = BuiltInRegistries.ITEM.getOptional(BONE_SPEAR_ITEM_ID);
        spearItem.ifPresent(item -> applyCustomPickupItem(projectile, new ItemStack(item)));

        server.addFreshEntity(projectile);

        server.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z, 16, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SKELETON_AMBIENT, SoundSource.PLAYERS, 0.7f, 1.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
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
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        // LuoXuanGuQianggu does not contribute to INCREASE effects.
    }
}
