package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordModelTuning;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.override.SwordModelOverrideDef;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.override.SwordModelOverrideRegistry;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.profile.SwordVisualProfile;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.profile.SwordVisualProfileRegistry;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.pipeline.AimResolver;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.gecko.SwordModelObject;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.gecko.SwordModelObjectRenderer;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.gecko.SwordGeoProfileObject;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.gecko.SwordGeoProfileObjectRenderer;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation.OrientationOps;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation.OrientationMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation.UpMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;

/**
 * 飞剑渲染器（Flying Sword Renderer）
 *
 * <p>基础渲染实现：
 * <ul>
 *   <li>使用铁剑物品模型</li>
 *   <li>根据移动方向旋转</li>
 *   <li>支持发光效果</li>
 * </ul>
 */
public class FlyingSwordRenderer extends EntityRenderer<FlyingSwordEntity> {

  private final ItemRenderer itemRenderer;
  // 由实体提供显示用物品栈；保留铁剑作为兜底
  private static final ItemStack FALLBACK_DISPLAY_ITEM = new ItemStack(Items.IRON_SWORD);

  public FlyingSwordRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.itemRenderer = context.getItemRenderer();
    this.shadowRadius = 0.15f;
  }

  @Override
  public void render(
      FlyingSwordEntity entity,
      float entityYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight) {

    // Phase 5: 仅在开关启用时查询 Gecko/Override/Profile（避免空查询开销）
    SwordModelOverrideDef def = null;
    SwordVisualProfile prof = null;
    if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning
        .ENABLE_GEO_OVERRIDE_PROFILE) {
      def = SwordModelOverrideRegistry.getForSword(entity).orElse(null);
      prof = SwordVisualProfileRegistry.getForSword(entity).orElse(null);
    }
    boolean useGecko =
        def != null
            && def.renderer == SwordModelOverrideDef.RendererKind.GECKO
            && FlyingSwordModelTuning.ENABLE_GECKOLIB
            && def.model != null
            && def.texture != null;

    poseStack.pushPose();

    // 使用平滑后的朝向向量，避免因速度微小变化导致的抖动
    Vec3 look;
    if (prof != null) {
      look = AimResolver.resolve(entity, switch (prof.align) {
        case TARGET -> SwordVisualProfile.AlignMode.TARGET;
        case OWNER -> SwordVisualProfile.AlignMode.OWNER;
        case NONE -> SwordVisualProfile.AlignMode.NONE;
        default -> SwordVisualProfile.AlignMode.VELOCITY;
      });
    } else if (def != null && def.alignMode == SwordModelOverrideDef.AlignMode.TARGET
        && entity.getTargetEntity() != null && entity.getTargetEntity().isAlive()) {
      var tgt = entity.getTargetEntity();
      look = tgt.position().add(0, tgt.getBbHeight() * 0.5, 0).subtract(entity.position()).normalize();
    } else {
      look = entity.getSmoothedLookAngle();
    }

    // Phase 8: 使用 OrientationOps 统一姿态计算
    float preRoll = prof != null ? prof.preRollDeg : (def != null ? def.preRollDeg : FlyingSwordModelTuning.BLADE_ROLL_DEGREES);
    float yawOffset = (prof != null ? prof.yawOffsetDeg - 90.0f : -90.0f + (def != null ? def.yawOffsetDeg : 0.0f));
    float pitchOffset = (prof != null ? prof.pitchOffsetDeg : (def != null ? def.pitchOffsetDeg : 0.0f));

    // 确定姿态计算模式
    OrientationMode orientationMode;
    if (FlyingSwordModelTuning.USE_BASIS_ORIENTATION) {
      // 优先使用 Profile/Override 指定的模式，否则使用全局默认
      orientationMode = prof != null ? prof.orientationMode : (def != null ? def.orientationMode : OrientationMode.BASIS);
    } else {
      // 全局禁用 BASIS，强制使用 LEGACY_EULER
      orientationMode = OrientationMode.LEGACY_EULER;
    }

    UpMode upMode = prof != null ? prof.upMode : (def != null ? def.upMode : UpMode.WORLD_Y);

    // 计算姿态四元数
    Vec3 up = new Vec3(0, 1, 0); // 默认世界 Y 轴
    Quaternionf orientation = OrientationOps.orientationFromForwardUp(
        look, up, preRoll, yawOffset, pitchOffset, orientationMode, upMode);

    // 应用姿态
    poseStack.mulPose(orientation);

    // 取消自旋效果，保持剑头朝向路线
    // float spinAngle = (entity.tickCount + partialTicks) * 20.0f;
    // poseStack.mulPose(Axis.ZP.rotationDegrees(spinAngle));

    // 缩放
    float scale = prof != null ? prof.scale : (def != null ? def.scale : 1.0f);
    poseStack.scale(scale, scale, scale);

    if (useGecko) {
      if (geckoRenderer == null) geckoRenderer = new SwordModelObjectRenderer();
      SwordModelObject obj = new SwordModelObject(def);
      obj.updateTick(entity.tickCount + partialTicks);
      // 分层渲染：如存在 textures 数组，逐层叠加；否则使用单一 texture
      java.util.List<net.minecraft.resources.ResourceLocation> layers =
          (def.textures != null && !def.textures.isEmpty())
              ? def.textures
              : java.util.List.of(def.texture);
      for (var tex : layers) {
        if (tex == null) continue;
        var rt = geckoRenderer.getRenderType(obj, tex, buffer, partialTicks);
        VertexConsumer vc = buffer.getBuffer(rt);
        geckoRenderer.render(poseStack, obj, buffer, rt, vc, packedLight, partialTicks);
      }
    } else if (prof != null && prof.renderer == SwordVisualProfile.RendererKind.GECKO
        && net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning
            .FlyingSwordModelTuning.ENABLE_GECKOLIB
        && prof.model != null) {
      if (profileGeckoRenderer == null) profileGeckoRenderer = new SwordGeoProfileObjectRenderer();
      SwordGeoProfileObject obj = new SwordGeoProfileObject(prof.model, prof.animation);
      obj.updateTick(entity.tickCount + partialTicks);
      java.util.List<net.minecraft.resources.ResourceLocation> layers =
          (prof.textures != null && !prof.textures.isEmpty())
              ? prof.textures
              : java.util.List.of();
      if (layers.isEmpty()) {
        // 无纹理则跳过（避免渲染错误）
      } else {
        for (var tex : layers) {
          if (tex == null) continue;
          obj.setTexture(tex);
          var rt = profileGeckoRenderer.getRenderType(obj, tex, buffer, partialTicks);
          VertexConsumer vc = buffer.getBuffer(rt);
          profileGeckoRenderer.render(poseStack, obj, buffer, rt, vc, packedLight, partialTicks);
        }
      }
    } else {
      // 渲染实体指定的物品模型
    ItemStack display = entity.getDisplayItemStack();
    ItemStack baseDisplay = display;
    if (def != null && def.displayItem != null) {
      var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(def.displayItem);
      if (item != null) {
        display = new ItemStack(item);
        // 尝试沿用原显示物品的附魔/荧光效果；若没有，则强制开启荧光以保留“附魔外观”
        try {
          var ench = baseDisplay == null ? null : baseDisplay.get(DataComponents.ENCHANTMENTS);
          // 按 Profile 的 glint 策略处理；若无 Profile 则沿用旧逻辑
          SwordVisualProfile.GlintMode glintMode = prof != null ? prof.glint : SwordVisualProfile.GlintMode.INHERIT;
          switch (glintMode) {
            case FORCE_ON -> display.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            case FORCE_OFF -> display.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
            default -> {
              if (ench != null && !ench.isEmpty()) {
                display.set(DataComponents.ENCHANTMENTS, ench);
              } else {
                Boolean glint = baseDisplay == null ? null : baseDisplay.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
                if (glint != null) display.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, glint);
              }
            }
          }
        } catch (Throwable ignored) {}
      }
    }
      if (display == null || display.isEmpty()) {
        display = FALLBACK_DISPLAY_ITEM;
      }

      this.itemRenderer.renderStatic(
          display,
          ItemDisplayContext.NONE,
          packedLight,
          OverlayTexture.NO_OVERLAY,
          poseStack,
          buffer,
          entity.level(),
          entity.getId());
    }

    poseStack.popPose();

    super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
  }

  // Geckolib 对象渲染器（延迟初始化）
  private SwordModelObjectRenderer geckoRenderer;
  private SwordGeoProfileObjectRenderer profileGeckoRenderer;

  @Override
  public ResourceLocation getTextureLocation(FlyingSwordEntity entity) {
    return InventoryMenu.BLOCK_ATLAS;
  }
}
