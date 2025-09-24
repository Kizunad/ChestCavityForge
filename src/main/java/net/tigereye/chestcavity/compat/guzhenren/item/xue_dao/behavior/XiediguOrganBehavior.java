package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;

import net.minecraft.core.Holder;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.item.GuzhenrenItems;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.NBTWriter;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Behaviour implementation for 血滴蛊 (Xie Di Gu).
 */
public enum XiediguOrganBehavior implements OrganSlowTickListener, OrganRemovalListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_di_gu");
    public static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_di_gu_detonate");

    private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xue_dao_increase_effect");
    private static final ResourceLocation BLEED_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "lliuxue");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = "Xiedigu";
    private static final String TIMER_KEY = "Timer";
    private static final String STORED_KEY = "StoredDrops";
    private static final String DRY_KEY = "Dry";

    private static final int GENERATION_INTERVAL_SLOW_TICKS = 10;
    private static final int DRY_RETRY_INTERVAL_SLOW_TICKS = 1;
    private static final float HEALTH_COST_PER_DROP = 5.0f;
    private static final int DROP_CAPACITY_PER_STACK = 5;

    private static final double DETONATION_RADIUS = 5.0;
    private static final float DETONATION_DAMAGE_PER_DROP = 6.0f;
    private static final int BLEED_DURATION_SECONDS = 3;
    private static final int TICKS_PER_SECOND = 20;
    private static final double BLEED_DAMAGE_PER_SECOND = 1.0;

    private static final double ZHENYUAN_PER_DROP = 20.0;
    private static final double JINGLI_PER_DROP = 20.0;
    private static final float HEAL_PER_DROP = 2.0f;

    private static final int WEAKNESS_DURATION_TICKS = 60;
    private static final int DRY_SMOKE_COUNT = 12;
    private static final DustParticleOptions BLOOD_MIST =
            new DustParticleOptions(new Vector3f(0.85f, 0.06f, 0.06f), 1.0f);

    private static final Component EQUIP_MESSAGE =
            Component.translatable("message.guzhenren.xie_di_gu.on_equip");

    static {
        OrganActivationListeners.register(ABILITY_ID, XiediguOrganBehavior::activateAbility);
    }

    /** Ensures the linkage channel exists for this cavity. */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ensureChannel(GuzhenrenLinkageManager.getContext(cc));
    }

    /** Called when the organ is evaluated inside the chest cavity. */
    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ)) {
            return;
        }

        int slotIndex = ChestCavityUtil.findOrganSlot(cc, organ);
        boolean alreadyRegistered = staleRemovalContexts.removeIf(old ->
                ChestCavityUtil.matchesRemovalContext(old, slotIndex, organ, this));
        cc.onRemovedListeners.add(new OrganRemovalContext(slotIndex, organ, this));

        clampStoredDrops(organ, capacityFor(Math.max(1, organ.getCount())));
        if (readTimer(organ) <= 0) {
            writeTimer(organ, GENERATION_INTERVAL_SLOW_TICKS);
        }
        writeDry(organ, false);

        if (!alreadyRegistered && cc.owner instanceof Player player && !player.level().isClientSide()) {
            player.sendSystemMessage(EQUIP_MESSAGE);
        }
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ)) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        int capacity = capacityFor(stackCount);
        int stored = clampStoredDrops(organ, capacity);
        boolean atCapacity = stored >= capacity;

        int timer = Math.max(0, readTimer(organ));
        boolean wasDry = readDry(organ);
        boolean dry = wasDry;
        boolean dirty = false;

        double efficiency = computeXueDaoMultiplier(cc);
        float drainCost = (float) (HEALTH_COST_PER_DROP / Math.max(1.0, efficiency));

        if (atCapacity) {
            dry = false;
            if (timer <= 0) {
                timer = GENERATION_INTERVAL_SLOW_TICKS;
            }
        } else if (timer <= 0) {
            if (canAffordBlood(player, drainCost)) {
                applyHealthDrain(player, drainCost);
                if (!player.isDeadOrDying()) {
                    stored = Math.min(capacity, stored + 1);
                    writeStoredDrops(organ, stored);
                    spawnBleedGenerationEffects(player, stored, stackCount);
                    dirty = true;
                    dry = false;
                    timer = GENERATION_INTERVAL_SLOW_TICKS;
                }
            } else {
                dry = true;
                timer = DRY_RETRY_INTERVAL_SLOW_TICKS;
            }
        } else {
            timer--;
        }

        writeTimer(organ, timer);
        if (dry != wasDry) {
            writeDry(organ, dry);
            dirty = true;
        }

        if (dirty) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        handleDryState(player, dry, wasDry);
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        writeTimer(organ, 0);
        writeStoredDrops(organ, 0);
        writeDry(organ, false);
        if (entity instanceof Player player) {
            player.removeEffect(MobEffects.WEAKNESS);
        }
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

        int stackCount = Math.max(1, organ.getCount());
        int capacity = capacityFor(stackCount);
        int stored = clampStoredDrops(organ, capacity);
        if (stored <= 0) {
            return;
        }

        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double efficiency = computeXueDaoMultiplier(cc);
        List<LivingEntity> targets = gatherTargets(player, serverLevel, DETONATION_RADIUS);
        if (!targets.isEmpty()) {
            float damage = (float) (DETONATION_DAMAGE_PER_DROP * stored * efficiency);
            for (LivingEntity target : targets) {
                applyTrueDamage(player, target, damage);
            }
            scheduleBleedTicks(serverLevel, player, targets, stored, efficiency);
            applyBleedEffect(targets, stored, efficiency);
        }

        applyRecovery(player, stored);
        playDetonationCues(serverLevel, player, stored);

        writeStoredDrops(organ, 0);
        writeDry(organ, false);
        writeTimer(organ, GENERATION_INTERVAL_SLOW_TICKS);
        player.removeEffect(MobEffects.WEAKNESS);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private static boolean matchesOrgan(ItemStack organ) {
        if (organ == null || organ.isEmpty()) {
            return false;
        }
        if (organ.is(GuzhenrenItems.XIE_DI_GU)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(organ.getItem());
        return ORGAN_ID.equals(id);
    }

    private static int capacityFor(int stackCount) {
        return DROP_CAPACITY_PER_STACK * Math.max(1, stackCount);
    }

    private static List<LivingEntity> gatherTargets(Player player, ServerLevel level, double radius) {
        AABB area = player.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, area, target ->
                target != player && target.isAlive() && !target.isAlliedTo(player));
    }

    private static double computeXueDaoMultiplier(ChestCavityInstance cc) {
        LinkageChannel channel = ensureChannel(GuzhenrenLinkageManager.getContext(cc));
        return Math.max(0.0, 1.0 + channel.get());
    }

    private static void applyRecovery(Player player, int drops) {
        if (player == null || drops <= 0) {
            return;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        handleOpt.ifPresent(handle -> {
            handle.replenishScaledZhenyuan(ZHENYUAN_PER_DROP * drops, true);
            handle.adjustJingli(JINGLI_PER_DROP * drops, true);
        });
        float heal = HEAL_PER_DROP * drops;
        if (heal > 0.0f) {
            player.heal(heal);
        }
    }

    private static void playDetonationCues(ServerLevel server, Player player, int drops) {
        Level level = player.level();
        RandomSource random = player.getRandom();
        float volume = Mth.clamp(0.6f + drops * 0.15f, 0.6f, 2.0f);
        float pitch = Mth.clamp(0.8f - drops * 0.05f + random.nextFloat() * 0.1f, 0.3f, 1.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, volume, pitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8f, 0.55f + random.nextFloat() * 0.1f);

        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.6, 0.0);
        int mistCount = 24 + drops * 16;
        server.sendParticles(BLOOD_MIST, center.x, center.y, center.z, mistCount, 0.6, 0.9, 0.6, 0.2);
        server.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, Math.min(6, drops), 0.15, 0.15, 0.15, 0.02);
    }

    private static void scheduleBleedTicks(ServerLevel server, Player player, List<LivingEntity> victims, int drops, double efficiency) {
        if (victims.isEmpty() || drops <= 0) {
            return;
        }
        List<LivingEntity> targets = victims.stream()
                .filter(LivingEntity::isAlive)
                .collect(Collectors.toCollection(ArrayList::new));
        double perSecond = BLEED_DAMAGE_PER_SECOND * drops * efficiency;
        if (perSecond <= 0.0) {
            return;
        }
        for (int second = 1; second <= BLEED_DURATION_SECONDS; second++) {
            int delay = second * TICKS_PER_SECOND;
            schedule(server, () -> {
                for (LivingEntity target : targets) {
                    if (target.isAlive() && !target.isAlliedTo(player)) {
                        applyTrueDamage(player, target, (float) perSecond);
                    }
                }
            }, delay);
        }
    }

    private static void applyBleedEffect(List<LivingEntity> victims, int drops, double efficiency) {
        if (drops <= 0 || victims.isEmpty()) {
            return;
        }
        Optional<Holder.Reference<net.minecraft.world.effect.MobEffect>> effectOpt =
                BuiltInRegistries.MOB_EFFECT.getHolder(BLEED_EFFECT_ID);
        if (effectOpt.isEmpty()) {
            return;
        }
        double scaled = Math.max(1.0, drops * Math.max(0.0, efficiency));
        int amplifier = Math.max(0, (int) Math.round(scaled) - 1);
        int duration = BLEED_DURATION_SECONDS * TICKS_PER_SECOND;
        for (LivingEntity target : victims) {
            if (!target.isAlive()) {
                continue;
            }
            target.addEffect(new MobEffectInstance(effectOpt.get(), duration, amplifier, false, true, true));
        }
    }

    private static void spawnBleedGenerationEffects(Player player, int stored, int stackCount) {
        Level level = player.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        int count = 8 + stored + stackCount * 2;
        server.sendParticles(BLOOD_MIST, center.x, center.y, center.z, count, 0.25, 0.4, 0.25, 0.05);
        server.sendParticles(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, center.x, center.y, center.z, Math.min(6, stored), 0.05, 0.35, 0.05, 0.01);
    }

    private static void handleDryState(Player player, boolean dry, boolean wasDry) {
        if (player == null) {
            return;
        }
        if (dry) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_DURATION_TICKS, 1, false, true, true));
            if (!wasDry) {
                playDrynessCues(player);
            }
        } else if (wasDry) {
            player.removeEffect(MobEffects.WEAKNESS);
        }
    }

    private static void playDrynessCues(Player player) {
        Level level = player.level();
        RandomSource random = player.getRandom();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.7f, 0.45f + random.nextFloat() * 0.1f);
        if (level instanceof ServerLevel server) {
            Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
            server.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, DRY_SMOKE_COUNT, 0.3, 0.4, 0.3, 0.01);
        }
    }

    private static boolean canAffordBlood(Player player, float cost) {
        float healthPool = player.getHealth() + player.getAbsorptionAmount();
        return healthPool > cost;
    }

    private static void applyHealthDrain(Player player, float amount) {
        if (player == null || amount <= 0.0f) {
            return;
        }
        float startingHealth = player.getHealth();
        float startingAbsorption = player.getAbsorptionAmount();

        player.invulnerableTime = 0;
        player.hurt(player.damageSources().generic(), amount);
        player.invulnerableTime = 0;

        float remaining = amount;
        float absorptionConsumed = Math.min(startingAbsorption, remaining);
        remaining -= absorptionConsumed;
        float targetAbsorption = Math.max(0.0f, startingAbsorption - amount);

        if (!player.isDeadOrDying()) {
            player.setAbsorptionAmount(targetAbsorption);
            if (remaining > 0.0f) {
                float targetHealth = Math.max(0.0f, startingHealth - remaining);
                if (player.getHealth() > targetHealth) {
                    player.setHealth(targetHealth);
                }
            }
            player.hurtTime = 0;
            player.hurtDuration = 0;
        }
    }

    private static void applyTrueDamage(Player source, LivingEntity target, float amount) {
        if (target == null || amount <= 0.0f) {
            return;
        }
        float startingHealth = target.getHealth();
        float startingAbsorption = target.getAbsorptionAmount();

        target.invulnerableTime = 0;
        target.hurt(source == null
                ? target.damageSources().generic()
                : target.damageSources().playerAttack(source), amount);
        target.invulnerableTime = 0;

        float remaining = amount;
        float absorptionConsumed = Math.min(startingAbsorption, remaining);
        remaining -= absorptionConsumed;
        float targetAbsorption = Math.max(0.0f, startingAbsorption - amount);

        if (!target.isDeadOrDying()) {
            target.setAbsorptionAmount(targetAbsorption);
            if (remaining > 0.0f) {
                float targetHealth = Math.max(0.0f, startingHealth - remaining);
                if (target.getHealth() > targetHealth) {
                    target.setHealth(targetHealth);
                }
            }
            target.hurtTime = 0;
            target.invulnerableTime = 0;
        }
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
            if (matchesOrgan(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static LinkageChannel ensureChannel(ActiveLinkageContext context) {
        return context.getOrCreateChannel(XUE_DAO_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
    }

    private static int clampStoredDrops(ItemStack stack, int capacity) {
        int stored = Math.max(0, readStoredDrops(stack));
        int clamped = Math.min(capacity, stored);
        if (clamped != stored) {
            writeStoredDrops(stack, clamped);
        }
        return clamped;
    }

    private static int readTimer(ItemStack stack) {
        CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return 0;
        }
        CompoundTag state = tag.getCompound(STATE_KEY);
        return state.getInt(TIMER_KEY);
    }

    private static void writeTimer(ItemStack stack, int value) {
        int clamped = Math.max(0, value);
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putInt(TIMER_KEY, clamped);
            tag.put(STATE_KEY, state);
        });
    }

    private static int readStoredDrops(ItemStack stack) {
        CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return 0;
        }
        CompoundTag state = tag.getCompound(STATE_KEY);
        return state.getInt(STORED_KEY);
    }

    private static void writeStoredDrops(ItemStack stack, int value) {
        int clamped = Math.max(0, value);
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putInt(STORED_KEY, clamped);
            tag.put(STATE_KEY, state);
        });
    }

    private static boolean readDry(ItemStack stack) {
        CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag state = tag.getCompound(STATE_KEY);
        return state.getBoolean(DRY_KEY);
    }

    private static void writeDry(ItemStack stack, boolean dry) {
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putBoolean(DRY_KEY, dry);
            tag.put(STATE_KEY, state);
        });
    }

    private static void schedule(ServerLevel level, Runnable runnable, int delayTicks) {
        if (delayTicks <= 0) {
            runnable.run();
            return;
        }
        level.getServer().execute(() -> schedule(level, runnable, delayTicks - 1));
    }
}
