package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.GangjinguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.JingtieguguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.TieGuGuOrganBehavior;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;

import java.util.Objects;

/**
 * Shared utilities for the Steel Bone + Iron Bone combo logic.
 */
public final class SteelBoneComboHelper {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation STEEL_BONE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "ganjingu");
    private static final ResourceLocation IRON_BONE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tie_gu_gu");
    private static final ResourceLocation REFINED_IRON_BONE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jingtiegugu");

    private static final ResourceLocation GU_DAO_INCREASE_CHANNEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/gu_dao_increase_effect");
    private static final ResourceLocation JIN_DAO_INCREASE_CHANNEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jin_dao_increase_effect");
    private static final ResourceLocation BONE_GROWTH_CHANNEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bone_growth");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final DustParticleOptions PRIMARY_SPARK =
            new DustParticleOptions(new org.joml.Vector3f(1.0f, 0.94f, 0.70f), 1.25f);
    private static final DustParticleOptions SECONDARY_SPARK =
            new DustParticleOptions(new org.joml.Vector3f(0.86f, 0.42f, 0.20f), 1.1f);

    private SteelBoneComboHelper() {
    }

    public static ComboState analyse(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ComboState.EMPTY;
        }
        int steel = 0;
        int iron = 0;
        int refined = 0;
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (STEEL_BONE_ID.equals(id)) {
                steel += Math.max(1, stack.getCount());
            } else if (IRON_BONE_ID.equals(id)) {
                iron += Math.max(1, stack.getCount());
            } else if (REFINED_IRON_BONE_ID.equals(id)) {
                refined += Math.max(1, stack.getCount());
            }
        }
        return new ComboState(steel, iron, refined);
    }

    public static boolean hasSteel(ComboState state) {
        return state != null && state.steel() > 0;
    }

    public static boolean hasActiveCombo(ComboState state) {
        return state != null && state.steel() > 0 && (state.iron() > 0 || state.refined() > 0);
    }

    public static boolean hasRefinedCombo(ComboState state) {
        return state != null && state.steel() > 0 && state.refined() > 0;
    }

    public static boolean isPrimarySteelOrgan(ChestCavityInstance cc, ItemStack organ) {
        return isPrimaryMatch(cc, organ, STEEL_BONE_ID);
    }

    public static boolean isPrimaryRefinedOrgan(ChestCavityInstance cc, ItemStack organ) {
        return isPrimaryMatch(cc, organ, REFINED_IRON_BONE_ID);
    }

    private static boolean isPrimaryMatch(ChestCavityInstance cc, ItemStack organ, ResourceLocation id) {
        if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
            return false;
        }
        Item expected = organ.getItem();
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (stack == organ) {
                return true;
            }
            ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(stackId, id) || stack.getItem() == expected) {
                return false;
            }
        }
        return false;
    }

    public static boolean consumeMaintenanceHunger(Player player) {
        if (player == null || player.isCreative()) {
            return true;
        }
        FoodData food = player.getFoodData();
        if (food == null) {
            return false;
        }
        float saturation = food.getSaturationLevel();
        if (saturation > 0.0f) {
            float updated = Math.max(0.0f, saturation - 1.0f);
            food.setSaturation(Math.min(updated, food.getFoodLevel()));
            return true;
        }
        int hunger = food.getFoodLevel();
        if (hunger > 0) {
            food.setFoodLevel(Math.max(0, hunger - 1));
            return true;
        }
        return false;
    }

    public static void restoreJingli(Player player, double amount) {
        if (player == null || amount <= 0.0) {
            return;
        }
        GuzhenrenResourceBridge.open(player).ifPresent(handle -> handle.adjustJingli(amount, true));
    }

    public static double guDaoIncrease(ChestCavityInstance cc) {
        return readIncrease(cc, GU_DAO_INCREASE_CHANNEL);
    }

    public static double jinDaoIncrease(ChestCavityInstance cc) {
        return readIncrease(cc, JIN_DAO_INCREASE_CHANNEL);
    }

    private static double readIncrease(ChestCavityInstance cc, ResourceLocation channelId) {
        if (cc == null) {
            return 0.0;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        return context.lookupChannel(channelId).map(LinkageChannel::get).orElse(0.0);
    }

    public static boolean tryConsumeBoneEnergy(ChestCavityInstance cc, double amount) {
        if (cc == null || amount <= 0.0) {
            return true;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        LinkageChannel channel = context.getOrCreateChannel(BONE_GROWTH_CHANNEL).addPolicy(NON_NEGATIVE);
        double available = channel.get();
        if (available + 1.0E-4 < amount) {
            return false;
        }
        channel.adjust(-amount);
        return true;
    }

    public static void ensureAbsorptionCapacity(LivingEntity entity, ChestCavityInstance cc) {
        if (entity == null) {
            return;
        }
        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_ABSORPTION);
        if (attribute == null) {
            return;
        }
        double desired = computeAbsorptionCapacity(cc);
        if (Math.abs(attribute.getBaseValue() - desired) > 1.0E-3) {
            attribute.setBaseValue(desired);
        }
    }

    private static double computeAbsorptionCapacity(ChestCavityInstance cc) {
        if (cc == null) {
            return 0.0;
        }
        ComboState state = analyse(cc);
        double steelCap = state.steel() > 0 ? state.steel() * GangjinguOrganBehavior.ABSORPTION_PER_STACK : 0.0;
        double ironCap = state.iron() > 0 ? state.iron() * TieGuGuOrganBehavior.ABSORPTION_PER_STACK : 0.0;
        double refinedCap = state.refined() > 0 ? state.refined() * JingtieguguOrganBehavior.ABSORPTION_PER_STACK : 0.0;
        return Math.max(0.0, Math.max(steelCap, Math.max(ironCap, refinedCap)));
    }

    public static void spawnImpactFx(ServerLevel level, LivingEntity target, Vec3 hintPosition) {
        if (level == null || target == null) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 centre = hintPosition == null ? target.getBoundingBox().getCenter() : hintPosition;
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 0.15 + random.nextDouble() * 0.25;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = 0.18 + random.nextDouble() * 0.12;
            DustParticleOptions particle = (i % 2 == 0) ? PRIMARY_SPARK : SECONDARY_SPARK;
            level.sendParticles(particle, centre.x(), centre.y(), centre.z(), 1, vx, vy, vz, 0.0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x(), centre.y(), centre.z(), 6, 0.15, 0.15, 0.15, 0.02);
        level.playSound(null, centre.x(), centre.y(), centre.z(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f,
                1.05f + (random.nextFloat() - 0.5f) * 0.1f);
        level.playSound(null, centre.x(), centre.y(), centre.z(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.4f,
                1.3f + (random.nextFloat() - 0.5f) * 0.2f);
    }

    public static boolean shouldBlockNaturalRegen(Player player, ChestCavityInstance cc, ComboState state) {
        if (player == null || cc == null) {
            return false;
        }
        if (!player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
            return false;
        }
        return hasActiveCombo(state);
    }

    private static boolean isOrganMatch(ItemStack stack, ResourceLocation id) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return Objects.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()), id);
    }

    public static boolean isSteelOrgan(ItemStack stack) {
        return isOrganMatch(stack, STEEL_BONE_ID);
    }

    public static boolean isRefinedOrgan(ItemStack stack) {
        return isOrganMatch(stack, REFINED_IRON_BONE_ID);
    }

    public static boolean isIronOrgan(ItemStack stack) {
        return isOrganMatch(stack, IRON_BONE_ID);
    }

    public record ComboState(int steel, int iron, int refined) {
        private static final ComboState EMPTY = new ComboState(0, 0, 0);
    }
}
