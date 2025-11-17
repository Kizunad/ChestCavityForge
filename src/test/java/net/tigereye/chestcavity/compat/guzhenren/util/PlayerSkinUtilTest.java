package net.tigereye.chestcavity.compat.guzhenren.util;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;
import org.junit.jupiter.api.Test;

public class PlayerSkinUtilTest {

  @Test
  void withTint_nullBase_usesDefaultSteveTextureAndModel() {
    float r = 0.1f, g = 0.2f, b = 0.3f, a = 0.4f;
    PlayerSkinUtil.SkinSnapshot snap = PlayerSkinUtil.withTint(null, r, g, b, a);
    assertNotNull(snap);
    assertEquals("default", snap.model());
    ResourceLocation tex = snap.texture();
    assertEquals("minecraft", tex.getNamespace());
    assertEquals("textures/entity/player/wide/steve.png", tex.getPath());
    assertEquals(r, snap.red(), 1e-6);
    assertEquals(g, snap.green(), 1e-6);
    assertEquals(b, snap.blue(), 1e-6);
    assertEquals(a, snap.alpha(), 1e-6);
  }
}
