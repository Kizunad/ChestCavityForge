package net.tigereye.chestcavity.compat.common.tuning;

import net.minecraft.resources.ResourceLocation;

public final class ShouPiGuTuning {
    public static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "shou_pi_gu");
  public static final double THICK_SKIN_REDUCTION = 0.08D;
  public static final int THICK_SKIN_WINDOW_TICKS = 20;
  public static final int FASCIA_TRIGGER = 5;
  public static final long FASCIA_COOLDOWN_TICKS = 200L; // 10s
  public static final double FASCIA_ACTIVE_REDUCTION = 0.12D;
  public static final int STOIC_MAX_STACKS = 6;
  public static final long STOIC_DEFAULT_LOCK_TICKS = 8 * 20L;
  public static final long SOFT_POOL_WINDOW_TICKS = 5 * 20L;
  public static final long ROLL_DAMAGE_WINDOW_TICKS = 12L; // 0.6s
  public static final double ROLL_DAMAGE_REDUCTION = 0.6D;
  public static final double ROLL_DISTANCE = 3.0D;
  public static final double CRASH_DISTANCE = 4.0D;
  public static final long CRASH_IMMUNE_TICKS = 10L;
  public static final double CRASH_SPLASH_RADIUS = 1.5D;
  public static final double STOIC_SLOW_RADIUS = 3.0D;
  public static final int STOIC_SLOW_TICKS = 40;
  public static final int STOIC_SLOW_AMPLIFIER = 0;
  public static final long SOFT_PROJECTILE_COOLDOWN_TICKS = 12L; // 0.6s shared thorns window
  public static final double STOIC_ACTIVE_SOFT_BONUS = 0.05D;

  public static final double ACTIVE_DRUM_DEFENSE_BONUS = 0.06D;
  public static final double ACTIVE_DRUM_SOFT_BONUS = 0.10D;
  public static final int ACTIVE_DRUM_DURATION_TICKS = 5 * 20;
  public static final long ACTIVE_DRUM_COOLDOWN_TICKS = 20 * 20L;
  public static final double ACTIVE_DRUM_KNOCKBACK_RESIST = 0.5D;
  public static final double ACTIVE_DRUM_BASE_COST = 40.0D;

  public static final double ACTIVE_ROLL_BASE_COST = 25.0D;
  public static final long ACTIVE_ROLL_COOLDOWN_TICKS = 14 * 20L;

  public static final double SYNERGY_CRASH_BASE_COST = 60.0D;
  public static final long SYNERGY_CRASH_COOLDOWN_TICKS = 18 * 20L;

    public static final String KEY_STOIC_STACKS = "StoicStacks";
    public static final String KEY_STOIC_ACCUM = "StoicAccumulator";
    public static final String KEY_STOIC_ACTIVE_UNTIL = "StoicActiveUntil";
    public static final String KEY_STOIC_LOCK_UNTIL = "StoicLockUntil";
    public static final String KEY_SOFT_TEMP_BONUS = "SoftTempBonus";
    public static final String KEY_SOFT_TEMP_BONUS_EXPIRE = "SoftTempBonusExpire";
    public static final String KEY_FASCIA_COUNT = "FasciaCount";
    public static final String KEY_FASCIA_COOLDOWN = "FasciaCooldown";
    public static final String KEY_FASCIA_ACTIVE_UNTIL = "FasciaActiveUntil";
    public static final String KEY_ACTIVE_DRUM_READY = "ActiveDrumReady";
    public static final String KEY_ACTIVE_DRUM_EXPIRE = "ActiveDrumExpire";

    public static final net.minecraft.resources.ResourceLocation HUPI_GU_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("guzhenren", "hupigu");
    public static final net.minecraft.resources.ResourceLocation TIE_GU_GU_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("guzhenren", "tie_gu_gu");


    public enum Tier {
        STAGE1,
        STAGE2,
        STAGE3,
        STAGE4,
        STAGE5
    }

    public record TierParameters(Tier stage, double stoicMitigation, double stoicShield, long lockTicks) {}

    public static final String KEY_ROLL_READY = "RollReady";
    public static final String KEY_ROLL_EXPIRE = "RollExpire";
    public static final String KEY_CRASH_READY = "CrashReady";
    public static final String KEY_CRASH_IMMUNE = "CrashImmuneExpire";
    public static final String KEY_THICK_SKIN_READY = "ThickSkinReady";
    public static final String KEY_THICK_SKIN_EXPIRE = "ThickSkinExpire";
    public static final String KEY_SOFT_POOL_VALUE = "SoftReflectPool";
    public static final String KEY_SOFT_POOL_EXPIRE = "SoftReflectExpire";
    public static final ResourceLocation ACTIVE_DRUM_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "skill/shou_pi_gu_drum");
    public static final String KEY_STOIC_READY = "StoicReady";


  private ShouPiGuTuning() {}
}
