package net.tigereye.chestcavity.chestcavities.instance;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;
import net.tigereye.chestcavity.chestcavities.ChestCavityType;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganOnHitContext;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Consumer;

public class ChestCavityInstance implements ContainerListener {

    public static final Logger LOGGER = LogManager.getLogger();

    protected ChestCavityType type;
    public LivingEntity owner;
    public UUID compatibility_id;

    public boolean opened = false;
    public ChestCavityInventory inventory = new ChestCavityInventory();
    public Map<ResourceLocation,Float> oldOrganScores = new HashMap<>();
    protected Map<ResourceLocation,Float> organScores = new HashMap<>();
    public List<OrganOnHitContext> onHitListeners = new ArrayList<>();
    public LinkedList<Consumer<LivingEntity>> projectileQueue = new LinkedList<>();

    public int heartBleedTimer = 0;
    public int bloodPoisonTimer = 0;
    public int liverTimer = 0;
    public float metabolismRemainder = 0;
    public float lungRemainder = 0;
    public int projectileCooldown = 0;
    public int furnaceProgress = 0;
    public int photosynthesisProgress = 0;
    public EndCrystal connectedCrystal = null;

    //public FriendlyByteBuf updatePacket = null;
    public boolean updateInstantiated = false;
    public ChestCavityInstance ccBeingOpened = null;

    public ChestCavityInstance(ChestCavityType type, LivingEntity owner){
        this.type = type;
        this.owner = owner;
        this.compatibility_id = owner.getUUID();
        ChestCavityUtil.evaluateChestCavity(this);
    }

    public ChestCavityType getChestCavityType(){
        return this.type;
    }

    public Map<ResourceLocation,Float> getOrganScores() {
        return organScores;
    }

    public void setOrganScores(Map<ResourceLocation,Float> organScores) {
        this.organScores = organScores;
    }

    public float getOrganScore(ResourceLocation id) {
        return organScores.getOrDefault(id, 0f);
    }

    public float getOldOrganScore(ResourceLocation id) {
        return oldOrganScores.getOrDefault(id, 0f);
    }

    @Override
    public void containerChanged(Container sender) {
        ChestCavityUtil.clearForbiddenSlots(this);
        ChestCavityUtil.evaluateChestCavity(this);
    }

    public void fromTag(CompoundTag tag, LivingEntity owner) {
        LOGGER.debug("[Chest Cavity] Reading ChestCavityManager fromTag");
        this.owner = owner;
        if(tag.contains("ChestCavity")){
            ChestCavity.printOnDebug("Found Save Data");
            CompoundTag ccTag = tag.getCompound("ChestCavity");
            this.opened = ccTag.getBoolean("opened");
            this.heartBleedTimer = ccTag.getInt("HeartTimer");
            this.bloodPoisonTimer = ccTag.getInt("KidneyTimer");
            this.liverTimer = ccTag.getInt("LiverTimer");
            this.metabolismRemainder = ccTag.getFloat("MetabolismRemainder");
            this.lungRemainder = ccTag.getFloat("LungRemainder");
            this.furnaceProgress = ccTag.getInt("FurnaceProgress");
            this.photosynthesisProgress = ccTag.getInt("PhotosynthesisProgress");
            if(ccTag.contains("compatibility_id")){
                this.compatibility_id = ccTag.getUUID("compatibility_id");
            }
            else{
                this.compatibility_id = owner.getUUID();
            }
            try {
                inventory.removeListener(this);
            }
            catch(NullPointerException ignored){}
            if (ccTag.contains("Inventory")) {
                ListTag NbtList = ccTag.getList("Inventory", 10);
                this.inventory.readTags(NbtList);
            }
            else if(opened){
                LOGGER.warn("[Chest Cavity] "+owner.getName().getContents()+"'s Chest Cavity is mangled. It will be replaced");
                ChestCavityUtil.generateChestCavityIfOpened(this);
            }
            inventory.addListener(this);
        }
        else if(tag.contains("cardinal_components")){
            CompoundTag temp = tag.getCompound("cardinal_components");
            if(temp.contains("chestcavity:inventorycomponent")){
                temp = tag.getCompound("chestcavity:inventorycomponent");
                if(temp.contains("chestcavity")){
                    LOGGER.info("[Chest Cavity] Found "+owner.getName().getContents()+"'s old [Cardinal Components] Chest Cavity.");
                    opened = true;
                    ListTag NbtList = temp.getList("Inventory", 10);
                    try {
                        inventory.removeListener(this);
                    }
                    catch(NullPointerException ignored){}
                    inventory.readTags(NbtList);
                    inventory.addListener(this);
                }
            }
        }
        ChestCavityUtil.evaluateChestCavity(this);
    }

    public void toTag(CompoundTag tag) {
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
        ccTag.put("Inventory", this.inventory.getTags());
        tag.put("ChestCavity",ccTag);
    }

    public void clone(ChestCavityInstance other) {
        opened = other.opened;
        type = other.type;
        compatibility_id = other.compatibility_id;
        try {
            inventory.removeListener(this);
        }
        catch(NullPointerException ignored){}
        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            inventory.setItem(i, other.inventory.getItem(i));
            //inventory.forbiddenSlots = other.inventory.forbiddenSlots;
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
        ChestCavityUtil.evaluateChestCavity(this);
    }

}
