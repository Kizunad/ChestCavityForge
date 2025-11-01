package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.jianxin;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * 工具：定位并访问玩家身上的“剑心蛊” OrganState。
 */
public final class JianXinGuStateOps {

  private JianXinGuStateOps() {}

  /** 行为根键，需与 JianXinGuOrganBehavior 保持一致。 */
  public static final String STATE_ROOT = "JianXinGu";

  /**
   * 查找玩家胸腔中的“剑心蛊”并返回其 OrganState。
   */
  public static Optional<OrganState> resolve(ServerPlayer player) {
    if (player == null) return Optional.empty();
    ChestCavityInstance cc = CCAttachments.getExistingChestCavity(player).orElse(null);
    if (cc == null || cc.inventory == null) return Optional.empty();

    // 优先匹配确切物品 ID
    for (int i = 0, n = cc.inventory.getContainerSize(); i < n; i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (s.isEmpty()) continue;
      net.minecraft.resources.ResourceLocation id =
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
      if (id != null
          && id.equals(
              net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior
                  .JianXinGuOrganBehavior.ORGAN_ID)) {
        return Optional.of(OrganState.of(s, STATE_ROOT));
      }
    }

    // 回退：任取一个非空栈（兼容旧期逻辑）
    for (int i = 0, n = cc.inventory.getContainerSize(); i < n; i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (!s.isEmpty()) {
        return Optional.of(OrganState.of(s, STATE_ROOT));
      }
    }
    return Optional.empty();
  }
}

