package net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.fx.XiaoGuangFx;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TargetingOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TeleportOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.CombatUtil;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 光道·小光蛊（眼类器官）——提供折影残像、幻映分身与光遁步。
 */
public final class XiaoGuangGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganIncomingDamageListener {

    public static final XiaoGuangGuOrganBehavior INSTANCE = new XiaoGuangGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiao_guang_gu");
    public static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiao_guang_gu_illusion");

    private static final String STATE_ROOT = "XiaoGuangGu";
    private static final String KEY_TIER = "Tier";
    private static final String KEY_POINTS = "LuminaPoints";
    private static final String KEY_MIRROR_EXPIRE_TICK = "MirrorExpireTick";
    private static final String KEY_LAST_DODGE_TICK = "LastDodgeTick";
    private static final String KEY_LAST_KILL_POINT_TICK = "LastKillPointTick";
    private static final String KEY_LAST_DECOY_POINT_TICK = "LastDecoyPointTick";
    private static final String KEY_LAST_LIGHTSTEP_POINT_TICK = "LastLightstepPointTick";
    private static final String KEY_DODGE_READY_TICK = "BaseDodgeReadyTick";
    private static final String KEY_LIGHTSTEP_READY_TICK = "LightstepReadyTick";
    private static final String KEY_ABILITY_READY_TICK = "AbilityReadyTick";

    private static final int MIN_TIER = 2;
    private static final int MAX_TIER = 4;
    private static final int LUMINA_CAP = 200;
    private static final int REQUIRE_TIER3 = 40;
    private static final int REQUIRE_TIER4 = 120;

    private static final int MIRROR_DURATION_TICKS = 60;
    private static final double MIRROR_PROJECTILE_MISS_CHANCE = 0.15D;
    private static final double MIRROR_DAMAGE_REDUCTION = 0.15D;

    private static final double BASE_DODGE_CHANCE = 0.22D;
    private static final float DODGE_MIN_DISTANCE = 0.8F;
    private static final float DODGE_MAX_DISTANCE = 1.6F;
    private static final float DODGE_YAW_RANGE = 100.0F;
    private static final long DODGE_COOLDOWN_TICKS = 80L;

    private static final long LIGHTSTEP_WINDOW_TICKS = 10L;
    private static final long LIGHTSTEP_COOLDOWN_TICKS = 200L;
    private static final double LIGHTSTEP_DISTANCE = 5.0D;
    private static final int LIGHTSTEP_INVIS_TICKS = 20;
    private static final int LIGHTSTEP_EXTRA_INVIS_TICKS = 10;
    private static final int LIGHTSTEP_INVUL_TICKS = 20;

    private static final long ABILITY_COOLDOWN_TICKS = 360L;
    private static final double ABILITY_ZHENYUAN_COST = 200.0D;
    private static final double ABILITY_JINGLI_COST = 5.0D;
    private static final int DECOY_LIFETIME_TICKS = 40;
    private static final float DECOY_ATTACK_DAMAGE_RATIO = 0.3F;

    private static final int POINT_COOLDOWN_KILL_TICKS = 40;
    private static final int POINT_COOLDOWN_DECOY_TICKS = 100;
    private static final int POINT_COOLDOWN_LIGHTSTEP_TICKS = 600;

    private static final float ILLUSION_BURST_DAMAGE = 2.0F;
    private static final double ILLUSION_BURST_RADIUS = 3.0D;

    private static final Map<UUID, DecoyInfo> ACTIVE_DECOYS = new ConcurrentHashMap<>();

    private XiaoGuangGuOrganBehavior() {
    }

    static {
        OrganActivationListeners.register(ABILITY_ID, XiaoGuangGuOrganBehavior::activateAbility);
        NeoForge.EVENT_BUS.addListener(XiaoGuangGuOrganBehavior::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(XiaoGuangGuOrganBehavior::onLivingIncomingDamage);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        Level level = entity.level();
        long gameTime = level.getGameTime();
        OrganState state = organState(organ, STATE_ROOT);
        resolveTier(state);

        long expire = state.getLong(KEY_MIRROR_EXPIRE_TICK, 0L);
        if (expire > gameTime) {
            int remaining = (int) Math.max(1L, expire - gameTime);
            ReactionTagOps.add(entity, ReactionTagKeys.MIRROR_IMAGE, remaining);
        } else {
            if (ReactionTagOps.has(entity, ReactionTagKeys.MIRROR_IMAGE)) {
                ReactionTagOps.clear(entity, ReactionTagKeys.MIRROR_IMAGE);
            }
        }
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide() || damage <= 0.0F) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }

        Level level = victim.level();
        long gameTime = level.getGameTime();
        OrganState state = organState(organ, STATE_ROOT);
        int tier = resolveTier(state);

        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.Entry dodgeReady = cooldown.entry(KEY_DODGE_READY_TICK).withDefault(0L);
        MultiCooldown.Entry lightstepReady = cooldown.entry(KEY_LIGHTSTEP_READY_TICK).withDefault(0L);

        boolean dodged = false;
        if (gameTime >= dodgeReady.getReadyTick()) {
            if (victim.getRandom().nextDouble() < BASE_DODGE_CHANCE) {
                LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
                dodged = CombatUtil.performShortDodge(
                        victim,
                        attacker,
                        DODGE_MIN_DISTANCE,
                        DODGE_MAX_DISTANCE,
                        DODGE_YAW_RANGE,
                        SoundEvents.ALLAY_ITEM_TAKEN,
                        null
                );
                if (dodged) {
                    long previous = state.getLong(KEY_LAST_DODGE_TICK, 0L);
                    dodgeReady.setReadyAt(gameTime + DODGE_COOLDOWN_TICKS);
                    handleMirrorTrigger(victim, cc, organ, state, gameTime);
                    OrganStateOps.setLong(state, cc, organ, KEY_LAST_DODGE_TICK, gameTime, value -> Math.max(0L, value), 0L);
                    if (tier >= 4 && lightstepReady.isReady(gameTime)) {
                        if (previous > 0L && gameTime - previous <= LIGHTSTEP_WINDOW_TICKS) {
                            if (lightstepReady.tryStart(gameTime, LIGHTSTEP_COOLDOWN_TICKS)) {
                                performLightstep(victim, cc, organ, state, gameTime);
                            }
                        }
                    }
                    return 0.0F;
                }
            }
        }

        long mirrorExpire = state.getLong(KEY_MIRROR_EXPIRE_TICK, 0L);
        if (mirrorExpire > gameTime && isRangedAttack(source)) {
            if (victim.getRandom().nextDouble() < MIRROR_PROJECTILE_MISS_CHANCE) {
                XiaoGuangFx.playMirrorEvade(victim);
                return 0.0F;
            }
        }

        if (mirrorExpire > gameTime) {
            return (float) Math.max(0.0F, damage * (1.0F - MIRROR_DAMAGE_REDUCTION));
        }
        return damage;
    }

    private void handleMirrorTrigger(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state, long gameTime) {
        long expire = gameTime + MIRROR_DURATION_TICKS;
        OrganStateOps.setLong(state, cc, organ, KEY_MIRROR_EXPIRE_TICK, expire, value -> Math.max(0L, value), 0L);
        ReactionTagOps.add(entity, ReactionTagKeys.MIRROR_IMAGE, MIRROR_DURATION_TICKS);
        XiaoGuangFx.playMirrorTrigger(entity);
    }

    private void performLightstep(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state, long gameTime) {
        boolean hasWind = hasFlowKeyword(cc, "feng");
        boolean hasLightning = hasFlowKeyword(cc, "lei");
        double distance = LIGHTSTEP_DISTANCE + (hasWind ? 2.0D : 0.0D);
        Vec3 direction = entity.getLookAngle().normalize().scale(distance);
        TeleportOps.blinkOffset(entity, direction);

        int invisTicks = LIGHTSTEP_INVIS_TICKS + (hasWind ? LIGHTSTEP_EXTRA_INVIS_TICKS : 0);
        entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, invisTicks, 0, false, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, LIGHTSTEP_INVUL_TICKS, 4, false, false, true));

        XiaoGuangFx.playLightstep(entity);

        if (entity instanceof ServerPlayer player) {
            ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, gameTime + LIGHTSTEP_COOLDOWN_TICKS, gameTime);
        }
        maybeGrantPoints(cc, organ, state, entity, 1, KEY_LAST_LIGHTSTEP_POINT_TICK,
                POINT_COOLDOWN_LIGHTSTEP_TICKS, gameTime);
    }

    private void triggerLightningPulse(ServerLevel level, LivingEntity owner, Vec3 center) {
        if (level == null || owner == null || center == null) {
            return;
        }
        AABB box = new AABB(center, center).inflate(ILLUSION_BURST_RADIUS);
        List<LivingEntity> hostiles = level.getEntitiesOfClass(LivingEntity.class, box,
                target -> target != null && target.isAlive() && !target.isAlliedTo(owner));
        for (LivingEntity hostile : hostiles) {
            hostile.hurt(owner.damageSources().indirectMagic(owner, owner), 3.0F);
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.2F);
    }

    private static boolean isRangedAttack(DamageSource source) {
        if (source == null) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)) {
            return true;
        }
        Entity attacker = source.getEntity();
        return attacker != null && !source.isDirect();
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
        int tier = INSTANCE.resolveTier(state);
        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
        MultiCooldown.Entry abilityReady = cooldown.entry(KEY_ABILITY_READY_TICK).withDefault(0L);
        long gameTime = player.level().getGameTime();
        if (!abilityReady.isReady(gameTime)) {
            return;
        }
        var payment = ResourceOps.consumeStrict(player, ABILITY_ZHENYUAN_COST, ABILITY_JINGLI_COST);
        if (!payment.succeeded()) {
            return;
        }

        abilityReady.setReadyAt(gameTime + ABILITY_COOLDOWN_TICKS);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
        if (player instanceof ServerPlayer serverPlayer) {
            ActiveSkillRegistry.scheduleReadyToast(serverPlayer, ABILITY_ID, abilityReady.getReadyTick(), gameTime);
        }

        spawnIllusionDecoy(player, cc, organ, state, tier, gameTime);
    }

    private static void spawnIllusionDecoy(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, int tier, long gameTime) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(1.5D));
        ArmorStand decoy = new ArmorStand(server, spawnPos.x, spawnPos.y, spawnPos.z);
        decoy.setNoGravity(true);
        decoy.setInvisible(false);
        decoy.setInvulnerable(false);
        decoy.setCustomNameVisible(false);
        decoy.setYRot(player.getYRot());
        decoy.setXRot(player.getXRot());
        decoy.yHeadRot = player.getYHeadRot();
        decoy.setShowArms(true);
        copyAppearance(player, decoy);
        decoy.setItemSlot(EquipmentSlot.MAINHAND, safeCopy(player.getMainHandItem()));
        decoy.setItemSlot(EquipmentSlot.OFFHAND, safeCopy(player.getOffhandItem()));

        server.addFreshEntity(decoy);
        XiaoGuangFx.playIllusionSummon(player, decoy);

        boolean hasLightning = INSTANCE.hasFlowKeyword(cc, "lei");
        ACTIVE_DECOYS.put(decoy.getUUID(), new DecoyInfo(player.getUUID(), hasLightning));

        TickOps.schedule(server, () -> removeDecoy(decoy, RemovalCause.EXPIRE), DECOY_LIFETIME_TICKS);
        if (tier >= 3) {
            TickOps.schedule(server, () -> performDecoyAttack(decoy), 10);
        }
    }

    private static void performDecoyAttack(ArmorStand decoy) {
        DecoyInfo info = ACTIVE_DECOYS.get(decoy.getUUID());
        if (info == null) {
            return;
        }
        ServerLevel level = (ServerLevel) decoy.level();
        Player owner = level.getPlayerByUUID(info.ownerId());
        if (owner == null || !owner.isAlive()) {
            return;
        }
        List<LivingEntity> hostiles = TargetingOps.hostilesWithinRadius(owner, level, 4.0D);
        if (hostiles.isEmpty()) {
            return;
        }
        LivingEntity target = hostiles.get(0);
        double baseDamage = owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null
                ? owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue()
                : owner.getAttackStrengthScale(0.5F) * 4.0F;
        float damage = (float) (baseDamage * DECOY_ATTACK_DAMAGE_RATIO);
        target.hurt(owner.damageSources().playerAttack(owner), damage);
    }

    private static void removeDecoy(ArmorStand decoy, RemovalCause cause) {
        if (decoy == null || !decoy.isAlive()) {
            return;
        }
        DecoyInfo info = ACTIVE_DECOYS.remove(decoy.getUUID());
        decoy.discard();
        if (info == null) {
            return;
        }
        if (!(decoy.level() instanceof ServerLevel server)) {
            return;
        }
        Player owner = server.getPlayerByUUID(info.ownerId());
        if (owner == null) {
            return;
        }
        Vec3 center = decoy.position();
        XiaoGuangFx.playIllusionBurst(server, center);
        applyBurstDamage(owner, server, center);
        if (info.hasLightning()) {
            INSTANCE.triggerLightningPulse(server, owner, center);
        }
        if (cause == RemovalCause.HIT) {
            Optional<ChestCavityEntity> optional = ChestCavityEntity.of(owner);
            if (optional.isPresent()) {
                ChestCavityInstance cc = optional.get().getChestCavityInstance();
                ItemStack organ = findOrgan(cc);
                if (!organ.isEmpty()) {
                    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
                    INSTANCE.maybeGrantPoints(cc, organ, state, owner, 1, KEY_LAST_DECOY_POINT_TICK,
                            POINT_COOLDOWN_DECOY_TICKS, server.getGameTime());
                }
            }
        }
    }

    private static void applyBurstDamage(Player owner, ServerLevel level, Vec3 center) {
        AABB box = new AABB(center, center).inflate(ILLUSION_BURST_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, entity ->
                entity != null && entity.isAlive() && !entity.isAlliedTo(owner));
        for (LivingEntity target : targets) {
            target.hurt(owner.damageSources().indirectMagic(owner, owner), ILLUSION_BURST_DAMAGE);
            ReactionTagOps.add(target, ReactionTagKeys.ILLUSION_BURST, 40);
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    private static void copyAppearance(Player player, ArmorStand decoy) {
        decoy.setItemSlot(EquipmentSlot.HEAD, safeCopy(player.getItemBySlot(EquipmentSlot.HEAD)));
        decoy.setItemSlot(EquipmentSlot.CHEST, safeCopy(player.getItemBySlot(EquipmentSlot.CHEST)));
        decoy.setItemSlot(EquipmentSlot.LEGS, safeCopy(player.getItemBySlot(EquipmentSlot.LEGS)));
        decoy.setItemSlot(EquipmentSlot.FEET, safeCopy(player.getItemBySlot(EquipmentSlot.FEET)));
    }

    private static ItemStack safeCopy(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private boolean hasFlowKeyword(ChestCavityInstance cc, String keyword) {
        if (cc == null || cc.inventory == null || keyword == null || keyword.isBlank()) {
            return false;
        }
        String lower = keyword.toLowerCase();
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null) {
                String path = id.toString().toLowerCase();
                if (path.contains(lower)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void maybeGrantPoints(ChestCavityInstance cc, ItemStack organ, OrganState state, LivingEntity owner,
                                   int amount, String cooldownKey, int cooldownTicks, long gameTime) {
        if (amount <= 0) {
            return;
        }
        long last = state.getLong(cooldownKey, 0L);
        if (gameTime - last < cooldownTicks) {
            return;
        }
        boolean changed = grantLuminaPoints(cc, organ, state, owner, amount);
        if (changed) {
            OrganStateOps.setLong(state, cc, organ, cooldownKey, gameTime, value -> Math.max(0L, value), 0L);
        }
    }

    private boolean grantLuminaPoints(ChestCavityInstance cc, ItemStack organ, OrganState state, LivingEntity owner, int amount) {
        if (amount <= 0) {
            return false;
        }
        int tier = resolveTier(state);
        int current = Math.max(0, state.getInt(KEY_POINTS, 0));
        int added = Math.min(LUMINA_CAP, current + amount);
        if (added != current) {
            OrganStateOps.setInt(state, cc, organ, KEY_POINTS, added, value -> Mth.clamp(value, 0, LUMINA_CAP), 0);
        }
        boolean upgraded = false;
        while (tier < MAX_TIER) {
            int requirement = tier == 2 ? REQUIRE_TIER3 : REQUIRE_TIER4;
            if (added < requirement) {
                break;
            }
            if (!meetsUpgradeRequirement(owner, tier + 1)) {
                break;
            }
            tier++;
            OrganStateOps.setInt(state, cc, organ, KEY_TIER, tier, value -> Mth.clamp(value, MIN_TIER, MAX_TIER), MIN_TIER);
            added = 0;
            OrganStateOps.setInt(state, cc, organ, KEY_POINTS, 0, value -> Mth.clamp(value, 0, LUMINA_CAP), 0);
            upgraded = true;
            if (owner instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("小光蛊晋入第" + tier + "转"), true);
            }
        }
        return added != current || upgraded;
    }

    private boolean meetsUpgradeRequirement(LivingEntity owner, int targetTier) {
        if (owner == null) {
            return false;
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(owner);
        if (handleOpt.isEmpty()) {
            return false;
        }
        ResourceHandle handle = handleOpt.get();
        double stability = handle.read("hunpo_stability").orElse(0.0D);
        double maxZhenyuan = handle.read("zuida_zhenyuan").orElse(0.0D);
        return switch (targetTier) {
            case 3 -> stability >= 0.3D && maxZhenyuan >= 0.5D;
            case 4 -> stability >= 0.6D && maxZhenyuan >= 1.0D;
            default -> true;
        };
    }

    private int resolveTier(OrganState state) {
        if (state == null) {
            return MIN_TIER;
        }
        int tier = Mth.clamp(state.getInt(KEY_TIER, MIN_TIER), MIN_TIER, MAX_TIER);
        state.setInt(KEY_TIER, tier, value -> Mth.clamp(value, MIN_TIER, MAX_TIER), MIN_TIER);
        return tier;
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(id, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
        MultiCooldown.Builder builder = MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
                .withLongClamp(value -> Math.max(0L, value), 0L);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else {
            builder.withOrgan(organ);
        }
        return builder.build();
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (!player.level().isDay()) {
            return;
        }
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
        if (optional.isEmpty()) {
            return;
        }
        ChestCavityInstance cc = optional.get().getChestCavityInstance();
        if (cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        long gameTime = player.level().getGameTime();
        INSTANCE.maybeGrantPoints(cc, organ, state, player, 1, KEY_LAST_KILL_POINT_TICK, POINT_COOLDOWN_KILL_TICKS, gameTime);
    }

    private static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) {
            return;
        }
        DecoyInfo info = ACTIVE_DECOYS.get(stand.getUUID());
        if (info == null) {
            return;
        }
        event.setCanceled(true);
        removeDecoy(stand, RemovalCause.HIT);
    }

    private enum RemovalCause {
        EXPIRE,
        HIT
    }

    private record DecoyInfo(UUID ownerId, boolean hasLightning) {
    }
}
