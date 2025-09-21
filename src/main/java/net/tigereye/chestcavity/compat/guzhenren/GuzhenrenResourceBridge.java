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
 * <p>
 * 在不直接依赖蛊真人模组 API 的前提下，提供类型安全的读写接口，便于统一处理真元、阶段、转数等核心字段。
 * - 初始化阶段通过反射解析 {@code PlayerVariables} 类与字段句柄；
 * - {@link ResourceHandle} 暴露高层方法（如 {@link ResourceHandle#consumeScaledZhenyuan(double)}），便于调用方按统一公式扣减真元；
 * - 使用 {@link PlayerField} 枚举缓存反射字段，避免散落的字符串字段名，降低维护风险。
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
    /**
     * Mapping of all supported player variable fields. 承载附件字段映射，保证读写时使用固定枚举常量而非裸字符串。
     */
    private enum PlayerField {
        ZHENYUAN("zhenyuan"),
        MAX_ZHENYUAN("zuida_zhenyuan"),
        JINGLI("jingli"),
        MAX_JINGLI("zuida_jingli"),
        JIEDUAN("jieduan"),
        ZHUANSHU("zhuanshu");

        private final String fieldName;

        PlayerField(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private static final Map<PlayerField, Field> FIELD_CACHE = new ConcurrentHashMap<>();

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

    /**
     * Lazily resolve Guzhenren 的附件类型与字段信息；只执行一次，失败时保持 {@code available=false} 以便调用方判定是否启用兼容逻辑。
     */
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

    private static Optional<Field> resolveField(PlayerField fieldKey) {
        if (fieldKey == null || playerVariablesClass == null) {
            return Optional.empty();
        }
        Field cached = FIELD_CACHE.get(fieldKey);
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            Field field = playerVariablesClass.getField(fieldKey.fieldName);
            FIELD_CACHE.put(fieldKey, field);
            return Optional.of(field);
        } catch (NoSuchFieldException e) {
            LOGGER.debug("Guzhenren PlayerVariables missing field '{}'", fieldKey.fieldName);
            return Optional.empty();
        }
    }

    /**
     * 利用反射读取指定字段的 double 值。
     */
    private static OptionalDouble readDouble(Object variables, PlayerField fieldKey) {
        return resolveField(fieldKey).filter(field -> field.getType() == double.class).map(field -> {
            try {
                return OptionalDouble.of(field.getDouble(variables));
            } catch (IllegalAccessException e) {
                LOGGER.warn("Failed to read Guzhenren field '{}'", fieldKey.fieldName, e);
                return OptionalDouble.empty();
            }
        }).orElseGet(OptionalDouble::empty);
    }

    /**
     * 利用反射写入指定字段的 double 值。
     */
    private static boolean writeDouble(Object variables, PlayerField fieldKey, double value) {
        Optional<Field> fieldOpt = resolveField(fieldKey);
        if (fieldOpt.isEmpty()) {
            return false;
        }
        Field field = fieldOpt.get();
        if (field.getType() != double.class) {
            LOGGER.debug("Guzhenren field '{}' is not a double", fieldKey.fieldName);
            return false;
        }
        try {
            field.setDouble(variables, value);
            return true;
        } catch (IllegalAccessException e) {
            LOGGER.warn("Failed to write Guzhenren field '{}'", fieldKey.fieldName, e);
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

    /**
     * 封装对单个玩家附件的读写操作。实例在 {@link #open(Player)} 时创建，并在操作成功后负责触发同步。
     */
    public static final class ResourceHandle {
        private final Player player;
        private final Object variables;

        private ResourceHandle(Player player, Object variables) {
            this.player = player;
            this.variables = variables;
        }

        /**
         * 写入指定字段并同步到客户端。
         */
        public OptionalDouble writeDouble(PlayerField fieldKey, double value) {
            if (!Double.isFinite(value)) {
                LOGGER.debug("Refusing to write non-finite Guzhenren value '{}' -> {}", fieldKey.fieldName, value);
                return OptionalDouble.empty();
            }
            if (!GuzhenrenResourceBridge.writeDouble(this.variables, fieldKey, value)) {
                return OptionalDouble.empty();
            }
            sync(this.player, this.variables);
            return OptionalDouble.of(value);
        }

        /**
         * 在原值基础上加减 {@code delta}，支持下限为 0 与上限裁剪。
         */
        public OptionalDouble adjustDouble(PlayerField fieldKey, double delta, boolean clampZero, PlayerField maxFieldKey) {
            OptionalDouble currentOpt = GuzhenrenResourceBridge.readDouble(this.variables, fieldKey);
            if (currentOpt.isEmpty()) {
                return OptionalDouble.empty();
            }
            double current = currentOpt.getAsDouble();
            double result = current + delta;
            if (clampZero) {
                result = Math.max(0.0, result);
            }
            if (maxFieldKey != null) {
                OptionalDouble maxValue = GuzhenrenResourceBridge.readDouble(this.variables, maxFieldKey);
                if (maxValue.isPresent()) {
                    result = Math.min(result, maxValue.getAsDouble());
                }
            }
            return writeDouble(fieldKey, result);
        }

        /**
         * 将字段值限制在对应的最大字段之内。
         */
        public OptionalDouble clampToMax(PlayerField fieldKey, PlayerField maxFieldKey) {
            OptionalDouble currentOpt = GuzhenrenResourceBridge.readDouble(this.variables, fieldKey);
            if (currentOpt.isEmpty()) {
                return OptionalDouble.empty();
            }
            OptionalDouble maxOpt = GuzhenrenResourceBridge.readDouble(this.variables, maxFieldKey);
            if (maxOpt.isEmpty()) {
                return currentOpt;
            }
            double clamped = Math.min(currentOpt.getAsDouble(), maxOpt.getAsDouble());
            return writeDouble(fieldKey, clamped);
        }

        /**
         * 按统一公式扣减真元：{@code baseCost / (2^(jieduan + zhuanshu*4) * zhuanshu * 3 / 96)}。
         * 若附件缺失相关字段，则退化为直接扣 {@code baseCost}。
         */
        public OptionalDouble consumeScaledZhenyuan(double baseCost) {
            if (baseCost <= 0 || !Double.isFinite(baseCost)) {
                return OptionalDouble.empty();
            }
            OptionalDouble currentOpt = getZhenyuan();
            if (currentOpt.isEmpty()) {
                return OptionalDouble.empty();
            }
            double current = currentOpt.getAsDouble();
            double jieduan = getJieduan().orElse(0.0);
            double effectiveZhuanshu = Math.max(1.0, getZhuanshu().orElse(1.0));
            double denominator = Math.pow(2.0, jieduan + effectiveZhuanshu * 4.0) * effectiveZhuanshu * 3.0 / 96.0;
            double required = baseCost;
            if (denominator > 0 && Double.isFinite(denominator)) {
                required = baseCost / denominator;
            }
            if (!Double.isFinite(required) || required <= 0) {
                required = baseCost;
            }
            if (current < required) {
                return OptionalDouble.empty();
            }
            return adjustZhenyuan(-required, true);
        }

        /** 当前真元 */
        public OptionalDouble getZhenyuan() {
            return GuzhenrenResourceBridge.readDouble(this.variables, PlayerField.ZHENYUAN);
        }

        /** 设置真元 */
        public OptionalDouble setZhenyuan(double value) {
            return writeDouble(PlayerField.ZHENYUAN, value);
        }

        /** 真元增减（可选择限制为非负） */
        public OptionalDouble adjustZhenyuan(double delta, boolean clampZero) {
            return adjustDouble(PlayerField.ZHENYUAN, delta, clampZero, null);
        }

        /** 真元上限 */
        public OptionalDouble getMaxZhenyuan() {
            return GuzhenrenResourceBridge.readDouble(this.variables, PlayerField.MAX_ZHENYUAN);
        }

        /** 阶段（阶位） */
        public OptionalDouble getJieduan() {
            return GuzhenrenResourceBridge.readDouble(this.variables, PlayerField.JIEDUAN);
        }

        /** 转数 */
        public OptionalDouble getZhuanshu() {
            return GuzhenrenResourceBridge.readDouble(this.variables, PlayerField.ZHUANSHU);
        }

        /** 当前精力 */
        public OptionalDouble getJingli() {
            return GuzhenrenResourceBridge.readDouble(this.variables, PlayerField.JINGLI);
        }

        /** 设置精力 */
        public OptionalDouble setJingli(double value) {
            return writeDouble(PlayerField.JINGLI, value);
        }

        /** 精力增减（可选非负、可选上限字段） */
        public OptionalDouble adjustJingli(double delta, boolean clampZero) {
            return adjustDouble(PlayerField.JINGLI, delta, clampZero, PlayerField.MAX_JINGLI);
        }

        /** 精力上限 */
        public OptionalDouble getMaxJingli() {
            return GuzhenrenResourceBridge.readDouble(this.variables, PlayerField.MAX_JINGLI);
        }
    }
}
