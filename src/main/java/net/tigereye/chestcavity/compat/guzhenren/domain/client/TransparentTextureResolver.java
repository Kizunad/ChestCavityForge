package net.tigereye.chestcavity.compat.guzhenren.domain.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 将带“预览棋盘格”的 PNG 在客户端运行时转为透明背景的动态纹理。
 *
 * <p>注意：这只是兜底修正，最佳实践仍是提供真正透明的源 PNG。
 */
public final class TransparentTextureResolver {

  private TransparentTextureResolver() {}

  private static final Map<ResourceLocation, ResourceLocation> CACHE = new HashMap<>();

  /** 返回已处理（去棋盘格）的纹理路径，失败时回退原路径。 */
  public static ResourceLocation getOrProcess(ResourceLocation original) {
    if (original == null) return original;
    ResourceLocation cached = CACHE.get(original);
    if (cached != null) return cached;

    Minecraft mc = Minecraft.getInstance();
    try {
      var resOpt = mc.getResourceManager().getResource(original);
      if (resOpt.isEmpty()) {
        return original;
      }
      try (InputStream in = resOpt.get().open()) {
        NativeImage src = NativeImage.read(in);
        NativeImage out = new NativeImage(src.getWidth(), src.getHeight(), false);

        // 近似识别两种常见的棋盘格灰度色（亮/暗），允许一定偏差
        int[] grid = new int[] {0xB3, 0xB3, 0xB3, 0xFF, 0x8F, 0x8F, 0x8F, 0xFF};
        int thr = 14; // 容差

        for (int y = 0; y < src.getHeight(); y++) {
          for (int x = 0; x < src.getWidth(); x++) {
            int argb = src.getPixelRGBA(x, y);
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = (argb) & 0xFF;

            if (a == 0) {
              out.setPixelRGBA(x, y, 0x00000000);
              continue;
            }

            boolean isGrid =
                approx(r, grid[0], thr) && approx(g, grid[1], thr) && approx(b, grid[2], thr)
                    || approx(r, grid[4], thr)
                        && approx(g, grid[5], thr)
                        && approx(b, grid[6], thr);

            if (isGrid) {
              // 直接抹为透明
              out.setPixelRGBA(x, y, 0x00000000);
            } else {
              // 保留像素，保持原 alpha
              out.setPixelRGBA(x, y, argb);
            }
          }
        }

        DynamicTexture dyn = new DynamicTexture(out);
        ResourceLocation dynId =
            ResourceLocation.fromNamespaceAndPath(
                original.getNamespace(), "dynamic/clean/" + original.getPath().replace('/', '_'));
        mc.getTextureManager().register(dynId, dyn);
        CACHE.put(original, dynId);
        src.close();
        return dynId;
      }
    } catch (IOException | RuntimeException ex) {
      // 回退：使用原图
      return original;
    }
  }

  private static boolean approx(int v, int t, int thr) {
    return Math.abs(v - t) <= thr;
  }
}
