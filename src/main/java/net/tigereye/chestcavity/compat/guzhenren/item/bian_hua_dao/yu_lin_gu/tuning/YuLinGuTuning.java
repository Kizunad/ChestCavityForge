package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yu_lin_gu.tuning;

import net.minecraft.resources.ResourceLocation;

public final class YuLinGuTuning {
    public static final String MOD_ID = "guzhenren";
    public static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_lin_gu");
    public static final ResourceLocation SHUI_JIA_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "shui_jia_gu");
    public static final ResourceLocation JIAO_WEI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiao_wei_gu");

    public static final int FISH_ARMOR_MAX_PROGRESS = 10;
    public static final int SHARK_ARMOR_THRESHOLD = 10;
    public static final int WET_BUFFER_TICKS = 20 * 4;
    public static final double HUNGER_COST_PER_SECOND = 1.0 / 4.0;
    public static final int WATER_HEAL_COOLDOWN_TICKS = 20 * 8;
    public static final int WATER_HEAL_COOLDOWN_FINAL_TICKS = 20 * 12;
    public static final int MAX_SUMMONS = 5;

    public static final String STATE_ROOT = "YuLinGu";
    public static final String PROGRESS_KEY = "FishArmorProgress";
    public static final String HAS_FISH_ARMOR_KEY = "HasFishArmor";
    public static final String HAS_SHARK_ARMOR_KEY = "HasSharkArmor";
    public static final String SHARK_TIER_UNLOCKED_KEY = "SharkTierUnlocked";
    public static final String HUNGER_PROGRESS_KEY = "HungerDebt";
    public static final String LAST_WET_TICK_KEY = "LastWetTick";
    public static final String WATER_HEAL_READY_AT_KEY = "WaterHealReadyAt";
    public static final String SUMMON_SEQUENCE_KEY = "SummonSequence";
    public static final String ACTIVE_SUMMONS_KEY = "ActiveSummons";

    public static final ResourceLocation WATER_HEAL_COOLDOWN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "cooldowns/yu_lin_gu_water_heal");

    private YuLinGuTuning() {}
}
