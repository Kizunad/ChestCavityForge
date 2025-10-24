package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/** Shared constants for the Hun Shou Hua soul beast transformation flow. */
public final class HunShouHuaConstants {

  private HunShouHuaConstants() {}

  public static final ResourceLocation FLOW_ID =
      ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "hun_dao/hun_shou_hua");
  public static final String ABILITY_FLAG = ChestCavity.MODID + ":hun_shou_hua";
  public static final String FAIL_REASON_VARIABLE = "hun_shou_hua.fail_reason";
  public static final ResourceLocation FX_PREPARE =
      ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "soulbeast_transform_prepare");
  public static final ResourceLocation FX_CHANNEL =
      ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "soulbeast_transform_channel");
  public static final ResourceLocation FX_SUCCESS =
      ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "soulbeast_transform_success");
  public static final ResourceLocation FX_FAIL =
      ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "soulbeast_transform_fail");
  public static final ResourceLocation SOUND_FAIL =
      ResourceLocation.fromNamespaceAndPath(
          ChestCavity.MODID, "custom.soulbeast.fail_soulbeast_transform");
  public static final ResourceLocation TRANSFORM_SOURCE =
      ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "hun_dao/hun_shou_hua");

  public static final long FAILURE_REASON_NONE = 0L;
  public static final long FAILURE_REASON_RESOURCES = 1L;
  public static final long FAILURE_REASON_ALREADY_USED = 2L;
}
