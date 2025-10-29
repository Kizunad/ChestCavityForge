package net.tigereye.chestcavity.chestcavities.instance;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;
import net.tigereye.chestcavity.chestcavities.ChestCavityType;
import net.tigereye.chestcavity.chestcavities.types.GeneratedChestCavityType;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.listeners.OrganHealContext;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageContext;
import net.tigereye.chestcavity.listeners.OrganOnFireContext;
import net.tigereye.chestcavity.listeners.OrganOnGroundContext;
import net.tigereye.chestcavity.listeners.OrganOnHitContext;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganSlowTickContext;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChestCavityInstance implements ContainerListener {

  public static final Logger LOGGER = LogManager.getLogger();

  protected ChestCavityType type;
  public LivingEntity owner;
  public UUID compatibility_id;

  public boolean opened = false;
  public ChestCavityInventory inventory = new ChestCavityInventory();
  public Map<ResourceLocation, Float> oldOrganScores = new HashMap<>();
  protected Map<ResourceLocation, Float> organScores = new HashMap<>();
  public List<OrganOnHitContext> onHitListeners = new ArrayList<>();
  public List<OrganIncomingDamageContext> onDamageListeners = new ArrayList<>();
  public List<OrganOnFireContext> onFireListeners = new ArrayList<>();
  public List<OrganHealContext> onHealListeners = new ArrayList<>();
  public List<OrganOnGroundContext> onGroundListeners = new ArrayList<>();
  public List<OrganSlowTickContext> onSlowTickListeners = new ArrayList<>();
  public List<OrganRemovalContext> onRemovedListeners = new ArrayList<>();
  public LinkedList<Consumer<LivingEntity>> projectileQueue = new LinkedList<>();
  private final Set<ResourceLocation> scoreboardUpgrades = new HashSet<>();
  private boolean randomFillersGenerated = false;
  private final List<RandomFillerEntry> preGeneratedRandomFillers = new ArrayList<>();

  public int heartBleedTimer = 0;
  public int bloodPoisonTimer = 0;
  public int liverTimer = 0;
  public float metabolismRemainder = 0;
  public float lungRemainder = 0;
  public int projectileCooldown = 0;
  public int furnaceProgress = 0;
  public int photosynthesisProgress = 0;
  public EndCrystal connectedCrystal = null;

  // public FriendlyByteBuf updatePacket = null;
  public boolean updateInstantiated = false;
  public ChestCavityInstance ccBeingOpened = null;

  public ChestCavityInstance(ChestCavityType type, LivingEntity owner) {
    this.type = type;
    this.owner = owner;
    this.compatibility_id = owner.getUUID();
    ChestCavityUtil.evaluateChestCavity(this);
    this.inventory.setInstance(this);
  }

  public ChestCavityType getChestCavityType() {
    return this.type;
  }

  public Map<ResourceLocation, Float> getOrganScores() {
    return organScores;
  }

  public void setOrganScores(Map<ResourceLocation, Float> organScores) {
    this.organScores = organScores;
  }

  public float getOrganScore(ResourceLocation id) {
    return organScores.getOrDefault(id, 0f);
  }

  public float getOldOrganScore(ResourceLocation id) {
    return oldOrganScores.getOrDefault(id, 0f);
  }

  public boolean hasScoreboardUpgrade(ResourceLocation id) {
    return scoreboardUpgrades.contains(id);
  }

  public void addScoreboardUpgrade(ResourceLocation id) {
    scoreboardUpgrades.add(id);
  }

  public Set<ResourceLocation> getScoreboardUpgrades() {
    return Collections.unmodifiableSet(scoreboardUpgrades);
  }

  public boolean areRandomFillersGenerated() {
    return randomFillersGenerated;
  }

  public void setRandomFillersGenerated(boolean generated) {
    this.randomFillersGenerated = generated;
  }

  public List<RandomFillerEntry> getPreGeneratedRandomFillers() {
    return Collections.unmodifiableList(preGeneratedRandomFillers);
  }

  public void replacePreGeneratedRandomFillers(List<RandomFillerEntry> entries) {
    preGeneratedRandomFillers.clear();
    if (entries == null || entries.isEmpty()) {
      return;
    }
    for (RandomFillerEntry entry : entries) {
      if (entry == null) {
        continue;
      }
      ItemStack stack = entry.stack();
      if (stack == null || stack.isEmpty()) {
        continue;
      }
      preGeneratedRandomFillers.add(new RandomFillerEntry(entry.slot(), stack.copy()));
    }
  }

  public void initializeRandomFillersOnSpawn() {
    if (randomFillersGenerated) {
      return;
    }
    if (type instanceof GeneratedChestCavityType generated) {
      RandomSource random = owner != null ? owner.getRandom() : RandomSource.create();
      generated.preGenerateRandomFillers(this, random);
    }
  }

  @Override
  public void containerChanged(Container sender) {
    ChestCavityUtil.clearForbiddenSlots(this);
    ChestCavityUtil.evaluateChestCavity(this);
  }

  public void fromTag(CompoundTag tag, LivingEntity owner) {
    fromTag(tag, owner, null);
  }

  public void fromTag(CompoundTag tag, LivingEntity owner, HolderLookup.Provider lookup) {
    LOGGER.debug("[Chest Cavity] Reading ChestCavityManager fromTag");
    this.owner = owner;
    boolean suppressGuzhenrenSync = GuzhenrenResourceBridge.beginPlayerDataLoad(owner);
    try {
      CompoundTag ccTag = null;
      if (tag.contains("ChestCavity")) {
        ccTag = tag.getCompound("ChestCavity");
        ChestCavity.printOnDebug("Found Save Data");
        readChestCavityTag(ccTag, lookup);
      } else if (tag.contains("cardinal_components")) {
        readLegacyCardinalComponents(tag, lookup);
      }
      ChestCavityUtil.evaluateChestCavity(this);
      if (ccTag != null) {
        LinkageManager.load(this, ccTag);
      }
    } finally {
      GuzhenrenResourceBridge.endPlayerDataLoad(owner, suppressGuzhenrenSync);
    }
  }

  /** 解析标准 ChestCavity NBT，重建背包、计时与随机填充信息。 */
  private void readChestCavityTag(CompoundTag ccTag, HolderLookup.Provider lookup) {
    applyCoreState(ccTag);
    reloadInventory(ccTag, lookup);
    restoreScoreboardUpgrades(ccTag);
    restoreRandomFillers(ccTag, lookup);
  }

  private void applyCoreState(CompoundTag ccTag) {
    this.opened = ccTag.getBoolean("opened");
    this.heartBleedTimer = ccTag.getInt("HeartTimer");
    this.bloodPoisonTimer = ccTag.getInt("KidneyTimer");
    this.liverTimer = ccTag.getInt("LiverTimer");
    this.metabolismRemainder = ccTag.getFloat("MetabolismRemainder");
    this.lungRemainder = ccTag.getFloat("LungRemainder");
    this.furnaceProgress = ccTag.getInt("FurnaceProgress");
    this.photosynthesisProgress = ccTag.getInt("PhotosynthesisProgress");
    if (ccTag.contains("compatibility_id")) {
      this.compatibility_id = ccTag.getUUID("compatibility_id");
    } else if (owner != null) {
      this.compatibility_id = owner.getUUID();
    }
  }

  private void reloadInventory(CompoundTag ccTag, HolderLookup.Provider lookup) {
    try {
      inventory.removeListener(this);
    } catch (NullPointerException ignored) {
    }
    if (ccTag.contains("Inventory")) {
      ListTag nbtList = ccTag.getList("Inventory", 10);
      inventory.readTags(nbtList, lookup);
    } else if (opened) {
      LOGGER.warn(
          "[Chest Cavity] "
              + owner.getName().getContents()
              + "'s Chest Cavity is mangled. It will be replaced");
      ChestCavityUtil.generateChestCavityIfOpened(this);
    }
    inventory.addListener(this);
  }

  private void restoreScoreboardUpgrades(CompoundTag ccTag) {
    scoreboardUpgrades.clear();
    if (!ccTag.contains("ScoreboardUpgrades", Tag.TAG_LIST)) {
      return;
    }
    ListTag upgrades = ccTag.getList("ScoreboardUpgrades", Tag.TAG_STRING);
    for (int i = 0; i < upgrades.size(); i++) {
      String rawId = upgrades.getString(i);
      try {
        scoreboardUpgrades.add(ResourceLocation.parse(rawId));
      } catch (IllegalArgumentException ignored) {
        ChestCavity.LOGGER.warn(
            "Ignored invalid scoreboard upgrade id '{}' while loading chest cavity data", rawId);
      }
    }
  }

  private void restoreRandomFillers(CompoundTag ccTag, HolderLookup.Provider lookup) {
    randomFillersGenerated = ccTag.getBoolean("RandomFillersGenerated");
    preGeneratedRandomFillers.clear();
    if (!ccTag.contains("RandomFillers", Tag.TAG_LIST)) {
      return;
    }
    HolderLookup.Provider effectiveLookup = lookup;
    if (effectiveLookup == null && owner != null) {
      effectiveLookup = owner.level().registryAccess();
    }
    if (effectiveLookup == null) {
      return;
    }
    ListTag fillers = ccTag.getList("RandomFillers", Tag.TAG_COMPOUND);
    List<RandomFillerEntry> loaded = new ArrayList<>();
    for (int i = 0; i < fillers.size(); i++) {
      CompoundTag fillerTag = fillers.getCompound(i);
      if (!fillerTag.contains("Slot", Tag.TAG_INT) || !fillerTag.contains("Stack", Tag.TAG_COMPOUND)) {
        continue;
      }
      int slot = fillerTag.getInt("Slot");
      ItemStack stack = ItemStack.parseOptional(effectiveLookup, fillerTag.getCompound("Stack"));
      if (!stack.isEmpty()) {
        loaded.add(new RandomFillerEntry(slot, stack));
      }
    }
    replacePreGeneratedRandomFillers(loaded);
    if (!loaded.isEmpty()) {
      randomFillersGenerated = true;
    }
  }

  /** 兼容旧版 Cardinal Components 存档结构。 */
  private void readLegacyCardinalComponents(CompoundTag tag, HolderLookup.Provider lookup) {
    CompoundTag cardinal = tag.getCompound("cardinal_components");
    if (!cardinal.contains("chestcavity:inventorycomponent")) {
      return;
    }
    CompoundTag legacy = tag.getCompound("chestcavity:inventorycomponent");
    if (!legacy.contains("chestcavity")) {
      return;
    }
    LOGGER.info(
        "[Chest Cavity] Found {}'s old [Cardinal Components] Chest Cavity.",
        owner.getName().getContents());
    opened = true;
    try {
      inventory.removeListener(this);
    } catch (NullPointerException ignored) {
    }
    ListTag nbtList = legacy.getList("Inventory", 10);
    inventory.readTags(nbtList, lookup);
    inventory.addListener(this);
  }

  public void toTag(CompoundTag tag) {
    toTag(tag, null);
  }

  public void toTag(CompoundTag tag, HolderLookup.Provider lookup) {
    ChestCavity.printOnDebug("Writing ChestCavityManager toTag");
    CompoundTag ccTag = new CompoundTag();
    ccTag.putBoolean("opened", this.opened);
    ccTag.putUUID("compatibility_id", this.compatibility_id);
    ccTag.putInt("HeartTimer", this.heartBleedTimer);
    ccTag.putInt("KidneyTimer", this.bloodPoisonTimer);
    ccTag.putInt("LiverTimer", this.liverTimer);
    ccTag.putFloat("MetabolismRemainder", this.metabolismRemainder);
    ccTag.putFloat("LungRemainder", this.lungRemainder);
    ccTag.putInt("FurnaceProgress", this.furnaceProgress);
    ccTag.putInt("PhotosynthesisProgress", this.photosynthesisProgress);
    ccTag.put("Inventory", this.inventory.getTags(lookup));
    if (!scoreboardUpgrades.isEmpty()) {
      ListTag upgrades = new ListTag();
      for (ResourceLocation id : scoreboardUpgrades) {
        upgrades.add(StringTag.valueOf(id.toString()));
      }
      ccTag.put("ScoreboardUpgrades", upgrades);
    }
    ccTag.putBoolean("RandomFillersGenerated", randomFillersGenerated);
    if (!preGeneratedRandomFillers.isEmpty()) {
      HolderLookup.Provider effectiveLookup = lookup;
      if (effectiveLookup == null && owner != null) {
        effectiveLookup = owner.level().registryAccess();
      }
      if (effectiveLookup != null) {
        ListTag fillers = new ListTag();
        for (RandomFillerEntry entry : preGeneratedRandomFillers) {
          CompoundTag fillerTag = new CompoundTag();
          fillerTag.putInt("Slot", entry.slot());
          fillerTag.put("Stack", entry.stack().copy().save(effectiveLookup));
          fillers.add(fillerTag);
        }
        ccTag.put("RandomFillers", fillers);
      }
    }
    LinkageManager.save(this, ccTag);
    tag.put("ChestCavity", ccTag);
  }

  public void clone(ChestCavityInstance other) {
    opened = other.opened;
    type = other.type;
    compatibility_id = other.compatibility_id;
    inventory.setInstance(this);
    try {
      inventory.removeListener(this);
    } catch (NullPointerException ignored) {
    }
    for (int i = 0; i < inventory.getContainerSize(); ++i) {
      inventory.setItem(i, other.inventory.getItem(i));
      // inventory.forbiddenSlots = other.inventory.forbiddenSlots;
    }
    inventory.readTags(other.inventory.getTags());
    inventory.addListener(this);

    heartBleedTimer = other.heartBleedTimer;
    liverTimer = other.liverTimer;
    bloodPoisonTimer = other.bloodPoisonTimer;
    metabolismRemainder = other.metabolismRemainder;
    lungRemainder = other.lungRemainder;
    furnaceProgress = other.furnaceProgress;
    connectedCrystal = other.connectedCrystal;
    scoreboardUpgrades.clear();
    scoreboardUpgrades.addAll(other.scoreboardUpgrades);
    randomFillersGenerated = other.randomFillersGenerated;
    preGeneratedRandomFillers.clear();
    for (RandomFillerEntry entry : other.preGeneratedRandomFillers) {
      preGeneratedRandomFillers.add(new RandomFillerEntry(entry.slot(), entry.stack().copy()));
    }
    ChestCavityUtil.evaluateChestCavity(this);
  }

  public void refreshType() {
    if (owner == null) {
      return;
    }
    ChestCavityType resolved = ChestCavityInstanceFactory.resolveChestCavityType(owner);
    if (this.type == resolved) {
      return;
    }
    this.type = resolved;
    this.inventory.setInstance(this);
    if (opened) {
      ChestCavityUtil.generateChestCavityIfOpened(this);
    }
    ChestCavityUtil.evaluateChestCavity(this);
  }

  /** 记录预生成随机物品的槽位与物品堆，用于掉落与打开胸腔时复用。 */
  public record RandomFillerEntry(int slot, ItemStack stack) {}
}
