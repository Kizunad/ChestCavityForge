package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import org.slf4j.Logger;

/**
 * Player attachment that stores all state for 阴阳转身蛊（阴/阳双态资源池、锚点、冷却等）。
 *
 * <p>该附件挂在 Player 上，以避免器官堆叠或物品替换导致的状态丢失。序列化/反序列化以 {@link CompoundTag}
 * 为载体，确保可跨存档持久化，并允许后续在 Capability 层扩展。
 */
public final class YinYangDualityAttachment {

  private static final Logger LOGGER = LogUtils.getLogger();

  public enum Mode {
    YIN,
    YANG;

    public Mode opposite() {
      return this == YIN ? YANG : YIN;
    }
  }

  private Mode currentMode = Mode.YANG;
  private int sharedCultivation;
  private long sealEndTick;
  private long swapWindowEndTick;
  private long fallGuardEndTick;

  private ResourcePool yinPool = ResourcePool.empty();
  private ResourcePool yangPool = ResourcePool.empty();
  private Anchor yinAnchor = Anchor.unset();
  private Anchor yangAnchor = Anchor.unset();
  private final DualStrikeWindow dualStrike = new DualStrikeWindow();
  private final Map<String, Long> cooldownTable = new HashMap<>();

  public Mode currentMode() {
    return currentMode;
  }

  public void setCurrentMode(Mode mode) {
    this.currentMode = mode == null ? Mode.YANG : mode;
  }

  public ResourcePool pool(Mode mode) {
    return switch (mode == null ? Mode.YANG : mode) {
      case YIN -> yinPool;
      case YANG -> yangPool;
    };
  }

  public ResourcePool yinPool() {
    return yinPool;
  }

  public ResourcePool yangPool() {
    return yangPool;
  }

  public Anchor anchor(Mode mode) {
    return switch (mode == null ? Mode.YANG : mode) {
      case YIN -> yinAnchor;
      case YANG -> yangAnchor;
    };
  }

  public void setAnchor(Mode mode, Anchor anchor) {
    if (mode == null) {
      return;
    }
    if (mode == Mode.YIN) {
      this.yinAnchor = anchor == null ? Anchor.unset() : anchor;
    } else {
      this.yangAnchor = anchor == null ? Anchor.unset() : anchor;
    }
  }

  public int sharedCultivation() {
    return sharedCultivation;
  }

  public void setSharedCultivation(int value) {
    this.sharedCultivation = value;
  }

  public long sealEndTick() {
    return sealEndTick;
  }

  public void setSealEndTick(long value) {
    this.sealEndTick = Math.max(0L, value);
  }

  public long swapWindowEndTick() {
    return swapWindowEndTick;
  }

  public void setSwapWindowEndTick(long value) {
    this.swapWindowEndTick = Math.max(0L, value);
  }

  public long fallGuardEndTick() {
    return fallGuardEndTick;
  }

  public void setFallGuardEndTick(long value) {
    this.fallGuardEndTick = Math.max(0L, value);
  }

  public DualStrikeWindow dualStrike() {
    return dualStrike;
  }

  public long getCooldown(ResourceLocation abilityId) {
    if (abilityId == null) {
      return 0L;
    }
    return cooldownTable.getOrDefault(abilityId.toString(), 0L);
  }

  public void setCooldown(ResourceLocation abilityId, long readyAtTick) {
    if (abilityId == null) {
      return;
    }
    if (readyAtTick <= 0L) {
      cooldownTable.remove(abilityId.toString());
    } else {
      cooldownTable.put(abilityId.toString(), readyAtTick);
    }
  }

  public Map<String, Long> cooldownTable() {
    return cooldownTable;
  }

  public CompoundTag save(HolderLookup.Provider provider) {
    CompoundTag tag = new CompoundTag();
    tag.putString("Mode", currentMode.name());
    tag.putInt("SharedCultivation", sharedCultivation);
    tag.putLong("SealEnd", sealEndTick);
    tag.putLong("SwapWindowEnd", swapWindowEndTick);
    tag.putLong("FallGuardEnd", fallGuardEndTick);
    tag.put("YinPool", yinPool.save());
    tag.put("YangPool", yangPool.save());
    tag.put("YinAnchor", yinAnchor.save());
    tag.put("YangAnchor", yangAnchor.save());
    tag.put("DualStrike", dualStrike.save());
    CompoundTag cooldowns = new CompoundTag();
    for (Map.Entry<String, Long> entry : cooldownTable.entrySet()) {
      cooldowns.putLong(entry.getKey(), entry.getValue());
    }
    tag.put("Cooldowns", cooldowns);
    return tag;
  }

  public void load(CompoundTag tag, HolderLookup.Provider provider) {
    if (tag == null || tag.isEmpty()) {
      return;
    }
    currentMode = Mode.valueOf(tag.getString("Mode").isEmpty() ? "YANG" : tag.getString("Mode"));
    sharedCultivation = tag.getInt("SharedCultivation");
    sealEndTick = Math.max(0L, tag.getLong("SealEnd"));
    swapWindowEndTick = Math.max(0L, tag.getLong("SwapWindowEnd"));
    fallGuardEndTick = Math.max(0L, tag.getLong("FallGuardEnd"));
    yinPool = ResourcePool.from(tag.getCompound("YinPool"));
    yangPool = ResourcePool.from(tag.getCompound("YangPool"));
    yinAnchor = Anchor.from(tag.getCompound("YinAnchor"));
    yangAnchor = Anchor.from(tag.getCompound("YangAnchor"));
    dualStrike.load(tag.getCompound("DualStrike"));
    cooldownTable.clear();
    CompoundTag cooldowns = tag.getCompound("Cooldowns");
    for (String key : cooldowns.getAllKeys()) {
      cooldownTable.put(key, Math.max(0L, cooldowns.getLong(key)));
    }
  }

  /** Anchor snapshot for一次态锚点（记录维度、坐标与朝向）。 */
  public static final class Anchor {
    private ResourceLocation dimension;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean valid;

    private Anchor() {}

    public static Anchor unset() {
      return new Anchor();
    }

    public static Anchor from(CompoundTag tag) {
      Anchor anchor = new Anchor();
      anchor.load(tag);
      return anchor;
    }

    public boolean isValid() {
      return valid && dimension != null;
    }

    public ResourceLocation dimension() {
      return dimension;
    }

    public double x() {
      return x;
    }

    public double y() {
      return y;
    }

    public double z() {
      return z;
    }

    public float yaw() {
      return yaw;
    }

    public float pitch() {
      return pitch;
    }

    public static Anchor create(
        ResourceLocation dimension, double x, double y, double z, float yaw, float pitch) {
      Anchor anchor = new Anchor();
      anchor.dimension = dimension;
      anchor.x = x;
      anchor.y = y;
      anchor.z = z;
      anchor.yaw = yaw;
      anchor.pitch = pitch;
      anchor.valid = dimension != null;
      return anchor;
    }

    public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      if (!isValid()) {
        tag.putBoolean("Valid", false);
        return tag;
      }
      tag.putBoolean("Valid", true);
      tag.putString("Dim", dimension.toString());
      tag.putDouble("X", x);
      tag.putDouble("Y", y);
      tag.putDouble("Z", z);
      tag.putFloat("Yaw", yaw);
      tag.putFloat("Pitch", pitch);
      return tag;
    }

    public void load(CompoundTag tag) {
      if (tag == null || !tag.getBoolean("Valid")) {
        this.dimension = null;
        this.valid = false;
        return;
      }
      String dim = tag.getString("Dim");
      this.dimension = ResourceLocation.tryParse(dim);
      this.x = tag.getDouble("X");
      this.y = tag.getDouble("Y");
      this.z = tag.getDouble("Z");
      this.yaw = tag.getFloat("Yaw");
      this.pitch = tag.getFloat("Pitch");
      this.valid = this.dimension != null;
    }
  }

  /** 十维资源池（真元、精力、魂魄等）。 */
  public static final class ResourcePool {
    private double zhenyuan;
    private double maxZhenyuan;
    private double jingli;
    private double maxJingli;
    private double soul;
    private double maxSoul;
    private double nianTou;
    private double maxNianTou;
    private int food;
    private double hpMark;
    private double shouYuan;
    private double renQi;
    private double daoDe;
    private double qiYun;
    private double attackSnapshot;
    private boolean initialized;

    private ResourcePool() {}

    public static ResourcePool empty() {
      return new ResourcePool();
    }

    public static ResourcePool from(CompoundTag tag) {
      ResourcePool pool = new ResourcePool();
      pool.load(tag);
      return pool;
    }

    public boolean initialized() {
      return initialized;
    }

    public double attackSnapshot() {
      return attackSnapshot;
    }

    public void setAttackSnapshot(double value) {
      this.attackSnapshot = value;
    }

    public void ensureInitializedFrom(ResourcePool template) {
      if (initialized || template == null || !template.initialized) {
        return;
      }
      copyFrom(template);
      this.initialized = true;
    }

    public void capture(ServerPlayer player, ResourceHandle handle) {
      if (handle != null) {
        this.zhenyuan = handle.getZhenyuan().orElse(zhenyuan);
        this.maxZhenyuan = handle.getMaxZhenyuan().orElse(maxZhenyuan);
        this.jingli = handle.getJingli().orElse(jingli);
        this.maxJingli = handle.getMaxJingli().orElse(maxJingli);
        this.soul = handle.getHunpo().orElse(soul);
        this.maxSoul = handle.getMaxHunpo().orElse(maxSoul);
        this.nianTou = handle.getNiantou().orElse(nianTou);
        this.maxNianTou = handle.getMaxNiantou().orElse(maxNianTou);
        this.shouYuan = handle.getShouyuan().orElse(shouYuan);
        this.daoDe = handle.getDaode().orElse(daoDe);
      }
      if (player != null) {
        this.food = player.getFoodData().getFoodLevel();
        this.hpMark = player.getHealth();
      }
      this.initialized = true;
    }

    public void apply(ServerPlayer player, ResourceHandle handle) {
      if (handle == null || player == null) {
        return;
      }
      if (!initialized) {
        capture(player, handle);
        return;
      }
      double targetZ =
          Mth.clamp(
              zhenyuan,
              0.0D,
              maxZhenyuan > 0.0D
                  ? Math.min(maxZhenyuan, handle.getMaxZhenyuan().orElse(maxZhenyuan))
                  : Double.MAX_VALUE);
      handle.setZhenyuan(targetZ);

      double targetJ =
          Mth.clamp(
              jingli,
              0.0D,
              maxJingli > 0.0D
                  ? Math.min(maxJingli, handle.getMaxJingli().orElse(maxJingli))
                  : Double.MAX_VALUE);
      handle.setJingli(targetJ);

      double currentSoul = handle.getHunpo().orElse(0.0D);
      double targetSoul =
          Mth.clamp(
              soul,
              0.0D,
              maxSoul > 0.0D ? Math.min(maxSoul, handle.getMaxHunpo().orElse(maxSoul)) : Double.MAX_VALUE);
      handle.adjustHunpo(targetSoul - currentSoul, true);

      double currentNiantou = handle.getNiantou().orElse(0.0D);
      double targetNiantou =
          Mth.clamp(
              nianTou,
              0.0D,
              maxNianTou > 0.0D
                  ? Math.min(maxNianTou, handle.getMaxNiantou().orElse(maxNianTou))
                  : Double.MAX_VALUE);
      handle.adjustNiantou(targetNiantou - currentNiantou, true);

      player
          .getFoodData()
          .setFoodLevel(Mth.clamp(food, 0, 20));
      player.setHealth((float) Mth.clamp(hpMark, 1.0D, player.getMaxHealth()));
    }

    public double receiveZhenyuan(double amount) {
      if (amount == 0.0D) {
        return 0.0D;
      }
      double before = zhenyuan;
      double limit = maxZhenyuan > 0.0D ? maxZhenyuan : Double.MAX_VALUE;
      zhenyuan = Mth.clamp(zhenyuan + amount, 0.0D, limit);
      return zhenyuan - before;
    }

    public double receiveJingli(double amount) {
      if (amount == 0.0D) {
        return 0.0D;
      }
      double before = jingli;
      double limit = maxJingli > 0.0D ? maxJingli : Double.MAX_VALUE;
      jingli = Mth.clamp(jingli + amount, 0.0D, limit);
      return jingli - before;
    }

    public double receiveSoul(double amount) {
      if (amount == 0.0D) {
        return 0.0D;
      }
      double before = soul;
      double limit = maxSoul > 0.0D ? maxSoul : Double.MAX_VALUE;
      soul = Mth.clamp(soul + amount, 0.0D, limit);
      return soul - before;
    }

    public double receiveNiantou(double amount) {
      if (amount == 0.0D) {
        return 0.0D;
      }
      double before = nianTou;
      double limit = maxNianTou > 0.0D ? maxNianTou : Double.MAX_VALUE;
      nianTou = Mth.clamp(nianTou + amount, 0.0D, limit);
      return nianTou - before;
    }

    public void load(CompoundTag tag) {
      if (tag == null) {
        return;
      }
      zhenyuan = tag.getDouble("zy");
      maxZhenyuan = tag.getDouble("zyMax");
      jingli = tag.getDouble("jl");
      maxJingli = tag.getDouble("jlMax");
      soul = tag.getDouble("soul");
      maxSoul = tag.getDouble("soulMax");
      food = tag.getInt("food");
      hpMark = tag.getDouble("hp");
      nianTou = tag.getDouble("nianTou");
      maxNianTou = tag.getDouble("nianTouMax");
      shouYuan = tag.getDouble("shouYuan");
      renQi = tag.getDouble("renQi");
      daoDe = tag.getDouble("daoDe");
      qiYun = tag.getDouble("qiYun");
      attackSnapshot = tag.getDouble("attack");
      initialized = tag.getBoolean("initialized");
    }

    public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putDouble("zy", zhenyuan);
      tag.putDouble("zyMax", maxZhenyuan);
      tag.putDouble("jl", jingli);
      tag.putDouble("jlMax", maxJingli);
      tag.putDouble("soul", soul);
      tag.putDouble("soulMax", maxSoul);
      tag.putInt("food", food);
      tag.putDouble("hp", hpMark);
      tag.putDouble("nianTou", nianTou);
      tag.putDouble("nianTouMax", maxNianTou);
      tag.putDouble("shouYuan", shouYuan);
      tag.putDouble("renQi", renQi);
      tag.putDouble("daoDe", daoDe);
      tag.putDouble("qiYun", qiYun);
      tag.putDouble("attack", attackSnapshot);
      tag.putBoolean("initialized", initialized);
      return tag;
    }

    public void copyFrom(ResourcePool other) {
      if (other == null) {
        return;
      }
      this.zhenyuan = other.zhenyuan;
      this.maxZhenyuan = other.maxZhenyuan;
      this.jingli = other.jingli;
      this.maxJingli = other.maxJingli;
      this.soul = other.soul;
      this.maxSoul = other.maxSoul;
      this.nianTou = other.nianTou;
      this.maxNianTou = other.maxNianTou;
      this.food = other.food;
      this.hpMark = other.hpMark;
      this.shouYuan = other.shouYuan;
      this.renQi = other.renQi;
      this.daoDe = other.daoDe;
      this.qiYun = other.qiYun;
      this.attackSnapshot = other.attackSnapshot;
    }
  }

  /** 两界同击窗口记录。 */
  public static final class DualStrikeWindow {
    private UUID targetId;
    private boolean yinHit;
    private boolean yangHit;
    private long expireTick;
    private double baseAttackYin;
    private double baseAttackYang;

    public Optional<UUID> targetId() {
      return Optional.ofNullable(targetId);
    }

    public void start(UUID initialTarget, long expireTick) {
      this.targetId = initialTarget;
      this.expireTick = expireTick;
      this.yinHit = false;
      this.yangHit = false;
    }

    public void clear() {
      this.targetId = null;
      this.expireTick = 0L;
      this.yinHit = false;
      this.yangHit = false;
      this.baseAttackYin = 0.0D;
      this.baseAttackYang = 0.0D;
    }

    public boolean yinHit() {
      return yinHit;
    }

    public void setYinHit(boolean value) {
      this.yinHit = value;
    }

    public boolean yangHit() {
      return yangHit;
    }

    public void setYangHit(boolean value) {
      this.yangHit = value;
    }

    public long expireTick() {
      return expireTick;
    }

    public boolean isActive(long now) {
      return expireTick > now;
    }

    public double baseAttackYin() {
      return baseAttackYin;
    }

    public double baseAttackYang() {
      return baseAttackYang;
    }

    public void setBaseAttacks(double yin, double yang) {
      this.baseAttackYin = yin;
      this.baseAttackYang = yang;
    }

    public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      if (targetId != null) {
        tag.putUUID("Target", targetId);
      }
      tag.putBoolean("YinHit", yinHit);
      tag.putBoolean("YangHit", yangHit);
      tag.putLong("Expire", expireTick);
      tag.putDouble("YinAttack", baseAttackYin);
      tag.putDouble("YangAttack", baseAttackYang);
      return tag;
    }

    public void load(CompoundTag tag) {
      if (tag == null || tag.isEmpty()) {
        clear();
        return;
      }
      if (tag.hasUUID("Target")) {
        this.targetId = tag.getUUID("Target");
      } else {
        this.targetId = null;
      }
      this.yinHit = tag.getBoolean("YinHit");
      this.yangHit = tag.getBoolean("YangHit");
      this.expireTick = Math.max(0L, tag.getLong("Expire"));
      this.baseAttackYin = tag.getDouble("YinAttack");
      this.baseAttackYang = tag.getDouble("YangAttack");
    }

    public void markYinHit() {
      this.yinHit = true;
    }

    public void markYangHit() {
      this.yangHit = true;
    }

    public boolean matchOrSetTarget(UUID candidate) {
      if (candidate == null) {
        return false;
      }
      if (this.targetId == null) {
        this.targetId = candidate;
        return true;
      }
      return this.targetId.equals(candidate);
    }
  }

  /** Serializer wiring for NeoForge attachment registration. */
  public static final class Serializer
      implements IAttachmentSerializer<CompoundTag, YinYangDualityAttachment> {

    @Override
    public YinYangDualityAttachment read(
        IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
      YinYangDualityAttachment attachment = new YinYangDualityAttachment();
      try {
        attachment.load(tag, provider);
      } catch (Throwable t) {
        LOGGER.error("[yin_yang] Failed to read attachment data, resetting", t);
        return new YinYangDualityAttachment();
      }
      return attachment;
    }

    @Override
    public CompoundTag write(
        YinYangDualityAttachment attachment, HolderLookup.Provider provider) {
      Objects.requireNonNull(attachment, "attachment");
      return attachment.save(provider);
    }
  }
}
