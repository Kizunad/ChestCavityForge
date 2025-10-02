package net.tigereye.chestcavity.guzhenren.resource;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.tigereye.chestcavity.ChestCavity;


/**
 * Reflection-based bridge for interacting with the Guzhenren mod's player
 * variables attachment.
 * <p>
 * 在不直接依赖蛊真人模组 API 的前提下，提供类型安全的读写接口，便于统一处理真元、阶段、转数等核心字段。
 * - 初始化阶段通过反射解析 {@code PlayerVariables} 类与字段句柄；
 * - {@link ResourceHandle} 暴露高层方法（如 {@link ResourceHandle#consumeScaledZhenyuan(double)}），便于调用方按统一公式扣减真元；
 * - 使用 {@link PlayerField} 枚举缓存反射字段，避免散落的字符串字段名，降低维护风险。
 * <p>
 * Relocated from the legacy compat namespace so ChestCavity Forge treats
 * Guzhenren resources as first-class integration entry points.
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
        ZHENYUAN("zhenyuan", "真元"),
        MAX_ZHENYUAN("zuida_zhenyuan", "最大真元"),
        GUSHI_XIULIAN_JINDU("gushi_xiulian_jindu", "蛊师所需修炼进度"),
        GUSHI_XIULIAN_DANGQIAN("gushi_xiulian_dangqian", "蛊师当前修炼进度"),
        ZHUANSHU("zhuanshu", "境界转数"),
        JIEDUAN("jieduan", "小境界阶段"),
        KONGQIAO("kongqiao", "空窍品阶"),
        BENMINGGU("benminggu", "本命蛊"),
        DI_YU("di_yu", "地域"),
        SHOUYUAN("shouyuan", "寿元"),
        JINGLI("jingli", "精力"),
        MAX_JINGLI("zuida_jingli", "最大精力", "zuida_jingli"),
        TIZHI("tizhi", "体质"),
        XINGBIE("xingbie", "性别"),
        HUNPO("hunpo", "魂魄"),
        MAX_HUNPO("zuida_hunpo", "最大魂魄"),
        HUNPO_STABILITY("hunpo_kangxing", "魂魄抗性", "魂魄稳定度", "hunpo_stability"),
        MAX_HUNPO_STABILITY("hunpo_kangxing_shangxian", "魂魄抗性上限", "魂魄稳定度上限", "hunpo_stability_max"),
        NIANTOU("niantou", "念头"),
        MAX_NIANTOU("niantou_zhida", "最大念头", "niantou_zuida"),
        RENQI("renqi", "人气"),
        QIYUN("qiyun", "气运"),
        DAODE("daode", "道德"),
        // --  炼蛊  --
        GU_FANG("GuFang", "蛊方ID"),
        LIANGU_JINDU("LianGuJinDu", "炼蛊进度"),
        // --  道痕  --
        DAOHEN_JINDAO("daohen_jindao", "金道道痕"),
        DAOHEN_SHUIDAO("daohen_shuidao", "水道道痕"),
        DAOHEN_MUDAO("daohen_mudao", "木道道痕"),
        DAOHEN_YANDAO("daohen_yandao", "炎道道痕"),
        DAOHEN_TUDAO("daohen_tudao", "土道道痕"),
        DAOHEN_FENGDAO("daohen_fengdao", "风道道痕"),
        DAOHEN_GUANGDAO("daohen_guangdao", "光道道痕"),
        DAOHEN_ANDAO("daohen_andao", "暗道道痕"),
        DAOHEN_LEIDAO("daohen_leidao", "雷道道痕"),
        DAOHEN_DUDAO("daohen_dudao", "毒道道痕"),
        DAOHEN_YUDAO("daohen_yudao", "宇道道痕"),
        DAOHEN_ZHOUDAO("dahen_zhoudao", "宙道道痕", "daohen_zhoudao"),
        DAOHEN_RENDAO("dahen_rendao", "人道道痕", "daohen_rendao"),
        DAOHEN_TIANDAO("dahen_tiandao", "天道道痕", "daohen_tiandao"),
        DAOHEN_BINGXUE("daohen_bingxuedao", "冰雪道痕", "daohen_bingxue"),
        DAOHEN_QIDAO("dahen_qidao", "气道道痕", "daohen_qidao"),
        DAOHEN_NUDAO("dahen_nudao", "奴道道痕", "daohen_nudao"),
        DAOHEN_ZHIDAO("dahen_zhidao", "智道道痕", "daohen_zhidao"),
        DAOHEN_XINGDAO("dahen_xingdao", "星道道痕", "daohen_xingdao"),
        DAOHEN_ZHENDAO("dahen_zhendao", "阵道道痕", "daohen_zhendao"),
        DAOHEN_YINGDAO("daohen_yingdao", "影道道痕"),
        DAOHEN_LVDAO("daohen_lvdao", "律道道痕"),
        DAOHEN_LIANDAO("dahen_liandao", "炼道道痕", "daohen_liandao"),
        DAOHEN_LIDAO("daohen_lidao", "力道道痕"),
        DAOHEN_SHIDAO("daohen_shidao", "食道道痕"),
        DAOHEN_HUADAO("daohen_huadao", "画道道痕"),
        DAOHEN_TOUDAO("daohen_toudao", "偷道道痕"),
        DAOHEN_YUNDAO("daohen_yundao", "运道道痕"),
        DAOHEN_YUNDAO_CLOUD("daohen_yundao2", "云道道痕", "daohen_yundao", "daohen_yundao_cloud"),
        DAOHEN_XINDAO("daohen_xindao", "信道道痕"),
        DAOHEN_YINDAO("daohen_yindao", "音道道痕"),
        DAOHEN_GUDAO("daohen_gudao", "骨道道痕"),
        DAOHEN_XUDAO("daohen_xudao", "虚道道痕"),
        DAOHEN_JINDAO_FORBIDDEN("daohen_jindao2", "禁道道痕", "daohen_jindao", "daohen_jindao_forbidden"),
        DAOHEN_JIANDAO("daohen_jiandao", "剑道道痕"),
        DAOHEN_DAODAO("daohen_daodao", "刀道道痕"),
        DAOHEN_HUNDAO("daohen_hundao", "魂道道痕"),
        DAOHEN_DANDAO("daohen_dandao", "丹道道痕"),
        DAOHEN_XUEDAO("daohen_xuedao", "血道道痕"),
        DAOHEN_HUANDAO("daohen_huandao", "幻道道痕"),
        DAOHEN_YUEDAO("daohen_yuedao", "月道道痕"),
        DAOHEN_MENGDAO("daohen_mengdao", "梦道道痕"),
        DAOHEN_BINGDAO("daohen_bingdao", "兵道道痕"),
        DAOHEN_BIANHUADAO("daohen_bianhuadao", "变化道道痕"),
         // --  经验  --
        LIUPAI_JINDAO("liupai_jindao", "金道流派经验"),
        LIUPAI_SHUIDAO("liupai_shuidao", "水道流派经验"),
        LIUPAI_MUDAO("liupai_mudao", "木道流派经验"),
        LIUPAI_YANDAO("liupai_yandao", "炎道流派经验"),
        LIUPAI_TUDAO("liupai_tudao", "土道流派经验"),
        LIUPAI_FENGDAO("liupai_fengdao", "风道流派经验"),
        LIUPAI_GUANGDAO("liupai_guangdao", "光道流派经验"),
        LIUPAI_ANDAO("liupai_andao", "暗道流派经验"),
        LIUPAI_LEIDAO("liupai_leidao", "雷道流派经验"),
        LIUPAI_DUDAO("liupai_dudao", "毒道流派经验"),
        LIUPAI_YUDAO("liupai_yudao", "宇道流派经验"),
        LIUPAI_ZHOUDAO("liupai_zhoudao", "宙道流派经验"),
        LIUPAI_RENDAO("liupai_rendao", "人道流派经验"),
        LIUPAI_TIANDAO("liupai_tiandao", "天道流派经验"),
        LIUPAI_BINGXUEDAO("liupai_bingxuedao", "冰雪流派经验"),
        LIUPAI_QIDAO("liupai_qidao", "气道流派经验"),
        LIUPAI_NUDAO("liupai_nudao", "奴道流派经验"),
        LIUPAI_ZHIDAO("liupai_zhidao", "智道流派经验"),
        LIUPAI_XINGDAO("liupai_xingdao", "星道流派经验"),
        LIUPAI_ZHENDAO("liupai_zhendao", "阵道流派经验"),
        LIUPAI_YINGDAO("liupai_yingdao", "影道流派经验"),
        LIUPAI_LVDAO("liupai_lvdao", "律道流派经验"),
        LIUPAI_LIANDAO("liupai_liandao", "炼道流派经验"),
        LIUPAI_LIDAO("liupai_lidao", "力道流派经验"),
        LIUPAI_SHIDAO("liupai_shidao", "食道流派经验"),
        LIUPAI_HUADAO("liupai_huadao", "画道流派经验"),
        LIUPAI_TOUDAO("liupai_toudao", "偷道流派经验"),
        LIUPAI_YUNDAO("liupai_yundao", "运道流派经验"),
        LIUPAI_YUNDAO_CLOUD("liupai_yundao2", "云道流派经验", "liupai_yundao", "liupai_yundao_cloud"),
        LIUPAI_XINDAO("liupai_xindao", "信道流派经验"),
        LIUPAI_YINDAO("liupai_yindao", "音道流派经验"),
        LIUPAI_GUDAO("Liupai_gudao", "骨道流派经验", "liupai_gudao"),
        LIUPAI_XUDAO("liupai_xudao", "虚道流派经验"),
        LIUPAI_JINDAO_FORBIDDEN("liupai_jindao2", "禁道流派经验", "liupai_jindao", "liupai_jindao_forbidden"),
        LIUPAI_JIANDAO("liupai_jiandao", "剑道流派经验"),
        LIUPAI_DAODAO("liupai_daodao", "刀道流派经验"),
        LIUPAI_HUNDAO("liupai_hundao", "魂道流派经验"),
        LIUPAI_DANDAO("liupai_dandao", "丹道流派经验"),
        LIUPAI_XUEDAO("liupai_xuedao", "血道流派经验"),
        LIUPAI_HUANDAO("liupai_huandao", "幻道流派经验"),
        LIUPAI_YUEDAO("liupai_yuedao", "月道流派经验"),
        LIUPAI_MENGDAO("liupai_mengdao", "梦道流派经验"),
        LIUPAI_BINGDAO("liupai_bingdao", "兵道流派经验"),
        LIUPAI_BIANHUADAO("liupai_bianhuadao", "变化道流派经验");

        private static final Map<String, PlayerField> BY_ALIAS = new ConcurrentHashMap<>();
        private static final Map<String, PlayerField> BY_DOC_LABEL = new ConcurrentHashMap<>();

        private final String fieldName;
        private final String docLabel;
        private final Set<String> aliases;

        static {
            for (PlayerField field : values()) {
                for (String alias : field.aliases) {
                    PlayerField existing = BY_ALIAS.putIfAbsent(alias, field);
                    if (existing != null && existing != field) {
                        LOGGER.debug("Duplicate Guzhenren alias '{}' maps to {} and {}", alias, existing.displayName(), field.displayName());
                    }
                }
                if (field.docLabel != null && !field.docLabel.isBlank()) {
                    PlayerField existing = BY_DOC_LABEL.putIfAbsent(field.docLabel, field);
                    if (existing != null && existing != field) {
                        LOGGER.debug("Duplicate Guzhenren doc label '{}' maps to {} and {}", field.docLabel, existing.displayName(), field.displayName());
                    }
                }
            }
        }

        PlayerField(String fieldName, String docLabel, String... extraAliases) {
            this.fieldName = fieldName;
            this.docLabel = docLabel;
            Set<String> aliasSet = new LinkedHashSet<>();
            aliasSet.add(fieldName);
            if (extraAliases != null) {
                for (String alias : extraAliases) {
                    if (alias != null && !alias.isBlank()) {
                        aliasSet.add(alias);
                    }
                }
            }
            this.aliases = Collections.unmodifiableSet(aliasSet);
        }

        String fieldName() {
            return fieldName;
        }

        String displayName() {
            return docLabel == null || docLabel.isBlank() ? fieldName : docLabel + " (" + fieldName + ")";
        }

        Optional<String> docLabel() {
            return Optional.ofNullable(docLabel);
        }

        String docLabelRaw() {
            return docLabel;
        }

        static Optional<PlayerField> fromIdentifier(String identifier) {
            if (identifier == null || identifier.isBlank()) {
                return Optional.empty();
            }
            PlayerField byAlias = BY_ALIAS.get(identifier);
            if (byAlias != null) {
                return Optional.of(byAlias);
            }
            PlayerField byLabel = BY_DOC_LABEL.get(identifier);
            if (byLabel != null) {
                return Optional.of(byLabel);
            }
            try {
                return Optional.of(PlayerField.valueOf(identifier.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
    }

    public static Optional<String> resolveCanonicalFieldName(String identifier) {
        return PlayerField.fromIdentifier(identifier).map(PlayerField::fieldName);
    }

    public static Optional<String> documentationLabel(String identifier) {
        return PlayerField.fromIdentifier(identifier).flatMap(PlayerField::docLabel);
    }

    public static Set<String> canonicalFieldNames() {
        return Arrays.stream(PlayerField.values())
                .map(PlayerField::fieldName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
     * Attempts to open a resource handle for the given entity.
     *
     * @param entity living entity to inspect
     * @return optional handle when the Guzhenren attachment exists
     */
    public static Optional<ResourceHandle> open(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        Optional<Object> variables = fetchVariables(entity);
        if (variables.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ResourceHandle(entity, variables.get()));
    }

    /**
     * Attempts to open a resource handle for the given player.
     *
     * @param player player to inspect
     * @return optional handle when the Guzhenren attachment exists
     */
    public static Optional<ResourceHandle> open(Player player) {
        return open((LivingEntity) player);
    }

    public static Optional<Object> fetchVariables(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        AttachmentType<Object> attachmentType = resolveAttachmentType();
        if (attachmentType == null) {
            return Optional.empty();
        }
        Object variables;
        try {
            variables = entity.getData(attachmentType);
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to access Guzhenren attachment for {}", entity.getName().getString(), throwable);
            return Optional.empty();
        }
        if (variables == null) {
            return Optional.empty();
        }
        return Optional.of(variables);
    }

    public static Optional<Class<?>> getPlayerVariablesClass() {
        ensureInitialised();
        return Optional.ofNullable(playerVariablesClass);
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
            Field field = playerVariablesClass.getField(fieldKey.fieldName());
            FIELD_CACHE.put(fieldKey, field);
            return Optional.of(field);
        } catch (NoSuchFieldException e) {
            LOGGER.debug("Guzhenren PlayerVariables missing field '{}'", fieldKey.displayName());
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
                LOGGER.warn("Failed to read Guzhenren field '{}'", fieldKey.displayName(), e);
                return OptionalDouble.empty();
            }
        }).orElseGet(OptionalDouble::empty);
    }

    private static Optional<String> readString(Object variables, PlayerField fieldKey) {
        return resolveField(fieldKey).filter(field -> field.getType() == String.class).map(field -> {
            try {
                Object value = field.get(variables);
                return Optional.ofNullable(value == null ? null : value.toString());
            } catch (IllegalAccessException e) {
                LOGGER.warn("Failed to read Guzhenren field '{}'", fieldKey.displayName(), e);
                return Optional.<String>empty();
            }
        }).orElseGet(Optional::empty);
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
            LOGGER.debug("Guzhenren field '{}' is not a double", fieldKey.displayName());
            return false;
        }
        try {
            field.setDouble(variables, value);
            return true;
        } catch (IllegalAccessException e) {
            LOGGER.warn("Failed to write Guzhenren field '{}'", fieldKey.displayName(), e);
            return false;
        }
    }

    public static boolean syncEntity(Entity entity, Object variables) {
        if (entity == null || entity.level().isClientSide() || syncPlayerVariables == null) {
            return false;
        }
        try {
            syncPlayerVariables.invoke(variables, entity);
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Failed to sync Guzhenren player variables for {}", entity.getName().getString(), e);
            return false;
        }
    }

    /**
     * 封装对单个玩家附件的读写操作。实例在 {@link #open(Player)} 时创建，并在操作成功后负责触发同步。
     */
    public static final class ResourceHandle {
        private final LivingEntity owner;
        private final Object variables;

        private ResourceHandle(LivingEntity owner, Object variables) {
            this.owner = owner;
            this.variables = variables;
        }

        public OptionalDouble read(PlayerField fieldKey) {
            return GuzhenrenResourceBridge.readDouble(this.variables, fieldKey);
        }

        public OptionalDouble read(String identifier) {
            Optional<PlayerField> field = PlayerField.fromIdentifier(identifier);
            return field.isPresent() ? read(field.get()) : OptionalDouble.empty();
        }

        public Optional<String> readString(PlayerField fieldKey) {
            return GuzhenrenResourceBridge.readString(this.variables, fieldKey);
        }

        public Optional<String> readString(String identifier) {
            Optional<PlayerField> field = PlayerField.fromIdentifier(identifier);
            return field.isPresent() ? readString(field.get()) : Optional.empty();
        }

        /**
         * 写入指定字段并同步到客户端。
         */
        public OptionalDouble writeDouble(PlayerField fieldKey, double value) {
            if (!Double.isFinite(value)) {
                LOGGER.debug("Refusing to write non-finite Guzhenren value '{}' -> {}", fieldKey.displayName(), value);
                return OptionalDouble.empty();
            }
            if (!GuzhenrenResourceBridge.writeDouble(this.variables, fieldKey, value)) {
                return OptionalDouble.empty();
            }
            syncEntity(this.owner, this.variables);
            return OptionalDouble.of(value);
        }

        public OptionalDouble writeDouble(String identifier, double value) {
            Optional<PlayerField> field = PlayerField.fromIdentifier(identifier);
            return field.isPresent() ? writeDouble(field.get(), value) : OptionalDouble.empty();
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

        public OptionalDouble adjustDouble(String identifier, double delta, boolean clampZero) {
            return adjustDouble(identifier, delta, clampZero, null);
        }

        public OptionalDouble adjustDouble(String identifier, double delta, boolean clampZero, String maxIdentifier) {
            Optional<PlayerField> field = PlayerField.fromIdentifier(identifier);
            if (field.isEmpty()) {
                return OptionalDouble.empty();
            }
            PlayerField maxField = null;
            if (maxIdentifier != null && !maxIdentifier.isBlank()) {
                maxField = PlayerField.fromIdentifier(maxIdentifier).orElse(null);
            }
            return adjustDouble(field.get(), delta, clampZero, maxField);
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

        public OptionalDouble clampToMax(String identifier, String maxIdentifier) {
            Optional<PlayerField> field = PlayerField.fromIdentifier(identifier);
            if (field.isEmpty()) {
                return OptionalDouble.empty();
            }
            PlayerField maxField = PlayerField.fromIdentifier(maxIdentifier).orElse(null);
            return clampToMax(field.get(), maxField);
        }

        /**
         * Builds an immutable snapshot of every known Guzhenren variable keyed by its canonical field name.
         */
        public Map<String, Double> snapshotAll() {
            Map<String, Double> values = new LinkedHashMap<>();
            for (PlayerField field : PlayerField.values()) {
                OptionalDouble valueOpt = GuzhenrenResourceBridge.readDouble(this.variables, field);
                if (valueOpt.isPresent()) {
                    values.put(field.fieldName(), valueOpt.getAsDouble());
                }
            }
            return Collections.unmodifiableMap(values);
        }

        public Optional<String> getConstitution() {
            return readString(PlayerField.TIZHI).filter(value -> !value.isBlank());
        }

        public boolean hasConstitution(String expected) {
            if (expected == null || expected.isBlank()) {
                return false;
            }
            String normalisedExpected = expected.trim();
            return getConstitution().map(value -> value.equalsIgnoreCase(normalisedExpected)).orElse(false)
                    || PlayerField.fromIdentifier(normalisedExpected)
                    .flatMap(PlayerField::docLabel)
                    .map(label -> getConstitution().map(value -> value.equalsIgnoreCase(label)).orElse(false))
                    .orElse(false);
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
            double required = scaleZhenyuanByCultivation(baseCost);
            if (!Double.isFinite(required) || required <= 0) {
                return OptionalDouble.empty();
            }
            if (current < required) {
                return OptionalDouble.empty();
            }
            return adjustZhenyuan(-required, true);
        }

        /**
         * Calculates the scaled zhenyuan requirement without mutating the attachment.
         */
        public OptionalDouble estimateScaledZhenyuanCost(double baseCost) {
            if (baseCost <= 0 || !Double.isFinite(baseCost)) {
                return OptionalDouble.of(0.0);
            }
            double scaled = scaleZhenyuanByCultivation(baseCost);
            if (!Double.isFinite(scaled) || scaled <= 0) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(scaled);
        }

        /** 真元补充：按与消耗相同的缩放公式增加真元值。 */
        public OptionalDouble replenishScaledZhenyuan(double baseAmount, boolean clampToMax) {
            if (baseAmount <= 0 || !Double.isFinite(baseAmount)) {
                return OptionalDouble.empty();
            }
            OptionalDouble currentOpt = getZhenyuan();
            if (currentOpt.isEmpty()) {
                return OptionalDouble.empty();
            }

            double scaled = scaleZhenyuanByCultivation(baseAmount);
            if (!Double.isFinite(scaled) || scaled <= 0) {
                return OptionalDouble.empty();
            }

            double target = currentOpt.getAsDouble() + scaled;
            if (clampToMax) {
                OptionalDouble maxOpt = getMaxZhenyuan();
                if (maxOpt.isPresent()) {
                    target = Math.min(target, maxOpt.getAsDouble());
                }
            }
            if (!Double.isFinite(target)) {
                return OptionalDouble.empty();
            }
            return writeDouble(PlayerField.ZHENYUAN, target);
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

        /** 魂魄稳定度（当前值） */
        public OptionalDouble getHunpoStability() {
            return GuzhenrenResourceBridge.readDouble(this.variables, PlayerField.HUNPO_STABILITY);
        }

        /** 设置魂魄稳定度 */
        public OptionalDouble setHunpoStability(double value) {
            return writeDouble(PlayerField.HUNPO_STABILITY, value);
        }

        /** 魂魄稳定度增减（可选非负，自动对齐上限） */
        public OptionalDouble adjustHunpoStability(double delta, boolean clampZero) {
            return adjustDouble(PlayerField.HUNPO_STABILITY, delta, clampZero, PlayerField.MAX_HUNPO_STABILITY);
        }

        /** 魂魄稳定度上限 */
        public OptionalDouble getMaxHunpoStability() {
            return GuzhenrenResourceBridge.readDouble(this.variables, PlayerField.MAX_HUNPO_STABILITY);
        }

        /** 设置魂魄稳定度上限 */
        public OptionalDouble setMaxHunpoStability(double value) {
            return writeDouble(PlayerField.MAX_HUNPO_STABILITY, value);
        }

        /** 将魂魄稳定度限制在对应上限内 */
        public OptionalDouble clampHunpoStabilityToMax() {
            return clampToMax(PlayerField.HUNPO_STABILITY, PlayerField.MAX_HUNPO_STABILITY);
        }

        private double scaleZhenyuanByCultivation(double baseAmount) {
            if (!Double.isFinite(baseAmount) || baseAmount <= 0) {
                return Double.NaN;
            }
            double jieduan = getJieduan().orElse(0.0);
            double effectiveZhuanshu = Math.max(1.0, getZhuanshu().orElse(1.0));
            double denominator = Math.pow(2.0, jieduan + effectiveZhuanshu * 4.0) * effectiveZhuanshu * 3.0 / 96.0;
            if (denominator > 0.0 && Double.isFinite(denominator)) {
                double scaled = baseAmount / denominator;
                if (Double.isFinite(scaled) && scaled > 0) {
                    return scaled;
                }
            }
            return baseAmount;
        }
    }
}
