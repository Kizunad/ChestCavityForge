package net.tigereye.chestcavity.compat.guzhenren.nudao;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Reflection-based bridge focused on Guzhenren's "奴道" (Nudao) attachment data. This exposes
 * an ergonomic API for third-party features to inspect and mutate slot assignments, ownership
 * markers and per-subject bonuses without hard-linking to the upstream mod.
 */
public final class GuzhenrenNudaoBridge {

    private static final Logger LOGGER = ChestCavity.LOGGER;
    private static final String LOG_PREFIX = "[compat/guzhenren][nudao]";

    private static final int SLOT_COUNT = 10;
    private static final double EPSILON = 1.0e-6;

    private static final String SLOT_COST_TEMPLATE = "nudaolanwei_%d";
    private static final String SLOT_SPECIES_TEMPLATE = "nudaolanwei_%d_1";
    private static final String FIELD_SELECTED_SLOT = "nudaolanwei";
    private static final String FIELD_SLOT_COUNT = "nudaoshuliang";
    private static final String FIELD_MAX_HUNPO = "zuida_hunpo";
    private static final String FIELD_OWNER = "nudaozhuren";
    private static final String FIELD_STAR_RATING = "nudaoxingji";

    private static volatile boolean attemptedInit = false;
    private static volatile boolean available = false;

    private static Class<?> playerVariablesClass;
    private static final Field[] SLOT_COST_FIELDS = new Field[SLOT_COUNT];
    private static final Field[] SLOT_SPECIES_FIELDS = new Field[SLOT_COUNT];
    private static Field selectedSlotField;
    private static Field slotCountField;
    private static Field maxHunpoField;
    private static Field ownerField;
    private static Field starRatingField;

    private GuzhenrenNudaoBridge() {
    }

    /** @return true when the Nudao bridge is initialised and can service requests. */
    public static boolean isAvailable() {
        ensureInitialised();
        return available;
    }

    /** Opens a handle for interacting with the player's Nudao slots. */
    public static Optional<NudaoPlayerHandle> openPlayer(Player player) {
        ensureInitialised();
        if (!available || player == null) {
            return Optional.empty();
        }
        Optional<Object> variables = GuzhenrenResourceBridge.fetchVariables(player);
        if (variables.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NudaoPlayerHandle(player, variables.get()));
    }

    /** Opens a handle for a living entity that may be enslaved via Nudao. */
    public static Optional<NudaoSubjectHandle> openSubject(LivingEntity entity) {
        ensureInitialised();
        if (!available || entity == null || ownerField == null) {
            return Optional.empty();
        }
        Optional<Object> variables = GuzhenrenResourceBridge.fetchVariables(entity);
        if (variables.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NudaoSubjectHandle(entity, variables.get()));
    }

    private static void ensureInitialised() {
        if (attemptedInit) {
            return;
        }
        synchronized (GuzhenrenNudaoBridge.class) {
            if (attemptedInit) {
                return;
            }
            attemptedInit = true;
            if (!GuzhenrenResourceBridge.isAvailable()) {
                if (verboseLoggingEnabled()) {
                    LOGGER.debug("{} Skipping Nudao bridge initialisation: resource bridge unavailable", LOG_PREFIX);
                }
                return;
            }
            Optional<Class<?>> clazzOpt = GuzhenrenResourceBridge.getPlayerVariablesClass();
            if (clazzOpt.isEmpty()) {

                if (verboseLoggingEnabled()) {
                    LOGGER.warn("{} Unable to resolve Guzhenren PlayerVariables class; Nudao bridge disabled", LOG_PREFIX);
                }

                return;
            }
            playerVariablesClass = clazzOpt.get();
            initialiseFields();
            available = validateFields();
            if (available) {

                if (verboseLoggingEnabled()) {
                    LOGGER.info("{} Nudao bridge initialised ({} slot pairs, owner field present={})", LOG_PREFIX,
                            SLOT_COUNT, ownerField != null);
                }
            } else {
                if (verboseLoggingEnabled()) {
                    LOGGER.warn("{} Nudao bridge disabled due to missing required fields", LOG_PREFIX);
                }

            }
        }
    }

    private static void initialiseFields() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            String costName = String.format(Locale.ROOT, SLOT_COST_TEMPLATE, i + 1);
            String speciesName = String.format(Locale.ROOT, SLOT_SPECIES_TEMPLATE, i + 1);
            SLOT_COST_FIELDS[i] = resolveField(costName, true);
            SLOT_SPECIES_FIELDS[i] = resolveField(speciesName, true);
        }
        selectedSlotField = resolveField(FIELD_SELECTED_SLOT, false);
        slotCountField = resolveField(FIELD_SLOT_COUNT, true);
        maxHunpoField = resolveField(FIELD_MAX_HUNPO, false);
        ownerField = resolveField(FIELD_OWNER, false);
        starRatingField = resolveField(FIELD_STAR_RATING, false);
    }

    private static boolean validateFields() {
        if (playerVariablesClass == null) {
            return false;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (SLOT_COST_FIELDS[i] == null || SLOT_SPECIES_FIELDS[i] == null) {
                if (verboseLoggingEnabled()) {
                    LOGGER.warn("{} Missing Nudao slot fields for index {}", LOG_PREFIX, i + 1);
                }

                return false;
            }
        }
        if (slotCountField == null) {

            if (verboseLoggingEnabled()) {
                LOGGER.warn("{} Missing Nudao slot count field '{}'", LOG_PREFIX, FIELD_SLOT_COUNT);
            }
            return false;
        }
        if (ownerField == null) {
            if (verboseLoggingEnabled()) {
                LOGGER.warn("{} Missing Nudao owner field '{}'; subject handles will be unavailable", LOG_PREFIX, FIELD_OWNER);
            }

        }
        return true;
    }

    private static Field resolveField(String name, boolean required) {
        if (playerVariablesClass == null || name == null || name.isBlank()) {
            return null;
        }
        try {
            Field field = playerVariablesClass.getField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            if (verboseLoggingEnabled()) {
                if (required) {
                    LOGGER.warn("{} PlayerVariables missing required field '{}'", LOG_PREFIX, name);
                } else {
                    LOGGER.debug("{} PlayerVariables missing optional field '{}'", LOG_PREFIX, name);
                }

            }
            return null;
        }
    }

    private static OptionalDouble readNumber(Field field, Object variables) {
        if (field == null || variables == null) {
            return OptionalDouble.empty();
        }
        try {
            Class<?> type = field.getType();
            if (type == double.class) {
                return OptionalDouble.of(field.getDouble(variables));
            } else if (type == float.class) {
                return OptionalDouble.of(field.getFloat(variables));
            } else if (type == int.class) {
                return OptionalDouble.of(field.getInt(variables));
            } else if (type == long.class) {
                return OptionalDouble.of(field.getLong(variables));
            }
            if (verboseLoggingEnabled()) {
                LOGGER.debug("{} Field '{}' is not numeric (found {})", LOG_PREFIX, field.getName(), type.getName());
            }
        } catch (IllegalAccessException e) {
            if (verboseLoggingEnabled()) {
                LOGGER.warn("{} Failed to read Nudao field '{}'", LOG_PREFIX, field.getName(), e);
            }

        }
        return OptionalDouble.empty();
    }

    private static OptionalInt readInt(Field field, Object variables) {
        OptionalDouble valueOpt = readNumber(field, variables);
        if (valueOpt.isEmpty()) {
            return OptionalInt.empty();
        }
        double value = valueOpt.getAsDouble();
        if (!Double.isFinite(value)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of((int) Math.round(value));
    }

    private static boolean writeNumber(Field field, Object variables, double value) {
        if (field == null || variables == null || !Double.isFinite(value)) {
            return false;
        }
        try {
            Class<?> type = field.getType();
            if (type == double.class) {
                field.setDouble(variables, value);
                return true;
            } else if (type == float.class) {
                field.setFloat(variables, (float) value);
                return true;
            } else if (type == int.class) {
                field.setInt(variables, (int) Math.round(value));
                return true;
            } else if (type == long.class) {
                field.setLong(variables, Math.round(value));
                return true;
            }
            if (verboseLoggingEnabled()) {
                LOGGER.debug("{} Field '{}' is not numeric (found {})", LOG_PREFIX, field.getName(), type.getName());
            }
        } catch (IllegalAccessException e) {
            if (verboseLoggingEnabled()) {
                LOGGER.warn("{} Failed to write Nudao field '{}'", LOG_PREFIX, field.getName(), e);
            }

        }
        return false;
    }

    private static Optional<String> readString(Field field, Object variables) {
        if (field == null || variables == null) {
            return Optional.empty();
        }
        if (field.getType() != String.class) {
            if (verboseLoggingEnabled()) {
                LOGGER.debug("{} Field '{}' is not a string (found {})", LOG_PREFIX, field.getName(), field.getType().getName());
            }

            return Optional.empty();
        }
        try {
            Object value = field.get(variables);
            if (value instanceof String string) {
                return Optional.of(string);
            }
        } catch (IllegalAccessException e) {
            if (verboseLoggingEnabled()) {
                LOGGER.warn("{} Failed to read Nudao string field '{}'", LOG_PREFIX, field.getName(), e);
            }

        }
        return Optional.empty();
    }

    private static boolean writeString(Field field, Object variables, String value) {
        if (field == null || variables == null) {
            return false;
        }
        if (field.getType() != String.class) {
            if (verboseLoggingEnabled()) {
                LOGGER.debug("{} Field '{}' is not a string (found {})", LOG_PREFIX, field.getName(), field.getType().getName());
            }

            return false;
        }
        try {
            field.set(variables, value);
            return true;
        } catch (IllegalAccessException e) {
            if (verboseLoggingEnabled()) {
                LOGGER.warn("{} Failed to write Nudao string field '{}'", LOG_PREFIX, field.getName(), e);
            }

            return false;
        }
    }

    private static boolean isValidSlot(int index) {
        return index >= 1 && index <= SLOT_COUNT;
    }

    private static boolean verboseLoggingEnabled() {
        return ChestCavity.config == null || ChestCavity.config.GUZHENREN_NUDAO_LOGGING;
    }


    /** Lightweight immutable representation of a Nudao slot entry. */
    public record NudaoSlotState(int index, double soulCost, int speciesIndex) {

        public NudaoSlotState {
            if (index < 1) {
                throw new IllegalArgumentException("Slot index must be >= 1");
            }
        }

        public boolean occupied() {
            double cost = Double.isFinite(soulCost) ? soulCost : 0.0;
            int species = Math.max(0, speciesIndex);
            return Math.abs(cost) > EPSILON || species > 0;
        }

        public NudaoSlotState sanitised() {
            double cost = Double.isFinite(soulCost) ? Math.max(0.0, soulCost) : 0.0;
            int species = Math.max(0, speciesIndex);
            return new NudaoSlotState(index, cost, species);
        }

        public static NudaoSlotState empty(int index) {
            return new NudaoSlotState(index, 0.0, 0);
        }
    }

    /** Handle for player-side Nudao slot management. */
    public static final class NudaoPlayerHandle {

        private final Player player;
        private final Object variables;

        private NudaoPlayerHandle(Player player, Object variables) {
            this.player = player;
            this.variables = variables;
        }

        public Player player() {
            return player;
        }

        public int slotCount() {
            return SLOT_COUNT;
        }

        public List<NudaoSlotState> getSlots() {
            List<NudaoSlotState> slots = new ArrayList<>(SLOT_COUNT);
            for (int i = 1; i <= SLOT_COUNT; i++) {
                slots.add(getSlot(i).orElse(NudaoSlotState.empty(i)));
            }
            return Collections.unmodifiableList(slots);
        }

        public Optional<NudaoSlotState> getSlot(int index) {
            if (!isValidSlot(index)) {
                return Optional.empty();
            }
            Field costField = SLOT_COST_FIELDS[index - 1];
            Field speciesField = SLOT_SPECIES_FIELDS[index - 1];
            OptionalDouble cost = readNumber(costField, variables);
            OptionalDouble species = readNumber(speciesField, variables);
            if (cost.isEmpty() && species.isEmpty()) {
                return Optional.empty();
            }
            double costValue = cost.orElse(0.0);
            int speciesValue = (int) Math.round(species.orElse(0.0));
            return Optional.of(new NudaoSlotState(index, costValue, speciesValue));
        }

        public OptionalInt findFirstAvailableSlot() {
            for (int i = 1; i <= SLOT_COUNT; i++) {
                Optional<NudaoSlotState> slotOpt = getSlot(i);
                if (slotOpt.isEmpty() || !slotOpt.get().occupied()) {
                    return OptionalInt.of(i);
                }
            }
            return OptionalInt.empty();
        }

        public OptionalInt getOccupiedCount() {
            return readInt(slotCountField, variables);
        }

        public boolean setOccupiedCount(int count) {
            if (slotCountField == null) {
                return false;
            }
            int value = Math.max(0, count);
            boolean success = writeNumber(slotCountField, variables, value);
            if (success) {
                sync();
            }
            return success;
        }

        public boolean adjustOccupiedCount(int delta) {
            OptionalInt currentOpt = getOccupiedCount();
            if (currentOpt.isEmpty()) {
                return false;
            }
            int target = Math.max(0, currentOpt.getAsInt() + delta);
            return setOccupiedCount(target);
        }

        public OptionalInt getSelectedSlot() {
            if (selectedSlotField == null) {
                return OptionalInt.empty();
            }
            return readInt(selectedSlotField, variables);
        }

        public boolean setSelectedSlot(int slotIndex) {
            if (selectedSlotField == null || !isValidSlot(slotIndex)) {
                return false;
            }
            boolean success = writeNumber(selectedSlotField, variables, slotIndex);
            if (success) {
                sync();
            }
            return success;
        }

        public OptionalDouble getMaxHunpo() {
            return readNumber(maxHunpoField, variables);
        }

        public double sumSoulCost() {
            double total = 0.0;
            for (int i = 1; i <= SLOT_COUNT; i++) {
                Optional<NudaoSlotState> slotOpt = getSlot(i);
                if (slotOpt.isPresent()) {
                    NudaoSlotState state = slotOpt.get().sanitised();
                    total += state.soulCost();
                }
            }
            return total;
        }

        public boolean canOccupy(double additionalCost) {
            if (!Double.isFinite(additionalCost)) {
                return false;
            }
            double cost = Math.max(0.0, additionalCost);
            OptionalDouble maxHunpoOpt = getMaxHunpo();
            if (maxHunpoOpt.isEmpty()) {
                return true;
            }
            double maxHunpo = maxHunpoOpt.getAsDouble();
            if (!Double.isFinite(maxHunpo)) {
                return true;
            }
            double occupied = sumSoulCost();
            return occupied + cost <= maxHunpo + EPSILON;
        }


        public boolean setSlot(int index, NudaoSlotState state, boolean adjustCount) {
            if (!isValidSlot(index) || state == null) {
                return false;
            }
            NudaoSlotState sanitized = state.sanitised();
            Field costField = SLOT_COST_FIELDS[index - 1];
            Field speciesField = SLOT_SPECIES_FIELDS[index - 1];
            Optional<NudaoSlotState> previousOpt = getSlot(index);
            boolean wroteCost = writeNumber(costField, variables, sanitized.soulCost());
            boolean wroteSpecies = writeNumber(speciesField, variables, sanitized.speciesIndex());
            boolean success = wroteCost && wroteSpecies;
            if (success && adjustCount && slotCountField != null && previousOpt.isPresent()) {
                boolean wasOccupied = previousOpt.get().occupied();
                boolean nowOccupied = sanitized.occupied();
                if (wasOccupied != nowOccupied) {
                    OptionalInt countOpt = readInt(slotCountField, variables);
                    if (countOpt.isPresent()) {
                        int newCount = Math.max(0, countOpt.getAsInt() + (nowOccupied ? 1 : -1));
                        success = writeNumber(slotCountField, variables, newCount);
                    }
                }
            }
            if (success) {
                sync();
            }
            return success;
        }

        public boolean clearSlot(int index, boolean adjustCount) {
            return setSlot(index, NudaoSlotState.empty(index), adjustCount);
        }

        public void sync() {
            GuzhenrenResourceBridge.syncEntity(player, variables);
        }
    }

    /** Handle for entity-side Nudao state (ownership, buffs, attribute tweaks). */
    public static final class NudaoSubjectHandle {

        private final LivingEntity entity;
        private final Object variables;

        private NudaoSubjectHandle(LivingEntity entity, Object variables) {
            this.entity = entity;
            this.variables = variables;
        }

        public LivingEntity entity() {
            return entity;
        }

        public Optional<String> getOwnerName() {
            return readString(ownerField, variables).map(String::trim).filter(name -> !name.isEmpty());
        }

        public boolean isOwnedBy(Player player) {
            if (player == null) {
                return false;
            }
            return getOwnerName().map(name -> name.equals(player.getScoreboardName())).orElse(false);
        }

        public boolean setOwner(Player player, boolean tame) {
            if (player == null) {
                return clearOwner();
            }
            boolean updated = setOwnerName(player.getScoreboardName());
            if (updated && tame && !entity.level().isClientSide() && entity instanceof TamableAnimal tamable) {
                tamable.tame(player);
            }
            return updated;
        }

        public boolean setOwnerName(String ownerName) {
            if (ownerField == null) {
                return false;
            }
            String value = ownerName == null ? "" : ownerName;
            boolean success = writeString(ownerField, variables, value);
            if (success) {
                sync();
            }
            return success;
        }

        public boolean clearOwner() {
            if (ownerField == null) {
                return false;
            }
            boolean success = writeString(ownerField, variables, "");
            if (success) {
                sync();
            }
            return success;
        }

        public OptionalInt getStarRating() {
            if (starRatingField == null) {
                return OptionalInt.empty();
            }
            return readInt(starRatingField, variables);
        }

        public boolean setStarRating(int rating) {
            if (starRatingField == null) {
                return false;
            }
            int value = Math.max(0, rating);
            boolean success = writeNumber(starRatingField, variables, value);
            if (success) {
                sync();
            }
            return success;
        }

        public boolean applyBuff(MobEffectInstance effect) {
            return applyBuff(effect, false);
        }

        public boolean applyBuff(MobEffectInstance effect, boolean allowClientSide) {
            if (effect == null) {
                return false;
            }
            if (!allowClientSide && entity.level().isClientSide()) {
                return false;
            }
            return entity.addEffect(effect);
        }

        public boolean addAttributeModifier(Holder<Attribute> attribute, AttributeModifier modifier) {
            if (attribute == null || modifier == null) {
                return false;
            }
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                return false;
            }
            instance.addTransientModifier(modifier);
            return true;
        }

        public boolean removeAttributeModifier(Holder<Attribute> attribute, AttributeModifier modifier) {
            if (attribute == null || modifier == null) {
                return false;
            }
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                return false;
            }
            instance.removeModifier(modifier);
            return true;
        }

        public boolean removeAttributeModifier(Holder<Attribute> attribute, ResourceLocation modifierId) {
            if (attribute == null || modifierId == null) {
                return false;
            }
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                return false;
            }
            instance.removeModifier(modifierId);
            return true;
        }

        public boolean setAttributeBaseValue(Holder<Attribute> attribute, double value) {
            if (attribute == null || !Double.isFinite(value)) {
                return false;
            }
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                return false;
            }
            instance.setBaseValue(value);
            return true;
        }

        public void sync() {
            GuzhenrenResourceBridge.syncEntity(entity, variables);
        }
    }
}

