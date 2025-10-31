package net.tigereye.chestcavity.compat.common.organ.yu;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.entity.summon.OwnedSharkEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.common.tuning.YuLinGuTuning;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;
import net.minecraft.util.Mth;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.minecraft.server.level.ServerPlayer;


public class YuLinGuCalculator {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean isPlayerMoist(Player player, OrganState state, long gameTime) {
        long lastWet = state.getLong(YuLinGuTuning.LAST_WET_TICK_KEY, gameTime);
        if (player.isInWaterRainOrBubble()) {
            return true;
        }
        return gameTime - lastWet <= YuLinGuTuning.WET_BUFFER_TICKS;
    }

    public static void applyArmorBuffs(Player player, boolean hasSharkArmor) {
        int graceAmplifier = hasSharkArmor ? YuLinGuTuning.ARMOR_GRACE_AMP_SHARK : 0;
        player.addEffect(new MobEffectInstance(
            MobEffects.DOLPHINS_GRACE, YuLinGuTuning.ARMOR_GRACE_TICKS, graceAmplifier, true, false));
        player.addEffect(new MobEffectInstance(
            MobEffects.WATER_BREATHING, YuLinGuTuning.ARMOR_GRACE_TICKS, 0, true, false));
        if (hasSharkArmor) {
            player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, YuLinGuTuning.ARMOR_RESIST_TICKS, 0, true, false));
        }
    }

    public static void drainHunger(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        FoodData stats = player.getFoodData();
        stats.setFoodLevel(Math.max(0, stats.getFoodLevel() - amount));
    }

    public static void tickSummons(ServerLevel level, Player owner, long gameTime) {
        if (level == null || owner == null) {
            return;
        }
        synchronized (owner) {
            ItemStack organ = findYuLinGuOrgan(owner);
            if (organ.isEmpty()) {
                return;
            }
            OrganState state = OrganState.of(organ, YuLinGuTuning.STATE_ROOT);
            List<OwnedSharkEntity> summons = loadSummonsFromState(state);
            if (summons.isEmpty()) {
                return;
            }
            List<OwnedSharkEntity> updated = new ArrayList<>(summons.size());
            for (OwnedSharkEntity summon : summons) {
                if (summon == null) {
                    continue;
                }
                if (!summon.tick(level, owner, gameTime)) {
                    continue;
                }
                updated.add(summon);
            }
            if (updated.size() != summons.size()) {
                saveSummonsToState(state, updated);
                ChestCavityInstance cc =
                    ChestCavityEntity.of(owner).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
                if (cc != null) {
                    NetworkUtil.sendOrganSlotUpdate(cc, organ);
                }
            }
        }
    }

    public static List<OwnedSharkEntity> loadSummonsFromState(OrganState state) {
        if (state == null) {
            return new ArrayList<>();
        }
        ListTag listTag = state.getList(YuLinGuTuning.ACTIVE_SUMMONS_KEY, Tag.TAG_COMPOUND);
        List<OwnedSharkEntity> summons = new ArrayList<>(listTag.size());
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag summonTag = listTag.getCompound(i);
            try {
                UUID entityId = summonTag.getUUID("EntityId");
                UUID ownerId = summonTag.getUUID("OwnerId");
                int tier = summonTag.getInt("Tier");
                long createdAt = summonTag.getLong("CreatedAt");
                long expiresAt = summonTag.getLong("ExpiresAt");
                summons.add(new OwnedSharkEntity(entityId, ownerId, tier, createdAt, expiresAt));
            } catch (Exception e) {
                LOGGER.warn("Failed to load summon from state", e);
            }
        }
        return summons;
    }

    public static void saveSummonsToState(OrganState state, List<OwnedSharkEntity> summons) {
        if (state == null) {
            return;
        }
        ListTag listTag = new ListTag();
        for (OwnedSharkEntity summon : summons) {
            CompoundTag summonTag = new CompoundTag();
            summonTag.putUUID("EntityId", summon.entityId());
            summonTag.putUUID("OwnerId", summon.ownerId());
            summonTag.putInt("Tier", summon.tier());
            summonTag.putLong("CreatedAt", summon.createdAt());
            summonTag.putLong("ExpiresAt", summon.expiresAt());
            listTag.add(summonTag);
        }
        state.setList(YuLinGuTuning.ACTIVE_SUMMONS_KEY, listTag);
    }

    public static ItemStack findYuLinGuOrgan(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ChestCavityInstance cc =
            ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (matchesOrgan(stack, YuLinGuTuning.ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean matchesOrgan(ItemStack stack, ResourceLocation organId) {
        if (stack.isEmpty() || organId == null) {
            return false;
        }
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return organId.equals(id);
    }

    public static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation organId) {
        if (cc == null || organId == null || cc.inventory == null) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (organId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasTailSynergy(ChestCavityInstance cc) {
        return hasOrgan(cc, YuLinGuTuning.JIAO_WEI_GU_ID);
    }

    public static boolean hasSharkArmor(ItemStack organ) {
        return OrganState.of(organ, YuLinGuTuning.STATE_ROOT)
            .getBoolean(YuLinGuTuning.HAS_SHARK_ARMOR_KEY, false);
    }

    public static void recordWetContact(Player player, ItemStack organ) {
        if (player == null || organ == null || organ.isEmpty()) {
            return;
        }
        OrganState state = OrganState.of(organ, YuLinGuTuning.STATE_ROOT);
        state.setLong(YuLinGuTuning.LAST_WET_TICK_KEY, player.level().getGameTime());
    }

    public static void addProgress(Player player, ChestCavityInstance cc, ItemStack organ, int amount) {
        if (organ == null || organ.isEmpty()) {
            return;
        }
        OrganState state = OrganState.of(organ, YuLinGuTuning.STATE_ROOT);
        grantProgress(player, cc, organ, state, amount);
    }

    private static void grantProgress(
        Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, int amount) {
        if (player == null || cc == null || organ == null || organ.isEmpty() || amount <= 0) {
            return;
        }
        int previous = Mth.clamp(state.getInt(YuLinGuTuning.PROGRESS_KEY, 0), 0, YuLinGuTuning.FISH_ARMOR_MAX_PROGRESS);
        int updated = Mth.clamp(previous + amount, 0, YuLinGuTuning.FISH_ARMOR_MAX_PROGRESS);
        if (updated != previous) {
            state.setInt(YuLinGuTuning.PROGRESS_KEY, updated);
            if (updated >= YuLinGuTuning.SHARK_ARMOR_THRESHOLD) {
                state.setBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, true);
            }
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    public static boolean hasFishArmor(ItemStack organ) {
        return OrganState.of(organ, YuLinGuTuning.STATE_ROOT)
            .getBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, false);
    }

    public static int unlockedSharkTier(ItemStack organ) {
        return Math.max(1, OrganState.of(organ, YuLinGuTuning.STATE_ROOT)
            .getInt(YuLinGuTuning.SHARK_TIER_UNLOCKED_KEY, 1));
    }

    public static void unlockSharkTier(ItemStack organ, int tier) {
        if (tier <= 0) {
            return;
        }
        OrganState state = OrganState.of(organ, YuLinGuTuning.STATE_ROOT);
        int previous = Math.max(1, state.getInt(YuLinGuTuning.SHARK_TIER_UNLOCKED_KEY, 1));
        if (tier > previous) {
            state.setInt(YuLinGuTuning.SHARK_TIER_UNLOCKED_KEY, tier);
            state.setBoolean(YuLinGuTuning.HAS_SHARK_ARMOR_KEY, true);
        }
    }

    public static void addSummon(Player owner, OwnedSharkEntity summon) {
        if (owner == null || summon == null) {
            return;
        }
        synchronized (owner) {
            ItemStack organ = findYuLinGuOrgan(owner);
            if (organ.isEmpty()) {
                return;
            }
            OrganState state = OrganState.of(organ, YuLinGuTuning.STATE_ROOT);
            List<OwnedSharkEntity> summons = loadSummonsFromState(state);
            summons.add(summon);
            saveSummonsToState(state, summons);

            ChestCavityInstance cc =
                ChestCavityEntity.of(owner).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
            if (cc != null) {
                NetworkUtil.sendOrganSlotUpdate(cc, organ);
            }
        }
    }

    public static List<OwnedSharkEntity> getSummons(Player owner) {
        if (owner == null) {
            return List.of();
        }
        ItemStack organ = findYuLinGuOrgan(owner);
        if (organ.isEmpty()) {
            return List.of();
        }
        OrganState state = OrganState.of(organ, YuLinGuTuning.STATE_ROOT);
        return loadSummonsFromState(state);
    }

    public static void removeSummon(Player owner, OwnedSharkEntity summon) {
        if (owner == null || summon == null) {
            return;
        }
        synchronized (owner) {
            ItemStack organ = findYuLinGuOrgan(owner);
            if (organ.isEmpty()) {
                return;
            }
            OrganState state = OrganState.of(organ, YuLinGuTuning.STATE_ROOT);
            List<OwnedSharkEntity> summons = loadSummonsFromState(state);
            summons.removeIf(candidate -> Objects.equals(candidate.entityId(), summon.entityId()));
            saveSummonsToState(state, summons);

            ChestCavityInstance cc =
                ChestCavityEntity.of(owner).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
            if (cc != null) {
                NetworkUtil.sendOrganSlotUpdate(cc, organ);
            }
        }
    }
}
