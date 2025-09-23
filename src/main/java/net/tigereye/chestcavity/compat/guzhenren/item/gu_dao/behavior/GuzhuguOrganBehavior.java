package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.SaturationPolicy;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bone Bamboo Gu (骨竹蛊) grows a shared bone_growth linkage channel.
 * Passive: each slow tick adds +5 per organ stack.
 * Active: using bone meal near the player injects +20 per stack.
 */
public enum GuzhuguOrganBehavior implements OrganSlowTickListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gu_zhu_gu");
    private static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bone_growth");

    private static final ResourceLocation EMERALD_BONE_GROWTH_CHANNEL =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/emerald_bone_growth");
    private static final ResourceLocation TU_DAO_INCREASE_EFFECT =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/tu_dao_increase_effect");
    private static final ResourceLocation GU_DAO_INCREASE_EFFECT =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/gu_dao_increase_effect");

    private static final double PASSIVE_GAIN = 5.0;
    private static final double ACTIVE_GAIN = 20.0;
    private static final double SOFT_CAP = 120.0;
    private static final double FALL_OFF = 0.5;

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);
    private static final SaturationPolicy SOFT_CAP_POLICY = new SaturationPolicy(SOFT_CAP, FALL_OFF);

    private static final long PASSIVE_LOG_INTERVAL_TICKS = 20L * 5L;
    private static final Map<UUID, Double> PASSIVE_GAIN_BUFFER = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PASSIVE_TICK_BUFFER = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PASSIVE_LAST_LOG_TICK = new ConcurrentHashMap<>();

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player) || entity.level().isClientSide()) {
            return;
        }
        int count = Math.max(1, organ.getCount());

        LinkageChannel channel = ensureChannel(cc,CHANNEL_ID);

        LinkageChannel guDaoEffChannel = ensureChannel(cc,GU_DAO_INCREASE_EFFECT);
        double guDaoEfficiency = (1 + guDaoEffChannel.get());

        double previous = channel.get();
        
        // 一转就不用扣点数了
        double expected = PASSIVE_GAIN * count * guDaoEfficiency;

        double newValue = channel.adjust(expected);
        double actual = newValue - previous;
        if (actual > 0.0) {
            recordPassiveGain((Player) entity, actual, newValue);
            handleSoftCapCross((Player) entity, previous, newValue);
        } else {
            maybeFlushPassiveBuffer((Player) entity, newValue);
        }

    }

    /** Triggered when the player uses bone meal; applies the active boost when off cooldown. */
    public boolean onBoneMealCatalyst(Player player, ChestCavityInstance cc) {
        if (player == null || cc == null || player.level().isClientSide()) {
            return false;
        }
        int totalStacks = countOrgans(cc);
        if (totalStacks <= 0) {
            return false;
        }

        LinkageChannel guDaoEffChannel = ensureChannel(cc,GU_DAO_INCREASE_EFFECT);
        double guDaoEfficiency = (1 + guDaoEffChannel.get());

        LinkageChannel channel = ensureChannel(cc,CHANNEL_ID);
        double previous = channel.get();
        flushPassiveDebug(player, previous);
        double expected = ACTIVE_GAIN * totalStacks * guDaoEfficiency;
        double newValue = channel.adjust(expected);
        double actual = newValue - previous;
        if (actual <= 0.0) {
            return false;
        }

        sendDebugMessage(player, "ACTIVE", actual, newValue, 1);
        playCatalystCue(player.level(), player);
        handleSoftCapCross(player, previous, newValue);
        return true;
    }

    /** Prepares the linkage channel for this chest cavity (idempotent). */
    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannelSoft(cc,CHANNEL_ID);
        ensureChannelSoft(cc,EMERALD_BONE_GROWTH_CHANNEL);
        ensureChannel(cc,GU_DAO_INCREASE_EFFECT);
    }

    public boolean hasOrgan(ChestCavityInstance cc) {
        return countOrgans(cc) > 0;
    }

        // --- 通用初始化 ---
    private static LinkageChannel ensureChannel(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(id)
                    .addPolicy(NON_NEGATIVE);
    }

            // --- 通用初始化 ---
    private static LinkageChannel ensureChannelSoft(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(id)
                    .addPolicy(NON_NEGATIVE)
                    .addPolicy(SOFT_CAP_POLICY);
    }

    private static int countOrgans(ChestCavityInstance cc) {
        int total = 0;
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (ORGAN_ID.equals(id)) {
                total += Math.max(1, stack.getCount());
            }
        }
        return total;
    }

    private static void playCatalystCue(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BONE_BLOCK_PLACE, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    private static void sendDebugMessage(Player player, String action, double actual, double newValue, int sampleCount) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        String formattedActual = String.format("%.1f", actual);
        String formattedNewValue = String.format("%.1f", newValue);
        if (sampleCount > 1) {
            ChestCavity.LOGGER.info(
                    "[Guzhugu] {} +{} -> {} ({} ticks aggregated)",
                    action,
                    formattedActual,
                    formattedNewValue,
                    sampleCount
            );
        } else {
            ChestCavity.LOGGER.info("[Guzhugu] {} +{} -> {}", action, formattedActual, formattedNewValue);
        }
    }

    private static void recordPassiveGain(Player player, double delta, double newValue) {
        UUID id = player.getUUID();
        long gameTime = player.level().getGameTime();
        PASSIVE_GAIN_BUFFER.merge(id, delta, Double::sum);
        PASSIVE_TICK_BUFFER.merge(id, 1, Integer::sum);
        long last = PASSIVE_LAST_LOG_TICK.getOrDefault(id, Long.MIN_VALUE);
        if (gameTime - last >= PASSIVE_LOG_INTERVAL_TICKS) {
            Double totalObj = PASSIVE_GAIN_BUFFER.remove(id);
            Integer tickObj = PASSIVE_TICK_BUFFER.remove(id);
            if (totalObj != null && totalObj > 0.0) {
                int samples = tickObj == null ? 1 : Math.max(1, tickObj);
                sendDebugMessage(player, "PASSIVE", totalObj, newValue, samples);
            }
            PASSIVE_LAST_LOG_TICK.put(id, gameTime);
        }
    }

    private static void flushPassiveDebug(Player player, double currentValue) {
        UUID id = player.getUUID();
        Double totalObj = PASSIVE_GAIN_BUFFER.remove(id);
        Integer tickObj = PASSIVE_TICK_BUFFER.remove(id);
        if (totalObj != null && totalObj > 0.0) {
            int samples = tickObj == null ? 1 : Math.max(1, tickObj);
            sendDebugMessage(player, "PASSIVE", totalObj, currentValue, samples);
        }
        PASSIVE_LAST_LOG_TICK.put(id, player.level().getGameTime());
    }

    private static void maybeFlushPassiveBuffer(Player player, double currentValue) {
        UUID id = player.getUUID();
        if (!PASSIVE_GAIN_BUFFER.containsKey(id)) {
            return;
        }
        long gameTime = player.level().getGameTime();
        long last = PASSIVE_LAST_LOG_TICK.getOrDefault(id, Long.MIN_VALUE);
        if (gameTime - last < PASSIVE_LOG_INTERVAL_TICKS) {
            return;
        }
        Double totalObj = PASSIVE_GAIN_BUFFER.remove(id);
        Integer tickObj = PASSIVE_TICK_BUFFER.remove(id);
        if (totalObj != null && totalObj > 0.0) {
            int samples = tickObj == null ? 1 : Math.max(1, tickObj);
            sendDebugMessage(player, "PASSIVE", totalObj, currentValue, samples);
        }
        PASSIVE_LAST_LOG_TICK.put(id, gameTime);
    }

    private static void handleSoftCapCross(Player player, double previous, double updated) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        // 设定: 100 超过 20 冗余
        if (previous < (SOFT_CAP-20) && updated >= (SOFT_CAP-20)) {
            playSoftCapEffects(player);
        }
    }

    private static void playSoftCapEffects(Player player) {
        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.9f, 1.1f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 0.7f, 1.0f);


        if (level instanceof ServerLevel server) {
            spawnRing(server, player, ParticleTypes.CRIT, 16, 0.8);
            spawnRing(server, player, ParticleTypes.ENCHANTED_HIT, 12, 0.6);
            spawnRing(server, player, ParticleTypes.HAPPY_VILLAGER, 10, 0.9);
        }
    }

    private static void spawnRing(ServerLevel server, Player player, ParticleOptions particle, int count, double radius) {
        double centerX = player.getX();
        double centerY = player.getY() + player.getBbHeight() * 0.6;
        double centerZ = player.getZ();
        var random = player.getRandom();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double jitter = 0.15 * random.nextDouble();
            double offsetX = Math.cos(angle) * radius + (random.nextDouble() - 0.5) * jitter;
            double offsetZ = Math.sin(angle) * radius + (random.nextDouble() - 0.5) * jitter;
            double offsetY = (random.nextDouble() - 0.5) * 0.2;
            server.sendParticles(particle,
                    centerX + offsetX,
                    centerY + offsetY,
                    centerZ + offsetZ,
                    1,
                    0.0, 0.02, 0.0,
                    0.0);
        }
    }
}
