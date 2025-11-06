package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.flyingsword;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordAttributes;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordStorage;
import org.junit.jupiter.api.Test;

/**
 * 纯NBT往返测试：验证 RecalledSword 额外字段（显示物品ID/模型键/音效档/类型）能正确序列化。
 */
public class FlyingSwordStorageTest {

  @Test
  public void testRecalledSwordSerializeRoundtrip() {
    FlyingSwordAttributes attrs = FlyingSwordAttributes.createDefault();
    attrs.damageBase = 7.5;
    attrs.maxDurability = 123.0;

    CompoundTag dummyCC = new CompoundTag();
    dummyCC.putString("dummy", "yes");

    // 构造一个简化的 ItemStack NBT（只需有 id 字段即可用于往返校验）
    CompoundTag displayItem = new CompoundTag();
    displayItem.putString("id", "minecraft:diamond_sword");
    String modelKey = "guzhenren:models/sword/gecko_key";
    String soundProfile = "jade";
    String swordType = "guzhenren:flying_sword_zheng_dao"; // 仅作为字符串往返校验

    FlyingSwordStorage.RecalledSword original =
        new FlyingSwordStorage.RecalledSword(
            attrs, 3, 42, 88.0f, dummyCC, displayItem, null, modelKey, soundProfile, swordType, "00000000-0000-0000-0000-000000000000");

    CompoundTag nbt = original.serializeNBT();
    assertTrue(nbt.contains("Attributes"));
    assertEquals(3, nbt.getInt("Level"));
    assertEquals(42, nbt.getInt("Experience"));
    assertEquals(88.0f, nbt.getFloat("Durability"));
    assertTrue(nbt.contains("DisplayItem"));
    assertEquals("minecraft:diamond_sword", nbt.getCompound("DisplayItem").getString("id"));
    assertEquals(modelKey, nbt.getString("ModelKey"));
    assertEquals(soundProfile, nbt.getString("SoundProfile"));
    assertEquals(swordType, nbt.getString("SwordType"));

    FlyingSwordStorage.RecalledSword restored =
        FlyingSwordStorage.RecalledSword.fromNBT(nbt);

    assertNotNull(restored.attributes);
    assertEquals(3, restored.level);
    assertEquals(42, restored.experience);
    assertEquals(88.0f, restored.durability);
    assertNotNull(restored.displayItem);
    assertEquals("minecraft:diamond_sword", restored.displayItem.getString("id"));
    assertEquals(modelKey, restored.modelKey);
    assertEquals(soundProfile, restored.soundProfile);
    assertEquals(swordType, restored.swordType);
    assertEquals("00000000-0000-0000-0000-000000000000", restored.displayItemUUID);
  }
}
