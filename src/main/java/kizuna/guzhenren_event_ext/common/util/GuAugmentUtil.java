package kizuna.guzhenren_event_ext.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * 通用增幅/镜像工具（KISS）：
 * - 属性增益（生命/攻击/速度倍增）
 * - 基础属性镜像（生命上限/攻击/护甲/护甲韧性/移动速度）
 * - 蛊真人变量镜像（转数/阶段/真元/精力/魂魄/念头/稳定度/道痕）
 * - 自定义道痕应用
 */
public final class GuAugmentUtil {

    private GuAugmentUtil() {}

    // -------------------- 属性增益 --------------------

    public static void applyAttributeMultipliers(LivingEntity entity, double healthMult, double damageMult, double speedMult) {
        // 生命
        if (healthMult != 1.0) {
            AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                double base = maxHealth.getBaseValue();
                maxHealth.setBaseValue(base * healthMult);
                entity.setHealth(entity.getMaxHealth());
            }
        }
        // 攻击
        if (damageMult != 1.0) {
            AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) {
                attack.setBaseValue(attack.getBaseValue() * damageMult);
            }
        }
        // 速度
        if (speedMult != 1.0) {
            AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * speedMult);
            }
        }
    }

    // -------------------- 基础属性镜像 --------------------

    public static void mirrorBaseAttributesFrom(LivingEntity target, LivingEntity source) {
        try {
            AttributeInstance sMax = source.getAttribute(Attributes.MAX_HEALTH);
            AttributeInstance tMax = target.getAttribute(Attributes.MAX_HEALTH);
            if (sMax != null && tMax != null) {
                tMax.setBaseValue(sMax.getBaseValue());
                target.setHealth(target.getMaxHealth());
            }

            AttributeInstance sAtk = source.getAttribute(Attributes.ATTACK_DAMAGE);
            AttributeInstance tAtk = target.getAttribute(Attributes.ATTACK_DAMAGE);
            if (sAtk != null && tAtk != null) {
                tAtk.setBaseValue(sAtk.getBaseValue());
            }

            AttributeInstance sArmor = source.getAttribute(Attributes.ARMOR);
            AttributeInstance tArmor = target.getAttribute(Attributes.ARMOR);
            if (sArmor != null && tArmor != null) {
                tArmor.setBaseValue(sArmor.getBaseValue());
            }

            AttributeInstance sArmorT = source.getAttribute(Attributes.ARMOR_TOUGHNESS);
            AttributeInstance tArmorT = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
            if (sArmorT != null && tArmorT != null) {
                tArmorT.setBaseValue(sArmorT.getBaseValue());
            }

            AttributeInstance sSpd = source.getAttribute(Attributes.MOVEMENT_SPEED);
            AttributeInstance tSpd = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (sSpd != null && tSpd != null) {
                tSpd.setBaseValue(sSpd.getBaseValue());
            }
        } catch (Throwable t) {
            GuzhenrenEventExtension.LOGGER.warn("镜像基础属性失败: {}", t.getMessage());
        }
    }

    // -------------------- 蛊真人变量镜像（含道痕） --------------------

    public static void mirrorGuVariablesFrom(LivingEntity target, Player source) {
        try {
            var sOpt = GuzhenrenResourceBridge.open(source);
            var tOpt = GuzhenrenResourceBridge.open(target);
            if (sOpt.isEmpty() || tOpt.isEmpty()) {
                GuzhenrenEventExtension.LOGGER.warn("无法打开资源句柄以镜像蛊真人变量");
                return;
            }
            var s = sOpt.get();
            var t = tOpt.get();

            // 境界
            s.getZhuanshu().ifPresent(v -> t.writeDouble("zhuanshu", v));
            s.getJieduan().ifPresent(v -> t.writeDouble("jieduan", v));

            // 真元
            s.getZhenyuan().ifPresent(t::setZhenyuan);
            s.getMaxZhenyuan().ifPresent(v -> t.writeDouble("zuida_zhenyuan", v));

            // 魂魄
            s.getHunpo().ifPresent(v -> t.writeDouble("hunpo", v));
            s.getMaxHunpo().ifPresent(v -> t.writeDouble("zuida_hunpo", v));

            // 精力
            s.getJingli().ifPresent(t::setJingli);
            s.getMaxJingli().ifPresent(v -> t.writeDouble("zuida_jingli", v));

            // 念头
            s.getNiantou().ifPresent(v -> t.writeDouble("niantou", v));
            s.getMaxNiantou().ifPresent(v -> t.writeDouble("niantou_rongliang", v));

            // 稳定度
            s.getHunpoStability().ifPresent(t::setHunpoStability);
            s.getMaxHunpoStability().ifPresent(t::setMaxHunpoStability);

            mirrorAllDaohen(s, t);
        } catch (Throwable t) {
            GuzhenrenEventExtension.LOGGER.error("镜像蛊真人变量失败", t);
        }
    }

    private static void mirrorAllDaohen(GuzhenrenResourceBridge.ResourceHandle src,
                                        GuzhenrenResourceBridge.ResourceHandle dst) {
        // 该列表来源于既有实现，保持兼容
        String[] fields = {
            // 五行
            "daohen_jindao", "daohen_shuidao", "daohen_mudao", "daohen_yandao", "daohen_tudao",
            // 元素
            "daohen_fengdao", "daohen_guangdao", "daohen_andao", "daohen_leidao", "daohen_dudao", "daohen_bingxuedao",
            // 宇宙
            "daohen_yudao", "dahen_zhoudao",
            // 人天
            "dahen_rendao", "dahen_tiandao",
            // 修炼
            "dahen_qidao", "dahen_nudao", "dahen_zhidao", "dahen_xingdao", "dahen_zhendao", "dahen_liandao",
            // 其他
            "daohen_yingdao", "daohen_lvdao", "daohen_lidao", "daohen_shidao", "daohen_huadao", "daohen_toudao",
            "daohen_yundao", "daohen_yundao2", "daohen_xindao", "daohen_yindao", "daohen_gudao", "daohen_xudao",
            "daohen_jindao2", "daohen_jiandao", "daohen_daodao", "daohen_hundao", "daohen_dandao", "daohen_xuedao",
            "daohen_huandao", "daohen_yuedao", "daohen_mengdao", "daohen_bingdao", "daohen_bianhuadao", "daohen_duandao",
            "daohen_kongdao", "daohen_zhanzhengdao"
        };

        int copied = 0;
        for (String f : fields) {
            try {
                src.read(f).ifPresent(v -> {
                    try { dst.writeDouble(f, v); } catch (Throwable ignored) {}
                });
                copied++;
            } catch (Throwable e) {
                // 某些字段可能不存在
                GuzhenrenEventExtension.LOGGER.trace("道痕镜像失败 {}: {}", f, e.getMessage());
            }
        }
        GuzhenrenEventExtension.LOGGER.debug("已镜像道痕字段数: {}", copied);
    }

    // -------------------- 自定义道痕应用 --------------------

    /**
     * 应用自定义道痕：数组元素形如 {"daohen":"daohen_jiandao","value":100.0,"override":true}
     */
    public static void applyCustomDaohen(LivingEntity entity, JsonArray configs) {
        try {
            var handleOpt = GuzhenrenResourceBridge.open(entity);
            if (handleOpt.isEmpty()) {
                GuzhenrenEventExtension.LOGGER.warn("无法打开实体资源句柄以应用自定义道痕");
                return;
            }
            var h = handleOpt.get();
            int applied = 0;
            for (JsonElement el : configs) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                if (!o.has("daohen") || !o.has("value")) continue;
                String field = o.get("daohen").getAsString();
                double value = o.get("value").getAsDouble();
                boolean override = !o.has("override") || o.get("override").getAsBoolean();
                try {
                    if (override) {
                        h.writeDouble(field, value);
                    } else {
                        double current = h.read(field).orElse(0.0);
                        h.writeDouble(field, current + value);
                    }
                    applied++;
                } catch (Throwable t) {
                    GuzhenrenEventExtension.LOGGER.warn("应用道痕失败 {}: {}", field, t.getMessage());
                }
            }
            GuzhenrenEventExtension.LOGGER.debug("自定义道痕应用数: {}", applied);
        } catch (Throwable t) {
            GuzhenrenEventExtension.LOGGER.error("应用自定义道痕失败", t);
        }
    }

    // -------------------- 资源顶满 --------------------

    /**
     * 将真元/精力/魂魄顶至各自上限。
     */
    public static void topOffGuResources(LivingEntity target) {
        try {
            var tOpt = GuzhenrenResourceBridge.open(target);
            if (tOpt.isEmpty()) return;
            var h = tOpt.get();
            try { h.getMaxZhenyuan().ifPresent(max -> h.setZhenyuan(max)); } catch (Throwable ignored) {}
            try { h.getMaxJingli().ifPresent(max -> h.setJingli(max)); } catch (Throwable ignored) {}
            try { h.read("zuida_hunpo").ifPresent(max -> h.writeDouble("hunpo", max)); } catch (Throwable ignored) {}
        } catch (Throwable t) {
            GuzhenrenEventExtension.LOGGER.debug("顶满资源失败: {}", t.getMessage());
        }
    }
}

