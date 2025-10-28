package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.slf4j.Logger;

/**
 * Player attachment for 五行化痕 skill state.
 * Stores last selected element, mode, and undo snapshot.
 */
public final class WuxingHuaHenAttachment {

  private static final Logger LOGGER = LogUtils.getLogger();

  /** Five Elements */
  public enum Element {
    JIN("daohen_jindao"),      // 金
    MU("daohen_mudao"),        // 木
    SHUI("daohen_shuidao"),    // 水
    YAN("daohen_yandao"),      // 炎
    TU("daohen_tudao");        // 土

    private final String poolKey;

    Element(String poolKey) {
      this.poolKey = poolKey;
    }

    public String poolKey() {
      return poolKey;
    }

    public static Element fromOrdinal(int ordinal) {
      Element[] values = values();
      if (ordinal < 0 || ordinal >= values.length) {
        return JIN;
      }
      return values[ordinal];
    }
  }

  /** Transmute mode */
  public enum Mode {
    LAST,      // 使用上次配置
    ALL,       // 全部转化
    RATIO_25,  // 25%
    RATIO_50,  // 50%
    RATIO_100, // 100%
    FIXED_10,  // 固定10
    FIXED_25,  // 固定25
    FIXED_50,  // 固定50
    FIXED_100; // 固定100

    public static Mode fromOrdinal(int ordinal) {
      Mode[] values = values();
      if (ordinal < 0 || ordinal >= values.length) {
        return LAST;
      }
      return values[ordinal];
    }
  }

  /** Undo snapshot */
  public static final class UndoSnapshot {
    private Element element = Element.JIN;
    private double amountReq = 0.0;
    private double amountOut = 0.0;
    private long expireTick = 0L;

    public Element element() {
      return element;
    }

    public double amountReq() {
      return amountReq;
    }

    public double amountOut() {
      return amountOut;
    }

    public long expireTick() {
      return expireTick;
    }

    public boolean isValid(long now) {
      return now < expireTick && amountReq > 0.0 && amountOut > 0.0;
    }

    public void set(Element element, double amountReq, double amountOut, long expireTick) {
      this.element = element;
      this.amountReq = amountReq;
      this.amountOut = amountOut;
      this.expireTick = expireTick;
    }

    public void clear() {
      this.amountReq = 0.0;
      this.amountOut = 0.0;
      this.expireTick = 0L;
    }

    public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("element", element.ordinal());
      tag.putDouble("amountReq", amountReq);
      tag.putDouble("amountOut", amountOut);
      tag.putLong("expireTick", expireTick);
      return tag;
    }

    public void load(CompoundTag tag) {
      this.element = Element.fromOrdinal(tag.getInt("element"));
      this.amountReq = tag.getDouble("amountReq");
      this.amountOut = tag.getDouble("amountOut");
      this.expireTick = tag.getLong("expireTick");
    }
  }

  private Element lastElement = Element.JIN;
  private Mode lastMode = Mode.LAST;
  private int lastFixedAmount = 10;
  private final UndoSnapshot undoSnapshot = new UndoSnapshot();

  public Element lastElement() {
    return lastElement;
  }

  public void setLastElement(Element element) {
    this.lastElement = element == null ? Element.JIN : element;
  }

  public Mode lastMode() {
    return lastMode;
  }

  public void setLastMode(Mode mode) {
    this.lastMode = mode == null ? Mode.LAST : mode;
  }

  public int lastFixedAmount() {
    return lastFixedAmount;
  }

  public void setLastFixedAmount(int amount) {
    this.lastFixedAmount = Math.max(1, amount);
  }

  public UndoSnapshot undoSnapshot() {
    return undoSnapshot;
  }

  public CompoundTag save(HolderLookup.Provider provider) {
    CompoundTag tag = new CompoundTag();
    tag.putInt("lastElement", lastElement.ordinal());
    tag.putInt("lastMode", lastMode.ordinal());
    tag.putInt("lastFixedAmount", lastFixedAmount);
    tag.put("undoSnapshot", undoSnapshot.save());
    return tag;
  }

  public void load(HolderLookup.Provider provider, CompoundTag tag) {
    this.lastElement = Element.fromOrdinal(tag.getInt("lastElement"));
    this.lastMode = Mode.fromOrdinal(tag.getInt("lastMode"));
    this.lastFixedAmount = tag.getInt("lastFixedAmount");
    if (tag.contains("undoSnapshot")) {
      this.undoSnapshot.load(tag.getCompound("undoSnapshot"));
    }
  }

  /** Serializer for NeoForge attachment system */
  public static final class Serializer
      implements IAttachmentSerializer<CompoundTag, WuxingHuaHenAttachment> {
    @Override
    public WuxingHuaHenAttachment read(
        IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
      WuxingHuaHenAttachment attachment = new WuxingHuaHenAttachment();
      attachment.load(provider, tag);
      return attachment;
    }

    @Override
    public CompoundTag write(WuxingHuaHenAttachment attachment, HolderLookup.Provider provider) {
      return attachment.save(provider);
    }
  }
}
