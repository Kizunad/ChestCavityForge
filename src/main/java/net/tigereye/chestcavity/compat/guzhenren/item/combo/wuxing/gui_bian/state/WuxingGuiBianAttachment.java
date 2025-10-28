package net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.gui_bian.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.hua_hen.state.WuxingHuaHenAttachment;

/**
 * 五行归变·逆转 玩家附加数据
 * 存储转化模式、冻结状态等
 */
public final class WuxingGuiBianAttachment {

  /** 转化模式 */
  public enum ConversionMode {
    TEMPORARY, // 暂时模式：20秒后返还
    PERMANENT  // 永久模式：有损耗
  }

  /** 冻结快照（暂时模式） */
  public static final class FreezeSnapshot {
    private WuxingHuaHenAttachment.Element element = WuxingHuaHenAttachment.Element.JIN;
    private double amountConsumed = 0.0; // 消耗的五行道痕量
    private double amountOut = 0.0;      // 产出的变化道痕量
    private long expireTick = 0L;        // 过期时刻

    public WuxingHuaHenAttachment.Element element() {
      return element;
    }

    public double amountConsumed() {
      return amountConsumed;
    }

    public double amountOut() {
      return amountOut;
    }

    public long expireTick() {
      return expireTick;
    }

    public boolean isValid(long now) {
      return now < expireTick && amountConsumed > 0.0 && amountOut > 0.0;
    }

    public void set(
        WuxingHuaHenAttachment.Element element,
        double amountConsumed,
        double amountOut,
        long expireTick) {
      this.element = element;
      this.amountConsumed = amountConsumed;
      this.amountOut = amountOut;
      this.expireTick = expireTick;
    }

    public void clear() {
      this.amountConsumed = 0.0;
      this.amountOut = 0.0;
      this.expireTick = 0L;
    }

    public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("element", element.ordinal());
      tag.putDouble("amountConsumed", amountConsumed);
      tag.putDouble("amountOut", amountOut);
      tag.putLong("expireTick", expireTick);
      return tag;
    }

    public void load(CompoundTag tag) {
      this.element = WuxingHuaHenAttachment.Element.fromOrdinal(tag.getInt("element"));
      this.amountConsumed = tag.getDouble("amountConsumed");
      this.amountOut = tag.getDouble("amountOut");
      this.expireTick = tag.getLong("expireTick");
    }
  }

  private ConversionMode lastMode = ConversionMode.TEMPORARY;
  private WuxingHuaHenAttachment.Element lastElement = WuxingHuaHenAttachment.Element.JIN;
  private WuxingHuaHenAttachment.Mode lastTransformMode = WuxingHuaHenAttachment.Mode.ALL;
  private double fixedAmount = 10.0;
  private final FreezeSnapshot freezeSnapshot = new FreezeSnapshot();

  public ConversionMode lastMode() {
    return lastMode;
  }

  public void setLastMode(ConversionMode mode) {
    this.lastMode = mode == null ? ConversionMode.TEMPORARY : mode;
  }

  public WuxingHuaHenAttachment.Element lastElement() {
    return lastElement;
  }

  public void setLastElement(WuxingHuaHenAttachment.Element element) {
    this.lastElement = element == null ? WuxingHuaHenAttachment.Element.JIN : element;
  }

  public WuxingHuaHenAttachment.Mode lastTransformMode() {
    return lastTransformMode;
  }

  public void setLastTransformMode(WuxingHuaHenAttachment.Mode mode) {
    this.lastTransformMode = mode == null ? WuxingHuaHenAttachment.Mode.ALL : mode;
  }

  public double fixedAmount() {
    return fixedAmount;
  }

  public void setFixedAmount(double amount) {
    this.fixedAmount = Math.max(1.0, amount);
  }

  public FreezeSnapshot freezeSnapshot() {
    return freezeSnapshot;
  }

  public CompoundTag save(HolderLookup.Provider provider) {
    CompoundTag tag = new CompoundTag();
    tag.putInt("lastMode", lastMode.ordinal());
    tag.put("freezeSnapshot", freezeSnapshot.save());
    return tag;
  }

  public void load(HolderLookup.Provider provider, CompoundTag tag) {
    int modeOrdinal = tag.getInt("lastMode");
    this.lastMode = modeOrdinal >= 0 && modeOrdinal < ConversionMode.values().length
        ? ConversionMode.values()[modeOrdinal]
        : ConversionMode.TEMPORARY;
    if (tag.contains("freezeSnapshot")) {
      this.freezeSnapshot.load(tag.getCompound("freezeSnapshot"));
    }
  }

  /** Serializer for NeoForge attachment system */
  public static final class Serializer
      implements IAttachmentSerializer<CompoundTag, WuxingGuiBianAttachment> {
    @Override
    public WuxingGuiBianAttachment read(
        IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
      WuxingGuiBianAttachment attachment = new WuxingGuiBianAttachment();
      attachment.load(provider, tag);
      return attachment;
    }

    @Override
    public CompoundTag write(WuxingGuiBianAttachment attachment, HolderLookup.Provider provider) {
      return attachment.save(provider);
    }
  }
}
