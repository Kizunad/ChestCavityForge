package net.tigereye.chestcavity.registration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstanceFactory;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.state.SoulBeastState;

import java.util.Optional;

public final class CCAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ChestCavity.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ChestCavityInstance>> CHEST_CAVITY =
            ATTACHMENT_TYPES.register("chest_cavity", () -> AttachmentType.builder(CCAttachments::createInstance)
                    .serialize(new ChestCavitySerializer())
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GuScriptAttachment>> GUSCRIPT =
            ATTACHMENT_TYPES.register("guscript", () -> AttachmentType.builder(GuScriptAttachment::create)
                    .serialize(new GuScriptAttachment.Serializer())
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SoulBeastState>> SOUL_BEAST_STATE =
            ATTACHMENT_TYPES.register("soul_beast_state", () -> AttachmentType.builder(SoulBeastState::new)
                    .serialize(new SoulBeastStateSerializer())
                    .build());

    private CCAttachments() {
    }

    private static ChestCavityInstance createInstance(IAttachmentHolder holder) {
        if (!(holder instanceof LivingEntity living)) {
            throw new IllegalStateException("Chest cavity attachment can only be applied to living entities");
        }
        return ChestCavityInstanceFactory.newChestCavityInstance(living);
    }

    public static ChestCavityInstance getChestCavity(LivingEntity entity) {
        return entity.getData(CHEST_CAVITY.get());
    }

    public static Optional<ChestCavityInstance> getExistingChestCavity(LivingEntity entity) {
        return entity.getExistingData(CHEST_CAVITY.get());
    }

    public static GuScriptAttachment getGuScript(LivingEntity entity) {
        return entity.getData(GUSCRIPT.get());
    }

    public static Optional<GuScriptAttachment> getExistingGuScript(LivingEntity entity) {
        return entity.getExistingData(GUSCRIPT.get());
    }

    public static SoulBeastState getSoulBeastState(LivingEntity entity) {
        return entity.getData(SOUL_BEAST_STATE.get());
    }

    public static Optional<SoulBeastState> getExistingSoulBeastState(LivingEntity entity) {
        return entity.getExistingData(SOUL_BEAST_STATE.get());
    }

    private static class ChestCavitySerializer implements IAttachmentSerializer<CompoundTag, ChestCavityInstance> {
        @Override
        public ChestCavityInstance read(IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
            if (!(holder instanceof LivingEntity living)) {
                throw new IllegalStateException("Chest cavity attachment can only be read for living entities");
            }
            ChestCavityInstance instance = ChestCavityInstanceFactory.newChestCavityInstance(living);
            if (!tag.isEmpty()) {
                CompoundTag wrapper = new CompoundTag();
                wrapper.put("ChestCavity", tag.copy());
                instance.fromTag(wrapper, living, provider);
            }
            return instance;
        }

        @Override
        public CompoundTag write(ChestCavityInstance attachment, net.minecraft.core.HolderLookup.Provider provider) {
            CompoundTag wrapper = new CompoundTag();
            attachment.toTag(wrapper, provider);
            return wrapper.getCompound("ChestCavity");
        }
    }

    private static class SoulBeastStateSerializer implements IAttachmentSerializer<CompoundTag, SoulBeastState> {
        @Override
        public SoulBeastState read(IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
            SoulBeastState state = new SoulBeastState();
            state.load(tag);
            return state;
        }

        @Override
        public CompoundTag write(SoulBeastState attachment, net.minecraft.core.HolderLookup.Provider provider) {
            return attachment.save();
        }
    }
}
