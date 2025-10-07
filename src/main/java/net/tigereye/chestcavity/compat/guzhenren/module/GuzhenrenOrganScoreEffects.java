package net.tigereye.chestcavity.compat.guzhenren.module;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.listeners.OrganScoreEffects;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Registers organ score effects that proxy Guzhenren resource adjustments through
 * {@link GuzhenrenResourceBridge}. Only applies to player-owned chest cavities.
 */
public final class GuzhenrenOrganScoreEffects {

    private static final double EPSILON = 1.0E-6;
    private static boolean registered;

    private GuzhenrenOrganScoreEffects() {
    }

    public static synchronized void bootstrap() {
        if (registered) {
            return;
        }
        registered = true;

        registerWithMax("zhenyuan", "zuida_zhenyuan");
        registerMaxField("zuida_zhenyuan", "zhenyuan", "zuida_zhenyuan");
        registerSimple("shouyuan");
        registerWithMax("jingli", "zuida_jingli", "zuida_jingli");
        registerMaxField("zuida_jingli", "jingli", "zuida_jingli");
        registerSimple("tizhi");
        registerWithMax("hunpo", "zuida_hunpo");
        registerMaxField("zuida_hunpo", "hunpo", "zuida_hunpo");
        registerWithMax("hunpo_kangxing", "hunpo_kangxing_shangxian", "hunpo_stability");
        registerMaxField("hunpo_kangxing_shangxian", "hunpo_kangxing", "hunpo_kangxing_shangxian", "hunpo_stability_max");
        registerWithMax("niantou", "niantou_zhida", "niantou_zuida");
        registerMaxField("niantou_zhida", "niantou", "niantou_zhida", "niantou_zuida");
        registerSimple("niantou_rongliang");
        
        registerSimple("renqi");
        registerSimple("qiyun");
        registerSimple("qiyun_shangxian");
        registerSimple("daode");

        registerSimple("daohen_jindao");
        registerSimple("daohen_shuidao");
        registerSimple("daohen_mudao");
        registerSimple("daohen_yandao");
        registerSimple("daohen_tudao");
        registerSimple("daohen_fengdao");
        registerSimple("daohen_guangdao");
        registerSimple("daohen_andao");
        registerSimple("daohen_leidao");
        registerSimple("daohen_dudao");
        registerSimple("daohen_yudao");
        registerSimple("dahen_zhoudao", "daohen_zhoudao");
        registerSimple("dahen_rendao", "daohen_rendao");
        registerSimple("dahen_tiandao", "daohen_tiandao");
        registerSimple("daohen_bingxuedao", "daohen_bingxue");
        registerSimple("dahen_qidao", "daohen_qidao");
        registerSimple("dahen_nudao", "daohen_nudao");
        registerSimple("dahen_zhidao", "daohen_zhidao");
        registerSimple("dahen_xingdao", "daohen_xingdao");
        registerSimple("dahen_zhendao", "daohen_zhendao");
        registerSimple("daohen_yingdao");
        registerSimple("daohen_lvdao");
        registerSimple("dahen_liandao", "daohen_liandao");
        registerSimple("daohen_lidao");
        registerSimple("daohen_shidao");
        registerSimple("daohen_huadao");
        registerSimple("daohen_toudao");
        registerSimple("daohen_yundao2", "daohen_yundao", "daohen_yundao_cloud");
        registerSimple("daohen_xindao");
        registerSimple("daohen_yindao");
        registerSimple("daohen_gudao");
        registerSimple("daohen_xudao");
        registerSimple("daohen_jindao2", "daohen_jindao", "daohen_jindao_forbidden");
        registerSimple("daohen_jiandao");
        registerSimple("daohen_daodao");
        registerSimple("daohen_hundao");
        registerSimple("daohen_dandao");
        registerSimple("daohen_xuedao");
        registerSimple("daohen_huandao");
        registerSimple("daohen_yuedao");
        registerSimple("daohen_mengdao");
        registerSimple("daohen_bingdao");
        registerSimple("daohen_bianhuadao");
    }

    private static void registerSimple(String canonicalField, String... aliases) {
        registerEffect(canonicalField, true, null, null, aliases);
    }

    private static void registerWithMax(String canonicalField, String maxField, String... aliases) {
        registerEffect(canonicalField, true, maxField, null, aliases);
    }

    private static void registerMaxField(String canonicalField, String baseField, String... aliases) {
        registerEffect(canonicalField, true, null, baseField, aliases);
    }

    private static void registerEffect(
            String canonicalField,
            boolean clampZero,
            String maxField,
            String clampBaseField,
            String... aliases
    ) {
        OrganScoreEffects.Effect effect = (entity, chestCavity, previous, current) -> {
            if (!(entity instanceof Player player) || player.level().isClientSide()) {
                return;
            }
            double delta = current - previous;
            if (Math.abs(delta) < EPSILON) {
                return;
            }
            GuzhenrenResourceBridge.open(player).ifPresent(handle -> {
                if (maxField != null && !maxField.isBlank()) {
                    handle.adjustDouble(canonicalField, delta, clampZero, maxField);
                } else {
                    handle.adjustDouble(canonicalField, delta, clampZero);
                }
                if (clampBaseField != null && !clampBaseField.isBlank()) {
                    handle.clampToMax(clampBaseField, canonicalField);
                }
            });
        };

        Set<String> scoreIds = new LinkedHashSet<>();
        scoreIds.add(canonicalField);
        if (aliases != null && aliases.length > 0) {
            scoreIds.addAll(Arrays.asList(aliases));
        }
        for (String id : scoreIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            registerForId(effect, id.toLowerCase(Locale.ROOT));
        }
    }

    private static void registerForId(OrganScoreEffects.Effect effect, String path) {
        ResourceLocation chestId = ChestCavity.id(path);
        OrganScoreEffects.register(chestId, effect);
        ResourceLocation compatId = ResourceLocation.fromNamespaceAndPath("guzhenren", path);
        OrganScoreEffects.register(compatId, effect);
    }
}
