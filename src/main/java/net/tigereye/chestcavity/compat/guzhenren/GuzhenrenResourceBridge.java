package net.tigereye.chestcavity.compat.guzhenren;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.tigereye.chestcavity.ChestCavity;
import net.neoforged.fml.ModList;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Reflection-based bridge for interacting with the Guzhenren mod's player variables attachment.
 * Keeps ChestCavity decoupled from optional dependencies while offering structured access.
 */
public final class GuzhenrenResourceBridge {

    private static final Logger LOGGER = ChestCavity.LOGGER;
    private static final String MOD_ID = "guzhenren";
    private static final String VARIABLES_CLASS = "net.guzhenren.network.GuzhenrenModVariables";
    private static final String PLAYER_VARIABLES_CLASS = VARIABLES_CLASS + "$PlayerVariables";
    private static final String PLAYER_VARIABLES_FIELD = "PLAYER_VARIABLES";
    private static final String SYNC_METHOD = "syncPlayerVariables";

    private static volatile boolean attemptedInit = false;
    private static volatile boolean available = false;

    private static Supplier<?> playerVariablesSupplier;
    private static Class<?> playerVariablesClass;
    private static Method syncPlayerVariables;
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private GuzhenrenResourceBridge() {
    }

    /**
     * @return true if the Guzhenren mod is loaded and the bridge initialised successfully.
     */
    public static boolean isAvailable() {
        ensureInitialised();
        return available;
    }

    /**
     * Attempts to open a resource handle for the given player.
     *
     * @param player player to inspect
     * @return optional handle when the Guzhenren attachment exists
     */
    public static Optional<ResourceHandle> open(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        AttachmentType<Object> attachmentType = resolveAttachmentType();
        if (attachmentType == null) {
            return Optional.empty();
        }
        Object variables;
        try {
            variables = player.getData(attachmentType);
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to access Guzhenren player attachment for {}", player.getScoreboardName(), throwable);
            return Optional.empty();
        }
        if (variables == null) {
            return Optional.empty();
        }
        return Optional.of(new ResourceHandle(player, variables));
    }

    private static void ensureInitialised() {
        if (attemptedInit) {
            return;
        }
        synchronized (GuzhenrenResourceBridge.class) {
            if (attemptedInit) {
                return;
            }
            attemptedInit = true;
            if (!ModList.get().isLoaded(MOD_ID)) {
                LOGGER.debug("Guzhenren mod not detected; compat bridge disabled");
                return;
            }
            try {
                Class<?> variablesRootClass = Class.forName(VARIABLES_CLASS);
                playerVariablesClass = Class.forName(PLAYER_VARIABLES_CLASS);

                Field supplierField = variablesRootClass.getDeclaredField(PLAYER_VARIABLES_FIELD);
                Object supplierRaw = supplierField.get(null);
                if (!(supplierRaw instanceof Supplier<?> supplier)) {
                    LOGGER.warn("Unexpected type for Guzhenren PLAYER_VARIABLES field: {}", supplierRaw);
                    return;
                }
                playerVariablesSupplier = supplier;

                syncPlayerVariables = playerVariablesClass.getMethod(SYNC_METHOD, Class.forName("net.minecraft.world.entity.Entity"));

                available = true;
                LOGGER.info("Guzhenren compat bridge initialised");
            } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
                LOGGER.warn("Failed to initialise Guzhenren compat bridge", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static AttachmentType<Object> resolveAttachmentType() {
        ensureInitialised();
        if (!available || playerVariablesSupplier == null) {
            return null;
        }
        Object type = playerVariablesSupplier.get();
        if (!(type instanceof AttachmentType<?> attachmentType)) {
            LOGGER.warn("Guzhenren PLAYER_VARIABLES supplier returned unexpected value: {}", type);
            return null;
        }
        return (AttachmentType<Object>) attachmentType;
    }

    private static Optional<Field> resolveField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty() || playerVariablesClass == null) {
            return Optional.empty();
        }
        Field cached = FIELD_CACHE.get(fieldName);
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            Field field = playerVariablesClass.getField(fieldName);
            FIELD_CACHE.put(fieldName, field);
            return Optional.of(field);
        } catch (NoSuchFieldException e) {
            LOGGER.debug("Guzhenren PlayerVariables missing field '{}'", fieldName);
            return Optional.empty();
        }
    }

    private static OptionalDouble readDouble(Object variables, String fieldName) {
        return resolveField(fieldName).filter(field -> field.getType() == double.class).map(field -> {
            try {
                return OptionalDouble.of(field.getDouble(variables));
            } catch (IllegalAccessException e) {
                LOGGER.warn("Failed to read Guzhenren field '{}'", fieldName, e);
                return OptionalDouble.empty();
            }
        }).orElseGet(OptionalDouble::empty);
    }

    private static boolean writeDouble(Object variables, String fieldName, double value) {
        Optional<Field> fieldOpt = resolveField(fieldName);
        if (fieldOpt.isEmpty()) {
            return false;
        }
        Field field = fieldOpt.get();
        if (field.getType() != double.class) {
            LOGGER.debug("Guzhenren field '{}' is not a double", fieldName);
            return false;
        }
        try {
            field.setDouble(variables, value);
            return true;
        } catch (IllegalAccessException e) {
            LOGGER.warn("Failed to write Guzhenren field '{}'", fieldName, e);
            return false;
        }
    }

    private static void sync(Player player, Object variables) {
        if (player == null || player.level().isClientSide() || syncPlayerVariables == null) {
            return;
        }
        try {
            syncPlayerVariables.invoke(variables, player);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Failed to sync Guzhenren player variables for {}", player.getScoreboardName(), e);
        }
    }

    public static final class ResourceHandle {
        private final Player player;
        private final Object variables;

        private ResourceHandle(Player player, Object variables) {
            this.player = player;
            this.variables = variables;
        }

        public boolean hasField(String fieldName) {
            return resolveField(fieldName).isPresent();
        }

        public OptionalDouble readDouble(String fieldName) {
            return GuzhenrenResourceBridge.readDouble(this.variables, fieldName);
        }

        public OptionalDouble writeDouble(String fieldName, double value) {
            if (!Double.isFinite(value)) {
                LOGGER.debug("Refusing to write non-finite Guzhenren value '{}' -> {}", fieldName, value);
                return OptionalDouble.empty();
            }
            if (!GuzhenrenResourceBridge.writeDouble(this.variables, fieldName, value)) {
                return OptionalDouble.empty();
            }
            sync(this.player, this.variables);
            return OptionalDouble.of(value);
        }

        public OptionalDouble adjustDouble(String fieldName, double delta, boolean clampZero, String maxFieldName) {
            OptionalDouble currentOpt = readDouble(fieldName);
            if (currentOpt.isEmpty()) {
                return OptionalDouble.empty();
            }
            double current = currentOpt.getAsDouble();
            double result = current + delta;
            if (clampZero) {
                result = Math.max(0.0, result);
            }
            if (maxFieldName != null) {
                OptionalDouble maxValue = readDouble(maxFieldName);
                if (maxValue.isPresent()) {
                    result = Math.min(result, maxValue.getAsDouble());
                }
            }
            return writeDouble(fieldName, result);
        }

        public OptionalDouble clampToMax(String fieldName, String maxFieldName) {
            OptionalDouble currentOpt = readDouble(fieldName);
            if (currentOpt.isEmpty()) {
                return OptionalDouble.empty();
            }
            OptionalDouble maxOpt = readDouble(maxFieldName);
            if (maxOpt.isEmpty()) {
                return currentOpt;
            }
            double clamped = Math.min(currentOpt.getAsDouble(), maxOpt.getAsDouble());
            return writeDouble(fieldName, clamped);
        }
    }
}
