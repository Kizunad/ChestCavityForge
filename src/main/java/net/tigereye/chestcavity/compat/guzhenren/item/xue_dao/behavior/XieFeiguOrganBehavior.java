package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.NBTWriter;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.joml.Vector3f;
import org.slf4j.Logger;
import net.tigereye.chestcavity.registration.CCItems;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Behaviour implementation for 血肺蛊 (Xie Fei Gu).
 */
public enum XieFeiguOrganBehavior implements OrganSlowTickListener, OrganRemovalListener, OrganIncomingDamageListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_fei_gu");
    private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xue_dao_increase_effect");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String STATE_KEY = "XieFeigu";
    private static final String MODE_KEY = "Mode";
    private static final String SLOT_KEY = "Slot";
    private static final String COOLDOWN_KEY = "Cooldown";

    private static final int MODE_NEUTRAL = 0;
    private static final int MODE_HIGH = 1;
    private static final int MODE_LOW = 2;

    private static final double HIGH_HEALTH_THRESHOLD = 0.5;
    private static final double LOW_HEALTH_THRESHOLD = 0.3;
    private static final double OXYGEN_THRESHOLD = 0.3;

    private static final double SPEED_BONUS_PER_STACK = 0.10;
    private static final double ATTACK_SPEED_BONUS_PER_STACK = 0.15;

    private static final float BASE_EXHAUSTION_PER_SLOW_TICK = 0.10f;
    private static final float RAPID_BREATH_EXHAUSTION = 0.6f;

    private static final float HEALTH_COST = 10.0f;
    private static final double ZHENYUAN_COST = 20.0;
    private static final double RESOURCE_TO_HEALTH_RATIO = 100.0;
    private static final float NEAR_DEATH_THRESHOLD = 4.0f;
    private static final double HEART_FAILURE_CHANCE = 0.35;
    private static final float AUTO_TRIGGER_CHANCE = 0.1f;

    private static final int COOLDOWN_TICKS = 200; // 10 seconds

    private static final int FOG_DURATION_SECONDS = 8;
    private static final double FOG_RADIUS = 6.0;
    private static final float FOG_DAMAGE = 4.0f;
    private static final int BLINDNESS_DURATION_TICKS = 60;
    private static final int POISON_DURATION_TICKS = 160;
    private static final int FOG_PARTICLE_COUNT = 120;

    private static final DustParticleOptions BLOOD_DUST =
            new DustParticleOptions(new Vector3f(0.85f, 0.1f, 0.1f), 1.3f);

    private static final Component HEART_FAILURE_MESSAGE =
            Component.literal("血肺蛊失衡，心肺衰竭！");

    static {
        OrganActivationListeners.register(ABILITY_ID, XieFeiguOrganBehavior::activateAbility);
    }

    @Override
    public float onIncomingDamage(
            DamageSource source,
            LivingEntity victim,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (victim == null || victim.level().isClientSide()) {
            return damage;
        }
        if (cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!organ.is(CCItems.GUZHENREN_XUE_FEI_GU)) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(organ.getItem());
            if (!ORGAN_ID.equals(id)) {
                return damage;
            }
        }

        // 仅非玩家在受击时以 1/10 概率触发
        if (victim instanceof Player) {
            return damage;
        }
        RandomSource random = victim.getRandom();
        if (random.nextFloat() < AUTO_TRIGGER_CHANCE) {
            activateAbilityNonPlayer(victim, cc, organ);
        }
        return damage;
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        if (context != null) {
            context.getOrCreateChannel(XUE_DAO_INCREASE_EFFECT);
        }
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!organ.is(CCItems.GUZHENREN_XUE_FEI_GU)) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(organ.getItem());
            if (!ORGAN_ID.equals(id)) {
                return;
            }
        }
        int slotIndex = ChestCavityUtil.findOrganSlot(cc, organ);
        boolean alreadyRegistered = staleRemovalContexts.removeIf(old ->
                ChestCavityUtil.matchesRemovalContext(old, slotIndex, organ, this));
        cc.onRemovedListeners.add(new OrganRemovalContext(slotIndex, organ, this));
        if (!alreadyRegistered) {
            if (readSlot(organ) != slotIndex) {
                writeSlot(organ, slotIndex);
                NetworkUtil.sendOrganSlotUpdate(cc, organ);
            }
        }
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        int storedSlot = readSlot(organ);
        int slotIndex = resolveSlotIndex(cc, organ);
        if (slotIndex >= 0 && storedSlot != slotIndex) {
            removeMovementModifier(entity, storedSlot);
            removeRapidBreathModifier(entity, storedSlot);
            writeSlot(organ, slotIndex);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        double maxHealth = entity.getMaxHealth();
        double healthRatio = maxHealth <= 0.0 ? 0.0 : entity.getHealth() / maxHealth;

        applyOxygenSupport(entity, healthRatio);
        applyMovementModifier(entity, organ, slotIndex, healthRatio);
        applyRapidBreathModifier(entity, organ, slotIndex, healthRatio);
        if (entity instanceof Player player) {
            applyHungerPenalties(player, healthRatio);
        }

        int mode = determineMode(healthRatio);
        if (mode != readMode(organ)) {
            writeMode(organ, mode);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
            playPassiveCue(entity, mode);
        }
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        int slotIndex = readSlot(organ);
        removeMovementModifier(entity, slotIndex);
        removeRapidBreathModifier(entity, slotIndex);
        writeMode(organ, MODE_NEUTRAL);
        writeSlot(organ, -1);
        writeCooldown(organ, 0L);
    }

    private static void applyOxygenSupport(LivingEntity entity, double healthRatio) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (healthRatio <= OXYGEN_THRESHOLD) {
            return;
        }
        if (entity.getAirSupply() < entity.getMaxAirSupply()) {
            entity.setAirSupply(entity.getMaxAirSupply());
        }
    }

    private static void applyMovementModifier(LivingEntity entity, ItemStack organ, int slotIndex, double healthRatio) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null || slotIndex < 0) {
            return;
        }
        ResourceLocation id = movementModifierId(slotIndex);
        if (healthRatio > HIGH_HEALTH_THRESHOLD) {
            double amount = SPEED_BONUS_PER_STACK * Math.max(1, organ.getCount());
            AttributeModifier modifier = new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            replaceModifier(attribute, id, modifier);
        } else {
            attribute.removeModifier(id);
        }
    }

    private static void removeMovementModifier(LivingEntity entity, int slotIndex) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null || slotIndex < 0) {
            return;
        }
        attribute.removeModifier(movementModifierId(slotIndex));
    }

    private static ResourceLocation movementModifierId(int slotIndex) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/xie_fei_speed_" + slotIndex);
    }

    private static void applyRapidBreathModifier(LivingEntity entity, ItemStack organ, int slotIndex, double healthRatio) {
        AttributeInstance attribute = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute == null || slotIndex < 0) {
            return;
        }
        ResourceLocation id = rapidBreathModifierId(slotIndex);
        if (healthRatio < LOW_HEALTH_THRESHOLD) {
            double amount = ATTACK_SPEED_BONUS_PER_STACK * Math.max(1, organ.getCount());
            AttributeModifier modifier = new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            replaceModifier(attribute, id, modifier);
        } else {
            attribute.removeModifier(id);
        }
    }

    private static void removeRapidBreathModifier(LivingEntity entity, int slotIndex) {
        AttributeInstance attribute = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute == null || slotIndex < 0) {
            return;
        }
        attribute.removeModifier(rapidBreathModifierId(slotIndex));
    }

    private static ResourceLocation rapidBreathModifierId(int slotIndex) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/xie_fei_rapid_breath_" + slotIndex);
    }

    private static void replaceModifier(AttributeInstance attribute, ResourceLocation id, AttributeModifier modifier) {
        attribute.removeModifier(id);
        attribute.addTransientModifier(modifier);
    }

    private static void applyHungerPenalties(Player player, double healthRatio) {
        if (player == null || player.getAbilities().instabuild) {
            return;
        }
        player.causeFoodExhaustion(BASE_EXHAUSTION_PER_SLOW_TICK);
        if (healthRatio < LOW_HEALTH_THRESHOLD) {
            player.causeFoodExhaustion(RAPID_BREATH_EXHAUSTION);
        }
    }

    private static int determineMode(double healthRatio) {
        if (healthRatio > HIGH_HEALTH_THRESHOLD) {
            return MODE_HIGH;
        }
        if (healthRatio < LOW_HEALTH_THRESHOLD) {
            return MODE_LOW;
        }
        return MODE_NEUTRAL;
    }

    private static void playPassiveCue(LivingEntity entity, int mode) {
        Level level = entity.level();
        if (level == null) {
            return;
        }
        RandomSource random = entity.getRandom();
        SoundSource source = SoundSource.PLAYERS;
        if (mode == MODE_HIGH) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.WARDEN_HEARTBEAT, source, 0.8f, 0.9f + random.nextFloat() * 0.2f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_BREATH, source, 0.6f, 0.8f + random.nextFloat() * 0.4f);
        } else if (mode == MODE_LOW) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.WARDEN_HEARTBEAT, source, 1.0f, 0.75f + random.nextFloat() * 0.15f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_BREATH, source, 0.9f, 0.6f + random.nextFloat() * 0.2f);
        }
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
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
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        long gameTime = level.getGameTime();
        long nextAllowed = readCooldown(organ);
        if (nextAllowed > gameTime) {
            return;
        }

        RandomSource random = player.getRandom();
        final double increaseMultiplier = resolveIncreaseMultiplier(cc);
        if (player.getHealth() < NEAR_DEATH_THRESHOLD && random.nextDouble() < HEART_FAILURE_CHANCE) {
            triggerHeartFailure(player);
            return;
        }

        final float adjustedHealthCost = (float) (HEALTH_COST / increaseMultiplier);
        final float resourceHealthCost = (float) (ZHENYUAN_COST / RESOURCE_TO_HEALTH_RATIO);
        final float totalHealthCost = adjustedHealthCost + resourceHealthCost;
        if (!applyHealthCost(player, totalHealthCost)) {
            return;
        }

        if (player.isDeadOrDying()) {
            return;
        }

        writeCooldown(organ, gameTime + COOLDOWN_TICKS);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);

        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        boolean hasPoison = hasPoisonOrgan(cc);
        final float fogDamage = (float) (FOG_DAMAGE * increaseMultiplier);

        playBloodFogCue(server, player);
        spawnFogParticles(server, center, FOG_PARTICLE_COUNT);

        for (int pulse = 0; pulse < FOG_DURATION_SECONDS; pulse++) {
            final int tickDelay = pulse * 20;
            schedule(server, () -> applyFogPulse(server, player, cc, center, hasPoison, fogDamage), tickDelay);
        }
    }

    private static void tryAutoActivateAbility(Player player, ChestCavityInstance cc) {
        if (player == null || cc == null || player.level().isClientSide()) {
            return;
        }
        OrganActivationListeners.activate(ABILITY_ID, cc);
    }

    // 非玩家触发版本：不收取资源，仅检查冷却与计算效果，复用雾脉冲逻辑
    private static void activateAbilityNonPlayer(LivingEntity user, ChestCavityInstance cc, ItemStack organ) {
        if (user == null || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        Level level = user.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        long gameTime = level.getGameTime();
        long nextAllowed = readCooldown(organ);
        if (nextAllowed > gameTime) {
            return;
        }

        writeCooldown(organ, gameTime + COOLDOWN_TICKS);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);

        Vec3 center = user.position().add(0.0, user.getBbHeight() * 0.5, 0.0);
        boolean hasPoison = hasPoisonOrgan(cc);
        final double increaseMultiplier = resolveIncreaseMultiplier(cc);
        final float fogDamage = (float) (FOG_DAMAGE * increaseMultiplier);

        playBloodFogCue(server, user);
        spawnFogParticles(server, center, FOG_PARTICLE_COUNT);

        for (int pulse = 0; pulse < FOG_DURATION_SECONDS; pulse++) {
            final int tickDelay = pulse * 20;
            schedule(server, () -> applyFogPulse(server, user, cc, center, hasPoison, fogDamage), tickDelay);
        }
    }

    private static void triggerHeartFailure(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        player.sendSystemMessage(HEART_FAILURE_MESSAGE);
        player.hurt(player.damageSources().magic(), player.getHealth() + player.getAbsorptionAmount() + 10.0f);
    }

    private static boolean applyHealthCost(Player player, float amount) {
        if (player == null || amount <= 0.0f) {
            return true;
        }

        float health = player.getHealth();
        float absorption = player.getAbsorptionAmount();

        float remaining = amount;

        if (absorption > 0.0f) {
            float absorbed = Math.min(absorption, remaining);
            player.setAbsorptionAmount(absorption - absorbed);
            remaining -= absorbed;
        }

        if (remaining > 0.0f) {
            float newHealth = Math.max(0.0f, health - remaining);
            player.setHealth(newHealth);
        }

        if (!player.level().isClientSide()) {
            player.level().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_HURT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    0.8f,
                    1.0f + player.getRandom().nextFloat() * 0.3f
            );
        }

        return !player.isDeadOrDying();
    }


    private static void playBloodFogCue(ServerLevel server, LivingEntity player) {
        RandomSource random = player.getRandom();
        SoundSource source = SoundSource.PLAYERS;
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT, source, 0.9f, 0.7f + random.nextFloat() * 0.3f);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_FLAP, source, 0.6f, 0.8f + random.nextFloat() * 0.2f);
    }

    private static void applyFogPulse(ServerLevel level, LivingEntity player, ChestCavityInstance cc, Vec3 center, boolean hasPoison, float fogDamage) {
        if (!player.isAlive()) {
            return;
        }
        spawnFogParticles(level, center, 30);

        AABB area = new AABB(center, center).inflate(FOG_RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, entity ->
                entity != null && entity.isAlive() && entity != player);
        for (LivingEntity target : victims) {
            if (target.isAlliedTo(player)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, 0, false, true, true));
            if (hasPoison) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, 0, false, true, true));
            }
            applyFogDamage(level, player, target, fogDamage);
        }

        // Poison fog no longer grants the caster recovery; only enemies are affected.
    }

    private static void applyFogDamage(ServerLevel level, LivingEntity player, LivingEntity target, float amount) {
        if (amount <= 0.0f) {
            return;
        }
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().magic(), amount);
        target.invulnerableTime = 0;
    }

    private static double resolveIncreaseMultiplier(ChestCavityInstance cc) {
        if (cc == null) {
            return 1.0;
        }
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        if (context == null) {
            return 1.0;
        }
        Optional<LinkageChannel> channel = context.lookupChannel(XUE_DAO_INCREASE_EFFECT);
        double effect = channel.map(LinkageChannel::get).orElse(0.0);
        if (effect < 0.0) {
            effect = 0.0;
        }
        double multiplier = 1.0 + effect;
        return multiplier > 0.0 ? multiplier : 1.0;
    }

    private static void spawnFogParticles(ServerLevel level, Vec3 center, int count) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double radius = FOG_RADIUS * random.nextDouble();
            double angle = random.nextDouble() * Math.PI * 2.0;
            double height = (random.nextDouble() - 0.5) * 2.0;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + height;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 1, 0.1, 0.1, 0.1, 0.0);
            level.sendParticles(BLOOD_DUST, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void schedule(ServerLevel level, Runnable runnable, int delayTicks) {
        if (delayTicks <= 0) {
            runnable.run();
            return;
        }
        level.getServer().execute(() -> schedule(level, runnable, delayTicks - 1));
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
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && ORGAN_ID.equals(id)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasPoisonOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(CCItems.GUZHENREN_CHOU_PI_GU)) {
                return true;
            }
        }
        return false;
    }

    private static int resolveSlotIndex(ChestCavityInstance cc, ItemStack organ) {
        int stored = readSlot(organ);
        if (stored >= 0) {
            return stored;
        }
        if (cc == null) {
            return -1;
        }
        return ChestCavityUtil.findOrganSlot(cc, organ);
    }

    private static int readMode(ItemStack stack) {
        CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return MODE_NEUTRAL;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return MODE_NEUTRAL;
        }
        CompoundTag state = root.getCompound(STATE_KEY);
        return state.getInt(MODE_KEY);
    }

    private static void writeMode(ItemStack stack, int mode) {
        int clamped = Math.max(0, mode);
        int previous = readMode(stack);
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putInt(MODE_KEY, clamped);
            tag.put(STATE_KEY, state);
        });
        logNbtChange(stack, MODE_KEY, previous, clamped);
    }

    private static int readSlot(ItemStack stack) {
        CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return -1;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return -1;
        }
        CompoundTag state = root.getCompound(STATE_KEY);
        if (!state.contains(SLOT_KEY, Tag.TAG_INT)) {
            return -1;
        }
        return state.getInt(SLOT_KEY);
    }

    private static void writeSlot(ItemStack stack, int slot) {
        int clamped = Math.max(-1, slot);
        int previous = readSlot(stack);
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putInt(SLOT_KEY, clamped);
            tag.put(STATE_KEY, state);
        });
        logNbtChange(stack, SLOT_KEY, previous, clamped);
    }

    private static long readCooldown(ItemStack stack) {
        CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0L;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return 0L;
        }
        CompoundTag state = root.getCompound(STATE_KEY);
        if (!state.contains(COOLDOWN_KEY, Tag.TAG_LONG)) {
            return 0L;
        }
        return state.getLong(COOLDOWN_KEY);
    }

    private static void writeCooldown(ItemStack stack, long value) {
        long clamped = Math.max(0L, value);
        long previous = readCooldown(stack);
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putLong(COOLDOWN_KEY, clamped);
            tag.put(STATE_KEY, state);
        });
        logNbtChange(stack, COOLDOWN_KEY, previous, clamped);
    }

    private static void logNbtChange(ItemStack stack, String key, Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Xie Fei Gu] Updated {} for {} from {} to {}", key, describeStack(stack), oldValue, newValue);
        }
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return stack.getCount() + "x " + id;
    }
}
