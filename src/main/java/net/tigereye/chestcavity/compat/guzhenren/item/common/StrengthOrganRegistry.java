package net.tigereye.chestcavity.compat.guzhenren.item.common;

import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Hard-coded registry of Guzhenren organs/items whose JSON declare a strength score.
 * Generated via script扫描 data/chestcavity/organs/guzhenren.
 */
public final class StrengthOrganRegistry {

    private static final Set<ResourceLocation> STRENGTH_ORGANS = Set.of(
            ResourceLocation.parse("guzhenren:bai_shouwangxiongpi"),
            ResourceLocation.parse("guzhenren:bailangwurou"),
            ResourceLocation.parse("guzhenren:baishouquanpi"),
            ResourceLocation.parse("guzhenren:baishouquanrou"),
            ResourceLocation.parse("guzhenren:baishouquanyan"),
            ResourceLocation.parse("guzhenren:baishouwanghugu"),
            ResourceLocation.parse("guzhenren:baishouwanghupi"),
            ResourceLocation.parse("guzhenren:baishouwanghurou"),
            ResourceLocation.parse("guzhenren:baishouwanghuya"),
            ResourceLocation.parse("guzhenren:baishouwangxiongyan"),
            ResourceLocation.parse("guzhenren:bbaishouwanglangpi"),
            ResourceLocation.parse("guzhenren:bing_ji_gu"),
            ResourceLocation.parse("guzhenren:ganjingu"),
            ResourceLocation.parse("guzhenren:langrou"),
            ResourceLocation.parse("guzhenren:langyan"),
            ResourceLocation.parse("guzhenren:lingrou"),
            ResourceLocation.parse("guzhenren:llangpi"),
            ResourceLocation.parse("guzhenren:qian_shouwangxiongpi"),
            ResourceLocation.parse("guzhenren:qianlangwangrou"),
            ResourceLocation.parse("guzhenren:qianshouquanpi"),
            ResourceLocation.parse("guzhenren:qianshouquanyan"),
            ResourceLocation.parse("guzhenren:qianshouwanghugu"),
            ResourceLocation.parse("guzhenren:qianshouwanghupi"),
            ResourceLocation.parse("guzhenren:qianshouwanghurou"),
            ResourceLocation.parse("guzhenren:qianshouwangxiongyan"),
            ResourceLocation.parse("guzhenren:qqianshouwanglangpi"),
            ResourceLocation.parse("guzhenren:qqianshouwanglangyan"),
            ResourceLocation.parse("guzhenren:quan_li_yi_fu_gu"),
            ResourceLocation.parse("guzhenren:quanpi"),
            ResourceLocation.parse("guzhenren:quanrou"),
            ResourceLocation.parse("guzhenren:quanyan"),
            ResourceLocation.parse("guzhenren:shouhanghugu"),
            ResourceLocation.parse("guzhenren:shouhanghupi"),
            ResourceLocation.parse("guzhenren:shouhanghurou"),
            ResourceLocation.parse("guzhenren:shouhangquanpi"),
            ResourceLocation.parse("guzhenren:shouhangquanrou"),
            ResourceLocation.parse("guzhenren:shouhangquanyan"),
            ResourceLocation.parse("guzhenren:shouhuanghuyan"),
            ResourceLocation.parse("guzhenren:shui_ti_gu"),
            ResourceLocation.parse("guzhenren:tiexuegu"),
            ResourceLocation.parse("guzhenren:tipogu"),
            ResourceLocation.parse("guzhenren:wan_shouquanpi"),
            ResourceLocation.parse("guzhenren:wan_shouwangxiongpi"),
            ResourceLocation.parse("guzhenren:wanlangwurou"),
            ResourceLocation.parse("guzhenren:wanshangwanglangpi"),
            ResourceLocation.parse("guzhenren:wanshouquanyan"),
            ResourceLocation.parse("guzhenren:wanshouwanghugu"),
            ResourceLocation.parse("guzhenren:wanshouwanghupi"),
            ResourceLocation.parse("guzhenren:wanshouwanghurou"),
            ResourceLocation.parse("guzhenren:wanshouwangxiongyan"),
            ResourceLocation.parse("guzhenren:wawanshouwanglangyan"),
            ResourceLocation.parse("guzhenren:xie_di_gu"),
            ResourceLocation.parse("guzhenren:xiongpi"),
            ResourceLocation.parse("guzhenren:zi_li_geng_sheng_gu_3")
    );

    private StrengthOrganRegistry() {}

    public static boolean isStrengthOrgan(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && STRENGTH_ORGANS.contains(id);
    }
}
