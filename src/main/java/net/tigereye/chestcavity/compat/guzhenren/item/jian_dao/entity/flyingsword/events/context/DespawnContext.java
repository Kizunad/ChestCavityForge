package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;

/**
 * 飞剑消散或召回事件上下文
 */
public class DespawnContext {
  public final FlyingSwordEntity sword;
  public final ServerLevel level;
  @Nullable public final Player owner;

  /** 消散原因 */
  public final Reason reason;

  /**
   * 如果是召回到物品，这里存储目标ItemStack（可修改NBT）
   * <p>如果为null，表示直接消散不回收
   */
  @Nullable public ItemStack targetStack;

  /** 额外的NBT数据（钩子可写入，会合并到targetStack） */
  public final CompoundTag customData = new CompoundTag();

  /** 是否阻止消散（设为true将保留实体） */
  public boolean preventDespawn = false;

  public DespawnContext(
      FlyingSwordEntity sword,
      ServerLevel level,
      @Nullable Player owner,
      Reason reason,
      @Nullable ItemStack targetStack) {
    this.sword = sword;
    this.level = level;
    this.owner = owner;
    this.reason = reason;
    this.targetStack = targetStack;
  }

  public enum Reason {
    /** 玩家主动召回 */
    RECALLED,
    /** 被收剑令等效果夺取 */
    CAPTURED,
    /** 耐久耗尽 */
    DURABILITY_DEPLETED,
    /** 维持不足 */
    UPKEEP_FAILED,
    /** 主人离线/死亡 */
    OWNER_GONE,
    /** 其他原因 */
    OTHER
  }
}
