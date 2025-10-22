package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BoneGunProjectile;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.registration.CCAttachments;

/** Spawns the Blood Bone Bomb custom projectile, computing damage scaling from linkage channels. */
public record EmitBloodBoneBombAction(double baseDamage) implements Action {
  public static final String ID = "emit.blood_bone_bomb";

  private static final ResourceLocation LI_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/li_dao_increase_effect");
  private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/xue_dao_increase_effect");
  private static final ResourceLocation GU_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/gu_dao_increase_effect");
  private static final ResourceLocation RENDER_ITEM_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_qiang");

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "发射血骨爆弹(基础伤害 " + baseDamage + ")";
  }

  @Override
  public void execute(GuScriptContext context) {
    if (context == null || context.performer() == null) {
      return;
    }
    if (!(context.performer().level() instanceof ServerLevel level)) {
      return;
    }
    ServerPlayer player = (ServerPlayer) context.performer();
    ChestCavityInstance cc = CCAttachments.getChestCavity(player);
    ActiveLinkageContext linkage = LinkageManager.getContext(cc);

    double liIncrease = read(linkage, LI_DAO_INCREASE_EFFECT);
    double xueIncrease = read(linkage, XUE_DAO_INCREASE_EFFECT);
    double guIncrease = read(linkage, GU_DAO_INCREASE_EFFECT);
    double multiplier =
        Math.max(0.0, (1.0 + liIncrease) * (1.0 + xueIncrease) * (1.0 + guIncrease));
    double damage = baseDamage * multiplier;

    // Log the final damage computation for auditing
    ChestCavity.LOGGER.info(
        "[GuScript][Damage] emit.blood_bone_bomb by {} base={} multiplier={} -> final={}",
        player.getGameProfile().getName(),
        String.format("%.3f", baseDamage),
        String.format("%.3f", multiplier),
        String.format("%.3f", damage));

    Vec3 look = player.getLookAngle().normalize();
    Vec3 origin = player.getEyePosition().add(look.scale(0.4));

    BoneGunProjectile projectile = new BoneGunProjectile(level, player, renderStack());
    projectile.configurePayload((float) damage, multiplier);
    projectile.setPos(origin);
    projectile.shoot(look.x, look.y, look.z, 3.75f, 0.0f);
    level.addFreshEntity(projectile);

    // ignition FX on spawn
    float intensity = (float) Math.max(1.0F, 1.0F + multiplier * 0.25F);
    net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher.play(
        level,
        net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BloodBoneBombFx
            .PROJECTILE_LAUNCH,
        origin,
        look,
        look,
        player,
        null,
        intensity);
  }

  private static double read(ActiveLinkageContext ctx, ResourceLocation id) {
    LinkageChannel ch = ctx.getOrCreateChannel(id);
    return ch.get();
  }

  private static ItemStack renderStack() {
    return BuiltInRegistries.ITEM
        .getOptional(RENDER_ITEM_ID)
        .map(ItemStack::new)
        .orElse(ItemStack.EMPTY);
  }
}
